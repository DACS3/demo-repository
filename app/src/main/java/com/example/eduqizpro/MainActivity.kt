package com.example.eduqizpro

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.eduqizpro.data.AuthRepository
import com.example.eduqizpro.data.FlashCardRepository
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.SummaryRepository
import com.example.eduqizpro.data.model.FlashCardDeck
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.ui.screens.*
import com.example.eduqizpro.ui.theme.EduQizProTheme
import com.google.firebase.auth.FirebaseAuth

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val quizRepository = remember { QuizRepository() }
    val summaryRepository = remember { SummaryRepository() }
    val flashcardRepository = remember { FlashCardRepository() }
    val authRepository = remember { AuthRepository() }

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("eduqiz_prefs", Context.MODE_PRIVATE)
        val rememberMe = sharedPrefs.getBoolean("remember_me", true)
        
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                if (!rememberMe) {
                    // If user didn't want to be remembered, log them out on fresh start
                    authRepository.logout()
                    startDestination = "login"
                } else {
                    val userData = try {
                        authRepository.getUserData(currentUser.uid)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (userData?.role == "ADMIN") {
                        startDestination = "admin_home"
                    } else {
                        startDestination = "home"
                    }
                }
            } else {
                startDestination = "login"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            startDestination = "login"
        }
    }

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
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
                            authRepository.logout()
                            navController.navigate("login") { popUpTo("home") { inclusive = true } }
                        },
                        onNavigateToCreateQuiz = { navController.navigate("create_quiz") },
                        onNavigateToQuizList = { navController.navigate("quiz_list") },
                        onNavigateToSummary = { navController.navigate("summary") },
                        onNavigateToSavedSummaries = { navController.navigate("saved_summaries") },
                        onNavigateToCreateFlashcard = { navController.navigate("create_flashcard") },
                        onNavigateToFlashcardList = { navController.navigate("flashcard_list") },
                        onNavigateToConverter = { navController.navigate("converter") }
                    )
                }

                composable("admin_home") {
                    AdminHomeScreen(onLogout = {
                        authRepository.logout()
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

                composable("create_flashcard") {
                    CreateFlashCardDeckScreen(onBack = { navController.popBackStack() })
                }

                composable("flashcard_list") {
                    FlashCardListScreen(
                        onBack = { navController.popBackStack() },
                        onDeckClick = { deck -> navController.navigate("flashcard_study/${deck.id}") }
                    )
                }

                composable("converter") {
                    DocumentConverterScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = "flashcard_study/{deckId}",
                    arguments = listOf(navArgument("deckId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val deckId = backStackEntry.arguments?.getString("deckId")
                    var deckData by remember { mutableStateOf<FlashCardDeck?>(null) }

                    LaunchedEffect(deckId) {
                        if (deckId != null) {
                            deckData = flashcardRepository.getDeckById(deckId)
                        }
                    }

                    deckData?.let {
                        FlashCardStudyScreen(deck = it, onBack = { navController.popBackStack() })
                    }
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
}
