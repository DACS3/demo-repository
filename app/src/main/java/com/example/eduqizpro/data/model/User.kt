package com.example.eduqizpro.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "USER", // "USER" hoặc "ADMIN"
    val fullName: String = ""
)
