package com.example.eduqizpro.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.model.FlashCardDeck
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardStudyScreen(
    deck: FlashCardDeck,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Animation states for the swiping effect
    val dragOffsetX = remember { Animatable(0f) }
    val dragRotationZ = remember { Animatable(0f) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(deck.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${deck.cards.size} thẻ", 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
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
                        colors = listOf(Color(0xFFF8F9FF), Color(0xFFE8EAF6))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (deck.cards.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Bộ thẻ này không có dữ liệu", style = MaterialTheme.typography.headlineSmall)
                    }
                } else if (currentIndex >= deck.cards.size) {
                    FinishedView(onRestart = { currentIndex = 0 })
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    StudyProgressBar(
                        currentIndex = currentIndex,
                        totalCards = deck.cards.size
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background card (next card)
                        if (currentIndex + 1 < deck.cards.size) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.9f)
                                    .graphicsLayer {
                                        scaleX = 0.92f
                                        scaleY = 0.92f
                                        translationY = 40f
                                        alpha = 0.4f
                                    },
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {}
                        }

                        // Top Card (current card)
                        FlashCardItem(
                            card = deck.cards[currentIndex],
                            key = currentIndex,
                            onFlip = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier
                                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                                .graphicsLayer {
                                    rotationZ = dragRotationZ.value
                                    cameraDistance = 15f * density
                                }
                                .pointerInput(currentIndex) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            scope.launch {
                                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                                dragRotationZ.snapTo(dragOffsetX.value / 25f)
                                            }
                                        },
                                        onDragEnd = {
                                            if (dragOffsetX.value > screenWidth / 4) {
                                                scope.launch {
                                                    dragOffsetX.animateTo(screenWidth, tween(300))
                                                    currentIndex++
                                                    dragOffsetX.snapTo(0f)
                                                    dragRotationZ.snapTo(0f)
                                                }
                                            } else if (dragOffsetX.value < -screenWidth / 4) {
                                                scope.launch {
                                                    dragOffsetX.animateTo(-screenWidth, tween(300))
                                                    currentIndex++
                                                    dragOffsetX.snapTo(0f)
                                                    dragRotationZ.snapTo(0f)
                                                }
                                            } else {
                                                scope.launch {
                                                    launch { dragOffsetX.animateTo(0f, tween(250)) }
                                                    launch { dragRotationZ.animateTo(0f, tween(250)) }
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                    
                    InstructionPanel()
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun StudyProgressBar(currentIndex: Int, totalCards: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalCards },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .height(8.dp)
                .graphicsLayer { 
                    shape = RoundedCornerShape(4.dp)
                    clip = true 
                },
            color = Color(0xFF673AB7),
            trackColor = Color(0xFFE0E0E0)
        )
        
        Text(
            "TIẾN ĐỘ: ${currentIndex + 1} / $totalCards",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun InstructionPanel() {
    Surface(
        color = Color.White.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF673AB7)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "CHẠM ĐỂ LẬT • VUỐT ĐỂ CHUYỂN",
                fontSize = 11.sp,
                color = Color(0xFF673AB7),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun FinishedView(onRestart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    modifier = Modifier.padding(24.dp), 
                    tint = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Tuyệt vời!", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2D3436)
            )
            Text(
                "Bạn đã hoàn thành tất cả các thẻ trong bộ này.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
            ) {
                Text("Học lại từ đầu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FlashCardItem(
    card: com.example.eduqizpro.data.model.FlashCard,
    key: Int,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFlipped by rememberSaveable(key) { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "cardFlip"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .clickable { 
                isFlipped = !isFlipped 
                onFlip()
            },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (rotation <= 90f) {
                        Brush.linearGradient(listOf(Color.White, Color(0xFFF0F2FF)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFF5FFF8), Color.White))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                FlashCardFace(
                    title = "CÂU HỎI",
                    text = card.front,
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    accentColor = Color(0xFF673AB7)
                )
            } else {
                FlashCardFace(
                    title = "ĐÁP ÁN",
                    text = card.back,
                    icon = Icons.Default.Lightbulb,
                    accentColor = Color(0xFF4CAF50),
                    isReversed = true
                )
            }
        }
    }
}

@Composable
fun FlashCardFace(
    title: String,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isReversed: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .graphicsLayer { 
                if (isReversed) rotationY = 180f 
            }
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF2D3436),
            lineHeight = 32.sp
        )
    }
}
