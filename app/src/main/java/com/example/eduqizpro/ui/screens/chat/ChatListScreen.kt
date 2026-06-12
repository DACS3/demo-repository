package com.example.eduqizpro.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.ChatRepository
import com.example.eduqizpro.data.model.ChatRoom
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onChatClick: (String, String) -> Unit
) {
    val chatRepository = remember { ChatRepository() }
    var chatRooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        chatRooms = chatRepository.getChatRooms()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (chatRooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có tin nhắn nào", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(chatRooms) { room ->
                    val otherId = room.participantIds.find { it != currentUserId } ?: ""
                    val otherName = room.participantNames[otherId] ?: "Người dùng"
                    var showMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        ListItem(
                            headlineContent = { Text(otherName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(room.lastMessage, maxLines = 1) },
                            trailingContent = {
                                Text(
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(room.lastTimestamp)),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            leadingContent = {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onChatClick(otherId, otherName) },
                                    onLongPress = { showMenu = true }
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Xóa cuộc trò chuyện", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        chatRooms = chatRooms.filter { it.id != room.id }
                                        chatRepository.deleteConversation(otherId)
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}
