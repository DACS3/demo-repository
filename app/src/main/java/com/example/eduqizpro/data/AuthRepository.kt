package com.example.eduqizpro.data

import com.example.eduqizpro.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, pass: String): User? {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        return result.user?.let { getUserData(it.uid) }
    }

    suspend fun register(email: String, pass: String, fullName: String): Boolean {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        return result.user?.let {
            val user = User(uid = it.uid, email = email, fullName = fullName, role = "USER", coins = 300)
            db.collection("users").document(it.uid).set(user).await()
            true
        } ?: false
    }
    suspend fun isUserCommentBlocked(uid: String): Boolean {
        return try {
            // Đọc trực tiếp từ snapshot để tránh lỗi mapping tên field với Kotlin boolean getter
            val snapshot = db.collection("users").document(uid).get().await()
            val blocked = snapshot.getBoolean("isCommentBlocked") ?: false
            val blockedUntil = snapshot.getLong("commentBlockedUntil") ?: 0L
            blocked && (blockedUntil == 0L || System.currentTimeMillis() < blockedUntil)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun blockUserFor24h(uid: String): Boolean {
        return try {
            val blockedUntil = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            db.collection("users").document(uid).update(
                "isCommentBlocked", true,
                "commentBlockedUntil", blockedUntil
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserData(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    // Realtime listener cho dữ liệu user (coins, tên...) — dùng cho UserHomeScreen
    fun getUserDataFlow(uid: String): Flow<User?> = callbackFlow {
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { registration.remove() }
    }

    // Realtime listener riêng cho trạng thái chặn bình luận
    // Đọc trực tiếp từ snapshot để tránh lỗi Kotlin 'is' prefix với Firestore toObject()
    fun getUserBlockStatusFlow(uid: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val blocked = snapshot?.getBoolean("isCommentBlocked") ?: false
                val blockedUntil = snapshot?.getLong("commentBlockedUntil") ?: 0L
                trySend(Pair(blocked, blockedUntil))
            }
        awaitClose { registration.remove() }
    }

    suspend fun buyCoins(uid: String, amountCoins: Int): Boolean {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCoins = snapshot.getLong("coins") ?: 0L
                val newCoins = currentCoins + amountCoins
                transaction.update(userRef, "coins", newCoins)
            }.await()
            
            // Lưu lịch sử giao dịch mua xu để Admin thống kê doanh thu
            val txData = hashMapOf(
                "uid" to uid,
                "amount" to amountCoins,
                "timestamp" to System.currentTimeMillis(),
                "type" to "buy"
            )
            db.collection("coin_transactions").add(txData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser

    suspend fun checkAndApplyDailyReward(uid: String): Pair<Boolean, Int> {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = dateFormat.format(java.util.Date())
        val userRef = db.collection("users").document(uid)

        return try {
            var rewardGiven = false
            var finalCoins = 0

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                if (!snapshot.exists()) {
                    throw Exception("User not found")
                }
                val lastRewardDate = snapshot.getString("lastDailyRewardDate") ?: ""
                val currentCoins = snapshot.getLong("coins") ?: 0L

                if (lastRewardDate != todayStr) {
                    val newCoins = currentCoins + 50L
                    transaction.update(userRef, "coins", newCoins)
                    transaction.update(userRef, "lastDailyRewardDate", todayStr)
                    rewardGiven = true
                    finalCoins = newCoins.toInt()
                } else {
                    rewardGiven = false
                    finalCoins = currentCoins.toInt()
                }
            }.await()

            if (rewardGiven) {
                // Ghi log giao dịch xu với type = "daily_reward"
                val txData = hashMapOf(
                    "uid" to uid,
                    "amount" to 50,
                    "timestamp" to System.currentTimeMillis(),
                    "type" to "daily_reward"
                )
                db.collection("coin_transactions").add(txData).await()
            }

            Pair(rewardGiven, finalCoins)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error daily reward check: ${e.message}")
            Pair(false, 0)
        }
    }
}