package com.example.eduqizpro.data

import com.example.eduqizpro.data.model.Summary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SummaryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveSummary(summary: Summary): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext false
            val summaryWithCreator = summary.copy(creatorId = userId)
            db.collection("summaries").document(summaryWithCreator.id).set(summaryWithCreator).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMySummaries(): List<Summary> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            db.collection("summaries")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
                .toObjects(Summary::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSummaryById(summaryId: String): Summary? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("summaries").document(summaryId).get().await()
            snapshot.toObject(Summary::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteSummary(summaryId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("summaries").document(summaryId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
