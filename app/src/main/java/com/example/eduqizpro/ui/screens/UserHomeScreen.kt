package com.example.eduqizpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.AuthRepository
import com.example.eduqizpro.data.SePayRepository
import com.example.eduqizpro.data.PaymentStatus
import com.example.eduqizpro.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
// ════════════════════════════════════════════════════════════════════════════
// CẤU HÌNH TÀI KHOẢN NGÂN HÀNG NHẬN TIỀN THẬT (DÙNG CHO SEPAY)
// BANK_ID: Dùng mã ngân hàng từ VietQR (Ví dụ: MB, VCB, TCB, ACB, VPB, VBA...)
// ════════════════════════════════════════════════════════════════════════════
const val BANK_ID = "MB"                           // Ngân hàng MBBank làm mặc định
const val ACCOUNT_NO = "0935927996"      // ← Số tài khoản ngân hàng của bạn
const val ACCOUNT_NAME = "NGO DINH GIANG"  // Họ tên chủ tài khoản (VIẾT HOA KHÔNG DẤU)
data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    onLogout: () -> Unit,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizList: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToSavedSummaries: () -> Unit,
    onNavigateToCommunityFeed: () -> Unit,
    onNavigateToChatList: () -> Unit,
    onNavigateToCreateFlashcard: () -> Unit,
    onNavigateToFlashcardList: () -> Unit,
    onNavigateToDocumentConverter: () -> Unit,
    onNavigateToConvertedFiles: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val sePayRepository = remember { SePayRepository() }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // Realtime Flow — tự cập nhật khi admin đổi xu, không cần restart app
    val userData by remember(currentUserId) {
        if (currentUserId != null) authRepository.getUserDataFlow(currentUserId)
        else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    // Tự động kiểm tra và tặng xu đăng nhập hàng ngày khi vào trang chủ
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val result = authRepository.checkAndApplyDailyReward(currentUserId)
            if (result.first) {
                Toast.makeText(context, "🎁 Bạn đã nhận 50 xu đăng nhập hôm nay!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // State mua xu
    var showBuyCoinsDialog by rememberSaveable { mutableStateOf(false) }

    val utilities = listOf(
        FeatureItem("Tạo trắc nghiệm", Icons.Default.Create, Color(0xFF6200EE), "create_quiz"),
        FeatureItem("Tóm tắt PDF", Icons.Default.Book, Color(0xFFF44336), "summary"),
        FeatureItem("Flash card", Icons.Default.Style, Color(0xFFFF9800), "create_flashcard"),
        FeatureItem("Word sang PDF", Icons.Default.CompareArrows, Color(0xFF2196F3), "document_converter")
    )

    val management = listOf(
        FeatureItem("Trắc nghiệm lưu", Icons.Default.Folder, Color(0xFF4CAF50), "quiz_list"),
        FeatureItem("Tóm tắt đã lưu", Icons.Default.Bookmark, Color(0xFF009688), "saved_summaries"),
        FeatureItem("Flash-card lưu", Icons.Default.Star, Color(0xFFFFC107), "flashcard_list"),
        FeatureItem("Tệp đã chuyển đổi", Icons.Default.FolderZip, Color(0xFF9C27B0), "converted_files")
    )

    Scaffold(
        // Không dùng topBar
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                item { UserHeader(userData) { showBuyCoinsDialog = true } }

            item { SectionTitle("Tiện ích") }
            item {
                FeatureGrid(utilities) { route ->
                    when (route) {
                        "create_quiz" -> onNavigateToCreateQuiz()
                        "summary" -> onNavigateToSummary()
                        "create_flashcard" -> onNavigateToCreateFlashcard()
                        "document_converter" -> onNavigateToDocumentConverter()
                    }
                }
            }

            item { SectionTitle("Quản lý") }
            item {
                FeatureGrid(management) { route ->
                    when (route) {
                        "quiz_list" -> onNavigateToQuizList()
                        "saved_summaries" -> onNavigateToSavedSummaries()
                        "flashcard_list" -> onNavigateToFlashcardList()
                        "converted_files" -> onNavigateToConvertedFiles()
                    }
                }
            }

            item { SectionTitle("Khám phá") }
            item {
                FeatureGrid(listOf(
                    FeatureItem("Kho cộng đồng", Icons.Default.Explore, Color(0xFF3F51B5), "community_feed"),
                    FeatureItem("Nhắn tin", Icons.Default.Mail, Color(0xFFE91E63), "chat_list")
                )) { route ->
                    when (route) {
                        "community_feed" -> onNavigateToCommunityFeed()
                        "chat_list" -> onNavigateToChatList()
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Nút Đăng xuất CỐ ĐỊNH ở góc trên bên phải, NỔI lên trên LazyColumn khi cuộn
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = Color.White
            )
        }

        // ═══════════════════════════════════════════════════
        // DIALOG NẠP XU — TỰ ĐỘNG XÁC MINH QUA SEPAY.VN
        // ═══════════════════════════════════════════════════
        if (showBuyCoinsDialog) {
            var coinsInput by rememberSaveable { mutableStateOf("100") }
            val coinsAmount = coinsInput.toIntOrNull() ?: 0
            val moneyAmount = (coinsAmount * 100).toLong() // 1 xu = 100 VNĐ

            var paymentStatus by remember { mutableStateOf<PaymentStatus>(PaymentStatus.Waiting) }
            var isPolling by rememberSaveable { mutableStateOf(false) }   // đang chờ SePay
            var creditDone by rememberSaveable { mutableStateOf(false) }  // tránh cộng xu 2 lần

            // Mã giao dịch duy nhất cho SePay (NX + 6 ký tự UID + 4 số random)
            val txCode = rememberSaveable {
                val uid6 = (currentUserId ?: "GUEST").take(6).uppercase()
                val rand = (1000..9999).random()
                "NX$uid6$rand"
            }

            // Kiểm tra xem cấu hình tài khoản ngân hàng của bạn có hợp lệ hay chưa
            val isAccountConfigured = ACCOUNT_NO.isNotBlank() && ACCOUNT_NO != "DIEN_SO_TAI_KHOAN_VAO_DAY"
            val isSePayConfigured = com.example.eduqizpro.data.SEPAY_API_KEY != "DIEN_API_KEY_SEPAY_VAO_DAY" &&
                    com.example.eduqizpro.data.SEPAY_API_KEY.isNotBlank()

            // Debug state hiển thị trên màn hình
            var debugText by remember { mutableStateOf("Chưa bắt đầu") }

            // ── Bắt đầu polling SePay khi isPolling = true ──────────────────
            LaunchedEffect(isPolling, txCode, moneyAmount) {
                if (!isPolling || !isSePayConfigured) return@LaunchedEffect
                android.util.Log.d("SePay", "🚀 Bắt đầu polling | txCode=$txCode | amount=$moneyAmount")
                debugText = "Đang gọi SePay API..."
                sePayRepository.pollForPayment(txCode, moneyAmount).collect { status ->
                    paymentStatus = status
                    when (status) {
                        is PaymentStatus.Waiting -> debugText = "⏳ Đang chờ giao dịch..."
                        is PaymentStatus.Error -> debugText = "❌ Lỗi: ${status.message}"
                        is PaymentStatus.Detected -> debugText = "✅ Phát hiện giao dịch! id=${status.transactionId}"
                    }
                    if (status is PaymentStatus.Detected && !creditDone && currentUserId != null) {
                        creditDone = true
                        isPolling = false
                        // Chạy coroutine cộng xu trên CoroutineScope của màn hình (scope) thay vì LaunchedEffect
                        // để tránh việc coroutine bị hủy giữa chừng khi isPolling thay đổi.
                        scope.launch {
                            val success = authRepository.buyCoins(currentUserId, coinsAmount)
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "✅ Nhận $coinsAmount xu thành công!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showBuyCoinsDialog = false
                            } else {
                                Toast.makeText(context, "Lỗi cộng xu!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // Hiệu ứng nhấp nháy cho trạng thái đang chờ
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            AlertDialog(
                onDismissRequest = {
                    if (!isPolling) {
                        showBuyCoinsDialog = false
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Nạp xu tự động",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Cảnh báo cấu hình
                        if (!isAccountConfigured || !isSePayConfigured) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!isAccountConfigured) {
                                        Text(
                                            "⚠️ Chưa điền ACCOUNT_NO trong UserHomeScreen.kt",
                                            fontSize = 11.sp, color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (!isSePayConfigured) {
                                        Text(
                                            "⚠️ Chưa điền SEPAY_API_KEY trong SePayRepository.kt",
                                            fontSize = 11.sp, color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "→ Đăng ký miễn phí tại sepay.vn, liên kết ngân hàng rồi lấy API Token",
                                            fontSize = 10.sp, color = Color(0xFF888888)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Nhập số xu (chỉ hiển thị khi chưa ấn chờ)
                        if (!isPolling) {
                            Text(
                                "Nhập số xu muốn nạp (1 xu = 100 VNĐ):",
                                fontSize = 13.sp, color = Color.Gray,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = coinsInput,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) coinsInput = it
                                },
                                label = { Text("Số lượng xu") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Gợi ý nhanh
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("100", "200", "500").forEach { preset ->
                                    Button(
                                        onClick = { coinsInput = preset },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (coinsInput == preset)
                                                MaterialTheme.colorScheme.primary
                                            else Color.LightGray.copy(alpha = 0.3f),
                                            contentColor = if (coinsInput == preset)
                                                Color.White else Color.DarkGray
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("$preset xu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Hiển thị VietQR sau khi chọn số xu hợp lệ
                        if (coinsAmount > 0 && isAccountConfigured) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                            ) {
                                Text(
                                    text = "${String.format("%,d", moneyAmount)} VNĐ  •  $coinsAmount xu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF5D4037),
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Gọi API VietQR công khai hoàn toàn miễn phí
                            val encodedInfo = java.net.URLEncoder.encode(txCode, "UTF-8")
                            val encodedName = java.net.URLEncoder.encode(ACCOUNT_NAME, "UTF-8")
                            val qrUrl = "https://img.vietqr.io/image/$BANK_ID-$ACCOUNT_NO-compact2.png" +
                                    "?amount=$moneyAmount&addInfo=$encodedInfo&accountName=$encodedName"

                            Text(
                                "Quét mã VietQR này bằng app ngân hàng của bạn:",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))



                            // Debug status hiển thị trực tiếp trên màn hình
                            if (isPolling) {
                                Text(
                                    "🔍 Debug: $debugText",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Surface(
                                modifier = Modifier.size(220.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (isPolling) Color(0xFF4CAF50) else Color.LightGray
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        strokeWidth = 3.dp
                                    )
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(qrUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "VietQR",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Hiển thị trạng thái chờ chuyển khoản
                            when {
                                isPolling -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF2E7D32)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Đang chờ hệ thống SePay xác nhận tiền...",
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.alpha(pulseAlpha)
                                            )
                                        }
                                    }
                                }
                                !isSePayConfigured -> {
                                    Text(
                                        "Cấu hình SEPAY_API_KEY để bật tự động cộng xu",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                else -> {
                                    Text(
                                        "Bấm nút \"Chờ thanh toán\" sau khi đã chuyển khoản thành công",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // ── NÚT DEBUG: Xem giao dịch SePay ngay trên màn hình ──
                            var debugInfo by remember { mutableStateOf("") }
                            var isLoadingDebug by remember { mutableStateOf(false) }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    isLoadingDebug = true
                                    scope.launch {
                                        debugInfo = sePayRepository.fetchDebugInfo()
                                        isLoadingDebug = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoadingDebug) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("🔍 Xem giao dịch SePay", fontSize = 12.sp)
                            }
                            if (debugInfo.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
                                ) {
                                    Text(
                                        text = "🔎 txCode cần khớp:\n$txCode\n══════\n$debugInfo",
                                        fontSize = 10.sp,
                                        color = Color(0xFF00FF88),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!isPolling && coinsAmount > 0 && isAccountConfigured && isSePayConfigured) {
                        Button(
                            onClick = {
                                isPolling = true
                                creditDone = false
                                paymentStatus = PaymentStatus.Waiting
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chờ thanh toán")
                        }
                    } else if (isPolling) {
                        OutlinedButton(
                            onClick = {
                                isPolling = false
                                paymentStatus = PaymentStatus.Waiting
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Huỷ chờ", color = Color(0xFFC62828))
                        }
                    }
                },
                dismissButton = {
                    if (!isPolling) {
                        TextButton(onClick = { showBuyCoinsDialog = false }) {
                            Text("Đóng")
                        }
                    }
                }
            )
        }
    } // Đóng Box
} // Đóng Scaffold
} // Đóng UserHomeScreen

@Composable
fun UserHeader(user: User?, onCoinsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Banner cao rộng rực rỡ
    ) {
        // Tải hình nền hoạt hình động vật cute đang học bài trực tiếp từ internet
        AsyncImage(
            model = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=800&auto=format&fit=crop",
            contentDescription = "Cute Header Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Lớp phủ Gradient màu tối dịu giúp hiển thị chữ trắng cực kỳ rõ nét và nghệ thuật
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x33000000), Color(0x77000000), Color(0xBB121212))
                    )
                )
        )
        
        // Tên ứng dụng EduQiz Pro ở góc trên bên trái
        Text(
            text = "EduQiz Pro",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 20.dp)
        )

        // Khu vực thông tin cá nhân và điểm số ở phía dưới Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.fullName ?: "Xin chào!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = user?.email ?: "Đang tải thông tin...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Số dư Xu được thiết kế cute, nổi bật và có thể click để nạp xu
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                modifier = Modifier.clickable { onCoinsClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user?.coins ?: 0} xu",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        color = Color(0xFF333333)
    )
}

@Composable
fun FeatureGrid(items: List<FeatureItem>, onItemClick: (String) -> Unit) {
    val chunks = items.chunked(2)
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        chunks.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        FeatureCardItem(item) { onItemClick(item.route) }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FeatureCardItem(item: FeatureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}