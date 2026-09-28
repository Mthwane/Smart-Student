package com.example.smartstudent.domain.model

import java.time.LocalDate

enum class GoalKind { SAVINGS_GOAL, SMART_BILL }

data class SavingsGoal(
    val id: String,
    val name: String,
    val kind: GoalKind,
    val targetAmount: Double,
    val savedAmount: Double,
    val dueDate: LocalDate? = null,
    val emoji: String = "🎯",
    val description: String = ""
) {
    val progress: Float
        get() = if (targetAmount <= 0.0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val isComplete: Boolean get() = savedAmount >= targetAmount
}
