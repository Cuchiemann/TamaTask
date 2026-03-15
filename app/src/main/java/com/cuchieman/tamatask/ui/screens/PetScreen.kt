package com.cuchieman.tamatask.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.ui.components.PixelNatureBackground
import com.cuchieman.tamatask.ui.components.PixelSpinosaurus
import com.cuchieman.tamatask.ui.theme.PixelFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// Background layout fractions — must match PixelNatureBackground
private const val WATER_TOP_FRACTION = 0.52f
private const val SHORE_BOTTOM_FRACTION = 0.92f  // don't walk off the bottom

// Foam color
private val WaterFoam = Color(0xFFB0DDD5)

// Speed: screen-fractions per second
private const val WALK_SPEED = 0.08f

// Idle pause range (ms)
private const val MIN_IDLE_MS = 2000L
private const val MAX_IDLE_MS = 6000L

// Screen-space X boundaries (fraction of screen width)
private const val X_MIN = 0.08f
private const val X_MAX = 0.92f

@Composable
fun PetScreen() {
    // ── Dino position on screen ──
    // dinoX: 0=left edge, 1=right edge (horizontal position ON SCREEN)
    // dinoY: 0=at water line, 1=at shore bottom (vertical position)
    val dinoX = remember { Animatable(0.5f) }
    val dinoY = remember { Animatable(0.3f) }  // start slightly below water
    var isWalking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }

    // Background scroll — gentle parallax linked to dino X
    val bgScroll = 0.3f + dinoX.value * 0.4f  // maps dino 0..1 → bg 0.3..0.7

    // ── Random walking behavior ──
    LaunchedEffect(Unit) {
        while (true) {
            // 1) Idle pause
            isWalking = false
            delay(Random.nextLong(MIN_IDLE_MS, MAX_IDLE_MS))

            // 2) Pick random target on screen
            val targetX = Random.nextFloat() * (X_MAX - X_MIN) + X_MIN
            val targetY = Random.nextFloat()  // 0 (in water) to 1 (on shore)

            val dx = abs(targetX - dinoX.value)
            val dy = abs(targetY - dinoY.value)
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < 0.05f) continue

            // 3) Direction: face the way we're going horizontally
            facingLeft = targetX < dinoX.value
            isWalking = true

            // 4) Animate X and Y together at constant speed
            val durationMs = (distance / WALK_SPEED * 1000f).toInt().coerceIn(500, 8000)

            coroutineScope {
                launch {
                    dinoX.animateTo(
                        targetX,
                        animationSpec = tween(durationMs, easing = LinearEasing)
                    )
                }
                launch {
                    dinoY.animateTo(
                        targetY,
                        animationSpec = tween(durationMs, easing = LinearEasing)
                    )
                }
            }
            // 5) Arrive → loop back to idle
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val screenW = maxWidth

        // Dino display size
        val dinoHeight = if (isWalking) 124.dp else 93.dp
        val dinoWidth = if (isWalking) 435.dp else 320.dp

        // ── Vertical position ──
        // dinoY=0 → center at waterTop (half submerged)
        // dinoY=1 → center at shore bottom (on dry land)
        val waterLineY = screenH * WATER_TOP_FRACTION
        val shoreBottomY = screenH * SHORE_BOTTOM_FRACTION
        val dinoCenterY = waterLineY + (shoreBottomY - waterLineY) * dinoY.value
        val dinoTopY = dinoCenterY - dinoHeight / 2

        // ── Horizontal position ──
        // dinoX maps to screen position (dino center)
        val dinoLeftX = screenW * dinoX.value - dinoWidth / 2

        // ── Submersion effect ──
        // At dinoY=0 → waterline crosses at 50% of sprite (half submerged)
        // At dinoY=0.4+ → waterline above sprite (no submersion)
        // waterFrac = where the waterline is within the dino (0=top, 1=bottom, >1=below dino)
        val waterLineInDino = (waterLineY - dinoTopY) / dinoHeight
        val hasSubmersion = waterLineInDino in 0.05f..0.95f

        // Animated pixel art swamp/mangrove panoramic background
        PixelNatureBackground(
            modifier = Modifier.fillMaxSize(),
            scrollOffset = bgScroll
        )

        // Pet name label
        Text(
            text = "Spinosaurus",
            fontFamily = PixelFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(top = 8.dp)
        )

        // ── Dino with dynamic submersion ──
        Box(
            modifier = Modifier
                .offset(x = dinoLeftX, y = dinoTopY)
                .width(dinoWidth)
                .height(dinoHeight)
                .then(
                    if (hasSubmersion) {
                        Modifier
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()

                                // Alpha mask for submersion
                                val wf = waterLineInDino.coerceIn(0.1f, 0.9f)
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Black,
                                            (wf - 0.02f) to Color.Black,
                                            wf to Color.Black.copy(alpha = 0.5f),
                                            (wf + 0.08f).coerceAtMost(1f) to Color.Black.copy(
                                                alpha = 0.35f
                                            ),
                                            1.00f to Color.Black.copy(alpha = 0.20f)
                                        )
                                    ),
                                    blendMode = BlendMode.DstIn
                                )

                                // Foam line at waterline
                                val waterY = size.height * wf
                                drawRect(
                                    color = WaterFoam.copy(alpha = 0.30f),
                                    topLeft = Offset(0f, waterY - 1f),
                                    size = Size(size.width, 2f)
                                )
                            }
                    } else {
                        Modifier // no submersion effect needed
                    }
                )
        ) {
            PixelSpinosaurus(
                modifier = Modifier.align(Alignment.Center),
                isWalking = isWalking,
                facingLeft = facingLeft
            )
        }
    }
}
