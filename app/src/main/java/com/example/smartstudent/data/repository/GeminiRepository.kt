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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends OCR'd bank statement text to Gemini (free tier, Google AI Studio) and asks it
 * to return structured JSON: parsed transactions, overspending categories, budget
 * status, and per-goal progress notes. Uses a plain HttpURLConnection rather than
 * pulling in Retrofit/OkHttp, since this is the only REST call in the app.
 */
class GeminiRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val model = "gemini-3.6-flash"

    companion object {
        private const val MAX_STATEMENT_CHARS = 12_000
    }

    suspend fun analyzeStatement(
        statementText: String,
        currentGoals: List<SavingsGoal>,
        monthlyAllowance: Double
    ): StatementAnalysis = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(statementText, currentGoals, monthlyAllowance)
        val responseText = callGemini(prompt, maxOutputTokens = 4096)
        val dto = parseModelJson(responseText)
        dto.toDomain()
    }

    /**
     * Short, friendly plain-text (not JSON) tip on how to spend less in the student's
     * top expense categories this month. Kept separate from analyzeStatement since
     * this is a much smaller, cheaper prompt with a plain-text reply.
     */
    suspend fun suggestSpendingHabitTip(
        categoryTotals: Map<String, Double>,
        monthlyIncome: Double,
        monthlyExpenses: Double
    ): String = withContext(Dispatchers.IO) {
        val topCategories = categoryTotals.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("\n") { "- ${it.key}: R${"%.2f".format(it.value)}" }

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

            Monthly income so far: R${"%.2f".format(monthlyIncome)}
            Monthly expenses so far: R${"%.2f".format(monthlyExpenses)}
            Top spending categories:
            $topCategories
        """.trimIndent()

        callGemini(prompt, maxOutputTokens = 500).trim()
    }

    private fun buildPrompt(statementText: String, goals: List<SavingsGoal>, monthlyAllowance: Double): String {
        val goalsDescription = if (goals.isEmpty()) "No savings goals set yet." else
            goals.joinToString("\n") { "- ${it.name}: R${it.savedAmount} saved of R${it.targetAmount} target" }

        // Cap how much OCR text goes into the prompt. Bank statements rarely need
        // more than this to capture every transaction line, and a much longer prompt
        // both costs more and takes noticeably longer for the model to process.
        val trimmedStatementText = statementText.take(MAX_STATEMENT_CHARS)

        return """
            You are a personal finance assistant analyzing a bank statement for a student budgeting app.
            Below is raw OCR text extracted from an uploaded bank statement image or PDF. It may contain
            OCR noise (misread characters, broken lines) — do your best to interpret it.

            Extract every individual transaction you can find, categorize each one into a short category
            label (e.g. "Groceries", "Coffee", "Transport", "Rent", "Entertainment", "Income", "Transfer"),
            and analyze overall spending.

            The student's monthly allowance/budget is: R$monthlyAllowance (0 means not set).
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
            $trimmedStatementText
            ---
        """.trimIndent()
    }

    private fun callGemini(prompt: String, maxOutputTokens: Int = 2048, thinkingLevel: String = "low"): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        require(apiKey.isNotBlank()) { "Gemini API key is missing. Add gemini.api.key to local.properties." }

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000

        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    maxOutputTokens = maxOutputTokens,
                    thinkingConfig = GeminiThinkingConfig(thinkingLevel = thinkingLevel)
                )
            )
        )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(requestBody) }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)

        if (responseCode !in 200..299) {
            error("Gemini API error ($responseCode): $responseBody")
        }

        val parsed = json.decodeFromString(GeminiResponse.serializer(), responseBody)
        val text = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return text ?: error("Gemini returned no content")
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