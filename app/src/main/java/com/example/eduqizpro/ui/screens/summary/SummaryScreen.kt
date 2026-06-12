package com.example.eduqizpro.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.data.model.SummaryBlock
import com.example.eduqizpro.ui.screens.summary.SummaryBlockEditor
import com.example.eduqizpro.utils.DocumentSummarizer
import kotlinx.coroutines.launch

enum class SummaryMode { INPUT, EDITING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summarizer = remember { DocumentSummarizer(context) }
    val summaryRepository = remember { SummaryRepository() }

    var mode by remember { mutableStateOf(SummaryMode.INPUT) }

    // INPUT state
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Chưa chọn file") }
    var userPrompt by remember { mutableStateOf("Hãy tóm tắt các nội dung chính của tài liệu này một cách súc tích.") }
    var isLoading by remember { mutableStateOf(false) }

    // EDITING state
    var editedTitle by remember { mutableStateOf("") }
    val blocks = remember { mutableStateListOf<SummaryBlock>() }
    var isSaving by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        fileName = uri?.lastPathSegment ?: "document.pdf"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == SummaryMode.INPUT) "Tóm tắt tài liệu AI" else "Chỉnh sửa tóm tắt") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode == SummaryMode.EDITING) { mode = SummaryMode.INPUT; blocks.clear() }
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (mode == SummaryMode.EDITING) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mode = SummaryMode.INPUT; blocks.clear() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("LÀM LẠI") }

                        Button(
                            onClick = {
                                if (editedTitle.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isSaving = true
                                    val cleanBlocks = blocks.filter { it.type == "image" || it.content.isNotBlank() }
                                    val summary = Summary(
                                        title = editedTitle.trim(),
                                        originalFileName = fileName,
                                        summaryText = cleanBlocks.filter { it.type == "text" }.joinToString("\n") { it.content },
                                        blocks = cleanBlocks
                                    )
                                    if (summaryRepository.saveSummary(summary)) {
                                        Toast.makeText(context, "Đã lưu tóm tắt thành công!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text("LƯU TÓM TẮT")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (mode) {
            SummaryMode.INPUT -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Tải lên file PDF để AI tóm tắt nội dung cho bạn.", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = Color.Gray)
                            Spacer(Modifier.width(8.dp))
                            Text(fileName, modifier = Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = { filePickerLauncher.launch("*/*") }) { Text("Chọn file") }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        label = { Text("Yêu cầu tóm tắt (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            selectedFileUri?.let { uri ->
                                scope.launch {
                                    isLoading = true
                                    val result = summarizer.summarizeDocument(uri, userPrompt)
                                    isLoading = false
                                    blocks.clear()
                                    blocks.add(SummaryBlock("text", result))
                                    editedTitle = "Tóm tắt: $fileName"
                                    mode = SummaryMode.EDITING
                                }
                            } ?: Toast.makeText(context, "Vui lòng chọn file trước", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && selectedFileUri != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Đang tóm tắt...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("BẮT ĐẦU TÓM TẮT")
                        }
                    }
                }
            }

            SummaryMode.EDITING -> {
                SummaryBlockEditor(
                    title = editedTitle,
                    onTitleChange = { editedTitle = it },
                    blocks = blocks,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}