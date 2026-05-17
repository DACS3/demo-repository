package com.example.eduqizpro.data.model

import java.util.UUID

data class FlashCard(
    val id: String = UUID.randomUUID().toString(),
    val front: String = "",
    val back: String = ""
)

data class FlashCardDeck(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val cards: List<FlashCard> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
