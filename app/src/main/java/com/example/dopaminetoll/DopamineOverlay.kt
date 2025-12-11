package com.example.dopaminetoll

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DopamineOverlay(
    tasks: List<String>,
    duration: Int,
    onUnlock: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(duration) }
    var isUnlocked by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (duration > 0) timeLeft.toFloat() / duration.toFloat() else 0f,
        label = "TimerProgress"
    )

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        isUnlocked = true
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF0000),
            onPrimary = Color.Black,
            surface = Color(0xFF0A0A0A),
            onSurface = Color(0xFFE0E0E0),
            outline = Color(0xFFFF0000)
        )
    ) {
        // --- FIX 3: CONSUME TOUCH EVENTS ---
        // The root Box is now clickable (without ripple) to eat all input.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // No ripple effect
                    onClick = { /* Do nothing, just block touch from passing through */ }
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) Color.Green else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "ACCESS DENIED",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(vertical = 16.dp),
                        color = Color(0xFF111111),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RectangleShape
                    ) {
                        if (tasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "No pending tasks.\nWait required.",
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(12.dp)) {
                                items(tasks) { task ->
                                    Text(
                                        text = ">> $task",
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isUnlocked) "UNLOCKED" else "$timeLeft",
                        fontSize = if (isUnlocked) 32.sp else 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Color.Green else Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF222222),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onUnlock,
                        enabled = isUnlocked,
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) Color.Green else Color(0xFF222222),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF222222),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = if (isUnlocked) "ENTER SYSTEM" else "LOCKED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}