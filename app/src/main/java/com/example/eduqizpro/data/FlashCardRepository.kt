package com.example.eduqizpro.data

import com.example.eduqizpro.data.model.FlashCardDeck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FlashCardRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveFlashCardDeck(deck: FlashCardDeck): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext false
            val deckWithCreator = deck.copy(creatorId = userId)
            db.collection("flashcard_decks").document(deckWithCreator.id).set(deckWithCreator).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMyDecks(): List<FlashCardDeck> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext emptyList()
            db.collection("flashcard_decks")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
                .toObjects(FlashCardDeck::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDeckById(deckId: String): FlashCardDeck? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("flashcard_decks").document(deckId).get().await()
            snapshot.toObject(FlashCardDeck::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteDeck(deckId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("flashcard_decks").document(deckId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
