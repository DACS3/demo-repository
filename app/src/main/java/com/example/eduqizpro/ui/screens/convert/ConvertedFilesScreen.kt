package com.example.eduqizpro.ui.screens.convert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConvertedFile(
    val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val type: String = "", // "WORD_TO_PDF" or "PDF_TO_WORD"
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertedFilesScreen(onBack: () -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()
    var fileList by remember { mutableStateOf<List<ConvertedFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val snapshot = db.collection("converted_documents")
                    .whereEqualTo("userId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                fileList = snapshot.documents.map { doc ->
                    ConvertedFile(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        fileName = doc.getString("fileName") ?: "",
                        type = doc.getString("type") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            } catch (e: Exception) {
                // Fallback nếu chưa tạo chỉ mục (Index) hoặc lỗi khác
                try {
                    val snapshot = db.collection("converted_documents")
                        .whereEqualTo("userId", currentUserId)
                        .get()
                        .await()
                    
                    val list = snapshot.documents.map { doc ->
                        ConvertedFile(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            fileName = doc.getString("fileName") ?: "",
                            type = doc.getString("type") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                    fileList = list.sortedByDescending { it.timestamp }
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử chuyển đổi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF673AB7))
            }
        } else if (fileList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Bạn chưa thực hiện chuyển đổi tệp nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(fileList) { file ->
                    ConvertedFileItem(
                        file = file,
                        onDelete = {
                            db.collection("converted_documents").document(file.id).delete()
                            fileList = fileList.filter { it.id != file.id }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConvertedFileItem(file: ConvertedFile, onDelete: () -> Unit) {
    val isWordToPdf = file.type == "WORD_TO_PDF"
    val accentColor = if (isWordToPdf) Color(0xFF2196F3) else Color(0xFFF44336)
    val dateStr = remember(file.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(file.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = file.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Text(
                        text = if (isWordToPdf) "Word ➔ PDF • $dateStr" else "PDF ➔ Word • $dateStr",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.LightGray
                )
            }
        }
    }
}
