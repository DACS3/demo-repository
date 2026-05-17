package com.example.eduqizpro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.FlashCardRepository
import com.example.eduqizpro.data.model.FlashCard
import com.example.eduqizpro.data.model.FlashCardDeck
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFlashCardDeckScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val cards = remember { mutableStateListOf(FlashCard(front = "", back = "")) }
    
    val scope = rememberCoroutineScope()
    val repository = remember { FlashCardRepository() }
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo Flashcard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cards.any { it.front.isBlank() || it.back.isBlank() }) {
                                Toast.makeText(context, "Vui lòng nhập đầy đủ nội dung các thẻ", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isSaving = true
                            scope.launch {
                                val deck = FlashCardDeck(
                                    title = title,
                                    description = description,
                                    cards = cards.toList()
                                )
                                val success = repository.saveFlashCardDeck(deck)
                                isSaving = false
                                if (success) {
                                    Toast.makeText(context, "Đã lưu bộ Flashcard!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Lỗi khi lưu bộ Flashcard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6200EE))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF6200EE),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Lưu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { cards.add(FlashCard(front = "", back = "")) },
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề bộ thẻ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả (không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Text(
                    "Danh sách các thẻ (${cards.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(cards) { index, card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thẻ #${index + 1}", fontWeight = FontWeight.SemiBold)
                            if (cards.size > 1) {
                                IconButton(onClick = { cards.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = card.front,
                            onValueChange = { cards[index] = card.copy(front = it) },
                            label = { Text("Mặt trước (Câu hỏi/Thuật ngữ)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = card.back,
                            onValueChange = { cards[index] = card.copy(back = it) },
                            label = { Text("Mặt sau (Câu trả lời/Định nghĩa)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
