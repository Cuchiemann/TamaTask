package com.cuchieman.tamatask.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.sin

// ── Panoramic config ──────────────────────────────────────────
// The panorama is PANORAMA_RATIO times wider than the visible screen
private const val PANORAMA_RATIO = 2.5f

// ── Sprite reference ──────────────────────────────────────────
// The Spinosaurus sprite: 500 source px displayed at 320.dp
// → 1 sprite pixel = 320/500 = 0.64 dp on screen
// We use this to compute the block size so details match the sprite.
private const val SPRITE_SOURCE_PX = 500f
private const val SPRITE_DISPLAY_DP = 320f

// ── Color palette ─────────────────────────────────────────────

// Sky
private val SkyTop = Color(0xFF4A6B7A)
private val SkyMid = Color(0xFF7A9DAD)
private val SkyLow = Color(0xFFB8CCD2)
private val SkyHorizon = Color(0xFFD4CEAA)

// Sun
private val SunGlow = Color(0xFFE8D8A8)
private val SunCore = Color(0xFFF5E8C0)

// Fog
private val FogColor = Color(0xFFCDD8D0)

// Mangroves
private val MangroveDeep = Color(0xFF1E3A1E)
private val MangroveMid = Color(0xFF2D4F2D)
private val MangroveLight = Color(0xFF3A6030)
private val MangroveRoot = Color(0xFF3A2E1E)

// Water
private val WaterDeepest = Color(0xFF1A4A40)
private val WaterDeep = Color(0xFF235A4E)
private val WaterMid = Color(0xFF2E6B5A)
private val WaterLight = Color(0xFF3A8070)
private val WaterSurface = Color(0xFF4A9585)
private val WaveCrest = Color(0xFF6BB8A8)
private val WaveFoam = Color(0xFFB0DDD5)
private val WaterReflect = Color(0xFF80C8B8)

// Lily pads
private val LilyPadDark = Color(0xFF2D5A2D)
private val LilyPadLight = Color(0xFF3D7A3D)
private val LilyFlower = Color(0xFFE0A0C0)
private val LilyCenter = Color(0xFFE8D070)

// Reeds
private val ReedDark = Color(0xFF3A5528)
private val ReedMid = Color(0xFF4A6B38)
private val ReedLight = Color(0xFF5D8048)
private val ReedTuft = Color(0xFF7A6540)

// Terrain (shore/mud)
private val ShoreDark = Color(0xFF4A3A20)
private val ShoreMid = Color(0xFF5D4E30)
private val ShoreLight = Color(0xFF6B5D3D)
private val ShoreGrass = Color(0xFF3A5528)
private val MudWet = Color(0xFF3A4A38)
private val PuddleColor = Color(0xFF3A7060)

// Mangrove trees (foreground, individual)
private val TrunkDark = Color(0xFF3A2A18)
private val TrunkMid = Color(0xFF4E3A22)
private val TrunkLight = Color(0xFF5E4A30)
private val CanopyDark = Color(0xFF1E4A1E)
private val CanopyMid = Color(0xFF2D6028)
private val CanopyLight = Color(0xFF3D7A35)
private val CanopyHighlight = Color(0xFF5D9A50)
private val RootWater = Color(0xFF3A5540)

// Foreground vegetation (ferns, bushes, moss)
private val FernDark = Color(0xFF2A5020)
private val FernMid = Color(0xFF3A6830)
private val FernLight = Color(0xFF50803E)
private val BushDark = Color(0xFF1E3A15)
private val BushMid = Color(0xFF2D4E22)
private val BushLight = Color(0xFF3D6530)
private val MossColor = Color(0xFF4A7038)
private val VineDark = Color(0xFF2A4A1E)
private val VineLight = Color(0xFF3D6530)

// Dragonflies
private val DfBlue = Color(0xFF50C0E0)
private val DfGreen = Color(0xFF50D070)
private val DfOrange = Color(0xFFD0A050)
private val DfBody = Color(0xFF2A3A4A)
private val DfWing = Color(0xFFD0E8F0)

// ── Data classes ──────────────────────────────────────────────

private data class ReedCluster(
    val xFraction: Float,  // position in panorama (0..1)
    val count: Int,        // reeds in cluster
    val baseHeight: Int,   // base height in blocks
    val phase: Float
)

private data class LilyPadData(
    val xFraction: Float,
    val yFraction: Float,  // within water (0=top, 1=bottom)
    val size: Float,       // radius multiplier
    val hasFlower: Boolean,
    val phase: Float
)

private data class DragonflyData(
    val startFraction: Float,
    val yFraction: Float,
    val color: Color,
    val speed: Float,
    val phaseOffset: Float
)

private data class MangroveTreeData(
    val xFraction: Float,   // position in panorama (0..1)
    val trunkHeight: Int,   // trunk height multiplier
    val canopyRadius: Float, // canopy size multiplier
    val rootCount: Int,      // how many visible roots
    val leanAngle: Float,   // slight lean
    val hasVines: Boolean
)

private data class VegetationData(
    val xFraction: Float,
    val type: Int,  // 0=fern, 1=bush, 2=grass_tuft, 3=mushroom, 4=moss_rock
    val sizeMul: Float,
    val phase: Float
)

// ── Main composable ──────────────────────────────────────────

@Composable
fun PixelNatureBackground(
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0.5f  // 0..1 — position in panorama
) {
    val density = LocalDensity.current.density

    val infiniteTransition = rememberInfiniteTransition(label = "swamp")

    // Fog drift
    val fogDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(120_000, easing = LinearEasing), RepeatMode.Restart
        ), label = "fog"
    )

    // Water wave (primary)
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing), RepeatMode.Restart
        ), label = "wave1"
    )

    // Water wave (secondary, slower)
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = LinearEasing), RepeatMode.Restart
        ), label = "wave2"
    )

    // Reed sway
    val reedSway by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "reed"
    )

    // Dragonfly time (faster cycle for quicker movement)
    val dfTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            tween(25_000, easing = LinearEasing), RepeatMode.Restart
        ), label = "df_time"
    )

    // Wing buzz — slowed to reduce recomposition rate (was 180ms → 400ms)
    val wingBuzz by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, easing = LinearEasing), RepeatMode.Reverse
        ), label = "buzz"
    )

    // Shimmer
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing), RepeatMode.Restart
        ), label = "shimmer"
    )

    // Wind gust — sweeps across in bursts (0→1→0 cycle)
    val windGust by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            tween(4500, easing = LinearEasing), RepeatMode.Restart
        ), label = "wind"
    )

    // Wind strength: combination of slow base + faster gusts
    // sin produces gentle sway, the abs(sin) peaks add gusts
    val windStrength = sin(windGust) * 0.6f + sin(windGust * 2.3f) * 0.4f

    // ── Scene data ──
    // Reeds — golden ratio scatter, moderate groups (perf-safe)
    val reeds = remember {
        val list = mutableListOf<ReedCluster>()
        val golden = 0.618033f
        var acc = 0.07f
        for (i in 0 until 28) {
            acc = (acc + golden) % 1f
            val count = 3 + (i * 5 + 2) % 4  // 3-6 reeds per cluster
            val height = 5 + (i * 3 + 1) % 4  // 5-8
            // Wave-like phase based on position: nearby clusters sway similarly
            val phase = acc * 4.5f  // spread across ~4.5 radians over panorama width
            list.add(ReedCluster(acc, count, height, phase))
        }
        list
    }

    val lilyPads = remember {
        listOf(
            LilyPadData(0.05f, 0.25f, 1.2f, true, 0f),
            LilyPadData(0.18f, 0.45f, 0.9f, false, 1.4f),
            LilyPadData(0.32f, 0.30f, 1.0f, true, 2.8f),
            LilyPadData(0.46f, 0.55f, 0.8f, false, 0.7f),
            LilyPadData(0.58f, 0.20f, 1.1f, true, 3.5f),
            LilyPadData(0.72f, 0.40f, 1.0f, true, 5.0f),
            LilyPadData(0.85f, 0.35f, 0.9f, false, 2.3f),
            LilyPadData(0.95f, 0.50f, 0.8f, true, 1.6f)
        )
    }

    val dragonflies = remember {
        listOf(
            DragonflyData(0.03f, 0.28f, DfBlue, 2.2f, 0f),
            DragonflyData(0.14f, 0.18f, DfGreen, 1.8f, 1.3f),
            DragonflyData(0.25f, 0.24f, DfOrange, 2.0f, 2.5f),
            DragonflyData(0.36f, 0.32f, DfBlue, 1.6f, 3.8f),
            DragonflyData(0.48f, 0.15f, DfGreen, 2.4f, 5.0f),
            DragonflyData(0.59f, 0.26f, DfOrange, 1.9f, 6.2f),
            DragonflyData(0.70f, 0.20f, DfBlue, 2.1f, 0.8f),
            DragonflyData(0.82f, 0.22f, DfGreen, 2.3f, 4.1f),
            DragonflyData(0.93f, 0.30f, DfOrange, 1.7f, 2.0f)
        )
    }

    // Mangrove trees — forest on far shore (reduced from 26→18 for perf)
    val mangroveTrees = remember {
        listOf(
            MangroveTreeData(0.02f, 45, 4.8f, 6, -0.10f, true),
            MangroveTreeData(0.08f, 51, 5.7f, 7, -0.04f, true),
            MangroveTreeData(0.14f, 36, 3.9f, 5, 0.06f, false),
            MangroveTreeData(0.20f, 48, 5.2f, 7, -0.07f, true),
            MangroveTreeData(0.27f, 54, 6.0f, 6, -0.05f, true),
            MangroveTreeData(0.33f, 33, 3.6f, 5, 0.07f, false),
            MangroveTreeData(0.39f, 45, 5.0f, 6, -0.06f, true),
            MangroveTreeData(0.46f, 52, 5.8f, 7, -0.03f, true),
            MangroveTreeData(0.52f, 39, 4.0f, 5, 0.05f, false),
            MangroveTreeData(0.58f, 51, 5.5f, 7, -0.04f, true),
            MangroveTreeData(0.64f, 34, 3.8f, 5, 0.07f, false),
            MangroveTreeData(0.71f, 54, 6.0f, 7, -0.05f, true),
            MangroveTreeData(0.77f, 40, 4.3f, 5, 0.04f, false),
            MangroveTreeData(0.83f, 49, 5.4f, 7, -0.07f, true),
            MangroveTreeData(0.89f, 36, 3.8f, 5, 0.06f, false),
            MangroveTreeData(0.95f, 46, 5.1f, 6, -0.06f, true),
            MangroveTreeData(0.11f, 42, 4.5f, 6, 0.03f, true),
            MangroveTreeData(0.67f, 37, 3.9f, 5, 0.03f, true)
        )
    }

    // Vegetation — organic, scattered, varied sizes
    // type: 0=fern, 1=bush, 2=grass_tuft, 4=rock
    // Placed with pseudo-random gaps using golden ratio for natural feel
    val vegetation = remember {
        val list = mutableListOf<VegetationData>()
        val golden = 0.618033f
        var accumulator = 0.13f  // starting seed
        for (i in 0 until 22) {  // reduced from 30 for performance
            accumulator = (accumulator + golden) % 1f
            // Type distribution: bushes 30%, grass 30%, ferns 25%, rocks 15%
            val type = when {
                i % 7 == 5 -> 4  // rock (less frequent)
                i % 4 == 0 -> 1  // bush
                i % 4 == 1 -> 2  // grass
                i % 4 == 2 -> 0  // fern
                else -> 2        // grass
            }
            // Bigger sizes with more variation
            val sizeMul = when (type) {
                4 -> 2.0f + (i * 3 + 2) % 4 * 0.5f   // rocks: 2.0-3.5
                1 -> 2.5f + (i * 5 + 1) % 5 * 0.4f   // bushes: 2.5-4.5
                else -> 2.0f + (i * 7 + 3) % 4 * 0.3f // ferns/grass: 2.0-2.9
            }
            val phase = (i * 2.17f) % 6.28f
            list.add(VegetationData(accumulator, type, sizeMul, phase))
        }
        list
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Block size matching the sprite's pixel resolution
        // 1 sprite pixel = SPRITE_DISPLAY_DP * density / SPRITE_SOURCE_PX
        val spritePx = SPRITE_DISPLAY_DP * density / SPRITE_SOURCE_PX
        // For detail elements we use a visible "art block" of 3 sprite pixels
        // (matches the smallest visible features in the sprite like claw outlines)
        val blk = spritePx * 3f

        // Panorama scroll: how much of the panorama is offscreen
        val panoramaWidth = w * PANORAMA_RATIO
        val maxScroll = panoramaWidth - w
        val scrollPx = scrollOffset.coerceIn(0f, 1f) * maxScroll

        // ── Layout (fractions of screen height) ──
        val skyEnd = h * 0.42f
        val mangroveY = h * 0.40f
        val waterTop = h * 0.52f
        val waterBottom = h * 0.78f
        val shoreTop = h * 0.75f

        // ══════════════════════════════════════════
        // 1. SKY (gradient — smooth, device resolution)
        // ══════════════════════════════════════════
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SkyTop, SkyMid, SkyLow, SkyHorizon),
                startY = 0f, endY = skyEnd + h * 0.1f
            ),
            size = Size(w, skyEnd + h * 0.1f)
        )

        // ══════════════════════════════════════════
        // 2. FOGGY SUN
        // ══════════════════════════════════════════
        drawFoggySun(w * 0.82f, h * 0.08f, blk)

        // ══════════════════════════════════════════
        // 3. FOG LAYERS
        // ══════════════════════════════════════════
        // Wind pushes fog faster during gusts
        val fogWindBoost = 1f + windStrength * 0.8f
        drawFogLayer(h * 0.16f, w, blk, fogDrift * 40f * fogWindBoost, 0.10f)
        drawFogLayer(h * 0.26f, w, blk, fogDrift * 25f * fogWindBoost + 20f, 0.07f)
        drawFogLayer(h * 0.34f, w, blk, fogDrift * 55f * fogWindBoost + 10f, 0.09f)

        // ══════════════════════════════════════════
        // 4. MANGROVE TREE LINE (far silhouette)
        // ══════════════════════════════════════════
        drawMangroves(mangroveY, blk, w, scrollPx, panoramaWidth)

        // ══════════════════════════════════════════
        // 4b. MANGROVE TREES on far shore (above water)
        // ══════════════════════════════════════════
        mangroveTrees.forEach { tree ->
            val worldX = tree.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 20 && screenX < w + blk * 20) {
                drawMangroveTree(
                    screenX, waterTop + blk * 2, blk,
                    tree.trunkHeight, tree.canopyRadius,
                    tree.rootCount, tree.leanAngle, tree.hasVines
                )
            }
        }

        // ══════════════════════════════════════════
        // 5. WATER with realistic waves
        // ══════════════════════════════════════════
        drawWaterBody(waterTop, waterBottom, w, wave1, wave2, shimmer)

        // ── Lily pads ──
        lilyPads.forEach { lp ->
            val worldX = lp.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 8 && screenX < w + blk * 8) {
                val lilyY = waterTop + (waterBottom - waterTop) * lp.yFraction
                val bob = sin(wave1 + lp.phase) * blk
                drawLilyPad(screenX, lilyY + bob, blk * lp.size, lp.hasFlower)
            }
        }

        // Wave overlay (foam lines on top of water)
        drawWaveFoamLines(waterTop, waterBottom, w, wave1, wave2, blk)

        // ══════════════════════════════════════════
        // 6. REEDS along the shore
        // ══════════════════════════════════════════
        reeds.forEach { cluster ->
            val worldX = cluster.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 12 && screenX < w + blk * 12) {
                // Wind adds extra sway to reeds
                val windySway = reedSway + windStrength * 1.2f
                drawReedCluster(
                    screenX, waterTop - blk, blk,
                    cluster.count, cluster.baseHeight,
                    windySway, cluster.phase
                )
            }
        }

        // ══════════════════════════════════════════
        // 7. SHORE / TERRAIN
        // ══════════════════════════════════════════
        drawShoreTerrain(shoreTop, h, w, blk, scrollPx, panoramaWidth)

        // ══════════════════════════════════════════
        // 7b. VEGETATION covering terrain (organic scatter)
        // ══════════════════════════════════════════
        val terrainH = h - shoreTop
        // 3 staggered rows for depth, covering all terrain including bottom
        val vegRows = listOf(0.08f, 0.40f, 0.75f)
        vegRows.forEachIndexed { rowIdx, rowFrac ->
            val rowBaseY = shoreTop + terrainH * rowFrac
            val perspScale = 0.85f + rowIdx * 0.2f
            vegetation.forEach { veg ->
                val worldX = veg.xFraction * panoramaWidth
                val rowOffsetX = rowIdx * panoramaWidth * 0.12f
                val screenX = (worldX + rowOffsetX) % panoramaWidth - scrollPx
                if (screenX > -blk * 15 && screenX < w + blk * 15) {
                    // Each item gets its own Y jitter based on its position
                    val yJitter = sin(worldX * 0.023f + veg.phase) * terrainH * 0.15f
                    // Clamp so vegetation never goes above shore into the water
                    val vegY = (rowBaseY + yJitter).coerceAtLeast(shoreTop + blk * 3)
                    val sz = blk * veg.sizeMul * perspScale
                    val windySway = reedSway + windStrength * 1.0f
                    when (veg.type) {
                        0 -> drawFern(screenX, vegY, sz, windySway, veg.phase)
                        1 -> drawBush(screenX, vegY, sz)
                        2 -> drawGrassTuft(screenX, vegY, sz, windySway, veg.phase)
                        4 -> drawMossRock(screenX, vegY, sz)
                    }
                }
            }
        }

        // ══════════════════════════════════════════
        // 8. DRAGONFLIES
        // ══════════════════════════════════════════
        dragonflies.forEach { df ->
            val travelRange = 1.4f
            val rawFrac = df.startFraction + dfTime * df.speed * 0.05f + df.phaseOffset * 0.1f
            val wrappedFrac = ((rawFrac % travelRange) + travelRange) % travelRange - 0.2f
            val worldX = wrappedFrac * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 10 && screenX < w + blk * 10) {
                val hover = sin(dfTime * 3f + df.phaseOffset) * h * 0.02f
                val screenY = df.yFraction * h + hover
                drawDragonfly(screenX, screenY, blk, df.color, wingBuzz > 0.5f)
            }
        }

        // ══════════════════════════════════════════
        // 9. WIND PARTICLES — small leaves blowing across
        // ══════════════════════════════════════════
        val windAlpha = (windStrength * 0.7f).coerceIn(0f, 0.6f)
        if (windAlpha > 0.1f) {
            for (p in 0 until 6) {
                val seed = p * 137.5f  // golden angle scatter
                val px = ((dfTime * 200f + seed * 80f + windGust * 60f) % (w + blk * 20)) - blk * 10
                val py = h * (0.15f + (seed % 0.7f)) +
                        sin(dfTime * 4f + p * 1.5f) * blk * 6
                val leafSize = blk * (0.4f + (p % 3) * 0.2f)
                val leafColor = when (p % 3) {
                    0 -> CanopyLight.copy(alpha = windAlpha)
                    1 -> FernMid.copy(alpha = windAlpha * 0.8f)
                    else -> CanopyMid.copy(alpha = windAlpha * 0.7f)
                }
                // Tiny spinning leaf
                val rot = dfTime * 8f + p * 2f
                drawOval(
                    color = leafColor,
                    topLeft = Offset(px, py),
                    size = Size(
                        leafSize * (1f + sin(rot) * 0.5f),
                        leafSize * (1f + cos(rot) * 0.5f)
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Drawing helpers
// ══════════════════════════════════════════════════════════════

private fun DrawScope.px(x: Float, y: Float, s: Float, color: Color) {
    drawRect(color = color, topLeft = Offset(x, y), size = Size(s + 0.5f, s + 0.5f))
}

// ── Foggy sun ──

private fun DrawScope.drawFoggySun(cx: Float, cy: Float, blk: Float) {
    // Outer glow
    val glowR = blk * 8
    drawCircle(color = SunGlow.copy(alpha = 0.08f), radius = glowR, center = Offset(cx, cy))
    drawCircle(color = SunGlow.copy(alpha = 0.12f), radius = glowR * 0.6f, center = Offset(cx, cy))
    // Inner
    drawCircle(color = SunCore.copy(alpha = 0.25f), radius = blk * 3, center = Offset(cx, cy))
    drawCircle(color = SunCore.copy(alpha = 0.45f), radius = blk * 1.5f, center = Offset(cx, cy))
}

// ── Fog layers ──

private fun DrawScope.drawFogLayer(
    y: Float, screenWidth: Float, blk: Float,
    drift: Float, alpha: Float
) {
    val path = Path()
    path.moveTo(-blk, y + blk * 4)
    val fogStep = blk * 3f  // coarser for performance
    val cols = (screenWidth / fogStep).toInt() + 2
    for (i in 0..cols) {
        val x = i * fogStep
        val fogH = sin((i + drift) * 0.06f) * blk * 2 +
                cos((i + drift) * 0.10f) * blk +
                blk * 2
        path.lineTo(x, y - fogH)
    }
    path.lineTo(screenWidth + blk, y + blk * 4)
    path.close()
    drawPath(path, color = FogColor.copy(alpha = alpha))
}

// ── Mangroves ──

private fun DrawScope.drawMangroves(
    baseY: Float, blk: Float, screenWidth: Float,
    scrollPx: Float, panoramaWidth: Float
) {
    // Far mangrove silhouette (drawn as a continuous shape)
    val path = Path()
    val step = blk * 2f  // coarser steps for performance
    val cols = (screenWidth / step).toInt() + 2
    path.moveTo(-blk, size.height * 0.6f)

    for (i in 0..cols) {
        val screenX = i * step
        val worldX = screenX + scrollPx

        // Procedural tree heights using sine combination (tall canopy)
        val treeH = (
            sin(worldX * 0.006f) * blk * 16 +
            sin(worldX * 0.015f + 1.5f) * blk * 8 +
            sin(worldX * 0.030f + 3f) * blk * 4 +
            blk * 20
        ).coerceAtLeast(blk * 6)

        path.lineTo(screenX, baseY - treeH)
    }
    path.lineTo(screenWidth + blk, size.height * 0.6f)
    path.close()

    // Dark silhouette layer
    drawPath(path, color = MangroveDeep)

    // Lighter overlay for canopy texture
    val canopyPath = Path()
    canopyPath.moveTo(-blk, size.height * 0.6f)
    for (i in 0..cols) {
        val screenX = i * step
        val worldX = screenX + scrollPx
        val treeH = (
            sin(worldX * 0.006f) * blk * 14 +
            sin(worldX * 0.018f + 2f) * blk * 6 +
            blk * 16
        ).coerceAtLeast(blk * 3)
        canopyPath.lineTo(screenX, baseY - treeH)
    }
    canopyPath.lineTo(screenWidth + blk, size.height * 0.6f)
    canopyPath.close()
    drawPath(canopyPath, color = MangroveMid.copy(alpha = 0.7f))

    // Aerial roots (detail blocks)
    val rootStep = blk * 6
    val rootCols = (screenWidth / rootStep).toInt() + 1
    for (i in 0..rootCols) {
        val screenX = i * rootStep
        val worldX = screenX + scrollPx
        val rootHash = ((worldX * 0.1f).toInt() * 7 + 13) % 5
        if (rootHash < 3) {
            val rootLen = 2 + rootHash
            for (r in 0 until rootLen) {
                val alpha = 0.5f - r * 0.1f
                px(screenX, baseY + blk * 4 + r * blk, blk, MangroveRoot.copy(alpha = alpha.coerceAtLeast(0.1f)))
            }
        }
    }
}

// ── Water body (smooth gradients + wave shapes) ──

private fun DrawScope.drawWaterBody(
    topY: Float, bottomY: Float, screenWidth: Float,
    wave1: Float, wave2: Float, shimmer: Float
) {
    val waterHeight = bottomY - topY

    // Base water gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(WaterSurface, WaterLight, WaterMid, WaterDeep, WaterDeepest),
            startY = topY,
            endY = bottomY
        ),
        topLeft = Offset(0f, topY),
        size = Size(screenWidth, waterHeight)
    )

    // Wave bands — sinusoidal color bands that move
    val bandCount = 5
    for (band in 0 until bandCount) {
        val bandFrac = band.toFloat() / bandCount
        val bandY = topY + waterHeight * bandFrac
        val bandH = waterHeight / bandCount

        val wavePath = Path()
        wavePath.moveTo(-10f, bandY + bandH)

        val waveStep = 8f  // coarser for performance (was 3f)
        val steps = (screenWidth / waveStep).toInt() + 1
        for (i in 0..steps) {
            val x = i * waveStep
            val waveY = sin(x * 0.02f + wave1 + band * 0.8f) * bandH * 0.3f +
                    sin(x * 0.035f + wave2 + band * 0.5f) * bandH * 0.15f
            wavePath.lineTo(x, bandY + waveY)
        }
        wavePath.lineTo(screenWidth + 10f, bandY + bandH)
        wavePath.close()

        val bandColor = when {
            bandFrac < 0.15f -> WaterReflect.copy(alpha = 0.15f)
            bandFrac < 0.3f -> WaveCrest.copy(alpha = 0.10f)
            bandFrac < 0.6f -> WaterLight.copy(alpha = 0.08f)
            else -> WaterDeep.copy(alpha = 0.06f)
        }
        drawPath(wavePath, color = bandColor)
    }

    // Shimmer highlights (moving light reflections)
    val shimmerStep = 12f  // coarser for performance (was 4f)
    val shimmerSteps = (screenWidth / shimmerStep).toInt()
    for (i in 0..shimmerSteps) {
        val x = i * shimmerStep
        val sparkle = sin(x * 0.08f + shimmer) * cos(x * 0.05f + shimmer * 0.5f)
        if (sparkle > 0.6f) {
            val sy = topY + waterHeight * 0.1f + sin(x * 0.03f + wave1) * waterHeight * 0.08f
            val alpha = (sparkle - 0.6f) * 0.6f
            drawRect(
                color = WaveFoam.copy(alpha = alpha),
                topLeft = Offset(x, sy),
                size = Size(3f, 1.5f)
            )
        }
    }
}

// ── Foam lines over water ──

private fun DrawScope.drawWaveFoamLines(
    topY: Float, bottomY: Float, screenWidth: Float,
    wave1: Float, wave2: Float, blk: Float
) {
    val waterHeight = bottomY - topY

    // 2 foam lines at different depths (was 3)
    for (line in 0..1) {
        val lineFrac = 0.05f + line * 0.25f
        val baseY = topY + waterHeight * lineFrac

        val foamPath = Path()
        var started = false
        val foamStep = 6f  // coarser for performance (was 2f)
        val steps = (screenWidth / foamStep).toInt() + 1

        for (i in 0..steps) {
            val x = i * foamStep
            val wy = sin(x * 0.018f + wave1 + line * 1.2f) * blk * 1.5f +
                    sin(x * 0.04f + wave2 + line * 0.7f) * blk * 0.8f
            if (!started) {
                foamPath.moveTo(x, baseY + wy)
                started = true
            } else {
                foamPath.lineTo(x, baseY + wy)
            }
        }

        // Draw as a thin line
        drawPath(
            foamPath,
            color = WaveFoam.copy(alpha = 0.2f - line * 0.05f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = blk * 0.5f)
        )
    }
}

// ── Lily pads ──

private fun DrawScope.drawLilyPad(
    x: Float, y: Float, radius: Float, hasFlower: Boolean
) {
    // Oval pad
    val padW = radius * 4
    val padH = radius * 2.5f
    drawOval(
        color = LilyPadDark,
        topLeft = Offset(x - padW / 2, y - padH / 2),
        size = Size(padW, padH)
    )
    // Light center
    drawOval(
        color = LilyPadLight.copy(alpha = 0.6f),
        topLeft = Offset(x - padW * 0.3f, y - padH * 0.3f),
        size = Size(padW * 0.6f, padH * 0.6f)
    )
    // Notch (small dark wedge) — draw a small rect to suggest the cut
    drawRect(
        color = WaterMid,
        topLeft = Offset(x - radius * 0.3f, y - padH / 2),
        size = Size(radius * 0.6f, padH * 0.3f)
    )

    if (hasFlower) {
        // Flower petals
        val fr = radius * 1.2f
        val fy = y - padH / 2 - fr
        drawCircle(color = LilyFlower.copy(alpha = 0.8f), radius = fr, center = Offset(x - fr, fy))
        drawCircle(color = LilyFlower.copy(alpha = 0.7f), radius = fr * 0.8f, center = Offset(x + fr, fy))
        drawCircle(color = LilyFlower, radius = fr * 0.9f, center = Offset(x, fy - fr * 0.3f))
        // Center
        drawCircle(color = LilyCenter, radius = fr * 0.4f, center = Offset(x, fy))
    }
}

// ── Reed clusters ──

private fun DrawScope.drawReedCluster(
    x: Float, baseY: Float, blk: Float,
    count: Int, baseHeight: Int,
    sway: Float, phase: Float
) {
    val spacing = blk * 1.5f

    for (i in 0 until count) {
        val rx = x + i * spacing
        val heightVariation = (i * 3 + 1) % 3
        val h = baseHeight + heightVariation

        // Natural sway: cluster phase + tiny per-reed offset for organic feel
        val reedPhase = phase + i * 0.15f  // subtle variation within cluster
        for (j in 0 until h) {
            val swayFrac = j.toFloat() / h
            val swayOff = sin(sway * 3.14f + reedPhase) * blk * swayFrac * 1.5f

            val color = when {
                j < h / 3 -> ReedLight
                j < h * 2 / 3 -> ReedMid
                else -> ReedDark
            }
            px(rx + swayOff, baseY - j * blk, blk, color)
        }

        // Tuft at top (cattail shape)
        if (i % 2 == 0) {
            val topSway = sin(sway * 3.14f + reedPhase) * blk * 1.5f
            val topY = baseY - h * blk
            // Cattail body (3 blocks tall, 1 wide)
            for (t in 0..2) {
                px(rx + topSway, topY - t * blk, blk, ReedTuft)
            }
            // Wider middle
            px(rx + topSway - blk, topY - blk, blk, ReedTuft.copy(alpha = 0.6f))
            px(rx + topSway + blk, topY - blk, blk, ReedTuft.copy(alpha = 0.6f))
        }
    }
}

// ── Shore terrain ──

private fun DrawScope.drawShoreTerrain(
    topY: Float, screenHeight: Float, screenWidth: Float,
    blk: Float, scrollPx: Float, panoramaWidth: Float
) {
    val terrainHeight = screenHeight - topY

    // Shore edge — organic wavy transition from water to land
    val edgePath = Path()
    edgePath.moveTo(-10f, screenHeight)
    val edgeStep = blk  // coarser for performance (was 2f)
    val steps = (screenWidth / edgeStep).toInt() + 1
    for (i in 0..steps) {
        val x = i * edgeStep
        val worldX = x + scrollPx
        val edgeY = topY +
                sin(worldX * 0.012f) * blk * 2 +
                sin(worldX * 0.025f + 1.5f) * blk +
                blk
        edgePath.lineTo(x, edgeY)
    }
    edgePath.lineTo(screenWidth + 10f, screenHeight)
    edgePath.close()

    // Main terrain fill (gradient — greener, more natural)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4A6838),  // grassy top
                Color(0xFF3D5A2E),  // mid green-brown
                ShoreLight,         // earthy
                ShoreMid,
                ShoreDark
            ),
            startY = topY,
            endY = screenHeight
        ),
        topLeft = Offset(0f, topY),
        size = Size(screenWidth, terrainHeight)
    )

    // Overwrite water peek above shore with shore edge shape
    drawPath(edgePath, color = ShoreMid)

    // Grass tufts along the shore edge
    val tuftsStep = blk * 6  // coarser (was blk*4)
    val tufts = (screenWidth / tuftsStep).toInt() + 1
    for (i in 0..tufts) {
        val x = i * tuftsStep
        val worldX = x + scrollPx
        val edgeY = topY +
                sin(worldX * 0.012f) * blk * 2 +
                sin(worldX * 0.025f + 1.5f) * blk
        // Small grass blades
        val grassHash = ((worldX * 0.1f).toInt() * 7 + 3) % 5
        if (grassHash < 3) {
            for (g in 0..grassHash) {
                px(x + g * blk * 0.8f, edgeY - blk * (grassHash - g + 1), blk, ShoreGrass.copy(alpha = 0.7f))
            }
        }
    }

    // Wet mud patches near water
    val patchStep = blk * 8
    val patches = (screenWidth / patchStep).toInt() + 1
    for (i in 0..patches) {
        val x = i * patchStep
        val worldX = x + scrollPx
        val hash = ((worldX * 0.07f).toInt() * 13 + 5) % 7
        if (hash < 3) {
            val patchW = blk * (2 + hash)
            val patchH = blk * 1.5f
            val py = topY + blk * 2 + (hash % 2) * blk * 3
            drawOval(
                color = MudWet.copy(alpha = 0.4f),
                topLeft = Offset(x, py),
                size = Size(patchW, patchH)
            )
        }
    }

    // Puddles (reflective water patches in mud)
    val puddleStep = blk * 12
    val puddles = (screenWidth / puddleStep).toInt() + 1
    for (i in 0..puddles) {
        val x = i * puddleStep + blk * 3
        val worldX = x + scrollPx
        val hash = ((worldX * 0.05f).toInt() * 11 + 7) % 9
        if (hash < 3) {
            val pw = blk * (3 + hash)
            val ph = blk * 1.5f
            val py = topY + blk * 6 + hash * blk * 2
            if (py + ph < screenHeight) {
                drawOval(
                    color = PuddleColor.copy(alpha = 0.35f),
                    topLeft = Offset(x, py),
                    size = Size(pw, ph)
                )
                // Tiny reflection highlight
                drawOval(
                    color = WaveFoam.copy(alpha = 0.1f),
                    topLeft = Offset(x + pw * 0.2f, py + ph * 0.1f),
                    size = Size(pw * 0.4f, ph * 0.3f)
                )
            }
        }
    }
}

// ── Mangrove tree (individual, foreground) — thick trunk, forking branches, big dome canopy ──

private fun DrawScope.drawMangroveTree(
    x: Float, baseY: Float, blk: Float,
    trunkHeight: Int, canopyMul: Float,
    rootCount: Int, lean: Float, hasVines: Boolean
) {
    val trunkW = blk * 3f  // thick trunk
    val groundY = baseY + blk * 2  // roots spread on the ground, above water

    // ── STILT ROOTS — prominent arching roots, above water ──
    for (r in 0 until rootCount) {
        val spread = (r - rootCount / 2f) / rootCount.coerceAtLeast(1)
        val rootSpread = spread * blk * (16 + rootCount)
        val rootThickness = blk * (1.5f - kotlin.math.abs(spread) * 0.4f)

        val rootFootX = x + rootSpread
        val rootFootY = groundY  // feet on shore, not in water
        val rootTopX = x + rootSpread * 0.25f
        val rootTopY = baseY - blk * 3

        val rootPath = Path()
        rootPath.moveTo(rootFootX, rootFootY)
        val archHeight = blk * (4 + (r % 3))
        rootPath.cubicTo(
            rootFootX + rootSpread * 0.05f, rootFootY - archHeight,
            rootTopX + rootSpread * 0.15f, rootTopY + blk * 5,
            rootTopX, rootTopY
        )
        drawPath(
            rootPath,
            color = if (r % 2 == 0) TrunkDark else TrunkMid,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = rootThickness)
        )

        // Secondary thin roots branching off (every 3rd for perf)
        if (r % 3 == 0) {
            val branchX = rootFootX + rootSpread * 0.12f
            val branchY = rootFootY - archHeight * 0.35f
            val thinRoot = Path()
            thinRoot.moveTo(branchX, branchY)
            thinRoot.cubicTo(
                branchX + rootSpread * 0.25f, branchY + blk * 2,
                branchX + rootSpread * 0.35f, groundY - blk,
                branchX + rootSpread * 0.30f, groundY
            )
            drawPath(
                thinRoot,
                color = TrunkDark.copy(alpha = 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = rootThickness * 0.35f)
            )
        }
    }

    // ── MAIN TRUNK — thick, wider at base, narrows toward fork ──
    val totalH = trunkHeight * blk
    val forkFrac = 0.38f  // fork at ~38% height
    val forkY = baseY - totalH * forkFrac
    val forkX = x + lean * totalH * forkFrac

    // Trunk shape: wide at base, narrows at fork
    val baseHalfW = trunkW * 0.7f
    val forkHalfW = trunkW * 0.35f
    val lowerTrunk = Path()
    lowerTrunk.moveTo(x - baseHalfW, baseY)
    lowerTrunk.cubicTo(
        x - baseHalfW * 0.9f, baseY - totalH * 0.15f,
        forkX - forkHalfW * 1.3f, forkY + totalH * 0.08f,
        forkX - forkHalfW, forkY
    )
    lowerTrunk.lineTo(forkX + forkHalfW, forkY)
    lowerTrunk.cubicTo(
        forkX + forkHalfW * 1.3f, forkY + totalH * 0.08f,
        x + baseHalfW * 0.9f, baseY - totalH * 0.15f,
        x + baseHalfW, baseY
    )
    lowerTrunk.close()
    drawPath(lowerTrunk, color = TrunkMid)
    // Light shading on trunk
    drawPath(
        lowerTrunk,
        brush = Brush.horizontalGradient(
            colors = listOf(TrunkDark.copy(alpha = 0.3f), Color.Transparent, TrunkLight.copy(alpha = 0.25f)),
            startX = x - baseHalfW, endX = x + baseHalfW
        )
    )

    // ── MAIN BRANCHES — 3 primary branches from fork ──
    val branchCount = 3
    // Branch definitions: (angle offset, length fraction, thickness fraction)
    val branchDefs = listOf(
        Triple(-0.35f + lean * 0.3f, 0.65f, 0.80f),  // left
        Triple(0.05f + lean * 0.2f, 0.72f, 0.90f),    // center (tallest)
        Triple(0.40f + lean * 0.3f, 0.60f, 0.75f)     // right
    )

    // Store branch tip positions for unified canopy
    val branchTips = mutableListOf<Pair<Float, Float>>()

    for ((b, def) in branchDefs.withIndex()) {
        val (angle, lenFrac, thickFrac) = def
        val branchLen = totalH * lenFrac * (1f - forkFrac)
        val branchThick = trunkW * thickFrac

        // Branch tip
        val tipX = forkX + angle * totalH * 0.45f
        val tipY = forkY - branchLen

        branchTips.add(tipX to tipY)

        // Curved branch (filled shape, not just stroke)
        val bwTop = branchThick * 0.25f  // thin at top
        val bwBot = branchThick * 0.45f  // thicker at fork

        val brPath = Path()
        // Left edge (bottom to top)
        brPath.moveTo(forkX - bwBot * 0.5f + angle * blk * 2, forkY)
        brPath.cubicTo(
            forkX + angle * totalH * 0.15f - bwBot * 0.3f, forkY - branchLen * 0.35f,
            tipX - bwTop * 0.8f + angle * blk, tipY + branchLen * 0.2f,
            tipX - bwTop, tipY
        )
        // Top cap
        brPath.lineTo(tipX + bwTop, tipY)
        // Right edge (top to bottom)
        brPath.cubicTo(
            tipX + bwTop * 0.8f + angle * blk, tipY + branchLen * 0.2f,
            forkX + angle * totalH * 0.15f + bwBot * 0.3f, forkY - branchLen * 0.35f,
            forkX + bwBot * 0.5f + angle * blk * 2, forkY
        )
        brPath.close()

        drawPath(brPath, color = TrunkMid)
        // Shading
        drawPath(brPath, color = TrunkLight.copy(alpha = 0.15f))

        // ── SUB-BRANCHES — 1-2 thinner forks (reduced for performance) ──
        val subCount = 1 + (b + trunkHeight) % 2
        for (s in 0 until subCount) {
            val subFrac = 0.45f + s * 0.22f  // start between 45%-89% up the branch
            val subStartX = forkX + (tipX - forkX) * subFrac
            val subStartY = forkY + (tipY - forkY) * subFrac

            val subAngle = angle + (s - subCount / 2f) * 0.35f
            val subLen = branchLen * 0.30f
            val subTipX = subStartX + subAngle * subLen * 0.5f
            val subTipY = subStartY - subLen * 0.55f

            branchTips.add(subTipX to subTipY)

            val subPath = Path()
            subPath.moveTo(subStartX, subStartY)
            subPath.cubicTo(
                subStartX + subAngle * blk * 3, subStartY - subLen * 0.25f,
                subTipX - subAngle * blk, subTipY + subLen * 0.1f,
                subTipX, subTipY
            )
            drawPath(
                subPath,
                color = TrunkMid.copy(alpha = 0.85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = branchThick * 0.18f
                )
            )
        }
    }

    // ── UNIFIED CANOPY — big dome formed by overlapping filled circles ──
    // The canopy covers all branch tips as one organic mass (like the sketch)
    val canR = blk * 5f * canopyMul  // overall canopy radius

    // Canopy center: average of branch tips, shifted up
    val canCx = branchTips.map { it.first }.average().toFloat()
    val canCy = branchTips.minOf { it.second } - canR * 0.15f

    // 1) Big dark base oval — defines the overall shape
    drawOval(
        color = CanopyDark,
        topLeft = Offset(canCx - canR * 1.5f, canCy - canR * 0.7f),
        size = Size(canR * 3.0f, canR * 1.8f)
    )

    // 2) Main mass — single large blob + side fill (was 3 circles → 2)
    drawCircle(color = CanopyMid, radius = canR * 1.15f,
        center = Offset(canCx, canCy + canR * 0.05f))
    drawOval(color = CanopyMid, topLeft = Offset(canCx - canR * 1.3f, canCy - canR * 0.5f),
        size = Size(canR * 2.6f, canR * 1.2f))

    // 3) Lighter patches (was 4 → 2)
    drawCircle(color = CanopyLight, radius = canR * 0.7f,
        center = Offset(canCx - canR * 0.2f, canCy - canR * 0.25f))
    drawCircle(color = CanopyLight, radius = canR * 0.55f,
        center = Offset(canCx + canR * 0.35f, canCy - canR * 0.15f))

    // 4) Single highlight spot
    drawCircle(color = CanopyHighlight.copy(alpha = 0.30f), radius = canR * 0.30f,
        center = Offset(canCx, canCy - canR * 0.35f))

    // 5) Bottom edge — darker underside
    drawOval(
        color = CanopyDark.copy(alpha = 0.4f),
        topLeft = Offset(canCx - canR * 1.2f, canCy + canR * 0.3f),
        size = Size(canR * 2.4f, canR * 0.6f)
    )

    // ── Vines hanging from canopy ──
    if (hasVines) {
        val vineBaseY = canCy + canR * 0.6f
        for (v in 0..2) {
            val vx = canCx - canR * 0.6f + v * canR * 0.6f
            val vineLen = canR * (0.5f + (v * 3 + 1) % 4 * 0.1f)
            val vinePath = Path()
            vinePath.moveTo(vx, vineBaseY)
            vinePath.cubicTo(
                vx + blk * 2, vineBaseY + vineLen * 0.3f,
                vx - blk * 1.5f, vineBaseY + vineLen * 0.6f,
                vx + blk * 0.5f, vineBaseY + vineLen
            )
            drawPath(
                vinePath,
                color = if (v % 2 == 0) VineDark.copy(alpha = 0.6f) else VineLight.copy(alpha = 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = blk * 0.5f)
            )
        }
    }
}

// ── Fern ──

private fun DrawScope.drawFern(
    x: Float, baseY: Float, blk: Float,
    sway: Float, phase: Float
) {
    val swayOff = sin(sway * 3.14f + phase) * blk * 0.6f

    // Central stem
    for (i in 0..4) {
        px(x + swayOff * (i / 4f), baseY - i * blk, blk * 0.7f, FernMid)
    }

    // Fronds (angled leaves on each side)
    for (i in 1..3) {
        val fy = baseY - i * blk
        val sw = swayOff * (i / 4f)
        // Left frond
        for (f in 1..3) {
            val alpha = 1f - f * 0.2f
            px(x + sw - f * blk, fy + f * blk * 0.3f, blk * 0.7f, FernLight.copy(alpha = alpha))
        }
        // Right frond
        for (f in 1..3) {
            val alpha = 1f - f * 0.2f
            px(x + sw + f * blk, fy + f * blk * 0.3f, blk * 0.7f, FernDark.copy(alpha = alpha))
        }
    }

    // Top curl
    px(x + swayOff + blk * 0.3f, baseY - 5 * blk, blk * 0.5f, FernLight.copy(alpha = 0.7f))
}

// ── Bush ──

private fun DrawScope.drawBush(x: Float, baseY: Float, blk: Float) {
    // Layered circles for a lush, wide bush
    val r = blk * 1.8f
    // Dark base (widest)
    drawOval(
        color = BushDark,
        topLeft = Offset(x - r * 1.4f, baseY - r * 1.6f),
        size = Size(r * 2.8f, r * 1.8f)
    )
    // Main body blobs
    drawCircle(color = BushMid, radius = r * 1.0f, center = Offset(x - r * 0.3f, baseY - r * 0.8f))
    drawCircle(color = BushMid, radius = r * 0.9f, center = Offset(x + r * 0.4f, baseY - r * 0.9f))
    drawCircle(color = BushMid, radius = r * 0.7f, center = Offset(x, baseY - r * 1.2f))
    // Light clusters
    drawCircle(color = BushLight, radius = r * 0.55f, center = Offset(x - r * 0.2f, baseY - r * 1.0f))
    drawCircle(color = BushLight, radius = r * 0.45f, center = Offset(x + r * 0.3f, baseY - r * 1.1f))
    // Highlight
    drawCircle(color = CanopyHighlight.copy(alpha = 0.3f), radius = r * 0.25f, center = Offset(x, baseY - r * 1.3f))
    drawCircle(color = CanopyHighlight.copy(alpha = 0.15f), radius = r * 0.2f, center = Offset(x + r * 0.4f, baseY - r * 1.0f))
}

// ── Grass tuft ──

private fun DrawScope.drawGrassTuft(
    x: Float, baseY: Float, blk: Float,
    sway: Float, phase: Float
) {
    val sw = sin(sway * 3.14f + phase) * blk * 0.4f
    // 5 blades of different heights
    val heights = listOf(3, 4, 5, 4, 3)
    val colors = listOf(FernDark, FernMid, FernLight, FernMid, FernDark)
    for ((i, h) in heights.withIndex()) {
        val bx = x + (i - 2) * blk * 0.6f
        for (j in 0 until h) {
            val frac = j.toFloat() / h
            px(bx + sw * frac, baseY - j * blk, blk * 0.5f, colors[i].copy(alpha = 0.8f))
        }
    }
}

// ── Mushroom ──

private fun DrawScope.drawMushroom(x: Float, baseY: Float, blk: Float) {
    // Stem
    drawRect(
        color = Color(0xFFD0C0A0),
        topLeft = Offset(x - blk * 0.3f, baseY - blk * 2),
        size = Size(blk * 0.6f, blk * 2)
    )
    // Cap
    drawOval(
        color = Color(0xFFA06030),
        topLeft = Offset(x - blk * 1.2f, baseY - blk * 3.2f),
        size = Size(blk * 2.4f, blk * 1.5f)
    )
    // Cap highlight
    drawOval(
        color = Color(0xFFB87040).copy(alpha = 0.5f),
        topLeft = Offset(x - blk * 0.5f, baseY - blk * 3f),
        size = Size(blk * 1f, blk * 0.6f)
    )
    // Spots
    drawCircle(color = Color(0xFFE0D0B0).copy(alpha = 0.6f), radius = blk * 0.2f, center = Offset(x - blk * 0.5f, baseY - blk * 2.6f))
    drawCircle(color = Color(0xFFE0D0B0).copy(alpha = 0.5f), radius = blk * 0.15f, center = Offset(x + blk * 0.4f, baseY - blk * 2.8f))
}

// ── Moss-covered rock ──

private fun DrawScope.drawMossRock(x: Float, baseY: Float, blk: Float) {
    val rw = blk * 3
    val rh = blk * 2

    // Rock body
    drawOval(
        color = Color(0xFF6A6A5A),
        topLeft = Offset(x - rw / 2, baseY - rh),
        size = Size(rw, rh)
    )
    // Rock highlight
    drawOval(
        color = Color(0xFF7A7A6A).copy(alpha = 0.5f),
        topLeft = Offset(x - rw * 0.3f, baseY - rh * 0.9f),
        size = Size(rw * 0.5f, rh * 0.4f)
    )
    // Moss on top
    drawOval(
        color = MossColor.copy(alpha = 0.7f),
        topLeft = Offset(x - rw * 0.4f, baseY - rh * 1.05f),
        size = Size(rw * 0.8f, rh * 0.35f)
    )
    // Moss drip on side
    drawOval(
        color = MossColor.copy(alpha = 0.4f),
        topLeft = Offset(x + rw * 0.15f, baseY - rh * 0.7f),
        size = Size(rw * 0.2f, rh * 0.4f)
    )
}

// ── Dragonfly ──

private fun DrawScope.drawDragonfly(
    x: Float, y: Float, blk: Float,
    accentColor: Color, wingsUp: Boolean
) {
    // Thin body (horizontal, 5 blocks)
    for (i in -2..2) {
        px(x + i * blk, y, blk, DfBody)
    }
    // Tail (2 thinner)
    px(x - 3 * blk, y, blk * 0.7f, DfBody.copy(alpha = 0.7f))
    px(x - 4 * blk, y, blk * 0.5f, DfBody.copy(alpha = 0.5f))
    // Head
    px(x + 3 * blk, y, blk, accentColor.copy(alpha = 0.7f))
    // Eyes
    px(x + 3 * blk, y - blk, blk * 0.7f, accentColor)

    // Wings (4 wings, translucent)
    if (wingsUp) {
        // Upper left wing
        drawOval(
            color = DfWing.copy(alpha = 0.4f),
            topLeft = Offset(x - blk, y - blk * 4),
            size = Size(blk * 4, blk * 3)
        )
        // Upper right wing
        drawOval(
            color = DfWing.copy(alpha = 0.35f),
            topLeft = Offset(x - blk, y + blk),
            size = Size(blk * 3.5f, blk * 2.5f)
        )
        // Wing veins (accent color)
        drawRect(color = accentColor.copy(alpha = 0.2f),
            topLeft = Offset(x, y - blk * 3), size = Size(blk * 2, blk * 0.5f))
    } else {
        // Wings down
        drawOval(
            color = DfWing.copy(alpha = 0.35f),
            topLeft = Offset(x - blk, y + blk),
            size = Size(blk * 4, blk * 3.5f)
        )
        drawOval(
            color = DfWing.copy(alpha = 0.4f),
            topLeft = Offset(x - blk, y - blk * 3),
            size = Size(blk * 3.5f, blk * 2)
        )
        drawRect(color = accentColor.copy(alpha = 0.2f),
            topLeft = Offset(x, y + blk * 2), size = Size(blk * 2, blk * 0.5f))
    }
}
