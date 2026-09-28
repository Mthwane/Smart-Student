package com.example.smartstudent.domain.model

import java.time.LocalDateTime

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: String,
    val title: String,
    val merchant: String? = null,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: LocalDateTime,
    val note: String? = null,
    val isRecurring: Boolean = false
)
