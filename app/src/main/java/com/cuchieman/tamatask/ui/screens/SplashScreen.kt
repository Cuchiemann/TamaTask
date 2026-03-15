package com.cuchieman.tamatask.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.ui.components.PixelDecorationsOverlay
import com.cuchieman.tamatask.ui.theme.SplashGradient1
import com.cuchieman.tamatask.ui.theme.SplashGradient2
import com.cuchieman.tamatask.ui.theme.SplashGradient3
import com.cuchieman.tamatask.ui.theme.SplashGradient4
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaYellow

@Composable
fun SplashScreen(onStartClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    // Pulsing animation for the button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_pulse"
    )

    // Floating egg bounce
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // === Layer 1: Blurred gradient background ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SplashGradient1,
                            SplashGradient2,
                            SplashGradient3,
                            SplashGradient4
                        )
                    )
                )
        )

        // === Layer 2: Pixel grid pattern (subtle) ===
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f)
        ) {
            val gridSize = 24f * density
            val cols = (size.width / gridSize).toInt() + 1
            val rows = (size.height / gridSize).toInt() + 1
            for (row in 0..rows) {
                for (col in 0..cols) {
                    if ((row + col) % 2 == 0) {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(col * gridSize, row * gridSize),
                            size = Size(gridSize, gridSize)
                        )
                    }
                }
            }
        }

        // === Layer 3: Radial overlay for depth ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        ),
                        radius = 900f
                    )
                )
        )

        // === Layer 4: Pixel art decorations (stars, hearts, diamonds) ===
        PixelDecorationsOverlay(
            modifier = Modifier.alpha(contentAlpha),
            alpha = 0.9f
        )

        // === Layer 5: Content - Title + Egg + Button ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            // App title with hard pixel shadow
            Text(
                text = "TamaTask",
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(
                        color = SplashGradient2.copy(alpha = 0.6f),
                        offset = Offset(4f, 4f),
                        blurRadius = 0f
                    ),
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cuida tu mascota, cumple tus tareas",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Pixel egg
            Canvas(modifier = Modifier.size(120.dp)) {
                drawPixelEgg(this, floatOffset)
            }

            Spacer(modifier = Modifier.weight(1f))

            // === START BUTTON ===
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height((56 * buttonScale).dp)
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = TamaPink
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "EMPEZAR",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Draws a cute pixel art egg with colorful zigzag pattern
 */
private fun drawPixelEgg(scope: DrawScope, bounceOffset: Float) {
    with(scope) {
        val b = size.width / 10f
        val cx = size.width / 2f
        val baseY = size.height / 2f - bounceOffset

        val eggWhite = Color(0xFFFFF8E7)
        val eggShell = Color(0xFFFFE4B5)
        val zigzagPink = TamaPink
        val zigzagYellow = TamaYellow
        val outline = Color(0xFF5D4E37)

        fun px(col: Int, row: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(cx + (col - 5) * b, baseY + (row - 6) * b),
                size = Size(b, b)
            )
        }

        // Row 0 (top)
        for (c in 3..6) px(c, 0, outline)
        // Row 1
        px(2, 1, outline); for (c in 3..6) px(c, 1, eggWhite); px(7, 1, outline)
        // Row 2
        px(1, 2, outline); for (c in 2..7) px(c, 2, eggWhite); px(8, 2, outline)
        // Row 3: zigzag
        px(1, 3, outline)
        px(2, 3, eggWhite); px(3, 3, zigzagPink); px(4, 3, eggWhite)
        px(5, 3, zigzagYellow); px(6, 3, eggWhite); px(7, 3, zigzagPink)
        px(8, 3, outline)
        // Row 4: zigzag
        px(1, 4, outline)
        px(2, 4, zigzagYellow); px(3, 4, eggWhite); px(4, 4, zigzagPink)
        px(5, 4, eggWhite); px(6, 4, zigzagYellow); px(7, 4, eggWhite)
        px(8, 4, outline)
        // Row 5: zigzag
        px(1, 5, outline)
        px(2, 5, eggWhite); px(3, 5, zigzagPink); px(4, 5, eggWhite)
        px(5, 5, zigzagYellow); px(6, 5, eggWhite); px(7, 5, zigzagPink)
        px(8, 5, outline)
        // Row 6
        px(1, 6, outline); for (c in 2..7) px(c, 6, eggShell); px(8, 6, outline)
        // Row 7
        px(1, 7, outline); for (c in 2..7) px(c, 7, eggShell); px(8, 7, outline)
        // Row 8
        px(2, 8, outline); for (c in 3..6) px(c, 8, eggShell); px(7, 8, outline)
        // Row 9 (bottom)
        for (c in 3..6) px(c, 9, outline)
    }
}
