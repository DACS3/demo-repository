package com.example.eduqizpro.ui.screens.convert

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

// ─── Trạng thái UI ───────────────────────────────────────────────
private sealed class ConvertState {
    object Idle : ConvertState()
    object Converting : ConvertState()
    object Success : ConvertState()
    data class Error(val message: String) : ConvertState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentConverterScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var conversionType by remember { mutableStateOf("WORD_TO_PDF") }
    var convertState by remember { mutableStateOf<ConvertState>(ConvertState.Idle) }

    // ─── Validate định dạng file ────────────────────────────────
    fun validateAndSetFile(uri: Uri, name: String): Boolean {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".docx") -> {
                conversionType = "WORD_TO_PDF"
                true
            }
            lower.endsWith(".pdf") -> {
                conversionType = "PDF_TO_WORD"
                true
            }
            lower.endsWith(".doc") -> {
                Toast.makeText(
                    context,
                    "Định dạng .doc (Word 97-2003) chưa được hỗ trợ.\nVui lòng chuyển sang .docx trước.",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
            else -> {
                Toast.makeText(
                    context,
                    "Định dạng không được hỗ trợ: \"$name\"\nChỉ chấp nhận tệp .docx hoặc .pdf",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }

    // ─── Chọn file ──────────────────────────────────────────────
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getDocFileName(context, uri)
            if (validateAndSetFile(uri, name)) {
                selectedUri = uri
                fileName = name
                convertState = ConvertState.Idle
            }
        }
    }

    // ─── Lưu file kết quả ───────────────────────────────────────
    val mimeType = if (conversionType == "WORD_TO_PDF") "application/pdf"
    else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { outputUri: Uri? ->
        if (outputUri == null) return@rememberLauncherForActivityResult

        scope.launch {
            convertState = ConvertState.Converting

            // DocumentConverter trả về null nếu thành công, String mô tả lỗi nếu thất bại
            val errorMsg: String? = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        val inputUri = selectedUri
                            ?: return@runCatching "Không tìm thấy tệp đầu vào."
                        if (conversionType == "WORD_TO_PDF") {
                            DocumentConverter.docxToPdf(context, inputUri, out)
                        } else {
                            DocumentConverter.pdfToDocx(context, inputUri, out)
                        }
                    } ?: "Không thể mở file đích để ghi. Thử chọn thư mục khác."
                }.getOrElse { e ->
                    "Lỗi không mong đợi: ${e.javaClass.simpleName}\n${e.message?.take(120)}"
                }
            }

            if (errorMsg == null) {
                // ─ Thành công ─ lưu lịch sử vào Firestore
                runCatching {
                    val baseName = fileName.substringBeforeLast(".")
                    val outName = if (conversionType == "WORD_TO_PDF") "$baseName.pdf" else "$baseName.docx"
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "GUEST"
                    db.collection("converted_documents").add(
                        hashMapOf(
                            "userId" to uid,
                            "fileName" to outName,
                            "type" to conversionType,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                }
                convertState = ConvertState.Success
            } else {
                convertState = ConvertState.Error(errorMsg)
            }
        }
    }

    // ─── UI ─────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Chuyển đổi tài liệu", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Lịch sử chuyển đổi",
                            tint = Color.White
                        )
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
                        listOf(Color(0xFFF8F9FF), Color(0xFFEDEEF7))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Hero icon ──
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = Color(0xFF673AB7)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Word ↔ PDF",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D3436)
                )
                Text(
                    "Nhanh chóng · An toàn · Bảo mật",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(32.dp))

                // ── Khu vực hành động ──
                when (convertState) {

                    // === IDLE / chưa chọn file ===
                    is ConvertState.Idle -> {
                        if (selectedUri == null) {
                            PickFileButton { pickFileLauncher.launch("*/*") }
                        } else {
                            FilePreviewCard(
                                fileName = fileName,
                                conversionType = conversionType,
                                onChangeFile = {
                                    selectedUri = null
                                    fileName = ""
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                            // Nút chuyển đổi
                            Button(
                                onClick = {
                                    val baseName = fileName.substringBeforeLast(".")
                                    val outName = if (conversionType == "WORD_TO_PDF") "$baseName.pdf" else "$baseName.docx"
                                    saveFileLauncher.launch(outName)
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(4.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Chuyển đổi và Lưu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { pickFileLauncher.launch("*/*") },
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Text("Chọn tệp khác", color = Color(0xFF673AB7))
                            }
                        }
                    }

                    // === ĐANG CHUYỂN ĐỔI ===
                    is ConvertState.Converting -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF673AB7),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Đang chuyển đổi...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2D3436)
                                )
                                Text(
                                    fileName,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // === THÀNH CÔNG ===
                    is ConvertState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(Color(0xFFF0FFF4)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Chuyển đổi thành công!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    "Tệp đã được lưu vào thiết bị của bạn.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF388E3C),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(Modifier.height(20.dp))
                                // Nút xem lịch sử
                                Button(
                                    onClick = onNavigateToHistory,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Xem Tệp đã chuyển đổi", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                // Nút chuyển tiếp
                                OutlinedButton(
                                    onClick = {
                                        selectedUri = null
                                        fileName = ""
                                        convertState = ConvertState.Idle
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Chuyển đổi tệp khác", color = Color(0xFF673AB7))
                                }
                            }
                        }
                    }

                    // === LỖI ===
                    is ConvertState.Error -> {
                        val msg = (convertState as ConvertState.Error).message
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(Color(0xFFFFF3F3)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    msg,
                                    fontSize = 14.sp,
                                    color = Color(0xFFB71C1C),
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        convertState = ConvertState.Idle
                                        selectedUri = null
                                        fileName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Thử lại với tệp khác", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // ── Info card dưới cùng ──
                Card(
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFE8EAF6),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF673AB7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Chỉ hỗ trợ .docx (Word 2007+) và .pdf.\nFile .doc (Word 97-2003) cần chuyển sang .docx trước.",
                            fontSize = 11.5.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Component: Nút chọn file ────────────────────────────────────
@Composable
private fun PickFileButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Icon(Icons.Default.FileUpload, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text("Chọn tệp .docx hoặc .pdf", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Component: Card xem trước file ─────────────────────────────
@Composable
private fun FilePreviewCard(
    fileName: String,
    conversionType: String,
    onChangeFile: () -> Unit
) {
    val isWordToPdf = conversionType == "WORD_TO_PDF"
    val accentColor = if (isWordToPdf) Color(0xFF2196F3) else Color(0xFFF44336)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = accentColor)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        fileName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    Text(
                        if (isWordToPdf) "Word → PDF" else "PDF → Word",
                        fontSize = 12.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = onChangeFile) {
                    Text("Đổi", color = Color.Gray, fontSize = 12.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatBadge(
                    text = if (isWordToPdf) "DOCX" else "PDF",
                    color = accentColor
                )
                Icon(Icons.Default.SwapHoriz, null, tint = Color.LightGray)
                FormatBadge(
                    text = if (isWordToPdf) "PDF" else "DOCX",
                    color = if (isWordToPdf) Color(0xFFF44336) else Color(0xFF2196F3)
                )
            }
        }
    }
}

// ─── Component: Badge định dạng ──────────────────────────────────
@Composable
fun FormatBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
        )
    }
}

// ─── Helper: lấy tên file từ Uri ─────────────────────────────────
private fun getDocFileName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) cursor.getString(idx) else null
            } else null
        } ?: uri.lastPathSegment ?: "unknown"
    } catch (e: Exception) {
        uri.lastPathSegment ?: "unknown"
    }
}
