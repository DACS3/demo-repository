package com.example.eduqizpro.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import com.example.eduqizpro.data.ChatRepository
import com.example.eduqizpro.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    receiverId: String,
    receiverName: String,
    onBack: () -> Unit
) {
    val chatRepository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val messages by chatRepository.getMessages(receiverId).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }

    var isUploading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                val success = chatRepository.sendImageMessage(context, receiverId, receiverName, uri)
                isUploading = false
                if (!success) {
                    Toast.makeText(context, "Gửi ảnh thất bại! Hãy thử ảnh khác nhẹ hơn.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(receiverName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    val isMine = message.senderId == currentUserId
                    ChatBubble(message, isMine, onDelete = {
                        scope.launch {
                            chatRepository.deleteMessage(receiverId, message.id)
                        }
                    })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Image, contentDescription = "Gửi ảnh", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Nhập tin nhắn...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val textToSend = messageText
                            messageText = ""
                            scope.launch {
                                chatRepository.sendMessage(receiverId, receiverName, textToSend)
                            }
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMine: Boolean, onDelete: () -> Unit) {
    val context = LocalContext.current
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (isMine) showMenu = true
                    }
                )
            }
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Thu hồi tin nhắn", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }

            // Hiển thị ảnh Base64 dữ liệu trần
            if (message.imageUrl.isNotEmpty()) {
                Box(modifier = Modifier.padding(bottom = 2.dp)) {
                    val bitmap = remember(message.imageUrl) {
                        try {
                            if (message.imageUrl.startsWith("data:image")) {
                                val base64String = message.imageUrl.substringAfter("base64,")
                                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Ảnh nhắn tin",
                            modifier = Modifier
                                .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Ảnh nhắn tin",
                            modifier = Modifier
                                .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Hiển thị tin nhắn văn bản văn bản
            if (message.message.isNotEmpty() && (message.message != "Đã gửi một ảnh" || message.imageUrl.isEmpty())) {
                Surface(
                    color = if (isMine) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                    contentColor = if (isMine) Color.White else Color.Black,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                ) {
                    Text(
                        text = message.message,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Hiển thị giờ
            Text(
                text = timeString,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 1.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}