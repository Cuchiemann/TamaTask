package com.cuchieman.tamatask.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.data.model.DinoCollection
import com.cuchieman.tamatask.data.model.DinoSpec
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
private const val WATER_TOP_FRACTION = 0.56f
private const val SHORE_BOTTOM_FRACTION = 0.92f

// Foam color
private val WaterFoam = Color(0xFFB0DDD5)

// Speed: screen-fractions per second
private const val WALK_SPEED = 0.025f

// Idle pause range (ms)
private const val MIN_IDLE_MS = 2000L
private const val MAX_IDLE_MS = 6000L

// World-space X boundaries (fraction of panorama, 0..1)
// Keep away from edges so camera can always keep dino fully on screen
private const val X_MIN = 0.15f
private const val X_MAX = 0.85f

// Panorama ratio — must match PixelNatureBackground.PANORAMA_RATIO
private const val PANORAMA_RATIO = 3.5f

// Y boundaries
private const val Y_MIN = 0.0f
private const val Y_MAX = 1.0f

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PetScreen() {
    val dinoX = remember { Animatable(0.5f) }
    val dinoY = remember { Animatable(0.7f) }
    var isWalking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }

    // Dino collection — unlocked first
    val dinos = remember { DinoCollection.sorted }
    val pagerState = rememberPagerState(initialPage = 0) { dinos.size }
    var selectedDino by remember { mutableStateOf(dinos.first()) }

    // Update selected dino when pager changes (only if unlocked)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val dino = dinos[page]
            if (dino.unlocked) {
                selectedDino = dino
            }
        }
    }

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

        val dinoHeight = if (isWalking) 110.dp else 110.dp
        val dinoWidth = if (isWalking) 333.dp else 313.dp

        // ── Camera system: dino walks in world, camera follows ──
        val maxScrollFrac = PANORAMA_RATIO - 1f
        val idealCameraFrac = dinoX.value * PANORAMA_RATIO - 0.5f
        val cameraFrac = idealCameraFrac.coerceIn(0f, maxScrollFrac)
        val bgScroll = cameraFrac / maxScrollFrac

        val dinoScreenCenterFrac = dinoX.value * PANORAMA_RATIO - cameraFrac
        val margin = dinoWidth / 2
        val dinoLeftXRaw = screenW * dinoScreenCenterFrac - dinoWidth / 2
        val clampMin = -margin * 0.1f
        val clampMax = screenW - dinoWidth + margin * 0.1f
        val dinoLeftX = if (clampMin <= clampMax) dinoLeftXRaw.coerceIn(clampMin, clampMax) else dinoLeftXRaw

        // ── Position dino by FEET (vertical) ──
        val shoreBottomY = screenH * SHORE_BOTTOM_FRACTION
        val deepFeetY = screenH * 0.65f
        val shoreFeetY = shoreBottomY
        val feetY: Dp = deepFeetY + (shoreFeetY - deepFeetY) * dinoY.value
        val dinoTopY: Dp = feetY - dinoHeight

        val dinoFeetFrac = feetY / screenH

        // ── Submersion ──
        val shoreThreshold = 0.42f
        val maxWaterFracFromBottom = 0.55f

        val waterFracFromBottom = if (dinoY.value >= shoreThreshold) {
            0f
        } else {
            ((shoreThreshold - dinoY.value) / shoreThreshold) * maxWaterFracFromBottom
        }
        val waterFracInDino = 1f - waterFracFromBottom
        val hasSubmersion = waterFracFromBottom > 0.05f

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

        // Background
        PixelNatureBackground(
            modifier = Modifier.fillMaxSize(),
            scrollOffset = bgScroll,
            dinoFeetScreenFrac = dinoFeetFrac
        )

        // Pet name label — shows selected dino's species
        Text(
            text = selectedDino.species,
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

        // Foreground vegetation
        PixelNatureBackground(
            modifier = Modifier.fillMaxSize(),
            scrollOffset = bgScroll,
            foregroundOnly = true,
            dinoFeetScreenFrac = dinoFeetFrac
        )

        // ══════════════════════════════════════════
        // DINO CAROUSEL — bottom, floating over terrain
        // ══════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = (screenW - 80.dp) / 2),
                pageSpacing = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) { page ->
                val dino = dinos[page]
                val isSelected = page == pagerState.currentPage
                DinoCarouselCard(dino = dino, isSelected = isSelected)
            }

            // Page indicator dots
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                dinos.forEachIndexed { index, dino ->
                    val isActive = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (isActive) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) dino.accentColor
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/** Single card in the dino carousel */
@Composable
private fun DinoCarouselCard(dino: DinoSpec, isSelected: Boolean) {
    val context = LocalContext.current

    // Load thumbnail: first frame of idle strip (if unlocked and has sprites)
    val thumbnail: ImageBitmap? = remember(dino.id) {
        if (dino.unlocked && dino.idleStripRes != null) {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val resId = context.resources.getIdentifier(dino.idleStripRes, "drawable", context.packageName)
            if (resId != 0) {
                val strip = BitmapFactory.decodeResource(context.resources, resId, options)
                if (strip != null) {
                    // Extract first frame (717px wide for spino)
                    val frameW = strip.width / 36  // assume 36 frames
                    val frame = Bitmap.createBitmap(strip, 0, 0, frameW, strip.height)
                    strip.recycle()
                    frame.asImageBitmap()
                } else null
            } else null
        } else null
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .then(
                if (isSelected) Modifier.border(2.dp, dino.accentColor, RoundedCornerShape(8.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dino.unlocked && thumbnail != null) {
            // Show dino thumbnail
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = dino.species,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None
                )
                Text(
                    text = dino.name,
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = if (isSelected) dino.accentColor else Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        } else {
            // Locked — show lock icon and species
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Pixel art lock
                Canvas(modifier = Modifier.size(32.dp)) {
                    val b = size.width / 10f
                    val lockColor = dino.accentColor.copy(alpha = 0.5f)
                    // Lock arch
                    drawRect(lockColor, Offset(2*b, 0f), Size(b, 4*b))
                    drawRect(lockColor, Offset(7*b, 0f), Size(b, 4*b))
                    drawRect(lockColor, Offset(2*b, 0f), Size(6*b, b))
                    // Lock body
                    drawRect(lockColor, Offset(b, 4*b), Size(8*b, 6*b))
                    // Keyhole
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(4*b, 6*b), Size(2*b, 2*b))
                }
                Text(
                    text = "???",
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
