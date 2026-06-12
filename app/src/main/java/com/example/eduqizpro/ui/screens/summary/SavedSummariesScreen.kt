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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.Summary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSummariesScreen(
    onBack: () -> Unit,
    onSummaryClick: (Summary) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summaryRepository = remember { SummaryRepository() }
    var summaryList by remember { mutableStateOf<List<Summary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // State cho việc xóa
    var summaryToDelete by remember { mutableStateOf<Summary?>(null) }
    
    // State cho việc sửa tên
    var summaryToEdit by remember { mutableStateOf<Summary?>(null) }
    var newTitle by remember { mutableStateOf("") }

    fun refreshList() {
        scope.launch {
            isLoading = true
            summaryList = summaryRepository.getMySummaries()
            isLoading = false
        }
    }

    // Reload danh sách mỗi khi màn hình được resume (kể cả khi navigate back)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshList()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tóm tắt đã lưu") },
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
            } else if (summaryList.isEmpty()) {
                Text(
                    "Bạn chưa có bản tóm tắt nào",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(summaryList) { summary ->
                        SummaryCard(
                            summary = summary,
                            onClick = { onSummaryClick(summary) },
                            onDelete = { summaryToDelete = summary },
                            onEdit = {
                                summaryToEdit = summary
                                newTitle = summary.title
                            }
                        )
                    }
                }
            }
        }

        // Dialog xác nhận xóa
        if (summaryToDelete != null) {
            AlertDialog(
                onDismissRequest = { summaryToDelete = null },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc muốn xóa bản tóm tắt '${summaryToDelete?.title}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val targetId = summaryToDelete!!.id
                                val success = summaryRepository.deleteSummary(targetId)
                                if (success) {
                                    Toast.makeText(context, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                                    // Cập nhật in-memory lập tức để xóa tóm tắt khỏi giao diện
                                    summaryList = summaryList.filter { it.id != targetId }
                                }
                                summaryToDelete = null
                            }
                        }
                    ) { Text("XÓA", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { summaryToDelete = null }) { Text("HỦY") }
                }
            )
        }

        // Dialog sửa tên tóm tắt
        if (summaryToEdit != null) {
            AlertDialog(
                onDismissRequest = { summaryToEdit = null },
                title = { Text("Đổi tên tóm tắt") },
                text = {
                    Column {
                        Text("Nhập tên mới cho bản tóm tắt này:", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Tiêu đề") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                scope.launch {
                                    val updatedSummary = summaryToEdit!!.copy(title = newTitle)
                                    val success = summaryRepository.saveSummary(updatedSummary)
                                    if (success) {
                                        Toast.makeText(context, "Đã cập nhật tên thành công", Toast.LENGTH_SHORT).show()
                                        // Cập nhật in-memory phần tử bị chỉnh sửa lập tức để tránh nhấp nháy
                                        summaryList = summaryList.map { if (it.id == updatedSummary.id) updatedSummary else it }
                                    }
                                    summaryToEdit = null
                                }
                            }
                        }
                    ) { Text("LƯU") }
                },
                dismissButton = {
                    TextButton(onClick = { summaryToEdit = null }) { Text("HỦY") }
                }
            )
        }
    }
}

@Composable
fun SummaryCard(
    summary: Summary, 
    onClick: () -> Unit, 
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(summary.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Tệp: ${summary.originalFileName}", fontSize = 12.sp, color = Color.Gray)
                Text(text = date, fontSize = 11.sp, color = Color.LightGray)
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
