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

class QuizRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveQuiz(context: Context, quiz: Quiz): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext false
            
            val updatedQuestions = quiz.questions.map { question ->
                if (question.imageUrl != null && question.imageUrl.startsWith("content://")) {
                    val base64Image = compressImageToBase64(context, Uri.parse(question.imageUrl))
                    question.copy(imageUrl = base64Image)
                } else {
                    question
                }
            }
            
            val quizWithCreator = quiz.copy(creatorId = userId, questions = updatedQuestions)
            db.collection("quizzes").document(quizWithCreator.id).set(quizWithCreator).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteQuiz(quizId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("quizzes").document(quizId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMyQuizzes(): List<Quiz> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext emptyList()
            db.collection("quizzes")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
                .toObjects(Quiz::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getQuizById(quizId: String): Quiz? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("quizzes").document(quizId).get().await()
            snapshot.toObject(Quiz::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val maxSize = 400
            val width = originalBitmap.width
            val height = originalBitmap.height
            val (targetWidth, targetHeight) = if (width > height) {
                maxSize to (maxSize * height / width)
            } else {
                (maxSize * width / height) to maxSize
            }
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
