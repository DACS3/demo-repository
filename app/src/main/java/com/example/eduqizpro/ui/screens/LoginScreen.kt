package com.example.eduqizpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.AuthRepository
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun LoginScreen(
    adminMessage: String? = null,        // ← thêm param nhận thông báo từ MainActivity
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Tải ảnh học tập nghệ thuật trực tiếp từ internet qua thư viện Coil
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=800&auto=format&fit=crop",
                    contentDescription = "Education Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Lớp phủ Gradient mờ mịn để chữ hiển thị rõ ràng và đẹp mắt
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000), Color(0xCC3700B3))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EduQizPro",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Học tập không giới hạn",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chào mừng trở lại!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3700B3)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF6200EE)) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6200EE)) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Nếu có lỗi đăng nhập thông thường → ưu tiên hiện lỗi đó
                    // Nếu không có lỗi nhưng có thông báo admin → hiện thông báo admin
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (adminMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = adminMessage,
                                    color = Color(0xFF856404),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val user = authRepository.login(email, password)
                                    if (user != null) {
                                        onLoginSuccess(user.role)
                                    } else {
                                        errorMessage = "Email hoặc mật khẩu không đúng"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Lỗi: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("ĐĂNG NHẬP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToRegister) {
                        Text("Chưa có tài khoản? Đăng ký ngay", color = Color(0xFF6200EE))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.width(50.dp).padding(horizontal = 8.dp))
                Text("EduQiz Excellence", color = Color.Gray, fontSize = 12.sp)
                Divider(modifier = Modifier.width(50.dp).padding(horizontal = 8.dp))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}