package com.example.smartstudent.data.repository

import com.example.smartstudent.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth only stores a single displayName + email, so first/last name and
 * app-specific fields (notificationsEnabled, monthlyAllowance) live in a companion
 * Firestore doc at users/{uid}.
 */
private data class UserProfileDto(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val notificationsEnabled: Boolean = false,
    val monthlyAllowance: Double = 0.0
)

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun doc(uid: String) = firestore.collection("users").document(uid)

    suspend fun createProfile(uid: String, firstName: String, lastName: String, email: String) {
        doc(uid).set(UserProfileDto(firstName = firstName, lastName = lastName, email = email)).await()
    }

    suspend fun getProfile(uid: String, phoneNumber: String = ""): User? {
        val snapshot = doc(uid).get().await()
        val dto = snapshot.toObject(UserProfileDto::class.java) ?: return null
        return User(
            id = uid,
            firstName = dto.firstName,
            lastName = dto.lastName,
            email = dto.email,
            phoneNumber = phoneNumber,
            notificationsEnabled = dto.notificationsEnabled,
            monthlyAllowance = dto.monthlyAllowance
        )
    }

    suspend fun setNotificationsEnabled(uid: String, enabled: Boolean) {
        doc(uid).update("notificationsEnabled", enabled).await()
    }

    suspend fun setMonthlyAllowance(uid: String, amount: Double) {
        doc(uid).update("monthlyAllowance", amount).await()
    }
}
