package com.example.eduqizpro.ui.screens.createquiz

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.eduqizpro.data.SaveQuizResult
import com.example.eduqizpro.data.model.Comment
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.QuizQuestion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    quiz: Quiz,
    onBack: () -> Unit,
    onNavigateToComments: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val editableQuestions = remember { mutableStateListOf<QuizQuestion>().apply { addAll(quiz.questions) } }

    var isSolvingMode by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showResultScreen by remember { mutableStateOf(false) }
    var showSolutionScreen by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    var isLoading by remember { mutableStateOf(false) }

    var score by remember { mutableStateOf(0f) }
    var correctCount by remember { mutableStateOf(0) }

    // Dialog states
    var showEditDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var tempQuestionText by remember { mutableStateOf("") }
    val tempOptions = remember { mutableStateListOf("", "", "", "") }
    var tempCorrectIdx by remember { mutableIntStateOf(0) }
    var tempImgUri by remember { mutableStateOf<Uri?>(null) }

    // Comment states
    var hasCompleted by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { tempImgUri = it }

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            hasCompleted = quizRepository.hasCompletedQuiz(currentUserId, quiz.id)
        }
    }

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
                    if (!isSolvingMode && !showResultScreen && !showSolutionScreen) {
                        IconButton(onClick = onNavigateToComments) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments")
                        }
                        if (quiz.creatorId == currentUserId) {
                            IconButton(onClick = { isEditMode = !isEditMode }) {
                                Icon(if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit, contentDescription = "Toggle Edit")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            // Chế độ xem thông tin
            if (!isSolvingMode && !isEditMode && !showResultScreen && !showSolutionScreen) {
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
                        if (hasCompleted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✅ Đã hoàn thành", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { isSolvingMode = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("BẮT ĐẦU GIẢI")
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { 
                    showSolutionScreen = true 
                    if (currentUserId != null && !hasCompleted) {
                        scope.launch {
                            quizRepository.markQuizAsCompleted(currentUserId, quiz.id)
                            hasCompleted = true
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("XEM ĐÁP ÁN")
                }
            }

            // Chế độ chỉnh sửa
            else if (isEditMode) {
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
                            val result = quizRepository.saveQuiz(context, updatedQuiz, isNew = false)

                            isLoading = false
                            when (result) {
                                is SaveQuizResult.Success -> {
                                    Toast.makeText(context, "Đã cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                    isEditMode = false
                                }
                                is SaveQuizResult.Error -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("LƯU THAY ĐỔI")
                }
            }

            // Chế độ giải đề
            else if (isSolvingMode && !showResultScreen) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(editableQuestions) { index, question ->
                        QuestionDisplayItem(
                            number = index + 1,
                            question = question,
                            isSolving = true,
                            selectedOption = userAnswers[index],
                            onOptionSelected = { userAnswers[index] = it },
                            showCorrect = false
                        )
                    }
                }

                Button(
                    onClick = { showSubmitDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("NỘP BÀI")
                }
            }

            // Màn hình kết quả (sau khi nộp bài)
            else if (showResultScreen && !showSolutionScreen) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kết quả bài làm", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "${"%.1f".format(score)} / 10",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$correctCount/${editableQuestions.size} câu đúng",
                                fontSize = 20.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Nút xem lời giải
                    OutlinedButton(
                        onClick = { showSolutionScreen = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("XEM LỜI GIẢI")
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showResultScreen = false
                            showSolutionScreen = false
                            isSolvingMode = false
                            userAnswers.clear()
                            score = 0f
                            correctCount = 0
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("QUAY LẠI")
                    }
                }
            }

            // Màn hình xem lời giải (từng câu)
            else if (showSolutionScreen) {
                Text(
                    text = if (showResultScreen) "Lời giải bài làm" else "Xem đáp án",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showSolutionScreen = false
                            if (!showResultScreen) {
                                // Vào từ nút XEM ĐÁP ÁN ở trang info
                                showResultScreen = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray.copy(alpha = 0.5f),
                            contentColor = Color.DarkGray
                        )
                    ) {
                        Text("QUAY LẠI")
                    }

                    Button(
                        onClick = { showChatSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("HỎI TRỢ LÝ AI")
                    }
                }
            }
        }
    }

    // ==================== DIALOG NỘP BÀI ====================
    if (showSubmitDialog) {
        val unanswered = editableQuestions.size - userAnswers.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Xác nhận nộp bài") },
            text = {
                if (unanswered > 0) {
                    Text("Bạn còn $unanswered câu chưa khoanh đáp án. Bạn có muốn nộp bài không?")
                } else {
                    Text("Bạn có chắc chắn muốn nộp bài không?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitDialog = false
                    val total = editableQuestions.size
                    if (total > 0) {
                        correctCount = userAnswers.count { (index, selected) ->
                            selected == editableQuestions[index].correctAnswer
                        }
                        score = (correctCount.toFloat() / total) * 10
                    }
                    showResultScreen = true
                    if (currentUserId != null && !hasCompleted) {
                        scope.launch {
                            quizRepository.markQuizAsCompleted(currentUserId, quiz.id)
                            hasCompleted = true
                        }
                    }
                }) { Text("NỘP BÀI", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("TIẾP TỤC LÀM") }
            }
        )
    }

    // ==================== DIALOG CHỈNH SỬA ====================
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
                                label = { Text("Đáp án ${'A' + i}") },
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

    if (showChatSheet) {
        QuizExplainChatSheet(
            quiz = quiz,
            onDismiss = { showChatSheet = false }
        )
    }
}

// ==================== CÁC COMPOSABLE HỖ TRỢ ====================
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
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EditQuestionListItem(
    number: Int,
    question: QuizQuestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    border = if (isSelected && !showCorrect) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
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