package com.example.smartstudent.domain.model

/**
 * Represents a derived spending-habit insight, e.g. "Coffee spend up 20% this week".
 */
data class HabitMetric(
    val id: String,
    val label: String,
    val category: String,
    val currentPeriodAmount: Double,
    val previousPeriodAmount: Double
) {
    val percentChange: Float
        get() = if (previousPeriodAmount == 0.0) 0f
        else (((currentPeriodAmount - previousPeriodAmount) / previousPeriodAmount) * 100).toFloat()

    val isImprovement: Boolean get() = currentPeriodAmount <= previousPeriodAmount
}
