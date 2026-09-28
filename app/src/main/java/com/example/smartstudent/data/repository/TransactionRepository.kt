package com.example.smartstudent.data.repository

import com.example.smartstudent.data.remote.TransactionDto
import com.example.smartstudent.domain.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Stores each user's transactions at users/{uid}/transactions/{transactionId}.
 * One-time fetch/write for now; upgrade to addSnapshotListener + callbackFlow
 * for live updates in a later iteration.
 */
class TransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("transactions")

    suspend fun getAll(uid: String): List<Transaction> {
        val snapshot = collection(uid).get().await()
        return snapshot.documents.mapNotNull { it.toObject(TransactionDto::class.java)?.toDomain() }
    }

    suspend fun add(uid: String, transaction: Transaction) {
        val dto = TransactionDto.fromDomain(transaction)
        val docRef = if (transaction.id.isBlank()) collection(uid).document() else collection(uid).document(transaction.id)
        docRef.set(dto.copy(id = docRef.id)).await()
    }

    /** Overwrites an existing transaction (same id) with new field values. */
    suspend fun update(uid: String, transaction: Transaction) {
        collection(uid).document(transaction.id).set(TransactionDto.fromDomain(transaction)).await()
    }

    suspend fun delete(uid: String, transactionId: String) {
        collection(uid).document(transactionId).delete().await()
    }
}
