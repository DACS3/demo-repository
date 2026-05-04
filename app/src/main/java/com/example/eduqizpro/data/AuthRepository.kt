package com.example.eduqizpro.data

import com.example.eduqizpro.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, pass: String): User? {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        return result.user?.let { getUserData(it.uid) }
    }

    suspend fun register(email: String, pass: String, fullName: String, role: String): Boolean {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        return result.user?.let {
            val user = User(uid = it.uid, email = email, fullName = fullName, role = role)
            db.collection("users").document(it.uid).set(user).await()
            true
        } ?: false
    }

    suspend fun getUserData(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    fun logout() = auth.signOut()
    
    fun getCurrentUser() = auth.currentUser
}
