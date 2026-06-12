package com.example.eduqizpro.data

import com.example.eduqizpro.data.model.Comment
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.Reply
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommunityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Feed ─────────────────────────────────────────────────────────────────

    suspend fun getCommunityQuizzes(): List<Quiz> = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes")
                .whereEqualTo("visibility", "public")
                .get().await()
                .toObjects(Quiz::class.java)
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Lấy quiz chế độ "friends" của những người trong danh sách friendIds.
     * Truyền vào (myFriendIds + currentUserId) để bản thân cũng thấy quiz của mình.
     * Chia batch 10 vì Firestore whereIn giới hạn 10 phần tử.
     */
    suspend fun getFriendsQuizzes(friendIds: List<String>): List<Quiz> = withContext(Dispatchers.IO) {
        if (friendIds.isEmpty()) return@withContext emptyList()
        try {
            val result = mutableListOf<Quiz>()
            friendIds.chunked(10).forEach { batch ->
                val quizzes = db.collection("quizzes")
                    .whereIn("creatorId", batch)
                    .whereEqualTo("visibility", "friends")
                    .get().await()
                    .toObjects(Quiz::class.java)
                result.addAll(quizzes)
            }
            result
        } catch (e: Exception) { emptyList() }
    }

    // ── Visibility ───────────────────────────────────────────────────────────

    suspend fun updateQuizVisibility(quizId: String, visibility: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId)
                .update("visibility", visibility).await()
            true
        } catch (e: Exception) { false }
    }

    // ── Tải bộ đề (Dùng Xu) ───────────────────────────────────────────────────

    suspend fun downloadQuiz(quiz: Quiz): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext Pair(false, "Chưa đăng nhập")
        try {
            val userRef = db.collection("users").document(currentUserId)
            val newQuizRef = db.collection("quizzes").document()

            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                val currentCoins = userSnapshot.getLong("coins")?.toInt() ?: 300
                val currentUserName = userSnapshot.getString("fullName") ?: "Người dùng"

                if (currentCoins < 50) {
                    throw Exception("INSUFFICIENT_COINS")
                }

                transaction.update(userRef, "coins", currentCoins - 50)

                val newQuiz = quiz.copy(
                    id = newQuizRef.id,
                    creatorId = currentUserId,
                    creatorName = currentUserName,
                    visibility = "private",
                    likes = emptyList(),
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(newQuizRef, newQuiz)
            }.await()
            Pair(true, "Tải bộ đề thành công. Đã lưu vào Kho của bạn.")
        } catch (e: Exception) {
            if (e.message == "INSUFFICIENT_COINS") {
                Pair(false, "Bạn không đủ xu (cần 50 xu)")
            } else {
                Pair(false, "Lỗi khi tải bộ đề")
            }
        }
    }

    // ── Like ─────────────────────────────────────────────────────────────────

    suspend fun toggleLike(quizId: String, userId: String, isLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val quizRef = db.collection("quizzes").document(quizId)
            if (isLiked) {
                quizRef.update("likes", FieldValue.arrayRemove(userId)).await()
            } else {
                quizRef.update("likes", FieldValue.arrayUnion(userId)).await()
            }
            true
        } catch (e: Exception) { false }
    }

    // ── Comment ──────────────────────────────────────────────────────────────

    // Realtime listener — tự động cập nhật khi admin xóa/thêm comment
    fun getCommentsFlow(quizId: String): Flow<List<Comment>> = callbackFlow {
        val registration = db.collection("quizzes").document(quizId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(comments)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getComments(quizId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(Comment::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addComment(comment: Comment): Boolean = withContext(Dispatchers.IO) {
        try {
            val userSnapshot = db.collection("users").document(comment.userId).get().await()

            // Kiểm tra lại isCommentBlocked ngay tại repository trước khi ghi
            val isBlocked = userSnapshot.getBoolean("isCommentBlocked") ?: false
            val blockedUntil = userSnapshot.getLong("commentBlockedUntil") ?: 0L
            if (isBlocked && (blockedUntil == 0L || System.currentTimeMillis() < blockedUntil)) return@withContext false

            val currentFullName = userSnapshot.getString("fullName") ?: comment.userName
            val finalComment = comment.copy(userName = currentFullName)
            db.collection("quizzes").document(finalComment.quizId)
                .collection("comments").document(finalComment.id)
                .set(finalComment).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun addReplyToComment(quizId: String, commentId: String, reply: Reply): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId)
                .collection("comments").document(commentId)
                .update("replies", FieldValue.arrayUnion(reply)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteReply(quizId: String, commentId: String, reply: Reply): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId)
                .collection("comments").document(commentId)
                .update("replies", FieldValue.arrayRemove(reply)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteComment(quizId: String, commentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("quizzes").document(quizId)
                .collection("comments").document(commentId)
                .delete().await()
            true
        } catch (e: Exception) { false }
    }
}