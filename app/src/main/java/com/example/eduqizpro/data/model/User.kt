package com.example.eduqizpro.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "USER", // "USER" hoặc "ADMIN"
    val fullName: String = "",
    val coins: Int = 300,
    val isCommentBlocked: Boolean = false, // Thêm trường kiểm tra trạng thái khóa bình luận
    val commentBlockedUntil: Long = 0L, // Thêm thời gian khóa bình luận đến (mili giây)
    val createdAt: Long = System.currentTimeMillis(), // Thêm thời gian tạo tài khoản (mili giây)
    val lastDailyRewardDate: String = "" // Lưu ngày nhận xu đăng nhập gần nhất (định dạng yyyy-MM-dd)
)