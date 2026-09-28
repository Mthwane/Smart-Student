package com.example.smartstudent.data.remote

import com.example.smartstudent.domain.model.Transaction
import com.example.smartstudent.domain.model.TransactionType
import com.google.firebase.firestore.DocumentId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Plain-data mirror of [Transaction] safe for Firestore (de)serialization.
 * Firestore's Kotlin mapper needs a no-arg constructor and no java.time types,
 * so dates are stored as epoch millis and converted back on the way out.
 */
data class TransactionDto(
    @DocumentId val id: String = "",
    val title: String = "",
    val merchant: String? = null,
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = "",
    val dateMillis: Long = 0L,
    val note: String? = null,
    val isRecurring: Boolean = false
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        title = title,
        merchant = merchant,
        amount = amount,
        type = TransactionType.valueOf(type),
        category = category,
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault()),
        note = note,
        isRecurring = isRecurring
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionDto = TransactionDto(
            id = transaction.id,
            title = transaction.title,
            merchant = transaction.merchant,
            amount = transaction.amount,
            type = transaction.type.name,
            category = transaction.category,
            dateMillis = transaction.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            note = transaction.note,
            isRecurring = transaction.isRecurring
        )
    }
}
