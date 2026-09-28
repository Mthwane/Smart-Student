package com.example.smartstudent.data.remote

import com.example.smartstudent.domain.model.GoalKind
import com.example.smartstudent.domain.model.SavingsGoal
import com.google.firebase.firestore.DocumentId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SavingsGoalDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val kind: String = GoalKind.SAVINGS_GOAL.name,
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val dueDateMillis: Long? = null,
    val emoji: String = "🎯",
    val description: String = ""
) {
    fun toDomain(): SavingsGoal = SavingsGoal(
        id = id,
        name = name,
        kind = runCatching { GoalKind.valueOf(kind) }.getOrDefault(GoalKind.SAVINGS_GOAL),
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        dueDate = dueDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        },
        emoji = emoji,
        description = description
    )

    companion object {
        fun fromDomain(goal: SavingsGoal): SavingsGoalDto = SavingsGoalDto(
            id = goal.id,
            name = goal.name,
            kind = goal.kind.name,
            targetAmount = goal.targetAmount,
            savedAmount = goal.savedAmount,
            dueDateMillis = goal.dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            emoji = goal.emoji,
            description = goal.description
        )
    }
}
