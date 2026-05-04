package com.example.eduqizpro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
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
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.utils.DocumentSummarizer
import com.example.eduqizpro.data.SummaryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // === SỬA Ở ĐÂY ===
    val summarizer = remember { DocumentSummarizer(context) }
    val summaryRepository = remember { SummaryRepository() }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Chưa chọn file") }
    var summaryResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var userPrompt by remember { mutableStateOf("Hãy tóm tắt các nội dung chính của tài liệu này một cách súc tích.") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        fileName = uri?.lastPathSegment ?: "document.pdf"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tóm tắt tài liệu AI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (summaryResult.isEmpty()) {
                Text(
                    "Tải lên file PDF hoặc Word để AI tóm tắt nội dung cho bạn.",
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            fileName,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text("Chọn file")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text("Yêu cầu tóm tắt (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            scope.launch {
                                isLoading = true
                                summaryResult = summarizer.summarizeDocument(uri, userPrompt)
                                isLoading = false
                            }
                        } ?: Toast.makeText(context, "Vui lòng chọn file trước", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && selectedFileUri != null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("BẮT ĐẦU TÓM TẮT")
                    }
                }
            } else {
                // Kết quả tóm tắt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kết quả tóm tắt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(text = summaryResult, lineHeight = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { summaryResult = "" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LÀM LẠI")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val summary = Summary(
                                    title = "Tóm tắt: $fileName",
                                    originalFileName = fileName,
                                    summaryText = summaryResult
                                )
                                if (summaryRepository.saveSummary(summary)) {
                                    Toast.makeText(context, "Đã lưu tóm tắt thành công!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Lỗi khi lưu tóm tắt!", Toast.LENGTH_SHORT).show()
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("LƯU TÓM TẮT")
                        }
                    }
                }
            }
        }
    }
}