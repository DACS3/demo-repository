package com.example.eduqizpro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.eduqizpro.data.model.ChatMessage
import com.example.eduqizpro.data.model.ChatRoom
import com.example.eduqizpro.data.model.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // ─── Friend Requests ──────────────────────────────────────────────────────

    suspend fun sendFriendRequest(toId: String, toName: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        val currentUserEmail = auth.currentUser?.email ?: ""

        val userDoc = db.collection("users").document(currentUserId).get().await()
        val fromName = userDoc.getString("fullName") ?: currentUserEmail

        val request = FriendRequest(
            fromId = currentUserId,
            fromName = fromName,
            toId = toId,
            status = "pending"
        )

        return@withContext try {
            db.collection("friend_requests").document("${currentUserId}_${toId}").set(request).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPendingFriendRequests(): List<FriendRequest> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            db.collection("friend_requests")
                .whereEqualTo("toId", currentUserId)
                .whereEqualTo("status", "pending")
                .get().await()
                .toObjects(FriendRequest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        return@withContext try {
            val docId = "${request.fromId}_${currentUserId}"

            db.collection("friend_requests").document(docId)
                .update("status", "accepted").await()

            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            val currentUserName = currentUserDoc.getString("fullName") ?: "Người dùng"

            db.collection("friends").document("${currentUserId}_${request.fromId}")
                .set(mapOf(
                    "userId" to currentUserId,
                    "friendId" to request.fromId,
                    "friendName" to request.fromName,
                    "timestamp" to System.currentTimeMillis()
                )).await()

            db.collection("friends").document("${request.fromId}_${currentUserId}")
                .set(mapOf(
                    "userId" to request.fromId,
                    "friendId" to currentUserId,
                    "friendName" to currentUserName,
                    "timestamp" to System.currentTimeMillis()
                )).await()

            true
        } catch (e: Exception) { false }
    }

    suspend fun unfriend(otherUserId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        return@withContext try {
            db.collection("friends").document("${currentUserId}_${otherUserId}").delete().await()
            db.collection("friends").document("${otherUserId}_${currentUserId}").delete().await()

            db.collection("friend_requests").document("${currentUserId}_${otherUserId}").delete().await()
            db.collection("friend_requests").document("${otherUserId}_${currentUserId}").delete().await()

            true
        } catch (e: Exception) { false }
    }

    suspend fun declineFriendRequest(request: FriendRequest): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        return@withContext try {
            db.collection("friend_requests")
                .document("${request.fromId}_${currentUserId}")
                .delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getMyFriends(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            db.collection("friends")
                .whereEqualTo("userId", currentUserId)
                .get().await()
                .documents
                .mapNotNull { it.data }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getMyFriendIds(): List<String> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            db.collection("friends")
                .whereEqualTo("userId", currentUserId)
                .get().await()
                .documents
                .mapNotNull { it.getString("friendId") }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun isFriend(otherUserId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        return@withContext try {
            db.collection("friends")
                .document("${currentUserId}_${otherUserId}")
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    suspend fun sendMessage(receiverId: String, receiverName: String, messageText: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        val roomId = getRoomId(currentUserId, receiverId)

        val userDoc = db.collection("users").document(currentUserId).get().await()
        val currentUserName = userDoc.getString("fullName") ?: "Người dùng"

        val message = ChatMessage(
            id = db.collection("chats").document(roomId).collection("messages").document().id,
            senderId = currentUserId,
            receiverId = receiverId,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        val roomData = mapOf(
            "id" to roomId,
            "participantIds" to listOf(currentUserId, receiverId),
            "lastMessage" to messageText,
            "lastTimestamp" to System.currentTimeMillis(),
            "participantNames" to mapOf(currentUserId to currentUserName, receiverId to receiverName)
        )

        return@withContext try {
            db.runBatch { batch ->
                val roomRef = db.collection("chat_rooms").document(roomId)
                batch.set(roomRef, roomData, com.google.firebase.firestore.SetOptions.merge())

                val msgRef = db.collection("chat_rooms").document(roomId).collection("messages").document(message.id)
                batch.set(msgRef, message)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMessage(receiverId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        val roomId = getRoomId(currentUserId, receiverId)

        return@withContext try {
            // Delete the message document
            db.collection("chat_rooms").document(roomId).collection("messages").document(messageId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteConversation(receiverId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        val roomId = getRoomId(currentUserId, receiverId)

        return@withContext try {
            val roomRef = db.collection("chat_rooms").document(roomId)
            val roomSnapshot = roomRef.get().await()
            val currentTime = System.currentTimeMillis()

            if (roomSnapshot.exists()) {
                val room = roomSnapshot.toObject(ChatRoom::class.java)
                val otherUserId = room?.participantIds?.find { it != currentUserId } ?: receiverId
                val otherClearedAt = room?.clearedAt?.get(otherUserId) ?: 0L
                val lastTimestamp = room?.lastTimestamp ?: 0L

                val isOtherCleared = otherClearedAt > 0L && otherClearedAt >= lastTimestamp

                if (isOtherCleared) {
                    val messagesRef = roomRef.collection("messages")
                    val messagesSnapshot = messagesRef.get().await()

                    val chunks = messagesSnapshot.documents.chunked(400)
                    for (chunk in chunks) {
                        db.runBatch { batch ->
                            for (doc in chunk) {
                                batch.delete(doc.reference)
                            }
                        }.await()
                    }
                    
                    roomRef.delete().await()
                } else {
                    roomRef.update("clearedAt.$currentUserId", currentTime).await()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─── ĐĂNG ẢNH SỬ DỤNG CHUỖI BASE64 CHUẨN KHÔNG XUỐNG DÒNG ──────────────────

    private fun compressImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Giới hạn kích thước ảnh tối đa khoảng 800px để không làm tràn dung lượng Firestore 1MB
            val scaledBitmap = if (originalBitmap.width > 800 || originalBitmap.height > 800) {
                val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val width = if (aspectRatio > 1) 800 else (800 * aspectRatio).toInt()
                val height = if (aspectRatio > 1) (800 / aspectRatio).toInt() else 800
                Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            // Nén chất lượng 60% cân bằng giữa độ rõ nét và độ nhẹ chuỗi String
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            // BẮT BUỘC: Dùng Base64.NO_WRAP để dồn chuỗi văn bản trên một dòng, tránh sinh ra ký tự xuống dòng \n gây lỗi render
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    suspend fun sendImageMessage(context: Context, receiverId: String, receiverName: String, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext false
        val roomId = getRoomId(currentUserId, receiverId)

        val userDoc = db.collection("users").document(currentUserId).get().await()
        val currentUserName = userDoc.getString("fullName") ?: "Người dùng"

        return@withContext try {
            val base64String = compressImageToBase64(context, imageUri)

            if (!base64String.isNullOrBlank()) {
                val messageId = db.collection("chats").document(roomId).collection("messages").document().id

                val message = ChatMessage(
                    id = messageId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    message = "Đã gửi một ảnh",
                    imageUrl = base64String,
                    timestamp = System.currentTimeMillis()
                )

                val roomData = mapOf(
                    "id" to roomId,
                    "participantIds" to listOf(currentUserId, receiverId),
                    "lastMessage" to "Đã gửi một ảnh",
                    "lastTimestamp" to System.currentTimeMillis(),
                    "participantNames" to mapOf(currentUserId to currentUserName, receiverId to receiverName)
                )

                db.runBatch { batch ->
                    val roomRef = db.collection("chat_rooms").document(roomId)
                    batch.set(roomRef, roomData, com.google.firebase.firestore.SetOptions.merge())

                    val msgRef = db.collection("chat_rooms").document(roomId).collection("messages").document(messageId)
                    batch.set(msgRef, message)
                }.await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getMessages(receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        val roomId = getRoomId(currentUserId, receiverId)

        val roomDoc = try {
            db.collection("chat_rooms").document(roomId).get().await()
        } catch (e: Exception) { null }

        val clearedAtMap = roomDoc?.get("clearedAt") as? Map<String, Any> ?: emptyMap()
        val myClearedAt = (clearedAtMap[currentUserId] as? Number)?.toLong() ?: 0L

        val subscription = db.collection("chat_rooms").document(roomId).collection("messages")
            .whereGreaterThanOrEqualTo("timestamp", myClearedAt)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getChatRooms(): List<ChatRoom> = withContext(Dispatchers.IO) {
        val currentUserId = auth.currentUser?.uid ?: return@withContext emptyList()
        return@withContext try {
            val rooms = db.collection("chat_rooms")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .await()
                .toObjects(ChatRoom::class.java)
                
            rooms.filter { room ->
                val myClearedAt = room.clearedAt[currentUserId] ?: 0L
                myClearedAt == 0L || myClearedAt < room.lastTimestamp
            }.sortedByDescending { it.lastTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
}