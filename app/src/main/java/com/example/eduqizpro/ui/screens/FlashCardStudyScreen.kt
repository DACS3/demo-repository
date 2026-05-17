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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HelpOutline
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
    val offsetX = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(deck.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${deck.cards.size} thẻ", 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF4F6F9)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (deck.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bộ thẻ này không có dữ liệu", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (currentIndex >= deck.cards.size) {
                // All cards finished view
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF6200EE))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bạn đã học xong tất cả các thẻ!", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { currentIndex = 0 }) {
                            Text("Học lại từ đầu")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress tracker
                LinearProgressIndicator(
                    progress = (currentIndex + 1).toFloat() / deck.cards.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(10.dp)
                        .graphicsLayer { 
                            shape = RoundedCornerShape(5.dp)
                            clip = true 
                        },
                    color = Color(0xFF6200EE),
                    trackColor = Color(0xFFE0E0E0)
                )
                
                Text(
                    "THẺ: ${currentIndex + 1} / ${deck.cards.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    letterSpacing = 1.5.sp
                )

                // Card Stack
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background card (next card)
                    if (currentIndex + 1 < deck.cards.size) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.8f)
                                .graphicsLayer {
                                    scaleX = 0.9f
                                    scaleY = 0.9f
                                    alpha = 0.5f
                                },
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {}
                    }

                    // Top Card (current card)
                    FlashCardItem(
                        card = deck.cards[currentIndex],
                        key = currentIndex, // Important to reset flip state on index change
                        onFlip = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .graphicsLayer {
                                this.rotationZ = rotationZ.value // Sửa tại đây
                                cameraDistance = 15f * density
                            }
                            .pointerInput(currentIndex) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            rotationZ.snapTo(offsetX.value / 20f)
                                        }
                                    },
                                    onDragEnd = {
                                        if (offsetX.value > screenWidth / 3) {
                                            // Fly away right
                                            scope.launch {
                                                offsetX.animateTo(screenWidth, tween(300))
                                                currentIndex++
                                                offsetX.snapTo(0f)
                                                rotationZ.snapTo(0f)
                                            }
                                        } else if (offsetX.value < -screenWidth / 3) {
                                            // Fly away left
                                            scope.launch {
                                                offsetX.animateTo(-screenWidth, tween(300))
                                                currentIndex++
                                                offsetX.snapTo(0f)
                                                rotationZ.snapTo(0f)
                                            }
                                        } else {
                                            // Snap back
                                            scope.launch {
                                                launch { offsetX.animateTo(0f, tween(200)) }
                                                launch { rotationZ.animateTo(0f, tween(200)) }
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Interaction Instruction
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "NHẤN ĐỂ LẬT • VUỐT ĐỂ CHUYỂN",
                            fontSize = 13.sp,
                            color = Color(0xFF6200EE),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
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
            .fillMaxHeight(0.85f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .clickable { 
                isFlipped = !isFlipped 
                onFlip()
            },
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (rotation <= 90f) {
                            listOf(Color.White, Color(0xFFF9FAFF))
                        } else {
                            listOf(Color(0xFFF5FFF6), Color.White)
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front: The Question
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF6200EE).copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "CÂU HỎI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF6200EE).copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = card.front,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2D3436),
                        lineHeight = 34.sp
                    )
                }
            } else {
                // Back: The Answer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(32.dp)
                        .graphicsLayer { rotationY = 180f }
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50).copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ĐÁP ÁN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = card.back,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2D3436),
                        lineHeight = 32.sp
                    )
                }
            }
        }
    }
}
