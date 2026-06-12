package com.example.eduqizpro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
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
                var isImporting by remember { mutableStateOf(false) }
                val generator = remember { com.example.eduqizpro.utils.FlashCardGenerator(context) }
                val imagePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        isImporting = true
                        scope.launch {
                            val result = generator.generateFlashCardsFromImage(uri)
                            isImporting = false
                            if (result.startsWith("ERROR:")) {
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            } else {
                                try {
                                    val listType = object : com.google.gson.reflect.TypeToken<List<FlashCard>>() {}.type
                                    val imported: List<FlashCard> = com.google.gson.Gson().fromJson(result, listType)
                                    if (imported.isNotEmpty()) {
                                        if (cards.size == 1 && cards[0].front.isBlank() && cards[0].back.isBlank()) {
                                            cards.clear()
                                        }
                                        cards.addAll(imported)
                                        Toast.makeText(context, "Đã trích xuất thành công ${imported.size} thẻ!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Không tìm thấy thẻ nào trong ảnh", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi phân tích cú pháp: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Tạo thẻ tự động bằng AI",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3F51B5),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tải lên ảnh chụp bảng từ vựng, tài liệu hoặc câu hỏi để AI tự động chuyển thành bộ Flashcard.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang trích xuất bằng AI...")
                            } else {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHỌN ẢNH TÀI LIỆU")
                            }
                        }
                    }
                }
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
