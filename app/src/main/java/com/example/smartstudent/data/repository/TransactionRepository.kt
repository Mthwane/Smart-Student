package com.example.smartstudent.data.repository

import com.example.smartstudent.data.remote.TransactionDto
import com.example.smartstudent.domain.model.Transaction
import com.example.smartstudent.util.AppLogger
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

    suspend fun getAll(uid: String): List<Transaction> =
        collection(uid).get().await().documents.mapNotNull { doc ->
            try { doc.toObject(TransactionDto::class.java)?.toDomain() }
            catch (e: Exception) { AppLogger.e("Transactions/${doc.id}", e); null }   // one bad doc must not break the list
        }

    suspend fun add(uid: String, transaction: Transaction) {
        val dto = TransactionDto.fromDomain(transaction)
        val docRef = if (transaction.id.isBlank()) collection(uid).document() else collection(uid).document(transaction.id)
        docRef.set(dto.copy(id = docRef.id)).await()
    }

    /** Atomic bulk insert (Firestore batches max out at 500 writes, so chunk at 400). */
    suspend fun addAll(uid: String, items: List<Transaction>) {
        items.chunked(400).forEach { part ->
            val batch = firestore.batch()
            part.forEach { batch.set(collection(uid).document(it.id), TransactionDto.fromDomain(it)) }
            batch.commit().await()
        }
    }

    /** Overwrites an existing transaction (same id) with new field values. */
    suspend fun update(uid: String, transaction: Transaction) {
        collection(uid).document(transaction.id).set(TransactionDto.fromDomain(transaction)).await()
    }

    suspend fun delete(uid: String, transactionId: String) {
        collection(uid).document(transactionId).delete().await()
    }
}
