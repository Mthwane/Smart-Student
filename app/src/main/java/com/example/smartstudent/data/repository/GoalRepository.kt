package com.example.smartstudent.data.repository

import com.example.smartstudent.data.remote.SavingsGoalDto
import com.example.smartstudent.domain.model.SavingsGoal
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.smartstudent.util.AppLogger

/** Stores each user's goals at users/{uid}/goals/{goalId}. */
class GoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("goals")

    suspend fun getAll(uid: String): List<SavingsGoal> =
        collection(uid).get().await().documents.mapNotNull { doc ->
            try { doc.toObject(SavingsGoalDto::class.java)?.toDomain() }
            catch (e: Exception) { AppLogger.e("Goals/${doc.id}", e); null }   // one bad doc must not break the list
        }

    suspend fun add(uid: String, goal: SavingsGoal) {
        val dto = SavingsGoalDto.fromDomain(goal)
        val docRef = if (goal.id.isBlank()) collection(uid).document() else collection(uid).document(goal.id)
        docRef.set(dto.copy(id = docRef.id)).await()
    }

    suspend fun update(uid: String, goal: SavingsGoal) {
        collection(uid).document(goal.id).set(SavingsGoalDto.fromDomain(goal)).await()
    }

    suspend fun delete(uid: String, goalId: String) {
        collection(uid).document(goalId).delete().await()
    }
}
