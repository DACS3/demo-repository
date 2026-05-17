package com.example.eduqizpro.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.utils.DocumentConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentConverterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var isConverting by remember { mutableStateOf(false) }
    var conversionType by remember { mutableStateOf("WORD_TO_PDF") }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            fileName = getFileName(context, uri)
            conversionType = if (fileName.lowercase().endsWith(".pdf")) {
                "PDF_TO_WORD"
            } else {
                "WORD_TO_PDF"
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (conversionType == "WORD_TO_PDF") "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri: Uri? ->
        uri?.let { outputUri ->
            scope.launch {
                isConverting = true
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                            selectedUri?.let { inputUri ->
                                if (conversionType == "WORD_TO_PDF") {
                                    DocumentConverter.docxToPdf(context, inputUri, outputStream)
                                } else {
                                    DocumentConverter.pdfToDocx(context, inputUri, outputStream)
                                }
                            } ?: false
                        } ?: false
                    } catch (t: Throwable) {
                        // Catch Throwable to prevent app from closing on fatal errors like NoClassDefFoundError
                        t.printStackTrace()
                        false
                    }
                }
                isConverting = false
                if (success) {
                    Toast.makeText(context, "Chuyển đổi thành công!", Toast.LENGTH_LONG).show()
                    selectedUri = null
                    fileName = ""
                } else {
                    Toast.makeText(context, "Chuyển đổi thất bại. Có thể tệp tin quá phức tạp hoặc bị lỗi hệ thống.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển đổi tài liệu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF6200EE)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Word ↔ PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Chuyển đổi và giữ định dạng cơ bản",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (selectedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val label = if (conversionType == "WORD_TO_PDF") "Chuyển sang PDF" else "Chuyển sang Word"
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = Color(0xFF6200EE)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = { pickFileLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selectedUri == null) "Chọn tệp tin" else "Chọn tệp khác")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val baseName = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
                    val outName = if (conversionType == "WORD_TO_PDF") "$baseName.pdf" else "$baseName.docx"
                    saveFileLauncher.launch(outName)
                },
                enabled = selectedUri != null && !isConverting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang xử lý...")
                } else {
                    Text("Chuyển đổi và Lưu")
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = cursor.getString(index) ?: "Unknown"
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return name
}
