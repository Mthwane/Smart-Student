package com.example.smartstudent.data.repository

import com.example.smartstudent.BuildConfig
import com.example.smartstudent.data.remote.GeminiContent
import com.example.smartstudent.data.remote.GeminiGenerationConfig
import com.example.smartstudent.data.remote.GeminiPart
import com.example.smartstudent.data.remote.GeminiRequest
import com.example.smartstudent.data.remote.GeminiResponse
import com.example.smartstudent.data.remote.GeminiThinkingConfig
import com.example.smartstudent.data.remote.StatementAnalysisDto
import com.example.smartstudent.domain.model.BudgetStatus
import com.example.smartstudent.domain.model.GoalInsight
import com.example.smartstudent.domain.model.ParsedTransaction
import com.example.smartstudent.domain.model.SavingsGoal
import com.example.smartstudent.domain.model.StatementAnalysis
import com.example.smartstudent.domain.model.TransactionType
import com.example.smartstudent.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Sends OCR'd bank statement text to Gemini (Google AI Studio) and asks it to return
 * structured JSON: parsed transactions, overspending categories, budget status, and
 * per-goal progress notes. Uses a plain HttpURLConnection rather than pulling in
 * Retrofit/OkHttp, since this is the only REST call in the app.
 *
 * Privacy: long digit runs (account / card numbers) are redacted before anything leaves
 * the device. The API key is sent in a header (not the URL) and the model name comes from
 * BuildConfig.GEMINI_MODEL (set gemini.model in local.properties).
 */
class GeminiRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    companion object {
        private const val CHUNK_CHARS = 10_000
        private const val MAX_CHUNKS = 6
        private const val ANALYSIS_TOKENS = 16_384
        private const val TIP_TOKENS = 2_048
    }

    suspend fun analyzeStatement(
        statementText: String,
        currentGoals: List<SavingsGoal>,
        monthlyAllowance: Double
    ): StatementAnalysis = withContext(Dispatchers.IO) {
        val chunks = chunk(redact(statementText))
        if (chunks.isEmpty()) throw IOException("Nothing to analyze")
        val results = chunks.map { part ->
            ensureActive()
            val raw = callGemini(buildPrompt(part, currentGoals, monthlyAllowance), ANALYSIS_TOKENS, asJson = true)
            parseModelJson(raw).toDomain()
        }
        merge(results)
    }

    /**
     * Short, friendly plain-text (not JSON) tip on how to spend less in the student's
     * top expense categories this month.
     */
    suspend fun suggestSpendingHabitTip(
        categoryTotals: Map<String, Double>,
        monthlyIncome: Double,
        monthlyExpenses: Double
    ): String = withContext(Dispatchers.IO) {
        val topCategories = categoryTotals.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("\n") { "- ${it.key}: R${fmt(it.value)}" }

        val prompt = """
            You are a friendly, sharp budgeting coach for a student finance app. Based on this
            month's spending below, write a short but substantive tip — AT LEAST 4 lines/sentences,
            plain text, no markdown, no headers, no bullet points. Structure it like this, in your
            own natural voice (don't literally label the parts):
            1. Name the specific pattern you noticed, with real numbers from the data below.
            2. Briefly explain why it matters (e.g. relative to their income, or vs other categories).
            3. Give ONE concrete, specific, actionable change they could make (not generic advice
               like "spend less" — name an actual swap, cap, or habit).
            4. Estimate the rough monthly saving if they made that change, and end on an encouraging note.
            Be specific and logical, not vague or generic. Do not lecture or moralize — keep the tone
            warm and casual, like a smart friend who's good with money, not a lecture from a textbook.

            Monthly income so far: R${fmt(monthlyIncome)}
            Monthly expenses so far: R${fmt(monthlyExpenses)}
            Top spending categories:
            $topCategories
        """.trimIndent()

        callGemini(prompt, TIP_TOKENS, asJson = false).trim()
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)

    /** Strips long digit runs (account / card numbers) before anything leaves the device. */
    private fun redact(text: String): String = text
        .replace(Regex("\\b(?:\\d[ -]?){12,19}\\b"), "[card]")
        .replace(Regex("\\b\\d{9,}\\b"), "[acct]")

    /** Splits on line boundaries so long statements are analysed in pieces instead of being silently cut. */
    private fun chunk(text: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (line in text.lines()) {
            if (sb.isNotEmpty() && sb.length + line.length + 1 > CHUNK_CHARS) {
                out += sb.toString(); sb.clear()
            }
            sb.appendLine(line.take(CHUNK_CHARS))
        }
        if (sb.isNotBlank()) out += sb.toString()
        if (out.size > MAX_CHUNKS) AppLogger.w("Gemini", "Statement truncated to $MAX_CHUNKS chunks")
        return out.take(MAX_CHUNKS)
    }

    private fun merge(parts: List<StatementAnalysis>): StatementAnalysis {
        val budget = when {
            parts.any { it.budgetStatus == BudgetStatus.OVER_BUDGET } -> BudgetStatus.OVER_BUDGET
            else -> parts.map { it.budgetStatus }.lastOrNull { it != BudgetStatus.UNKNOWN } ?: BudgetStatus.UNKNOWN
        }
        return StatementAnalysis(
            transactions = parts.flatMap { it.transactions },
            overspendingCategories = parts.flatMap { it.overspendingCategories }.distinct(),
            budgetStatus = budget,
            goalInsights = parts.first().goalInsights,
            summary = parts.first().summary
        )
    }

    private fun buildPrompt(statementText: String, goals: List<SavingsGoal>, monthlyAllowance: Double): String {
        val goalsDescription = if (goals.isEmpty()) "No savings goals set yet." else
            goals.joinToString("\n") { "- ${it.name}: R${fmt(it.savedAmount)} saved of R${fmt(it.targetAmount)} target" }

        return """
            You are a personal finance assistant analyzing a bank statement for a student budgeting app.
            Below is raw OCR text extracted from an uploaded bank statement image or PDF. It may contain
            OCR noise (misread characters, broken lines) — do your best to interpret it.

            Extract every individual transaction you can find, categorize each one into a short category
            label (e.g. "Groceries", "Coffee", "Transport", "Rent", "Entertainment", "Income", "Transfer"),
            and analyze overall spending. "amount" must be a POSITIVE number (absolute value, no currency
            symbol, no thousands separators); the direction is carried by "type".

            The student's monthly allowance/budget is: R${fmt(monthlyAllowance)} (0 means not set).
            The student's current savings goals are:
            $goalsDescription

            Respond with ONLY raw JSON (no markdown code fences, no commentary before or after) matching
            exactly this shape:
            {
              "transactions": [
                {"date": "YYYY-MM-DD", "description": "string", "amount": number, "type": "INCOME" or "EXPENSE", "category": "string"}
              ],
              "overspendingCategories": ["string"],
              "budgetStatus": "under_budget" or "on_track" or "over_budget" or "unknown",
              "goalInsights": [
                {"goalName": "string", "status": "on_track" or "behind" or "achieved", "note": "short natural language note"}
              ],
              "summary": "2-3 sentence plain-English summary of spending patterns and any concerns"
            }

            Bank statement OCR text:
            ---
            $statementText
            ---
        """.trimIndent()
    }

    private fun callGemini(prompt: String, maxOutputTokens: Int, asJson: Boolean, thinkingLevel: String = "low"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key is missing. Add gemini.api.key to local.properties.")

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)   // header, not the URL
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 90_000

            val requestBody = json.encodeToString(
                GeminiRequest.serializer(),
                GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(
                        maxOutputTokens = maxOutputTokens,
                        responseMimeType = if (asJson) "application/json" else null,
                        thinkingConfig = GeminiThinkingConfig(thinkingLevel = thinkingLevel)
                    )
                )
            )
            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                // Logged for debugging, never surfaced to the UI.
                AppLogger.e("Gemini", "HTTP $responseCode: ${responseBody.take(500)}")
                throw IOException("Gemini HTTP $responseCode")
            }

            val candidate = json.decodeFromString(GeminiResponse.serializer(), responseBody).candidates.firstOrNull()
                ?: throw IOException("Gemini returned no candidates")
            // Thinking models can emit "thought" parts first; only join the real answer text.
            val text = candidate.content?.parts.orEmpty()
                .filter { it.thought != true }
                .mapNotNull { it.text }
                .joinToString("")
            if (candidate.finishReason == "MAX_TOKENS") throw IOException("Gemini response was truncated")
            if (text.isBlank()) throw IOException("Gemini returned no content")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun parseModelJson(rawText: String): StatementAnalysisDto {
        // Models sometimes wrap JSON in ```json fences despite instructions — strip defensively.
        val cleaned = rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return json.decodeFromString(StatementAnalysisDto.serializer(), cleaned)
    }

    private fun StatementAnalysisDto.toDomain(): StatementAnalysis = StatementAnalysis(
        transactions = transactions.map {
            ParsedTransaction(
                date = it.date,
                description = it.description,
                amount = it.amount,
                type = runCatching { TransactionType.valueOf(it.type.uppercase()) }.getOrDefault(TransactionType.EXPENSE),
                category = it.category
            )
        },
        overspendingCategories = overspendingCategories,
        budgetStatus = when (budgetStatus.lowercase()) {
            "under_budget" -> BudgetStatus.UNDER_BUDGET
            "on_track" -> BudgetStatus.ON_TRACK
            "over_budget" -> BudgetStatus.OVER_BUDGET
            else -> BudgetStatus.UNKNOWN
        },
        goalInsights = goalInsights.map { GoalInsight(it.goalName, it.status, it.note) },
        summary = summary
    )
}
