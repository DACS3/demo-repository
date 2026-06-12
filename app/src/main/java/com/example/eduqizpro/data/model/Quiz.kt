package com.example.eduqizpro.data.model

import java.util.UUID

data class Quiz(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "Người dùng ẩn danh",
    val questions: List<QuizQuestion> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val visibility: String = "private", // private, public, friends
    val likes: List<String> = emptyList()
)
