package com.example.eduqizpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.ui.screens.*
import com.example.eduqizpro.ui.screens.chat.ChatDetailScreen
import com.example.eduqizpro.ui.screens.chat.ChatListScreen
import com.example.eduqizpro.ui.screens.createquiz.CreateQuizScreen
import com.example.eduqizpro.ui.screens.createquiz.QuizDetailScreen
import com.example.eduqizpro.ui.screens.community.QuizCommentScreen
import com.example.eduqizpro.ui.screens.createquiz.QuizListScreen
import com.example.eduqizpro.ui.screens.community.CommunityQuizFeedScreen
import com.example.eduqizpro.ui.screens.convert.ConvertedFilesScreen
import com.example.eduqizpro.ui.screens.convert.DocumentConverterScreen
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
    val quizRepository = remember { QuizRepository() }
    val summaryRepository = remember { SummaryRepository() }

    var adminBlockMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    adminMessage = adminBlockMessage,   // ← truyền thông báo xuống LoginScreen
                    onLoginSuccess = { role ->
                        // Reset thông báo admin mỗi lần có kết quả đăng nhập mới
                        adminBlockMessage = null
                        if (role == "ADMIN") {
                            adminBlockMessage = "Tài khoản Admin vui lòng đăng nhập trên máy tính để quản trị hệ thống!"
                        } else {
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        }
                    },
                    onNavigateToRegister = {
                        adminBlockMessage = null
                        navController.navigate("register")
                    }
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
                    onLogout = { navController.navigate("login") { popUpTo("home") { inclusive = true } } },
                    onNavigateToCreateQuiz = { navController.navigate("create_quiz") },
                    onNavigateToQuizList = { navController.navigate("quiz_list") },
                    onNavigateToSummary = { navController.navigate("summary") },
                    onNavigateToSavedSummaries = { navController.navigate("saved_summaries") },
                    onNavigateToCommunityFeed = { navController.navigate("community_feed") },
                    onNavigateToChatList = { navController.navigate("chat_list") },
                    onNavigateToCreateFlashcard = { navController.navigate("create_flashcard") },
                    onNavigateToFlashcardList = { navController.navigate("flashcard_list") },
                    onNavigateToDocumentConverter = { navController.navigate("document_converter") },
                    onNavigateToConvertedFiles = { navController.navigate("converted_files") }
                )
            }

            composable("community_feed") {
                CommunityQuizFeedScreen(
                    onBack = { navController.popBackStack() },
                    onQuizClick = { quiz -> navController.navigate("quiz_detail/${quiz.id}") },
                    onCommentClick = { quiz -> navController.navigate("quiz_comments/${quiz.id}") },
                    onNavigateToChat = { uid, name -> navController.navigate("chat_detail/$uid/$name") }
                )
            }

            composable("chat_list") {
                ChatListScreen(
                    onBack = { navController.popBackStack() },
                    onChatClick = { uid, name -> navController.navigate("chat_detail/$uid/$name") }
                )
            }

            composable(
                route = "chat_detail/{uid}/{name}",
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: ""
                val name = backStackEntry.arguments?.getString("name") ?: ""
                ChatDetailScreen(
                    receiverId = uid,
                    receiverName = name,
                    onBack = { navController.popBackStack() })
            }

            composable("admin_home") {
                AdminHomeScreen(onLogout = {
                    navController.navigate("login") { popUpTo("admin_home") { inclusive = true } }
                })
            }

            composable("create_quiz") { CreateQuizScreen(onBack = { navController.popBackStack() }) }

            composable("document_converter") {
                DocumentConverterScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate("converted_files") }
                )
            }

            composable("converted_files") {
                ConvertedFilesScreen(onBack = { navController.popBackStack() })
            }

            composable("create_flashcard") {
                CreateFlashCardDeckScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("flashcard_list") {
                FlashCardListScreen(
                    onBack = { navController.popBackStack() },
                    onDeckClick = { deck ->
                        navController.navigate("flashcard_study/${deck.id}")
                    }
                )
            }

            composable(
                route = "flashcard_study/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
                val repository = remember { com.example.eduqizpro.data.FlashCardRepository() }
                var deckData by remember { mutableStateOf<com.example.eduqizpro.data.model.FlashCardDeck?>(null) }
                
                LaunchedEffect(deckId) {
                    if (deckId.isNotEmpty()) {
                        deckData = repository.getDeckById(deckId)
                    }
                }
                
                deckData?.let {
                    FlashCardStudyScreen(
                        deck = it,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

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
                LaunchedEffect(summaryId) { if (summaryId != null) summaryData = summaryRepository.getSummaryById(summaryId) }
                summaryData?.let { SummaryDetailScreen(summary = it, onBack = { navController.popBackStack() }) }
            }

            composable(
                route = "quiz_detail/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId")
                var quizData by remember { mutableStateOf<com.example.eduqizpro.data.model.Quiz?>(null) }
                LaunchedEffect(quizId) { if (quizId != null) quizData = quizRepository.getQuizById(quizId) }
                quizData?.let {
                    QuizDetailScreen(
                        quiz = it,
                        onBack = { navController.popBackStack() },
                        onNavigateToComments = { navController.navigate("quiz_comments/${it.id}") }
                    )
                }
            }

            composable(
                route = "quiz_comments/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId")
                var quizData by remember { mutableStateOf<com.example.eduqizpro.data.model.Quiz?>(null) }
                LaunchedEffect(quizId) { if (quizId != null) quizData = quizRepository.getQuizById(quizId) }
                quizData?.let {
                    QuizCommentScreen(
                        quiz = it,
                        onBack = { navController.popBackStack() },
                        onNavigateToQuiz = {
                            navController.navigate("quiz_detail/${it.id}") { popUpTo("community_feed") }
                        }
                    )
                }
            }
        }
    }
}