package com.example.eduqizpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.ui.screens.*
import com.example.eduqizpro.ui.theme.EduQizProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduQizProTheme {
                EduQizApp()
            }
        }
    }
}

@Composable
fun EduQizApp() {
    val navController = rememberNavController()
    // Tách riêng các Repository để quản lý code sạch sẽ hơn
    val quizRepository = remember { QuizRepository() }
    val summaryRepository = remember { SummaryRepository() }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { role ->
                        if (role == "ADMIN") {
                            navController.navigate("admin_home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate("login") },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable("home") {
                UserHomeScreen(
                    onLogout = {
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                    onNavigateToCreateQuiz = { navController.navigate("create_quiz") },
                    onNavigateToQuizList = { navController.navigate("quiz_list") },
                    onNavigateToSummary = { navController.navigate("summary") },
                    onNavigateToSavedSummaries = { navController.navigate("saved_summaries") }
                )
            }

            composable("admin_home") {
                AdminHomeScreen(onLogout = {
                    navController.navigate("login") { popUpTo("admin_home") { inclusive = true } }
                })
            }

            composable("create_quiz") { CreateQuizScreen(onBack = { navController.popBackStack() }) }

            composable("quiz_list") {
                QuizListScreen(
                    onBack = { navController.popBackStack() },
                    onQuizClick = { quiz -> navController.navigate("quiz_detail/${quiz.id}") }
                )
            }

            composable("summary") { SummaryScreen(onBack = { navController.popBackStack() }) }

            composable("saved_summaries") {
                SavedSummariesScreen(
                    onBack = { navController.popBackStack() },
                    onSummaryClick = { summary -> navController.navigate("summary_detail/${summary.id}") }
                )
            }

            composable(
                route = "summary_detail/{summaryId}",
                arguments = listOf(navArgument("summaryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val summaryId = backStackEntry.arguments?.getString("summaryId")
                var summaryData by remember { mutableStateOf<Summary?>(null) }

                LaunchedEffect(summaryId) {
                    if (summaryId != null) {
                        summaryData = summaryRepository.getSummaryById(summaryId)
                    }
                }

                summaryData?.let {
                    SummaryDetailScreen(summary = it, onBack = { navController.popBackStack() })
                }
            }

            composable(
                route = "quiz_detail/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId")
                var quizData by remember { mutableStateOf<com.example.eduqizpro.data.model.Quiz?>(null) }

                LaunchedEffect(quizId) {
                    if (quizId != null) {
                        quizData = quizRepository.getQuizById(quizId)
                    }
                }

                quizData?.let {
                    QuizDetailScreen(quiz = it, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}