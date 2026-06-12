package com.example.eduqizpro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.eduqizpro.data.model.Quiz
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed class SaveQuizResult {
    data object Success : SaveQuizResult()
    data class Error(val message: String) : SaveQuizResult()
}

class QuizRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Lưu / Tạo ────────────────────────────────────────────────────────────

    suspend fun saveQuiz(context: Context, quiz: Quiz, isNew: Boolean = false): SaveQuizResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid
                ?: return@withContext SaveQuizResult.Error("Bạn chưa đăng nhập")

            if (isTitleExists(quiz.title, if (isNew) null else quiz.id)) {
                return@withContext SaveQuizResult.Error("Tên bộ đề đã tồn tại. Vui lòng chọn tên khác.")
            }

            val userSnapshot = db.collection("users").document(userId).get().await()
            val fullName = userSnapshot.getString("fullName") ?: "Người dùng"

            val updatedQuestions = quiz.questions.map { question ->
                if (question.imageUrl != null && question.imageUrl.startsWith("content://")) {
                    val base64Image = compressImageToBase64(context, Uri.parse(question.imageUrl))
                    question.copy(imageUrl = base64Image)
                } else {
                    question
                }
            }

            val quizWithCreator = quiz.copy(
                creatorId = userId,
                creatorName = fullName,
                questions = updatedQuestions,
                timestamp = System.currentTimeMillis() // Sử dụng chính xác trường timestamp gốc của bạn
            )

            db.collection("quizzes").document(quizWithCreator.id).set(quizWithCreator).await()
            SaveQuizResult.Success
        } catch (e: Exception) {
            SaveQuizResult.Error("Lỗi khi lưu bộ đề: ${e.localizedMessage ?: "Vui lòng thử lại"}")
        }
    }

    // ── Kiểm tra trùng tên ────────────────────────────────────────────────────

    suspend fun isTitleExists(title: String, excludeQuizId: String? = null): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext false
            val snapshots = db.collection("quizzes")
                .whereEqualTo("creatorId", userId)
                .whereEqualTo("title", title.trim())
                .get().await()

            if (excludeQuizId != null) {
                snapshots.documents.any { it.id != excludeQuizId }
            } else {
                !snapshots.isEmpty
            }
        } catch (e: Exception) { false }
    }

    // ── Kiểm tra số dư xu trước khi tạo đề với AI ─────────────────────────────
    suspend fun checkCoinsForAiGeneration(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext Pair(false, "Chưa đăng nhập")
        try {
            val userRef = db.collection("users").document(currentUserId)
            val userSnapshot = userRef.get().await()
            val currentCoins = userSnapshot.getLong("coins")?.toInt() ?: 300
            if (currentCoins < 50) {
                Pair(false, "Bạn không đủ xu (cần 50 xu để dùng AI)")
            } else {
                Pair(true, "Đủ xu")
            }
        } catch (e: Exception) {
            Pair(false, "Lỗi kiểm tra số dư xu: ${e.localizedMessage}")
        }
    }

    // ── Khấu trừ xu khi tạo đề với AI ─────────────────────────────────────────
    suspend fun deductCoinsForAiGeneration(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext Pair(false, "Chưa đăng nhập")
        try {
            val userRef = db.collection("users").document(currentUserId)
            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                val currentCoins = userSnapshot.getLong("coins")?.toInt() ?: 300

                if (currentCoins < 50) {
                    throw Exception("INSUFFICIENT_COINS")
                }
                transaction.update(userRef, "coins", currentCoins - 50)
            }.await()
            Pair(true, "Trừ xu thành công")
        } catch (e: Exception) {
            if (e.message == "INSUFFICIENT_COINS") {
                Pair(false, "Bạn không đủ xu (cần 50 xu để dùng AI)")
            } else {
                Pair(false, "Lỗi trừ xu: ${e.localizedMessage}")
            }
        }
    }

    // ── Xóa ──────────────────────────────────────────────────────────────────

    suspend fun deleteQuiz(quizId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val quizRef = db.collection("quizzes").document(quizId)
            val snapshot = quizRef.get().await()
            val visibility = snapshot.getString("visibility") ?: "private"

            if (visibility == "public" || visibility == "friends") {
                val oldCreatorId = snapshot.getString("creatorId") ?: ""
                quizRef.update("creatorId", "deleted_$oldCreatorId").await()
            } else {
                quizRef.delete().await()
            }
            true
        } catch (e: Exception) { false }
    }

    // ── Lấy danh sách ────────────────────────────────────────────────────────

    suspend fun getMyQuizzes(): List<Quiz> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            db.collection("quizzes")
                .whereEqualTo("creatorId", userId)
                .get().await()
                .toObjects(Quiz::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getMyDeletedPublicQuizzes(): List<Quiz> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext emptyList()
        try {
            db.collection("quizzes")
                .whereEqualTo("creatorId", "deleted_$userId")
                .get().await()
                .toObjects(Quiz::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getQuizById(quizId: String): Quiz? = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId).get().await().toObject(Quiz::class.java)
        } catch (e: Exception) { null }
    }

    // ── Lịch sử hoàn thành ───────────────────────────────────────────────────

    suspend fun markQuizAsCompleted(userId: String, quizId: String) = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "quizId" to quizId,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("completed_quizzes")
                .document("${userId}_${quizId}")
                .set(data).await()
        } catch (e: Exception) { }
    }

    suspend fun hasCompletedQuiz(userId: String, quizId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("completed_quizzes")
                .document("${userId}_${quizId}")
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun compressImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }
}