package com.example.eduqizpro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.model.Quiz
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    onBack: () -> Unit,
    onQuizClick: (Quiz) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepository() }
    var quizList by remember { mutableStateOf<List<Quiz>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // State cho Dialog xác nhận xóa
    var quizToDelete by remember { mutableStateOf<Quiz?>(null) }
    
    // State cho Dialog sửa thông tin
    var quizToEdit by remember { mutableStateOf<Quiz?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    fun refreshList() {
        scope.launch {
            isLoading = true
            quizList = quizRepository.getMyQuizzes()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trắc nghiệm của tôi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (quizList.isEmpty()) {
                Text("Bạn chưa có bộ đề nào", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quizList) { quiz ->
                        QuizCard(
                            quiz = quiz, 
                            onClick = { onQuizClick(quiz) },
                            onDelete = { quizToDelete = quiz },
                            onEdit = {
                                quizToEdit = quiz
                                editTitle = quiz.title
                                editDescription = quiz.description
                            }
                        )
                    }
                }
            }
        }
        
        // Dialog xác nhận xóa
        if (quizToDelete != null) {
            AlertDialog(
                onDismissRequest = { quizToDelete = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc chắn muốn xóa bộ đề '${quizToDelete?.title}' không? Hành động này không thể hoàn tác.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val success = quizRepository.deleteQuiz(quizToDelete!!.id)
                                if (success) {
                                    Toast.makeText(context, "Đã xóa bộ đề", Toast.LENGTH_SHORT).show()
                                    refreshList()
                                }
                                quizToDelete = null
                            }
                        }
                    ) { Text("XÓA", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { quizToDelete = null }) { Text("HỦY") }
                }
            )
        }

        // Dialog sửa thông tin bộ đề
        if (quizToEdit != null) {
            AlertDialog(
                onDismissRequest = { quizToEdit = null },
                title = { Text("Sửa thông tin bộ đề") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Tiêu đề") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Mô tả") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val updatedQuiz = quizToEdit!!.copy(
                                    title = editTitle,
                                    description = editDescription
                                )
                                val success = quizRepository.saveQuiz(context, updatedQuiz)
                                if (success) {
                                    Toast.makeText(context, "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show()
                                    refreshList()
                                }
                                quizToEdit = null
                            }
                        },
                        enabled = editTitle.isNotBlank()
                    ) { Text("LƯU") }
                },
                dismissButton = {
                    TextButton(onClick = { quizToEdit = null }) { Text("HỦY") }
                }
            )
        }
    }
}

@Composable
fun QuizCard(
    quiz: Quiz, 
    onClick: () -> Unit, 
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(quiz.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quiz.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = quiz.description, fontSize = 14.sp, color = Color.Gray, maxLines = 1)
                Text(text = "Ngày tạo: $date", fontSize = 12.sp, color = Color.LightGray)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }
    }
}
