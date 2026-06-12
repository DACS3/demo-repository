package com.example.eduqizpro.data.model

import java.util.UUID

data class Reply(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
