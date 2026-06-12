package com.example.eduqizpro.ui.screens.createquiz

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.SaveQuizResult
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.QuizQuestion
import com.example.eduqizpro.utils.QuizGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.UUID

enum class CreateMode { INFO, SELECT_TYPE, AI, MANUAL, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizGenerator = remember { QuizGenerator(context) }
    val quizRepository = remember { QuizRepository() }

    var currentMode by remember { mutableStateOf(CreateMode.INFO) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("private") }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Chưa chọn file") }
    var userPrompt by remember { mutableStateOf("Tạo bộ đề trắc nghiệm 20 câu từ file này") }

    val manualQuestions = remember { mutableStateListOf<QuizQuestion>() }
    var currentQText by remember { mutableStateOf("") }
    val currentOptions = remember { mutableStateListOf("", "", "", "") }
    var currentCorrectIdx by remember { mutableIntStateOf(0) }
    var currentImgUri by remember { mutableStateOf<Uri?>(null) }

    var finalQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isTitleChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
        fileName = uri?.lastPathSegment ?: "document.pdf"
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        currentImgUri = uri
    }

    fun saveQuizToFirebase(questions: List<QuizQuestion>) {
        if (title.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập tên bộ đề", Toast.LENGTH_SHORT).show()
            currentMode = CreateMode.INFO
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            val quiz = Quiz(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description,
                questions = questions,
                visibility = visibility,
                timestamp = System.currentTimeMillis()
            )

            val result = quizRepository.saveQuiz(context, quiz, isNew = true)

            isLoading = false

            when (result) {
                is SaveQuizResult.Success -> {
                    Toast.makeText(context, "Đã lưu bộ đề thành công!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is SaveQuizResult.Error -> {
                    errorMessage = result.message
                    if (result.message.contains("tồn tại", ignoreCase = true)) {
                        currentMode = CreateMode.INFO
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thiết lập bộ đề") },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentMode) {
                            CreateMode.INFO -> onBack()
                            CreateMode.AI, CreateMode.MANUAL -> currentMode = CreateMode.SELECT_TYPE
                            CreateMode.PREVIEW -> currentMode = if (selectedFileUri != null) CreateMode.AI else CreateMode.MANUAL
                            else -> currentMode = CreateMode.INFO
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Thoát")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (currentMode) {
                CreateMode.INFO -> {
                    Text("Thông tin cơ bản", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tiêu đề bộ đề *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage?.contains("tồn tại") == true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả ngắn") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    errorMessage?.let {
                        Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            errorMessage = null
                            scope.launch {
                                isTitleChecking = true
                                val exists = quizRepository.isTitleExists(title.trim())
                                isTitleChecking = false
                                if (exists) {
                                    errorMessage = "Bộ đề \"${title.trim()}\" đã tồn tại. Vui lòng chọn tên khác."
                                } else {
                                    currentMode = CreateMode.SELECT_TYPE
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() && !isTitleChecking,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTitleChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("TIẾP TỤC")
                    }
                }

                CreateMode.SELECT_TYPE -> {
                    Text("Chọn phương thức tạo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    ModeCard("Tạo với AI -50 Xu", "AI soạn đề từ file của bạn", Icons.Default.AutoAwesome, Color(0xFF6200EE)) { currentMode = CreateMode.AI }
                    Spacer(modifier = Modifier.height(16.dp))
                    ModeCard("Tạo thủ công", "Tự nhập từng câu hỏi", Icons.Default.Edit, Color(0xFF4CAF50)) { currentMode = CreateMode.MANUAL }
                }

                CreateMode.AI -> {
                    Text("Tạo đề với AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phí dịch vụ: 50 Xu / lần tạo", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fileName, modifier = Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = { filePickerLauncher.launch("*/*") }) { Text("Chọn file") }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = userPrompt, onValueChange = { userPrompt = it }, label = { Text("Lệnh cho AI") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    // 1. Kiểm tra số dư xu trước khi chạy AI (chưa khấu trừ thực tế)
                                    val checkResult = quizRepository.checkCoinsForAiGeneration()
                                    if (!checkResult.first) {
                                        errorMessage = checkResult.second
                                        isLoading = false
                                        return@launch
                                    }

                                    // 2. Chạy AI sinh câu hỏi từ file
                                    val result = quizGenerator.generateQuiz(selectedFileUri, userPrompt)
                                    if (result.startsWith("[")) {
                                        // 3. Tạo đề thành công mới khấu trừ xu của người dùng
                                        val coinResult = quizRepository.deductCoinsForAiGeneration()
                                        if (!coinResult.first) {
                                            errorMessage = "Tạo đề thành công nhưng không thể khấu trừ xu: ${coinResult.second}"
                                            isLoading = false
                                            return@launch
                                        }

                                        val listType = object : TypeToken<List<QuizQuestion>>() {}.type
                                        finalQuestions = Gson().fromJson(result, listType)
                                        currentMode = CreateMode.PREVIEW
                                        Toast.makeText(context, "Đã khấu trừ 50 xu tạo đề với AI!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = result
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && userPrompt.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("BẮT ĐẦU TẠO (-50 XU)")
                    }
                    errorMessage?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
                }

                CreateMode.MANUAL -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text("Câu hỏi thứ ${manualQuestions.size + 1}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = currentQText, onValueChange = { currentQText = it }, label = { Text("Nội dung câu hỏi") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentImgUri != null) {
                                    AsyncImage(model = currentImgUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray)
                                        Text("Chèn hình ảnh đề", color = Color.Gray)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(4) { idx ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                RadioButton(selected = (currentCorrectIdx == idx), onClick = { currentCorrectIdx = idx })
                                OutlinedTextField(value = currentOptions[idx], onValueChange = { currentOptions[idx] = it }, label = { Text("Đáp án ${'A' + idx}") }, modifier = Modifier.weight(1f))
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    manualQuestions.add(QuizQuestion(currentQText, currentOptions.toList(), currentCorrectIdx, currentImgUri?.toString()))
                                    currentQText = ""
                                    currentOptions[0] = ""; currentOptions[1] = ""; currentOptions[2] = ""; currentOptions[3] = ""
                                    currentImgUri = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = currentQText.isNotBlank() && currentOptions.all { it.isNotBlank() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("THÊM CÂU TIẾP THEO") }

                            if (manualQuestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = visibility == "public", onCheckedChange = { visibility = if(it) "public" else "private" })
                                    Text("Chia sẻ lên kho cộng đồng sau khi lưu")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { saveQuizToFirebase(manualQuestions.toList()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("LƯU TẤT CẢ & VỀ TRANG CHỦ")
                                }
                            }
                        }
                    }
                }

                CreateMode.PREVIEW -> {
                    Text("Xem lại trước khi lưu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = visibility == "public", onCheckedChange = { visibility = if(it) "public" else "private" })
                        Text("Chia sẻ công khai lên cộng đồng")
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(finalQuestions) { index, q -> QuestionItem(index + 1, q) }
                    }
                    Button(
                        onClick = { saveQuizToFirebase(finalQuestions) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("XÁC NHẬN LƯU VÀO KHO")
                    }
                    errorMessage?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
    }
}

@Composable
fun QuestionItem(number: Int, question: QuizQuestion) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sửa lại thành .question chuẩn theo model của bạn
            Text(text = "Câu $number: ${question.question}", fontWeight = FontWeight.Bold)
            if (question.imageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(model = question.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.height(8.dp))
            question.options.forEachIndexed { index, option ->
                val isCorrect = index == question.correctAnswer
                Text(text = "${'A' + index}. $option", color = if (isCorrect) Color(0xFF388E3C) else Color.Black, fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun ModeCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), border = BorderStroke(1.dp, color)) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color); Text(desc, fontSize = 13.sp, color = Color.Gray, maxLines = 1) }
        }
    }
}