package com.example.smartstudent.data.repository

import com.example.smartstudent.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth only stores a single displayName + email, so first/last name and
 * app-specific fields (notificationsEnabled, monthlyAllowance) live in a companion
 * Firestore doc at users/{uid}.
 */
data class UserProfileDto(
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

    private fun UserProfileDto.toUser(uid: String) = User(
        id = uid, firstName = firstName, lastName = lastName, email = email,
        notificationsEnabled = notificationsEnabled, monthlyAllowance = monthlyAllowance
    )

    /**
     * Returns the existing profile untouched, or creates one if it's missing.
     * Never overwrites, so re-logging in (e.g. with Google) can't wipe the allowance.
     */
    suspend fun getOrCreateProfile(uid: String, firstName: String, lastName: String, email: String): User {
        val snapshot = doc(uid).get().await()
        val existing = if (snapshot.exists()) snapshot.toObject(UserProfileDto::class.java) else null
        val dto = existing ?: UserProfileDto(firstName = firstName, lastName = lastName, email = email).also {
            doc(uid).set(it).await()
        }
        return dto.toUser(uid)
    }

    suspend fun getProfile(uid: String): User? {
        val snapshot = doc(uid).get().await()
        return snapshot.toObject(UserProfileDto::class.java)?.toUser(uid)
    }

    // merge-sets (not update()) so they still work if the profile doc doesn't exist yet
    suspend fun setNotificationsEnabled(uid: String, enabled: Boolean) {
        doc(uid).set(mapOf("notificationsEnabled" to enabled), SetOptions.merge()).await()
    }

    suspend fun setMonthlyAllowance(uid: String, amount: Double) {
        doc(uid).set(mapOf("monthlyAllowance" to amount), SetOptions.merge()).await()
    }
}
