package com.example.smartstudent.domain.model

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String = "",
    val notificationsEnabled: Boolean = false,
    val monthlyAllowance: Double = 0.0
) {
    val fullName: String get() = "$firstName $lastName"
}
