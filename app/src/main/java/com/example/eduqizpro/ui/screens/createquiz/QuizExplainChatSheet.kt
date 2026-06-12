package com.example.eduqizpro.ui.screens.createquiz

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.utils.QuizExplainAssistant
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizExplainChatSheet(
    quiz: Quiz,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assistant = remember { QuizExplainAssistant(context) }

    var userQuestionText by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<ChatMessage>().apply {
        add(ChatMessage("Xin chào! Tôi là trợ lý học tập AI. Bạn có câu hỏi nào xoay quanh bộ đề này không?", false))
    }}
    var isSendingMessage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trợ lý Học tập AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF3700B3)
                    )
                    Text(
                        text = "Giải đáp thắc mắc xoay quanh bộ đề",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Chat Messages List
            val lazyListState = rememberLazyListState()

            LaunchedEffect(chatMessages.size) {
                if (chatMessages.isNotEmpty()) {
                    lazyListState.animateScrollToItem(chatMessages.size - 1)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(message = msg)
                }
                if (isSendingMessage) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Trợ lý đang soạn câu trả lời...",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Field area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userQuestionText,
                    onValueChange = { userQuestionText = it },
                    placeholder = { Text("Hỏi về bộ đề này...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (userQuestionText.isNotBlank() && !isSendingMessage) {
                            val question = userQuestionText.trim()
                            userQuestionText = ""
                            chatMessages.add(ChatMessage(question, true))
                            isSendingMessage = true

                            scope.launch {
                                val reply = assistant.askAssistant(quiz, question)
                                chatMessages.add(ChatMessage(reply, false))
                                isSendingMessage = false
                            }
                        }
                    },
                    enabled = userQuestionText.isNotBlank() && !isSendingMessage,
                    modifier = Modifier
                        .background(
                            color = if (userQuestionText.isNotBlank() && !isSendingMessage)
                                MaterialTheme.colorScheme.primary
                            else Color.LightGray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    MaterialTheme.colorScheme.primary
                else Color(0xFFF1F1F1)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (message.isUser) Color.White else Color.Black,
                fontSize = 14.sp
            )
        }
    }
}
