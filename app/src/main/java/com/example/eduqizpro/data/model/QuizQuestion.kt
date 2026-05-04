package com.example.eduqizpro.data.model

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctAnswer: Int = 0,
    val imageUrl: String? = null
)
