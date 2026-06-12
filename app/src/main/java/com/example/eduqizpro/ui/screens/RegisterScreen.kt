package com.example.eduqizpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
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
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF03DAC5), Color(0xFF018786))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tạo Tài Khoản",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Họ và tên") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Xác nhận mật khẩu") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (password != confirmPassword) {
                                errorMessage = "Mật khẩu không khớp"
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val success = authRepository.register(email, password, fullName)
                                    if (success) {
                                        onRegisterSuccess()
                                    } else {
                                        errorMessage = "Đăng ký thất bại"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF018786))
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("ĐĂNG KÝ")
                    }

                    TextButton(onClick = onNavigateToLogin) {
                        Text("Đã có tài khoản? Đăng nhập", color = Color(0xFF018786))
                    }
                }
            }
        }
    }
}