package com.example.smartstudent.data.remote

import kotlinx.serialization.Serializable

// --- Gemini REST API request/response shapes (generateContent endpoint) ---

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiGenerationConfig(
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
    val thinkingConfig: GeminiThinkingConfig? = null
)

@Serializable
data class GeminiThinkingConfig(
    val thinkingLevel: String = "minimal"
)

@Serializable
data class GeminiContent(val parts: List<GeminiPart> = emptyList())

@Serializable
data class GeminiPart(val text: String? = null, val thought: Boolean? = null)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null, val finishReason: String? = null)

// --- The JSON schema we instruct the model to reply with (parsed out of GeminiResponse's text) ---

@Serializable
data class StatementAnalysisDto(
    val transactions: List<ParsedTransactionDto> = emptyList(),
    val overspendingCategories: List<String> = emptyList(),
    val budgetStatus: String = "unknown",
    val goalInsights: List<GoalInsightDto> = emptyList(),
    val summary: String = ""
)

@Serializable
data class ParsedTransactionDto(
    val date: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "Uncategorized"
)

@Serializable
data class GoalInsightDto(
    val goalName: String = "",
    val status: String = "",
    val note: String = ""
)
