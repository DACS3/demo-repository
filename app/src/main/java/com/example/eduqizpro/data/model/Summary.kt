package com.example.eduqizpro.data.model

import java.util.UUID

data class Summary(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val originalFileName: String = "",
    val summaryText: String = "",
    val creatorId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
