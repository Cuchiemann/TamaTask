package com.cuchieman.tamatask.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
private const val SHORE_BOTTOM_FRACTION = 0.92f

// Foam color
private val WaterFoam = Color(0xFFB0DDD5)

// Speed: screen-fractions per second
private const val WALK_SPEED = 0.035f

// Idle pause range (ms)
private const val MIN_IDLE_MS = 2000L
private const val MAX_IDLE_MS = 6000L

// World-space X boundaries (fraction of panorama, 0..1)
// Keep away from edges so camera can always keep dino fully on screen
private const val X_MIN = 0.15f
private const val X_MAX = 0.85f

// Panorama ratio — must match PixelNatureBackground.PANORAMA_RATIO
private const val PANORAMA_RATIO = 2.5f

// Y boundaries
private const val Y_MIN = 0.0f
private const val Y_MAX = 1.0f

@Composable
fun PetScreen() {
    val dinoX = remember { Animatable(0.5f) }
    val dinoY = remember { Animatable(0.7f) }
    var isWalking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    // ── Random walking behavior ──
    LaunchedEffect(Unit) {
        while (true) {
            isWalking = false
            delay(Random.nextLong(MIN_IDLE_MS, MAX_IDLE_MS))

            val targetX = Random.nextFloat() * (X_MAX - X_MIN) + X_MIN
            val targetY = Random.nextFloat() * (Y_MAX - Y_MIN) + Y_MIN

            val dx = abs(targetX - dinoX.value)
            val dy = abs(targetY - dinoY.value)
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < 0.05f) continue

            facingLeft = targetX > dinoX.value
            isWalking = true

            val durationMs = (distance / WALK_SPEED * 1000f).toInt().coerceIn(500, 8000)

            coroutineScope {
                launch {
                    dinoX.animateTo(targetX, tween(durationMs, easing = LinearEasing))
                }
                launch {
                    dinoY.animateTo(targetY, tween(durationMs, easing = LinearEasing))
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight
        val screenW = maxWidth
        val density = LocalDensity.current

        val dinoHeight = 93.dp
        val dinoWidth = if (isWalking) 325.dp else 320.dp

        // ── Camera system: dino walks in world, camera follows ──
        val maxScrollFrac = PANORAMA_RATIO - 1f  // 1.5
        // Camera tries to center on dino's world X
        val idealCameraFrac = dinoX.value * PANORAMA_RATIO - 0.5f
        val cameraFrac = idealCameraFrac.coerceIn(0f, maxScrollFrac)
        val bgScroll = cameraFrac / maxScrollFrac  // 0..1 for background

        // Dino screen X: world position minus camera scroll (in screen-widths)
        val dinoScreenCenterFrac = dinoX.value * PANORAMA_RATIO - cameraFrac
        // Clamp so the full dino (including head/tail) stays on screen
        val margin = dinoWidth / 2
        val dinoLeftXRaw = screenW * dinoScreenCenterFrac - dinoWidth / 2
        val dinoLeftX = dinoLeftXRaw.coerceIn(-margin * 0.1f, screenW - dinoWidth + margin * 0.1f)

        // ── Position dino by FEET (vertical) ──
        val shoreBottomY = screenH * SHORE_BOTTOM_FRACTION
        val deepFeetY = screenH * 0.65f
        val shoreFeetY = shoreBottomY
        val feetY: Dp = deepFeetY + (shoreFeetY - deepFeetY) * dinoY.value
        val dinoTopY: Dp = feetY - dinoHeight

        // Dino feet as fraction of screen height (for vegetation depth sorting)
        val dinoFeetFrac = feetY / screenH

        // ── Submersion based on dinoY directly ──
        // shoreThreshold: dino must be well into the water zone before submersion starts
        val shoreThreshold = 0.42f
        val maxWaterFracFromBottom = 0.55f

        val waterFracFromBottom = if (dinoY.value >= shoreThreshold) {
            0f
        } else {
            ((shoreThreshold - dinoY.value) / shoreThreshold) * maxWaterFracFromBottom
        }
        val waterFracInDino = 1f - waterFracFromBottom
        val hasSubmersion = waterFracFromBottom > 0.05f

        // Wave animation for foam line
        val waveTransition = rememberInfiniteTransition(label = "foam")
        val foamWave by waveTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2832f,
            animationSpec = infiniteRepeatable(
                tween(3000, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "foamWave"
        )

        // Background (everything except foreground vegetation)
        PixelNatureBackground(
            modifier = Modifier.fillMaxSize(),
            scrollOffset = bgScroll,
            dinoFeetScreenFrac = dinoFeetFrac
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

                                val wf = waterFracInDino.coerceIn(0.1f, 0.9f)
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color.Black,
                                            (wf - 0.02f) to Color.Black,
                                            wf to Color.Black.copy(alpha = 0.45f),
                                            (wf + 0.10f).coerceAtMost(1f) to Color.Black.copy(alpha = 0.30f),
                                            1.00f to Color.Black.copy(alpha = 0.15f)
                                        )
                                    ),
                                    blendMode = BlendMode.DstIn
                                )

                                // Wavy foam line — only on opaque sprite pixels
                                val waterY = size.height * wf
                                val foamPath = androidx.compose.ui.graphics.Path()
                                val step = 4f
                                val steps = (size.width / step).toInt() + 1
                                foamPath.moveTo(0f, waterY)
                                for (i in 0..steps) {
                                    val x = i * step
                                    val wave = kotlin.math.sin(x * 0.08f + foamWave) * 2.5f +
                                            kotlin.math.sin(x * 0.15f + foamWave * 1.6f) * 1.2f
                                    foamPath.lineTo(x, waterY + wave)
                                }
                                drawPath(
                                    foamPath,
                                    color = WaterFoam.copy(alpha = 0.45f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                    } else {
                        Modifier
                    }
                )
        ) {
            PixelSpinosaurus(
                modifier = Modifier.align(Alignment.Center),
                isWalking = isWalking,
                facingLeft = facingLeft
            )
        }

        // Foreground vegetation (plants in front of dino for depth)
        PixelNatureBackground(
            modifier = Modifier.fillMaxSize(),
            scrollOffset = bgScroll,
            foregroundOnly = true,
            dinoFeetScreenFrac = dinoFeetFrac
        )

    }
}
