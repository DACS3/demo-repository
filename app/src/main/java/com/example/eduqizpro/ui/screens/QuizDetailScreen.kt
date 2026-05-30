package com.example.eduqizpro.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.QuizQuestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(quiz: Quiz, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepository() }

    // State quản lý danh sách câu hỏi local để chỉnh sửa
    val editableQuestions = remember { mutableStateListOf<QuizQuestion>().apply { addAll(quiz.questions) } }
    var isSolvingMode by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var isLoading by remember { mutableStateOf(false) }

    // State cho chế độ giải bài (Quiz Mode)
    var currentSolvingIndex by remember { mutableIntStateOf(0) }
    val quizIndices = remember { mutableStateListOf<Int>() }

    // State cho Dialog Thêm/Sửa
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) } // -1 là thêm mới
    var tempQuestionText by remember { mutableStateOf("") }
    val tempOptions = remember { mutableStateListOf("", "", "", "") }
    var tempCorrectIdx by remember { mutableIntStateOf(0) }
    var tempImgUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { tempImgUri = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {
                    if (!isSolvingMode && !showResults) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit, contentDescription = "Toggle Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (!isSolvingMode && !showResults && !isEditMode) {
                // Chế độ xem ban đầu
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = quiz.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(text = quiz.description, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "${editableQuestions.size} câu hỏi", fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { 
                        // Khởi tạo thứ tự câu hỏi ngẫu nhiên
                        quizIndices.clear()
                        quizIndices.addAll(editableQuestions.indices)
                        quizIndices.shuffle()
                        currentSolvingIndex = 0
                        userAnswers.clear()
                        isSolvingMode = true 
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("BẮT ĐẦU GIẢI")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResults = true }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("XEM ĐÁP ÁN")
                }
            } else if (isEditMode) {
                // CHẾ ĐỘ CHỈNH SỬA
                Text("Chỉnh sửa bộ đề", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(editableQuestions) { index, q ->
                        EditQuestionListItem(
                            number = index + 1,
                            question = q,
                            onEdit = {
                                editingIndex = index
                                tempQuestionText = q.question
                                tempOptions.clear()
                                tempOptions.addAll(q.options)
                                tempCorrectIdx = q.correctAnswer
                                tempImgUri = null 
                                showEditDialog = true
                            },
                            onDelete = { editableQuestions.removeAt(index) }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                editingIndex = -1
                                tempQuestionText = ""
                                tempOptions.clear()
                                tempOptions.addAll(listOf("", "", "", ""))
                                tempCorrectIdx = 0
                                tempImgUri = null
                                showEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) { 
                            Icon(Icons.Default.Add, null)
                            Text("THÊM CÂU HỎI MỚI") 
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val updatedQuiz = quiz.copy(questions = editableQuestions.toList())
                            if (quizRepository.saveQuiz(context, updatedQuiz)) {
                                Toast.makeText(context, "Đã cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                isEditMode = false
                            } else {
                                Toast.makeText(context, "Lỗi khi lưu bộ đề", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("LƯU THAY ĐỔI")
                }
            } else if (isSolvingMode && !showResults) {
                // CHẾ ĐỘ GIẢI (QUIZ MODE) - HIỆN TỪNG CÂU VÀ TRỘN ĐỀ
                if (quizIndices.isNotEmpty()) {
                    val actualIndex = quizIndices[currentSolvingIndex]
                    val question = editableQuestions[actualIndex]

                    Column(modifier = Modifier.weight(1f)) {
                        // Hiển thị tiến độ
                        LinearProgressIndicator(
                            progress = { (currentSolvingIndex + 1).toFloat() / quizIndices.size },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "Câu ${currentSolvingIndex + 1} / ${quizIndices.size}", 
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(Modifier.height(16.dp))

                        QuestionDisplayItem(
                            number = currentSolvingIndex + 1,
                            question = question,
                            isSolving = true,
                            selectedOption = userAnswers[actualIndex],
                            onOptionSelected = { userAnswers[actualIndex] = it },
                            showCorrect = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { if (currentSolvingIndex > 0) currentSolvingIndex-- },
                            enabled = currentSolvingIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, null)
                            Text("CÂU TRƯỚC")
                        }

                        if (currentSolvingIndex < quizIndices.size - 1) {
                            Button(onClick = { currentSolvingIndex++ }) {
                                Text("TIẾP THEO")
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        } else {
                            Button(
                                onClick = { showResults = true },
                                enabled = userAnswers.size == editableQuestions.size,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text("NỘP BÀI")
                            }
                        }
                    }
                }
            } else if (showResults) {
                // CHẾ ĐỘ XEM ĐÁP ÁN (HIỆN TẤT CẢ ĐỂ REVIEW)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(editableQuestions) { index, question ->
                        QuestionDisplayItem(
                            number = index + 1,
                            question = question,
                            isSolving = false,
                            selectedOption = userAnswers[index],
                            onOptionSelected = {},
                            showCorrect = true
                        )
                    }
                }
                Button(
                    onClick = { 
                        showResults = false
                        isSolvingMode = false
                        userAnswers.clear()
                    }, 
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) { 
                    Text("QUAY LẠI")
                }
            }
        }
    }

    // Dialog Sửa/Thêm câu hỏi
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingIndex == -1) "Thêm câu hỏi" else "Sửa câu hỏi") },
            text = {
                LazyColumn {
                    item {
                        OutlinedTextField(
                            value = tempQuestionText, 
                            onValueChange = { tempQuestionText = it }, 
                            label = { Text("Câu hỏi") }, 
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .clickable { imagePicker.launch("image/*") }, 
                            contentAlignment = Alignment.Center
                        ) {
                           if (tempImgUri != null) {
                               AsyncImage(model = tempImgUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                           } else if (editingIndex != -1 && editableQuestions[editingIndex].imageUrl != null) {
                               Base64ImageDisplay(base64String = editableQuestions[editingIndex].imageUrl!!)
                           } else {
                               Text("Bấm để chọn ảnh", color = Color.Gray)
                           }
                        }
                    }
                    items(4) { i ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = tempCorrectIdx == i, onClick = { tempCorrectIdx = i })
                            OutlinedTextField(
                                value = tempOptions[i], 
                                onValueChange = { tempOptions[i] = it }, 
                                label = { Text("Đáp án ${'A'+i}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalImageUrl = tempImgUri?.toString() ?: if (editingIndex != -1) editableQuestions[editingIndex].imageUrl else null
                    val newQ = QuizQuestion(tempQuestionText, tempOptions.toList(), tempCorrectIdx, finalImageUrl)
                    if (editingIndex == -1) editableQuestions.add(newQ)
                    else editableQuestions[editingIndex] = newQ
                    showEditDialog = false
                }) { Text("XÁC NHẬN") }
            },
            dismissButton = { 
                TextButton(onClick = { showEditDialog = false }) { Text("HỦY") } 
            }
        )
    }
}

@Composable
fun Base64ImageDisplay(base64String: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64String) {
        try {
            val pureBase64 = if (base64String.contains(",")) base64String.split(",")[1] else base64String
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EditQuestionListItem(number: Int, question: QuizQuestion, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$number.", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(question.question, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color.Blue) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun QuestionDisplayItem(
    number: Int, 
    question: QuizQuestion, 
    isSolving: Boolean, 
    selectedOption: Int?, 
    onOptionSelected: (Int) -> Unit, 
    showCorrect: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
        shape = RoundedCornerShape(12.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White), 
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Câu $number: ${question.question}", fontWeight = FontWeight.Bold)
            
            if (question.imageUrl != null) {
                Spacer(Modifier.height(8.dp))
                if (question.imageUrl!!.startsWith("data:image")) {
                    Base64ImageDisplay(base64String = question.imageUrl!!)
                } else {
                    AsyncImage(
                        model = question.imageUrl, 
                        contentDescription = null, 
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), 
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            question.options.forEachIndexed { index, option ->
                val isCorrect = index == question.correctAnswer
                val isSelected = selectedOption == index
                val bgColor = when {
                    showCorrect && isCorrect -> Color(0xFFC8E6C9)
                    showCorrect && isSelected && !isCorrect -> Color(0xFFFFCDD2)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = isSolving) { onOptionSelected(index) }, 
                    shape = RoundedCornerShape(8.dp), 
                    color = bgColor,
                    border = if (isSelected && !showCorrect) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Text(
                        text = "${'A' + index}. $option", 
                        modifier = Modifier.padding(12.dp), 
                        color = if (showCorrect && isCorrect) Color(0xFF2E7D32) else Color.Black,
                        fontWeight = if (isSelected || (showCorrect && isCorrect)) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
