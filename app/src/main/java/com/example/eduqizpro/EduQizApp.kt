package com.example.eduqizpro

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eduqizpro.ui.screens.*
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.Summary
import com.example.eduqizpro.data.model.FlashCardDeck
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EduQizApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    
    // States to hold selected items for detail screens as we are passing them directly
    var selectedQuiz by remember { mutableStateOf<Quiz?>(null) }
    var selectedSummary by remember { mutableStateOf<Summary?>(null) }
    var selectedDeck by remember { mutableStateOf<FlashCardDeck?>(null) }

    // Initial destination depends on whether the user is logged in
    val startDestination = if (auth.currentUser != null) "user_home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "ADMIN") {
                        navController.navigate("admin_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("user_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("user_home") {
            UserHomeScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("user_home") { inclusive = true }
                    }
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
            AdminHomeScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("admin_home") { inclusive = true }
                    }
                }
            )
        }
        
        composable("create_quiz") {
            CreateQuizScreen(onBack = { navController.popBackStack() })
        }
        
        composable("quiz_list") {
            QuizListScreen(
                onBack = { navController.popBackStack() },
                onQuizClick = { quiz ->
                    selectedQuiz = quiz
                    navController.navigate("quiz_detail")
                }
            )
        }
        
        composable("quiz_detail") {
            selectedQuiz?.let { quiz ->
                QuizDetailScreen(
                    quiz = quiz,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        composable("summary") {
            SummaryScreen(onBack = { navController.popBackStack() })
        }
        
        composable("saved_summaries") {
            SavedSummariesScreen(
                onBack = { navController.popBackStack() },
                onSummaryClick = { summary ->
                    selectedSummary = summary
                    navController.navigate("summary_detail")
                }
            )
        }
        
        composable("summary_detail") {
            selectedSummary?.let { summary ->
                SummaryDetailScreen(
                    summary = summary,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        composable("create_flashcard") {
            CreateFlashCardDeckScreen(onBack = { navController.popBackStack() })
        }
        
        composable("flashcard_list") {
            FlashCardListScreen(
                onBack = { navController.popBackStack() },
                onDeckClick = { deck ->
                    selectedDeck = deck
                    navController.navigate("flashcard_study")
                }
            )
        }
        
        composable("flashcard_study") {
            selectedDeck?.let { deck ->
                FlashCardStudyScreen(
                    deck = deck,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        composable("converter") {
            DocumentConverterScreen(onBack = { navController.popBackStack() })
        }
    }
}
