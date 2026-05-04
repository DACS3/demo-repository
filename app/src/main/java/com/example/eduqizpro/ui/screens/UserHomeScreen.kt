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
import com.example.eduqizpro.data.model.User
import kotlinx.coroutines.launch

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
    onNavigateToSavedSummaries: () -> Unit   // ← Thêm callback mới
) {
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    var userData by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    userData = authRepository.getUserData(currentUser.uid)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val utilities = listOf(
        FeatureItem("Tạo trắc nghiệm", Icons.Default.AddCircle, Color(0xFF6200EE), "create_quiz"),
        FeatureItem("Tóm tắt PDF", Icons.Default.Description, Color(0xFFF44336), "summary"),
        FeatureItem("Flash card", Icons.Default.Star, Color(0xFFFF9800)),
        FeatureItem("Word sang PDF", Icons.Default.Share, Color(0xFF2196F3))
    )

    val management = listOf(
        FeatureItem("Trắc nghiệm lưu", Icons.Default.List, Color(0xFF4CAF50), "quiz_list"),
        FeatureItem("Tóm tắt đã lưu", Icons.Default.AccountBox, Color(0xFF009688), "saved_summaries"), // ← Đã sửa
        FeatureItem("Flash-card lưu", Icons.Default.Info, Color(0xFFFFC107)),
        FeatureItem("Shared items", Icons.Default.Send, Color(0xFF9C27B0))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduQiz Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            item { UserHeader(userData) }

            item { SectionTitle("Tiện ích") }
            item {
                FeatureGrid(utilities) { route ->
                    when (route) {
                        "create_quiz" -> onNavigateToCreateQuiz()
                        "summary" -> onNavigateToSummary()
                    }
                }
            }

            item { SectionTitle("Quản lý") }
            item {
                FeatureGrid(management) { route ->
                    when (route) {
                        "quiz_list" -> onNavigateToQuizList()
                        "saved_summaries" -> onNavigateToSavedSummaries()   // ← Xử lý click
                    }
                }
            }

            item { SectionTitle("Khám phá") }
            item {
                FeatureGrid(listOf(
                    FeatureItem("Kho cộng đồng", Icons.Default.Home, Color(0xFF3F51B5)),
                    FeatureItem("Nhắn tin", Icons.Default.Email, Color(0xFFE91E63))
                )) {}
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// Các hàm còn lại giữ nguyên
@Composable
fun UserHeader(user: User?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF6200EE), Color(0xFFF5F5F5))))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF6200EE))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user?.fullName ?: "Xin chào!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = user?.email ?: "Đang tải thông tin...",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
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