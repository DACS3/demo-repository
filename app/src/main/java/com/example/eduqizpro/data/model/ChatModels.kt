package com.example.eduqizpro.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatRoom(
    val id: String = "", // Ví dụ: uid1_uid2
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimestamp: Long = 0,
    val participantNames: Map<String, String> = emptyMap(), // Lưu tên để hiển thị nhanh
    val clearedAt: Map<String, Long> = emptyMap() // Lưu timestamp khi người dùng xóa chat
)

data class FriendRequest(
    val fromId: String = "",
    val fromName: String = "",
    val toId: String = "",
    val status: String = "pending", // pending, accepted
    val timestamp: Long = System.currentTimeMillis()
)
