package com.example.eduqizpro.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                    Toast.makeText(context, "Chuyển đổi thất bại. Vui lòng kiểm tra lại định dạng tệp.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển đổi tài liệu", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F9FF), Color(0xFFEDEEF7))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Hero Section
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF673AB7)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Word ↔ PDF",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D3436)
                )
                
                Text(
                    text = "Nhanh chóng, an toàn và bảo mật",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Action Area
                AnimatedVisibility(
                    visible = selectedUri == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = { pickFileLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Chọn tệp tin để chuyển đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(
                    visible = selectedUri != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        FilePreviewCard(
                            fileName = fileName,
                            conversionType = conversionType,
                            onCancel = { 
                                selectedUri = null
                                fileName = ""
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val baseName = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
                                val outName = if (conversionType == "WORD_TO_PDF") "$baseName.pdf" else "$baseName.docx"
                                saveFileLauncher.launch(outName)
                            },
                            enabled = !isConverting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Đang chuyển đổi...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Chuyển đổi và Lưu ngay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        TextButton(
                            onClick = { pickFileLauncher.launch("*/*") },
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                            enabled = !isConverting
                        ) {
                            Text("Chọn tệp khác", color = Color(0xFF673AB7))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // Info Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE8EAF6),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Hỗ trợ các định dạng .docx và .pdf với cấu trúc văn bản, bảng và hình ảnh cơ bản.",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilePreviewCard(fileName: String, conversionType: String, onCancel: () -> Unit) {
    val isWordToPdf = conversionType == "WORD_TO_PDF"
    val accentColor = if (isWordToPdf) Color(0xFF2196F3) else Color(0xFFF44336)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Description, 
                            contentDescription = null, 
                            tint = accentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isWordToPdf) "Microsoft Word Document" else "PDF Document",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Change", tint = Color.LightGray)
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatBadge(text = if (isWordToPdf) "DOCX" else "PDF", color = accentColor)
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.LightGray)
                FormatBadge(text = if (isWordToPdf) "PDF" else "DOCX", color = if (isWordToPdf) Color(0xFFF44336) else Color(0xFF2196F3))
            }
        }
    }
}

@Composable
fun FormatBadge(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
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
