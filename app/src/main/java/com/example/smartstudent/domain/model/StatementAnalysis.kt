package com.example.smartstudent.domain.model

/** A single transaction as parsed by Gemini from OCR'd bank statement text. */
data class ParsedTransaction(
    val date: String,          // as reported by the model, e.g. "2026-07-14"
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val category: String
)

enum class BudgetStatus { UNDER_BUDGET, ON_TRACK, OVER_BUDGET, UNKNOWN }

data class GoalInsight(val goalName: String, val status: String, val note: String)

/** Full result of scanning + analyzing a statement. */
data class StatementAnalysis(
    val transactions: List<ParsedTransaction>,
    val overspendingCategories: List<String>,
    val budgetStatus: BudgetStatus,
    val goalInsights: List<GoalInsight>,
    val summary: String
)
