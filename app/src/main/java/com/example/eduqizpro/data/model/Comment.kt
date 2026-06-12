package com.example.eduqizpro.data.model

import java.util.UUID

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val quizId: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val replies: List<Reply> = emptyList()
)
