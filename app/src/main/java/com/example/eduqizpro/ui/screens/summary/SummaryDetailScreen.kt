package com.example.eduqizpro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.data.model.SummaryBlock
import com.example.eduqizpro.ui.screens.summary.SummaryBlockEditor
import com.example.eduqizpro.ui.screens.summary.SummaryImageView
import com.example.eduqizpro.ui.screens.summary.buildBlocksFromLegacy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryDetailScreen(summary: Summary, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summaryRepository = remember { SummaryRepository() }

    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // currentSummary lưu dữ liệu mới nhất — cập nhật sau khi lưu
    var currentSummary by remember { mutableStateOf(summary) }

    // Khởi tạo blocks từ dữ liệu (hỗ trợ cả dữ liệu cũ không có blocks)
    val editBlocks = remember(summary) {
        val src = if (summary.blocks.isNotEmpty()) summary.blocks.toMutableList()
        else buildBlocksFromLegacy(summary.summaryText, summary.imageUrls)
        src.toMutableStateList()
    }
    var editTitle by remember(summary) { mutableStateOf(summary.title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Chỉnh sửa tóm tắt" else "Chi tiết tóm tắt") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) isEditMode = false else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = "Toggle edit"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (isEditMode) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            if (editTitle.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isSaving = true
                                val cleanBlocks = editBlocks.filter { it.type == "image" || it.content.isNotBlank() }
                                val updated = currentSummary.copy(
                                    title = editTitle.trim(),
                                    summaryText = cleanBlocks.filter { it.type == "text" }.joinToString("\n") { it.content },
                                    blocks = cleanBlocks,
                                    imageUrls = cleanBlocks.filter { it.type == "image" }.map { it.content }
                                )
                                if (summaryRepository.saveSummary(updated)) {
                                    currentSummary = updated  // cập nhật ngay — view mode sẽ hiện nội dung mới
                                    Toast.makeText(context, "Đã lưu thay đổi!", Toast.LENGTH_SHORT).show()
                                    isEditMode = false
                                } else {
                                    Toast.makeText(context, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show()
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("LƯU THAY ĐỔI")
                    }
                }
            }
        }
    ) { padding ->
        if (isEditMode) {
            // === CHẾ ĐỘ CHỈNH SỬA ===
            SummaryBlockEditor(
                title = editTitle,
                onTitleChange = { editTitle = it },
                blocks = editBlocks,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            )
        } else {
            // === CHẾ ĐỘ XEM ===
            val viewBlocks = if (currentSummary.blocks.isNotEmpty()) currentSummary.blocks
            else buildBlocksFromLegacy(currentSummary.summaryText, currentSummary.imageUrls)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = currentSummary.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ngày tạo: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(currentSummary.timestamp))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                viewBlocks.forEach { block ->
                    when (block.type) {
                        "text" -> {
                            if (block.content.isNotBlank()) {
                                Text(text = block.content, fontSize = 16.sp, lineHeight = 26.sp)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        "image" -> {
                            SummaryImageView(base64 = block.content)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}