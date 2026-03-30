package com.cuchieman.tamatask.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cuchieman.tamatask.R
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.data.model.DinoCollection
import com.cuchieman.tamatask.data.model.DinoSpec
import com.cuchieman.tamatask.ui.components.PixelButton
import com.cuchieman.tamatask.ui.theme.PixelFontFamily
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaRed
import com.cuchieman.tamatask.ui.theme.TamaYellow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ── Game phases ──
private enum class MinigamePhase {
    MAP, SITE_DETAIL, EXCAVATION, SUCCESS, FAILURE
}

// ── Dig site data (position on map as fractions 0..1) ──
private data class DigSite(
    val dino: DinoSpec,
    val mapX: Float,
    val mapY: Float
)

// ── Map colors ──
private val ParchmentLight = Color(0xFFE8D5A3)
private val ParchmentDark = Color(0xFFB8956A)
private val ParchmentEdge = Color(0xFF8B6914)
private val ParchmentShadow = Color(0xFF6B4E0A)
private val MapGreen1 = Color(0xFF5A8F3C)
private val MapGreen2 = Color(0xFF7AB356)
private val MapGreen3 = Color(0xFF4A7A2E)
private val MapWater = Color(0xFF4A90B8)
private val MapWaterLight = Color(0xFF6BB5D8)
private val MapSand = Color(0xFFD4B87A)
private val MapMountain = Color(0xFF8B7355)
private val MapMountainDark = Color(0xFF6B5340)
private val PinRed = Color(0xFFC0392B)
private val PinRedDark = Color(0xFF8B1A1A)

// ── Sky colors ──
private val SkyTop = Color(0xFF5BC8F5)
private val SkyBottom = Color(0xFFA8E6FF)
private val CloudWhite = Color(0xFFFFFFFF)
private val CloudShadow = Color(0xFFD4EEFF)

// ── Rock colors ──
private val RockDark = Color(0xFF3A3632)
private val RockMid = Color(0xFF5A5550)
private val RockLight = Color(0xFF6E6862)
private val RockHighlight = Color(0xFF807A72)
private val RockShadow = Color(0xFF2A2622)

// ── Sand/desert ──
private val SandLight = Color(0xFFE8D5A0)
private val SandDark = Color(0xFFD4B878)
private val SandShadow = Color(0xFFC0A060)

// ── Fossil/bone colors ──
private val BoneLight = Color(0xFFD4B896)
private val BoneMid = Color(0xFFB89878)
private val BoneDark = Color(0xFF8B7355)
private val BoneShadow = Color(0xFF6B5340)

// ── Marker ──
private val MarkerGreen = Color(0xFF27AE60)
private val MarkerGreenDark = Color(0xFF1E8449)

private data class HitPoint(
    val xFrac: Float,
    val yFrac: Float,
    val number: Int,
    val hit: Boolean = false
)

@Composable
fun MinigameScreen(onBack: () -> Unit, onShowBottomBar: (Boolean) -> Unit = {}) {
    var phase by remember { mutableStateOf(MinigamePhase.MAP) }
    var selectedSite by remember { mutableStateOf<DigSite?>(null) }
    var selectedSiteIndex by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }

    // Only show bottom bar on MAP and SITE_DETAIL
    LaunchedEffect(phase) {
        onShowBottomBar(phase == MinigamePhase.MAP || phase == MinigamePhase.SITE_DETAIL)
    }

    // Handle back button: return to map from any phase except MAP
    BackHandler(enabled = phase != MinigamePhase.MAP) {
        phase = MinigamePhase.MAP
    }

    // Build dig sites from all dinos
    val digSites = remember {
        val positions = listOf(
            0.22f to 0.60f,  // Spinosaurus (desierto izq)
            0.22f to 0.28f,  // Stegosaurus (bosque izq)
            0.65f to 0.33f,  // Velociraptor (pie de montaña)
            0.50f to 0.15f,  // Triceratops (pie montaña)
            0.43f to 0.87f,  // Pteranodon (abajo río)
            0.50f to 0.72f,  // T-Rex (junto al río)
        )
        DinoCollection.all.mapIndexed { i, dino ->
            val (x, y) = positions.getOrElse(i) { (0.5f to 0.5f) }
            DigSite(dino, x, y)
        }
    }

    when (phase) {
        MinigamePhase.MAP -> MapPhase(
            digSites = digSites,
            onSiteSelected = { site ->
                selectedSite = site
                selectedSiteIndex = digSites.indexOf(site)
                phase = MinigamePhase.SITE_DETAIL
            }
        )

        MinigamePhase.SITE_DETAIL -> SiteDetailPhase(
            site = selectedSite!!,
            onSearch = { phase = MinigamePhase.EXCAVATION },
            onBack = { phase = MinigamePhase.MAP }
        )

        MinigamePhase.EXCAVATION -> ExcavationPhase(
            round = round,
            hitCount = 5 + selectedSiteIndex,
            tapTimeLimitMs = 10000L / (5 + selectedSiteIndex),
            onComplete = { phase = MinigamePhase.SUCCESS },
            onTimeUp = { phase = MinigamePhase.FAILURE }
        )

        MinigamePhase.SUCCESS -> SuccessPhase(
            site = selectedSite!!,
            onContinue = {
                round++
                phase = MinigamePhase.MAP
            }
        )

        MinigamePhase.FAILURE -> FailurePhase(
            onRetry = { phase = MinigamePhase.EXCAVATION },
            onBack = { phase = MinigamePhase.MAP }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 1: MAP
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MapPhase(
    digSites: List<DigSite>,
    onSiteSelected: (DigSite) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val mapTerrainSprite = ImageBitmap.imageResource(R.drawable.map_terrain)
    val mapPinSprite = ImageBitmap.imageResource(R.drawable.map_pin)
    // Next dino to unlock (first locked in list order)
    val nextToUnlockId = digSites.firstOrNull { !it.dino.unlocked }?.dino?.id

    // Debug: unlock all sites
    var unlockAll by remember { mutableStateOf(false) }

    // Pin levitation animation
    val pinTransition = rememberInfiniteTransition(label = "pinFloat")
    val pinFloatOffset by pinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pinFloat"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9BC9F)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(digSites, nextToUnlockId, unlockAll) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val scrollMarginX = 0f
                            val scrollMarginTop = 0f
                            val scrollMarginBottom = 0f
                            val mapW = w - scrollMarginX * 2
                            val mapH = h - scrollMarginTop - scrollMarginBottom

                            val pinTouchRadius = w * 0.06f

                            for (site in digSites) {
                                // Only allow tapping unlocked sites and the next to unlock (unless unlockAll)
                                if (!unlockAll && !site.dino.unlocked && site.dino.id != nextToUnlockId) continue

                                val px = scrollMarginX + site.mapX * mapW
                                val py = scrollMarginTop + site.mapY * mapH
                                val dist = sqrt(
                                    (tapOffset.x - px) * (tapOffset.x - px) +
                                            (tapOffset.y - py) * (tapOffset.y - py)
                                )
                                if (dist < pinTouchRadius) {
                                    onSiteSelected(site)
                                    break
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw map filling the entire canvas
                drawImage(
                    image = mapTerrainSprite,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(mapTerrainSprite.width, mapTerrainSprite.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(w.toInt(), h.toInt()),
                    filterQuality = FilterQuality.None
                )

                // Map area = full canvas (map has its own border)
                val marginX = 0f
                val marginTop = 0f
                val marginBottom = 0f
                val mapW = w
                val mapH = h

                // (map already drawn above, filling entire canvas)

                // Draw pins with names (levitating)
                val floatAmplitude = w * 0.018f // how far pins float up/down
                digSites.forEachIndexed { index, site ->
                    val px = marginX + site.mapX * mapW
                    val py = marginTop + site.mapY * mapH
                    val pinSize = (w * 0.105f).toInt()

                    // All pins float together in sync
                    val floatY = sin(pinFloatOffset * Math.PI.toFloat()) * floatAmplitude

                    drawImage(
                        image = mapPinSprite,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(mapPinSprite.width, mapPinSprite.height),
                        dstOffset = IntOffset(
                            (px - pinSize / 2f).toInt(),
                            (py - pinSize + floatY).toInt()
                        ),
                        dstSize = IntSize(pinSize, pinSize),
                        filterQuality = FilterQuality.None
                    )

                    // Label: unlocked/next → species name, rest → "???"
                    val isNext = site.dino.id == nextToUnlockId
                    val label = if (site.dino.unlocked || isNext) site.dino.species else "???"
                    val outlineStyle = TextStyle(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    val nameStyle = outlineStyle.copy(color = Color.White)
                    val nameLayout = textMeasurer.measure(label, nameStyle)
                    val outlineLayout = textMeasurer.measure(label, outlineStyle)
                    val textX = px - nameLayout.size.width / 2f
                    val textY = py + pinSize * 0.15f
                    // Draw black outline (4 directions)
                    val o = 1.5f
                    for ((dx, dy) in listOf(-o to 0f, o to 0f, 0f to -o, 0f to o)) {
                        drawText(outlineLayout, topLeft = Offset(textX + dx, textY + dy))
                    }
                    // Draw white text on top
                    drawText(nameLayout, topLeft = Offset(textX, textY))
                }
            }
        }

        // Debug: unlock all checkbox
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "All",
                style = TextStyle(
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp,
                    color = Color.White
                )
            )
            Checkbox(
                checked = unlockAll,
                onCheckedChange = { unlockAll = it },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 2: SITE DETAIL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SiteDetailPhase(
    site: DigSite,
    onSearch: () -> Unit,
    onBack: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val mapPinSprite = ImageBitmap.imageResource(R.drawable.map_pin)

    // Site terrain sprites mapped by dino index
    val siteTerrainSprites = listOf(
        ImageBitmap.imageResource(R.drawable.site_desert),        // 0: Spinosaurus
        ImageBitmap.imageResource(R.drawable.site_forest),        // 1: Stegosaurus
        ImageBitmap.imageResource(R.drawable.site_meadow),         // 2: Velociraptor (prado)
        ImageBitmap.imageResource(R.drawable.site_forest_river),  // 3: Triceratops (bosque río)
        ImageBitmap.imageResource(R.drawable.site_riverbank),     // 4: Pteranodon (cañón árido)
        ImageBitmap.imageResource(R.drawable.site_clearing),      // 5: T-Rex (árido seco)
    )
    val siteIndex = DinoCollection.all.indexOfFirst { it.id == site.dino.id }.coerceIn(0, siteTerrainSprites.size - 1)
    val siteSprite = siteTerrainSprites[siteIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3E2E1A)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Parchment with zoomed map
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures { onBack() }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val borderWidth = w * 0.03f

                    // Draw site terrain sprite inside border area
                    drawImage(
                        image = siteSprite,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(siteSprite.width, siteSprite.height),
                        dstOffset = IntOffset(borderWidth.toInt(), borderWidth.toInt()),
                        dstSize = IntSize((w - borderWidth * 2).toInt(), (h - borderWidth * 2).toInt()),
                        filterQuality = FilterQuality.None
                    )

                    // Pixel art border frame
                    val bc = Color(0xFF5C3A1E) // dark wood brown
                    val bh = Color(0xFF8B6914) // highlight
                    // Top
                    drawRect(bc, Offset.Zero, Size(w, borderWidth))
                    // Bottom
                    drawRect(bc, Offset(0f, h - borderWidth), Size(w, borderWidth))
                    // Left
                    drawRect(bc, Offset.Zero, Size(borderWidth, h))
                    // Right
                    drawRect(bc, Offset(w - borderWidth, 0f), Size(borderWidth, h))
                    // Inner highlight lines
                    val ib = borderWidth * 0.4f
                    drawRect(bh, Offset(borderWidth - ib, borderWidth - ib), Size(w - (borderWidth - ib) * 2, ib))
                    drawRect(bh, Offset(borderWidth - ib, borderWidth - ib), Size(ib, h - (borderWidth - ib) * 2))
                    // Corner accents (pixel squares)
                    val cs = borderWidth * 0.8f
                    val cd = Color(0xFF3E2510)
                    drawRect(cd, Offset(0f, 0f), Size(cs, cs))
                    drawRect(cd, Offset(w - cs, 0f), Size(cs, cs))
                    drawRect(cd, Offset(0f, h - cs), Size(cs, cs))
                    drawRect(cd, Offset(w - cs, h - cs), Size(cs, cs))

                    // Central pin with label
                    val pinX = w * 0.5f
                    val pinY = h * 0.35f
                    val detailPinSize = (w * 0.07f).toInt()
                    drawImage(
                        image = mapPinSprite,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(mapPinSprite.width, mapPinSprite.height),
                        dstOffset = IntOffset(
                            (pinX - detailPinSize / 2f).toInt(),
                            (pinY - detailPinSize).toInt()
                        ),
                        dstSize = IntSize(detailPinSize, detailPinSize),
                        filterQuality = FilterQuality.None
                    )

                    // Species name below pin
                    val nameStyle = TextStyle(
                        fontFamily = PixelFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    val nameLayout = textMeasurer.measure(site.dino.species, nameStyle)
                    // Background behind text
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(
                            pinX - nameLayout.size.width / 2f - 12f,
                            pinY + w * 0.06f - 4f
                        ),
                        size = Size(
                            nameLayout.size.width + 24f,
                            nameLayout.size.height + 8f
                        ),
                        cornerRadius = CornerRadius(4f)
                    )
                    drawText(
                        textLayoutResult = nameLayout,
                        topLeft = Offset(
                            pinX - nameLayout.size.width / 2f,
                            pinY + w * 0.06f
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // "Buscar" button
                PixelButton(
                    text = "Buscar",
                    onClick = onSearch,
                    bgColor = PinRedDark,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 3: EXCAVATION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ExcavationPhase(
    round: Int,
    hitCount: Int,
    tapTimeLimitMs: Long,
    onComplete: () -> Unit,
    onTimeUp: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val hitPoints = remember(round) {
        val rng = Random(round * 17 + 42)
        val points = mutableStateListOf<HitPoint>()
        val count = hitCount
        for (i in 0 until count) {
            // Distribute on rock face
            val angle = i.toFloat() / count * 6.28f + rng.nextFloat() * 0.5f
            val dist = 0.2f + rng.nextFloat() * 0.45f
            points.add(
                HitPoint(
                    xFrac = cos(angle) * dist,
                    yFrac = sin(angle) * dist * 0.7f, // flatten vertically
                    number = i + 1
                )
            )
        }
        points
    }

    var hitsCount by remember(round) { mutableIntStateOf(0) }
    var nextExpected by remember(round) { mutableIntStateOf(1) }
    var completed by remember(round) { mutableStateOf(false) }

    // Per-tap timer: resets every time you hit the correct number
    var timeLeft by remember(round) { mutableStateOf(tapTimeLimitMs) }
    var tapStartNanos by remember(round) { mutableStateOf(0L) }

    LaunchedEffect(round, nextExpected) {
        tapStartNanos = withFrameNanos { it }
        timeLeft = tapTimeLimitMs
        while (timeLeft > 0 && !completed) {
            val now = withFrameNanos { it }
            val elapsed = (now - tapStartNanos) / 1_000_000
            timeLeft = (tapTimeLimitMs - elapsed).coerceAtLeast(0)
            if (timeLeft <= 0) {
                onTimeUp()
                return@LaunchedEffect
            }
        }
    }

    val crackProgress = remember { Animatable(0f) }

    // Cloud animation
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudDrift"
    )

    LaunchedEffect(hitsCount) {
        if (hitsCount == hitPoints.size && hitPoints.isNotEmpty()) {
            completed = true
            crackProgress.animateTo(1f, tween(500, easing = LinearEasing))
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(round, nextExpected) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val rockCx = w * 0.5f
                            val rockCy = h * 0.50f
                            val rockRadius = w * 0.35f
                            val frac = (timeLeft / tapTimeLimitMs.toFloat()).coerceIn(0f, 1f)
                            val hitRadius = rockRadius * 0.24f * (0.4f + 0.6f * frac)

                            for (i in hitPoints.indices) {
                                if (!hitPoints[i].hit && hitPoints[i].number == nextExpected) {
                                    val px = rockCx + hitPoints[i].xFrac * rockRadius
                                    val py = rockCy + hitPoints[i].yFrac * rockRadius
                                    val dist = sqrt(
                                        (tapOffset.x - px) * (tapOffset.x - px) +
                                                (tapOffset.y - py) * (tapOffset.y - py)
                                    )
                                    if (dist < hitRadius) {
                                        hitPoints[i] = hitPoints[i].copy(hit = true)
                                        hitsCount++
                                        nextExpected++
                                        break
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Clouds
                drawPixelClouds(w, h, cloudOffset)

                // Rock pile
                val rockCx = w * 0.5f
                val rockCy = h * 0.50f
                val rockRadius = w * 0.35f
                drawRockPile(rockCx, rockCy, rockRadius)

                // Hit point markers – only show the current target number
                val timerFrac = (timeLeft / tapTimeLimitMs.toFloat()).coerceIn(0f, 1f)
                val markerBase = rockRadius * 0.24f
                val markerSize = markerBase * (0.4f + 0.6f * timerFrac) // shrinks to 40% as time runs out
                hitPoints.forEach { point ->
                    val px = rockCx + point.xFrac * rockRadius
                    val py = rockCy + point.yFrac * rockRadius
                    if (point.hit) {
                        drawCrackMark(px, py, rockRadius * 0.06f)
                    } else if (point.number == nextExpected) {
                        drawNumberedMarker(px, py, markerSize, point.number, textMeasurer)
                    }
                }

                // Timer bar at top
                val timerFraction = timeLeft / tapTimeLimitMs.toFloat()
                val barMargin = w * 0.08f
                val barY = h * 0.05f
                val barH = h * 0.025f
                val barW = w - barMargin * 2

                // Background bar
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(barMargin, barY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barH / 2)
                )
                // Fill bar (green → yellow → red)
                val timerColor = when {
                    timerFraction > 0.5f -> MarkerGreen
                    timerFraction > 0.25f -> TamaYellow
                    else -> TamaRed
                }
                drawRoundRect(
                    color = timerColor,
                    topLeft = Offset(barMargin, barY),
                    size = Size(barW * timerFraction, barH),
                    cornerRadius = CornerRadius(barH / 2)
                )

                // Timer text
                val seconds = (timeLeft / 1000f)
                val timerText = String.format("%.1f", seconds)
                val timerStyle = TextStyle(
                    fontFamily = PixelFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                val timerLayout = textMeasurer.measure(timerText, timerStyle)
                drawText(
                    textLayoutResult = timerLayout,
                    topLeft = Offset(
                        w / 2f - timerLayout.size.width / 2f,
                        barY + barH + 4f
                    )
                )
            }
        }

        // Leather inventory bar
        LeatherInventoryBar()
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 4: SUCCESS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SuccessPhase(
    site: DigSite,
    onContinue: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "successClouds")
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudDrift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // "Lo conseguiste" title
            Text(
                text = "Lo conseguiste",
                fontFamily = PixelFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fossil display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Clouds
                    drawPixelClouds(w, h * 0.4f, cloudOffset)

                    // Desert ground
                    val groundY = h * 0.65f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(SandLight, SandDark, SandShadow),
                            startY = groundY,
                            endY = h
                        ),
                        topLeft = Offset(0f, groundY),
                        size = Size(w, h - groundY)
                    )

                    // Sand dunes/texture
                    val duneColor = SandShadow.copy(alpha = 0.3f)
                    for (i in 0..5) {
                        val dx = w * (i * 0.18f + 0.05f)
                        val dy = groundY + 20f + i * 15f
                        drawOval(
                            color = duneColor,
                            topLeft = Offset(dx - 40f, dy),
                            size = Size(80f, 8f)
                        )
                    }

                    // Cactus/desert detail
                    drawDesertDetails(w, h, groundY)

                    // Draw fossil skeleton
                    val fossilCx = w * 0.5f
                    val fossilCy = groundY - h * 0.12f
                    val fossilSize = w * 0.40f
                    drawFossilSkeleton(fossilCx, fossilCy, fossilSize, site.dino.species)
                }
            }

            // "Continuar" button
            PixelButton(
                text = "Continuar",
                onClick = onContinue,
                bgColor = PinRedDark,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 5: FAILURE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FailurePhase(
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1A0A), Color(0xFF1A0A00)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tiempo\nagotado!",
                fontFamily = PixelFontFamily,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TamaRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "El fosil se ha derrumbado...",
                fontFamily = PixelFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            PixelButton(
                text = "Reintentar",
                onClick = onRetry,
                bgColor = TamaOrange,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PixelButton(
                text = "Volver al mapa",
                onClick = onBack,
                bgColor = PinRedDark,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(48.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// LEATHER INVENTORY BAR
// ═══════════════════════════════════════════════════════════════

private val LeatherLight = Color(0xFFB8764F)
private val LeatherMid = Color(0xFF9A6240)
private val LeatherDark = Color(0xFF7A4E30)
private val LeatherEdge = Color(0xFF5A3820)
private val LeatherStitch = Color(0xFFD4A878)

@Composable
private fun LeatherInventoryBar() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .background(LeatherDark)
            .height(90.dp)
    ) {
        val w = size.width
        val h = size.height
        val px = w / 60f

        // Main leather body
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(LeatherDark, LeatherMid, LeatherLight, LeatherMid, LeatherDark)
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )

        // Leather grain texture (subtle horizontal lines)
        val grainColor = LeatherDark.copy(alpha = 0.15f)
        for (i in 0..(h / 3f).toInt()) {
            val y = i * 3f
            drawLine(grainColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
        }

        // Top edge - triangular leather flap pattern
        val flapH = px * 2f
        for (col in 0..(w / (px * 2)).toInt()) {
            val x = col * px * 2
            val trianglePath = Path().apply {
                moveTo(x, flapH)
                lineTo(x + px, 0f)
                lineTo(x + px * 2, flapH)
                close()
            }
            drawPath(trianglePath, LeatherEdge)
            // Lighter inner triangle
            val innerPath = Path().apply {
                moveTo(x + px * 0.3f, flapH - px * 0.2f)
                lineTo(x + px, px * 0.5f)
                lineTo(x + px * 1.7f, flapH - px * 0.2f)
                close()
            }
            drawPath(innerPath, LeatherDark)
        }

        // Stitch line below flaps
        val stitchY = flapH + px * 1.2f
        val stitchLen = px * 1.5f
        val stitchGap = px * 0.8f
        var sx = px
        while (sx < w - px) {
            drawLine(
                LeatherStitch,
                Offset(sx, stitchY),
                Offset(sx + stitchLen, stitchY),
                strokeWidth = px * 0.25f
            )
            sx += stitchLen + stitchGap
        }

        // Inventory slot dividers (5 slots)
        val slotCount = 5
        val slotMargin = px * 2f
        val slotAreaTop = stitchY + px * 1f
        val slotAreaBottom = h - px * 1f
        val slotW = (w - slotMargin * 2) / slotCount

        for (i in 0 until slotCount) {
            val slotX = slotMargin + i * slotW
            // Slot background (darker inset) - flush to bottom
            drawRect(
                color = LeatherEdge.copy(alpha = 0.4f),
                topLeft = Offset(slotX + px * 0.5f, slotAreaTop),
                size = Size(slotW - px, slotAreaBottom - slotAreaTop)
            )
            // Slot inner highlight
            drawRect(
                color = LeatherLight.copy(alpha = 0.15f),
                topLeft = Offset(slotX + px * 0.8f, slotAreaTop + px * 0.3f),
                size = Size(slotW - px * 1.6f, (slotAreaBottom - slotAreaTop) * 0.5f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DRAWING HELPERS
// ═══════════════════════════════════════════════════════════════

// ── Parchment scroll ──
private fun DrawScope.drawParchmentScroll(w: Float, h: Float) {
    val rollSize = w * 0.06f
    val borderWidth = w * 0.015f

    // Main parchment body
    drawRoundRect(
        color = ParchmentLight,
        topLeft = Offset(borderWidth, rollSize),
        size = Size(w - borderWidth * 2, h - rollSize * 2),
        cornerRadius = CornerRadius(4f)
    )

    // Parchment texture (subtle horizontal lines)
    val lineColor = ParchmentDark.copy(alpha = 0.15f)
    for (i in 0..(h / 8f).toInt()) {
        val y = rollSize + i * 8f
        if (y < h - rollSize) {
            drawLine(lineColor, Offset(borderWidth + 4f, y), Offset(w - borderWidth - 4f, y), 0.5f)
        }
    }

    // Top scroll roll
    drawRoundRect(
        color = ParchmentDark,
        topLeft = Offset(0f, 0f),
        size = Size(w, rollSize * 1.5f),
        cornerRadius = CornerRadius(rollSize * 0.6f)
    )
    drawRoundRect(
        color = ParchmentLight,
        topLeft = Offset(borderWidth, borderWidth),
        size = Size(w - borderWidth * 2, rollSize * 1.2f),
        cornerRadius = CornerRadius(rollSize * 0.4f)
    )
    // Roll shadow line
    drawLine(
        ParchmentShadow.copy(alpha = 0.4f),
        Offset(borderWidth * 2, rollSize * 1.3f),
        Offset(w - borderWidth * 2, rollSize * 1.3f),
        strokeWidth = 2f
    )

    // Bottom scroll roll
    drawRoundRect(
        color = ParchmentDark,
        topLeft = Offset(0f, h - rollSize * 1.5f),
        size = Size(w, rollSize * 1.5f),
        cornerRadius = CornerRadius(rollSize * 0.6f)
    )
    drawRoundRect(
        color = ParchmentLight,
        topLeft = Offset(borderWidth, h - rollSize * 1.3f),
        size = Size(w - borderWidth * 2, rollSize * 1.2f),
        cornerRadius = CornerRadius(rollSize * 0.4f)
    )
    // Roll shadow line
    drawLine(
        ParchmentShadow.copy(alpha = 0.4f),
        Offset(borderWidth * 2, h - rollSize * 1.3f),
        Offset(w - borderWidth * 2, h - rollSize * 1.3f),
        strokeWidth = 2f
    )

    // Side borders
    drawRect(
        color = ParchmentEdge.copy(alpha = 0.3f),
        topLeft = Offset(0f, rollSize),
        size = Size(borderWidth, h - rollSize * 2)
    )
    drawRect(
        color = ParchmentEdge.copy(alpha = 0.3f),
        topLeft = Offset(w - borderWidth, rollSize),
        size = Size(borderWidth, h - rollSize * 2)
    )
}

// ── Map terrain ──
private fun DrawScope.drawMapTerrain(ox: Float, oy: Float, mapW: Float, mapH: Float) {
    // Base green
    drawRect(MapGreen1, Offset(ox, oy), Size(mapW, mapH))

    // Varied terrain patches
    val rng = Random(123)
    for (i in 0..30) {
        val px = ox + rng.nextFloat() * mapW
        val py = oy + rng.nextFloat() * mapH
        val patchSize = mapW * (0.08f + rng.nextFloat() * 0.15f)
        val color = listOf(MapGreen2, MapGreen3, MapGreen1).random(rng)
        drawOval(color, Offset(px - patchSize / 2, py - patchSize / 2), Size(patchSize, patchSize * 0.7f))
    }

    // River (winding path from top to bottom-left)
    val riverWidth = mapW * 0.045f
    val riverPoints = listOf(
        Offset(ox + mapW * 0.55f, oy),
        Offset(ox + mapW * 0.50f, oy + mapH * 0.15f),
        Offset(ox + mapW * 0.42f, oy + mapH * 0.30f),
        Offset(ox + mapW * 0.38f, oy + mapH * 0.45f),
        Offset(ox + mapW * 0.30f, oy + mapH * 0.55f),
        Offset(ox + mapW * 0.22f, oy + mapH * 0.70f),
        Offset(ox + mapW * 0.18f, oy + mapH * 0.85f),
        Offset(ox + mapW * 0.15f, oy + mapH),
    )
    // Draw river as thick lines between points
    for (i in 0 until riverPoints.size - 1) {
        drawLine(MapWater, riverPoints[i], riverPoints[i + 1], strokeWidth = riverWidth)
        // Lighter center
        drawLine(MapWaterLight, riverPoints[i], riverPoints[i + 1], strokeWidth = riverWidth * 0.4f)
    }

    // Lake
    drawOval(
        MapWater,
        Offset(ox + mapW * 0.10f, oy + mapH * 0.78f),
        Size(mapW * 0.25f, mapH * 0.15f)
    )
    drawOval(
        MapWaterLight,
        Offset(ox + mapW * 0.13f, oy + mapH * 0.80f),
        Size(mapW * 0.15f, mapH * 0.08f)
    )

    // Mountains (top-right area)
    for (i in 0..3) {
        val mx = ox + mapW * (0.65f + i * 0.08f)
        val my = oy + mapH * (0.05f + i * 0.04f)
        val mSize = mapW * 0.10f
        val trianglePath = Path().apply {
            moveTo(mx, my)
            lineTo(mx - mSize * 0.5f, my + mSize * 0.7f)
            lineTo(mx + mSize * 0.5f, my + mSize * 0.7f)
            close()
        }
        drawPath(trianglePath, MapMountainDark)
        // Snow cap
        val snowPath = Path().apply {
            moveTo(mx, my)
            lineTo(mx - mSize * 0.15f, my + mSize * 0.2f)
            lineTo(mx + mSize * 0.15f, my + mSize * 0.2f)
            close()
        }
        drawPath(snowPath, Color.White.copy(alpha = 0.7f))
    }

    // Sandy area (bottom-right)
    drawOval(
        MapSand,
        Offset(ox + mapW * 0.55f, oy + mapH * 0.70f),
        Size(mapW * 0.40f, mapH * 0.25f)
    )
    drawOval(
        MapSand.copy(alpha = 0.7f),
        Offset(ox + mapW * 0.60f, oy + mapH * 0.60f),
        Size(mapW * 0.30f, mapH * 0.20f)
    )
}

// (drawZoomedTerrain removed — replaced by site terrain sprites)

// ── Map pin ──
private fun DrawScope.drawMapPin(x: Float, y: Float, size: Float) {
    // Pin shadow
    drawOval(
        Color.Black.copy(alpha = 0.3f),
        Offset(x - size * 0.5f, y + size * 0.8f),
        Size(size, size * 0.3f)
    )

    // Pin body (teardrop shape)
    val pinPath = Path().apply {
        moveTo(x, y + size * 1.0f) // bottom point
        cubicTo(
            x - size * 0.3f, y + size * 0.3f,
            x - size * 0.8f, y - size * 0.2f,
            x, y - size * 0.9f
        )
        cubicTo(
            x + size * 0.8f, y - size * 0.2f,
            x + size * 0.3f, y + size * 0.3f,
            x, y + size * 1.0f
        )
    }
    drawPath(pinPath, PinRed)

    // Pin highlight
    drawCircle(
        PinRedDark,
        radius = size * 0.3f,
        center = Offset(x, y - size * 0.15f)
    )
    // Inner white dot
    drawCircle(
        Color.White.copy(alpha = 0.8f),
        radius = size * 0.15f,
        center = Offset(x, y - size * 0.15f)
    )
}

// ── Pixel clouds ──
private fun DrawScope.drawPixelClouds(w: Float, h: Float, offset: Float) {
    val blk = w / 50f

    data class CloudDef(val xBase: Float, val y: Float, val scale: Float)

    val clouds = listOf(
        CloudDef(0.15f, 0.08f, 1.2f),
        CloudDef(0.55f, 0.05f, 1.0f),
        CloudDef(0.80f, 0.12f, 0.8f),
        CloudDef(0.35f, 0.18f, 0.9f),
    )

    clouds.forEach { cloud ->
        val cx = ((cloud.xBase + offset * 0.3f) % 1.3f - 0.15f) * w
        val cy = cloud.y * h
        val s = cloud.scale

        // Cloud body (overlapping pixel rectangles)
        val cloudColor = CloudWhite
        val shadowColor = CloudShadow

        // Bottom row (wide)
        drawRoundRect(
            cloudColor,
            Offset(cx - blk * 4 * s, cy),
            Size(blk * 8 * s, blk * 3 * s),
            CornerRadius(blk)
        )
        // Middle bump
        drawRoundRect(
            cloudColor,
            Offset(cx - blk * 2 * s, cy - blk * 2 * s),
            Size(blk * 5 * s, blk * 3 * s),
            CornerRadius(blk)
        )
        // Top bump
        drawRoundRect(
            cloudColor,
            Offset(cx - blk * 0.5f * s, cy - blk * 3.5f * s),
            Size(blk * 3 * s, blk * 2.5f * s),
            CornerRadius(blk)
        )
        // Left bump
        drawRoundRect(
            cloudColor,
            Offset(cx - blk * 5 * s, cy - blk * 1f * s),
            Size(blk * 3 * s, blk * 2.5f * s),
            CornerRadius(blk)
        )

        // Shadow underneath
        drawRoundRect(
            shadowColor.copy(alpha = 0.3f),
            Offset(cx - blk * 3.5f * s, cy + blk * 1.5f * s),
            Size(blk * 7 * s, blk * 1f * s),
            CornerRadius(blk * 0.5f)
        )
    }
}

// ── Rock pile for excavation ──
private fun DrawScope.drawRockPile(cx: Float, cy: Float, radius: Float) {
    // Ground shadow
    drawOval(
        Color.Black.copy(alpha = 0.35f),
        Offset(cx - radius * 1.15f, cy + radius * 0.55f),
        Size(radius * 2.3f, radius * 0.5f)
    )

    // Individual angular rocks built with Path shapes
    // Each rock is a polygon with irregular edges

    // ── Bottom layer (large base rocks) ──

    // Bottom-left large rock
    val rock1 = Path().apply {
        moveTo(cx - radius * 1.0f, cy + radius * 0.5f)
        lineTo(cx - radius * 0.95f, cy + radius * 0.1f)
        lineTo(cx - radius * 0.7f, cy - radius * 0.05f)
        lineTo(cx - radius * 0.35f, cy - radius * 0.1f)
        lineTo(cx - radius * 0.15f, cy + radius * 0.05f)
        lineTo(cx - radius * 0.1f, cy + radius * 0.5f)
        close()
    }
    drawPath(rock1, RockDark)
    // Edge highlight
    drawLine(RockLight, Offset(cx - radius * 0.95f, cy + radius * 0.1f), Offset(cx - radius * 0.35f, cy - radius * 0.1f), strokeWidth = radius * 0.02f)

    // Bottom-right large rock
    val rock2 = Path().apply {
        moveTo(cx - radius * 0.2f, cy + radius * 0.5f)
        lineTo(cx - radius * 0.25f, cy + radius * 0.05f)
        lineTo(cx - radius * 0.05f, cy - radius * 0.15f)
        lineTo(cx + radius * 0.4f, cy - radius * 0.1f)
        lineTo(cx + radius * 0.75f, cy + radius * 0.0f)
        lineTo(cx + radius * 0.9f, cy + radius * 0.15f)
        lineTo(cx + radius * 0.95f, cy + radius * 0.5f)
        close()
    }
    drawPath(rock2, Color(0xFF454038))
    drawLine(RockHighlight, Offset(cx - radius * 0.05f, cy - radius * 0.15f), Offset(cx + radius * 0.75f, cy), strokeWidth = radius * 0.02f)

    // Bottom-center connecting rock
    val rock3 = Path().apply {
        moveTo(cx - radius * 0.3f, cy + radius * 0.5f)
        lineTo(cx - radius * 0.2f, cy + radius * 0.15f)
        lineTo(cx + radius * 0.05f, cy + radius * 0.0f)
        lineTo(cx + radius * 0.25f, cy + radius * 0.1f)
        lineTo(cx + radius * 0.2f, cy + radius * 0.5f)
        close()
    }
    drawPath(rock3, RockMid)

    // ── Middle layer ──

    // Mid-left angular rock
    val rock4 = Path().apply {
        moveTo(cx - radius * 0.8f, cy + radius * 0.1f)
        lineTo(cx - radius * 0.75f, cy - radius * 0.25f)
        lineTo(cx - radius * 0.5f, cy - radius * 0.45f)
        lineTo(cx - radius * 0.2f, cy - radius * 0.4f)
        lineTo(cx - radius * 0.05f, cy - radius * 0.2f)
        lineTo(cx - radius * 0.15f, cy + radius * 0.05f)
        close()
    }
    drawPath(rock4, Color(0xFF4E4840))
    drawLine(RockHighlight, Offset(cx - radius * 0.75f, cy - radius * 0.25f), Offset(cx - radius * 0.2f, cy - radius * 0.4f), strokeWidth = radius * 0.025f)

    // Mid-right angular rock
    val rock5 = Path().apply {
        moveTo(cx - radius * 0.1f, cy - radius * 0.15f)
        lineTo(cx + radius * 0.05f, cy - radius * 0.45f)
        lineTo(cx + radius * 0.35f, cy - radius * 0.5f)
        lineTo(cx + radius * 0.65f, cy - radius * 0.35f)
        lineTo(cx + radius * 0.7f, cy - radius * 0.1f)
        lineTo(cx + radius * 0.5f, cy + radius * 0.05f)
        lineTo(cx + radius * 0.1f, cy + radius * 0.0f)
        close()
    }
    drawPath(rock5, RockDark)
    drawLine(RockLight, Offset(cx + radius * 0.05f, cy - radius * 0.45f), Offset(cx + radius * 0.65f, cy - radius * 0.35f), strokeWidth = radius * 0.02f)

    // Mid-center chunk
    val rock6 = Path().apply {
        moveTo(cx - radius * 0.35f, cy - radius * 0.2f)
        lineTo(cx - radius * 0.15f, cy - radius * 0.5f)
        lineTo(cx + radius * 0.15f, cy - radius * 0.45f)
        lineTo(cx + radius * 0.1f, cy - radius * 0.15f)
        close()
    }
    drawPath(rock6, RockMid)
    drawLine(RockHighlight, Offset(cx - radius * 0.15f, cy - radius * 0.5f), Offset(cx + radius * 0.15f, cy - radius * 0.45f), strokeWidth = radius * 0.015f)

    // ── Top layer (peak rocks) ──

    // Top-left peak
    val rock7 = Path().apply {
        moveTo(cx - radius * 0.45f, cy - radius * 0.4f)
        lineTo(cx - radius * 0.3f, cy - radius * 0.75f)
        lineTo(cx - radius * 0.1f, cy - radius * 0.8f)
        lineTo(cx + radius * 0.0f, cy - radius * 0.6f)
        lineTo(cx - radius * 0.1f, cy - radius * 0.4f)
        close()
    }
    drawPath(rock7, Color(0xFF555048))
    drawLine(Color(0xFF8A8278), Offset(cx - radius * 0.3f, cy - radius * 0.75f), Offset(cx - radius * 0.1f, cy - radius * 0.8f), strokeWidth = radius * 0.025f)

    // Top-right peak
    val rock8 = Path().apply {
        moveTo(cx + radius * 0.0f, cy - radius * 0.55f)
        lineTo(cx + radius * 0.15f, cy - radius * 0.85f)
        lineTo(cx + radius * 0.35f, cy - radius * 0.9f)
        lineTo(cx + radius * 0.45f, cy - radius * 0.7f)
        lineTo(cx + radius * 0.35f, cy - radius * 0.45f)
        close()
    }
    drawPath(rock8, RockDark)
    drawLine(RockLight, Offset(cx + radius * 0.15f, cy - radius * 0.85f), Offset(cx + radius * 0.35f, cy - radius * 0.9f), strokeWidth = radius * 0.02f)

    // Very top small rock
    val rock9 = Path().apply {
        moveTo(cx - radius * 0.05f, cy - radius * 0.75f)
        lineTo(cx + radius * 0.05f, cy - radius * 0.95f)
        lineTo(cx + radius * 0.2f, cy - radius * 0.9f)
        lineTo(cx + radius * 0.15f, cy - radius * 0.7f)
        close()
    }
    drawPath(rock9, Color(0xFF4A4540))
    drawLine(RockHighlight, Offset(cx + radius * 0.05f, cy - radius * 0.95f), Offset(cx + radius * 0.2f, cy - radius * 0.9f), strokeWidth = radius * 0.015f)

    // ── Surface detail: cracks and edge lines ──
    val crackColor = RockShadow.copy(alpha = 0.5f)

    // Horizontal cracks across rocks
    drawLine(crackColor, Offset(cx - radius * 0.6f, cy + radius * 0.08f), Offset(cx - radius * 0.2f, cy + radius * 0.12f), strokeWidth = 1.5f)
    drawLine(crackColor, Offset(cx + radius * 0.1f, cy - radius * 0.08f), Offset(cx + radius * 0.55f, cy - radius * 0.05f), strokeWidth = 1.5f)
    drawLine(crackColor, Offset(cx - radius * 0.4f, cy - radius * 0.35f), Offset(cx - radius * 0.1f, cy - radius * 0.38f), strokeWidth = 1.5f)
    drawLine(crackColor, Offset(cx + radius * 0.0f, cy - radius * 0.6f), Offset(cx + radius * 0.25f, cy - radius * 0.58f), strokeWidth = 1.5f)

    // Diagonal stress lines
    drawLine(crackColor, Offset(cx - radius * 0.5f, cy - radius * 0.1f), Offset(cx - radius * 0.35f, cy + radius * 0.15f), strokeWidth = 1f)
    drawLine(crackColor, Offset(cx + radius * 0.3f, cy - radius * 0.3f), Offset(cx + radius * 0.45f, cy - radius * 0.05f), strokeWidth = 1f)

    // Tiny specks/mineral highlights
    val specks = listOf(
        Offset(cx - radius * 0.5f, cy - radius * 0.15f),
        Offset(cx + radius * 0.2f, cy - radius * 0.3f),
        Offset(cx - radius * 0.1f, cy + radius * 0.2f),
        Offset(cx + radius * 0.4f, cy + radius * 0.1f),
        Offset(cx - radius * 0.3f, cy - radius * 0.55f),
        Offset(cx + radius * 0.1f, cy - radius * 0.7f),
    )
    specks.forEach { pos ->
        drawCircle(RockHighlight.copy(alpha = 0.6f), radius * 0.015f, pos)
    }
}

// ── Numbered circle marker ──
private fun DrawScope.drawNumberedMarker(
    x: Float, y: Float, radius: Float,
    number: Int,
    textMeasurer: TextMeasurer
) {
    // Outer circle (green border)
    drawCircle(
        color = MarkerGreen,
        radius = radius,
        center = Offset(x, y),
        style = Stroke(width = radius * 0.3f)
    )

    // Semi-transparent fill
    drawCircle(
        color = MarkerGreen.copy(alpha = 0.15f),
        radius = radius * 0.75f,
        center = Offset(x, y)
    )

    // Number text
    val style = TextStyle(
        fontFamily = PixelFontFamily,
        fontSize = (radius * 1.1f).toSp(),
        fontWeight = FontWeight.Bold,
        color = MarkerGreen,
        textAlign = TextAlign.Center
    )
    val layout = textMeasurer.measure(number.toString(), style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
    )
}

// ── Crack mark ──
private fun DrawScope.drawCrackMark(x: Float, y: Float, size: Float) {
    val crackColor = Color(0xFF2A2420)
    val lines = listOf(
        Offset(0f, -size * 1.5f),
        Offset(size, -size * 0.5f),
        Offset(size * 1.5f, 0f),
        Offset(size * 0.5f, size),
        Offset(0f, size * 1.5f),
        Offset(-size, size * 0.3f),
        Offset(-size * 1.5f, 0f),
        Offset(-size * 0.5f, -size * 0.8f),
    )
    lines.forEach { offset ->
        drawLine(crackColor, Offset(x, y), Offset(x + offset.x, y + offset.y), strokeWidth = size * 0.3f)
    }
    drawCircle(crackColor, radius = size * 0.5f, center = Offset(x, y))
}

// ── Desert details ──
private fun DrawScope.drawDesertDetails(w: Float, h: Float, groundY: Float) {
    // Cactus silhouette on the right
    val cactusX = w * 0.85f
    val cactusBottom = groundY
    val cactusColor = Color(0xFF8B6B3E)

    // Main trunk
    drawRect(cactusColor, Offset(cactusX - 4f, cactusBottom - 80f), Size(8f, 80f))
    // Left arm
    drawRect(cactusColor, Offset(cactusX - 20f, cactusBottom - 60f), Size(16f, 6f))
    drawRect(cactusColor, Offset(cactusX - 20f, cactusBottom - 70f), Size(6f, 16f))
    // Right arm
    drawRect(cactusColor, Offset(cactusX + 4f, cactusBottom - 50f), Size(14f, 6f))
    drawRect(cactusColor, Offset(cactusX + 14f, cactusBottom - 62f), Size(6f, 18f))

    // Small cactus on left
    val c2x = w * 0.12f
    drawRect(cactusColor.copy(alpha = 0.7f), Offset(c2x - 3f, cactusBottom - 40f), Size(6f, 40f))
    drawRect(cactusColor.copy(alpha = 0.7f), Offset(c2x - 12f, cactusBottom - 30f), Size(9f, 4f))
    drawRect(cactusColor.copy(alpha = 0.7f), Offset(c2x - 12f, cactusBottom - 38f), Size(4f, 12f))
}

// ── Fossil skeleton drawing ──
private fun DrawScope.drawFossilSkeleton(cx: Float, cy: Float, size: Float, species: String) {
    val blk = size / 20f

    // Generic dino skeleton that works for all species
    // Body spine (horizontal backbone)
    val spineY = cy
    val spineStartX = cx - size * 0.45f
    val spineEndX = cx + size * 0.45f

    // Spine
    drawLine(BoneMid, Offset(spineStartX, spineY), Offset(spineEndX, spineY), strokeWidth = blk * 1.5f)
    drawLine(BoneLight, Offset(spineStartX, spineY - blk * 0.3f), Offset(spineEndX, spineY - blk * 0.3f), strokeWidth = blk * 0.5f)

    // Vertebrae bumps
    for (i in 0..12) {
        val vx = spineStartX + (spineEndX - spineStartX) * (i / 12f)
        drawCircle(BoneMid, blk * 0.8f, Offset(vx, spineY))
        drawCircle(BoneLight, blk * 0.4f, Offset(vx, spineY - blk * 0.3f))
    }

    // Ribs (curving down from spine)
    for (i in 2..8) {
        val ribX = spineStartX + (spineEndX - spineStartX) * (i / 12f)
        val ribLen = blk * (3f + (4f - kotlin.math.abs(i - 5f)) * 0.8f)
        val ribPath = Path().apply {
            moveTo(ribX, spineY)
            quadraticBezierTo(ribX + blk, spineY + ribLen * 0.6f, ribX - blk * 0.5f, spineY + ribLen)
        }
        drawPath(ribPath, BoneMid, style = Stroke(width = blk * 0.8f))
    }

    // Head (right side)
    val headX = spineEndX
    val headY = spineY - blk * 2

    // Skull
    drawOval(BoneMid, Offset(headX - blk * 2, headY - blk * 3), Size(blk * 6, blk * 5))
    drawOval(BoneLight, Offset(headX - blk * 1, headY - blk * 2.5f), Size(blk * 4, blk * 3))

    // Eye socket
    drawCircle(BoneDark, blk * 0.9f, Offset(headX + blk * 1.5f, headY - blk * 0.8f))
    drawCircle(Color(0xFF2A2420), blk * 0.5f, Offset(headX + blk * 1.5f, headY - blk * 0.8f))

    // Jaw
    drawRoundRect(
        BoneMid,
        Offset(headX, headY + blk * 0.5f),
        Size(blk * 5, blk * 1.5f),
        CornerRadius(blk * 0.3f)
    )
    // Teeth
    for (i in 0..4) {
        val tx = headX + blk * (0.5f + i * 0.9f)
        drawRect(BoneLight, Offset(tx, headY + blk * 1.5f), Size(blk * 0.5f, blk * 0.8f))
    }

    // Neck connecting head to spine
    drawLine(BoneMid, Offset(headX, headY + blk), Offset(spineEndX - blk * 2, spineY), strokeWidth = blk * 1.2f)

    // Tail (left side, curving up)
    val tailPath = Path().apply {
        moveTo(spineStartX, spineY)
        quadraticBezierTo(
            spineStartX - size * 0.15f, spineY - size * 0.05f,
            spineStartX - size * 0.25f, spineY - size * 0.15f
        )
        quadraticBezierTo(
            spineStartX - size * 0.30f, spineY - size * 0.20f,
            spineStartX - size * 0.35f, spineY - size * 0.18f
        )
    }
    drawPath(tailPath, BoneMid, style = Stroke(width = blk * 1.2f))

    // Tail vertebrae
    for (i in 0..4) {
        val t = i / 4f
        val tx = spineStartX - size * 0.25f * t * 1.4f
        val ty = spineY - size * 0.15f * t - size * 0.02f * sin(t * 3f)
        drawCircle(BoneMid, blk * (0.6f - t * 0.1f), Offset(tx, ty))
    }

    // Legs
    // Front legs (near head)
    val frontLegX = spineEndX - blk * 4
    drawLine(BoneMid, Offset(frontLegX, spineY), Offset(frontLegX - blk, spineY + blk * 5), strokeWidth = blk)
    drawLine(BoneMid, Offset(frontLegX - blk, spineY + blk * 5), Offset(frontLegX + blk * 0.5f, spineY + blk * 8), strokeWidth = blk)
    // Foot
    drawLine(BoneMid, Offset(frontLegX + blk * 0.5f, spineY + blk * 8), Offset(frontLegX + blk * 2.5f, spineY + blk * 8.5f), strokeWidth = blk * 0.8f)

    // Back legs (near tail, larger)
    val backLegX = spineStartX + blk * 3
    drawLine(BoneMid, Offset(backLegX, spineY), Offset(backLegX + blk, spineY + blk * 6), strokeWidth = blk * 1.2f)
    drawLine(BoneMid, Offset(backLegX + blk, spineY + blk * 6), Offset(backLegX - blk, spineY + blk * 10), strokeWidth = blk * 1.0f)
    // Foot with toes
    drawLine(BoneMid, Offset(backLegX - blk, spineY + blk * 10), Offset(backLegX - blk * 3, spineY + blk * 10.5f), strokeWidth = blk * 0.7f)
    drawLine(BoneMid, Offset(backLegX - blk, spineY + blk * 10), Offset(backLegX + blk, spineY + blk * 10.8f), strokeWidth = blk * 0.7f)
}

// Helper to convert Float to Sp in DrawScope
private fun Float.toSp(): androidx.compose.ui.unit.TextUnit = (this / 3f).sp
