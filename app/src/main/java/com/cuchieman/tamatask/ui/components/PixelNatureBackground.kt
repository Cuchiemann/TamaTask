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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import com.cuchieman.tamatask.R
import kotlin.math.cos
import kotlin.math.sin

// ── Panoramic config ──────────────────────────────────────────
// The panorama is PANORAMA_RATIO times wider than the visible screen
private const val PANORAMA_RATIO = 3.5f

// ── Sprite reference ──────────────────────────────────────────
// The Spinosaurus sprite: 717 source px displayed at 313.dp
// → 1 sprite pixel = 313/717 ≈ 0.4365 dp on screen
// We use this to compute the block size so details match the sprite.
private const val SPRITE_SOURCE_PX = 717f
private const val SPRITE_DISPLAY_DP = 313f

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

// Water — mangrove swamp palette: murky greens/teals, dark far → light near shore
private val WaterBand1 = Color(0xFF1A3D35)   // darkest — far horizon
private val WaterBand2 = Color(0xFF1F4A40)
private val WaterBand3 = Color(0xFF24554A)
private val WaterBand4 = Color(0xFF2A6155)
private val WaterBand5 = Color(0xFF316D5F)
private val WaterBand6 = Color(0xFF3A7A6A)
private val WaterBand7 = Color(0xFF448876)
private val WaterBand8 = Color(0xFF509682)
private val WaterBand9 = Color(0xFF5EA48F)   // lightest — near shore
private val WaveFoam = Color(0xFFB0DDD5)

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
    val xFraction: Float,    // position in panorama (0..1)
    val spriteIndex: Int,    // which sprite (0, 1, or 2)
    val scale: Float,        // display scale multiplier
    val flipH: Boolean = false, // mirror horizontally for variety
    val yOffset: Float = 0f  // vertical offset fraction (-0.05..0.05) for natural scatter
)

private data class VegetationData(
    val xFraction: Float,
    val yFraction: Float,  // 0..1 position within terrain
    val type: Int,  // 0=fern, 1=bush, 2=grass_tuft, 3=mushroom, 4=moss_rock
    val sizeMul: Float,
    val phase: Float
)

// ── Main composable ──────────────────────────────────────────

/**
 * @param foregroundOnly when true, only draws vegetation that is below [dinoFeetScreenFrac].
 *        Use this as an overlay on top of the dino sprite for depth sorting.
 * @param dinoFeetScreenFrac dino feet position as fraction of screen height (0..1).
 */
@Composable
fun PixelNatureBackground(
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0.5f,  // 0..1 — position in panorama
    foregroundOnly: Boolean = false,
    dinoFeetScreenFrac: Float = 1f
) {
    val density = LocalDensity.current.density

    // Load tree sprites: araucaria, cycad (palmera), tree fern
    val treeSprites = listOf(
        ImageBitmap.imageResource(R.drawable.veg_araucaria),  // 0 = conifer
        ImageBitmap.imageResource(R.drawable.veg_cycad),       // 1 = cycad/palmera
        ImageBitmap.imageResource(R.drawable.veg_treefern)     // 2 = tree fern
    )

    // Load ground vegetation sprites
    val vegFernSprite = ImageBitmap.imageResource(R.drawable.veg_fern)
    val vegReedsSprite = ImageBitmap.imageResource(R.drawable.veg_reeds)

    val infiniteTransition = rememberInfiniteTransition(label = "swamp")

    // Fog drift
    val fogDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(120_000, easing = LinearEasing), RepeatMode.Restart
        ), label = "fog"
    )

    // Water wave (primary) — full cycle 0→2π, long duration to avoid visible reset
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 62.832f,
        animationSpec = infiniteRepeatable(
            tween(30_000, easing = LinearEasing), RepeatMode.Restart
        ), label = "wave1"
    )

    // Water wave (secondary, slower)
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 62.832f,
        animationSpec = infiniteRepeatable(
            tween(50_000, easing = LinearEasing), RepeatMode.Restart
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
            tween(15_625, easing = LinearEasing), RepeatMode.Restart
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
            // More dispersed: spread across panorama with varied depths & sizes
            LilyPadData(0.03f, 0.15f, 1.0f, true, 0f),
            LilyPadData(0.12f, 0.55f, 0.7f, false, 1.4f),
            LilyPadData(0.28f, 0.30f, 1.1f, true, 2.8f),
            LilyPadData(0.38f, 0.70f, 0.6f, false, 4.2f),
            LilyPadData(0.52f, 0.18f, 0.9f, true, 3.5f),
            LilyPadData(0.65f, 0.48f, 1.0f, false, 5.0f),
            LilyPadData(0.78f, 0.25f, 0.8f, true, 2.3f),
            LilyPadData(0.90f, 0.60f, 0.7f, false, 1.6f),
            LilyPadData(0.08f, 0.68f, 0.6f, false, 5.5f),
            LilyPadData(0.45f, 0.40f, 0.8f, true, 0.9f)
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

    // Mangrove trees — 42 trees (3x density), randomly placed
    val mangroveTrees = remember {
        val rng = java.util.Random(123L)
        (0 until 42).map {
            MangroveTreeData(
                xFraction = rng.nextFloat(),
                spriteIndex = rng.nextInt(3),
                scale = 0.65f + rng.nextFloat() * 0.55f, // 0.65-1.20
                flipH = rng.nextBoolean(),
                yOffset = rng.nextFloat() * 0.03f // 0 to +0.03 (never float above anchor)
            )
        }
    }

    // Vegetation — fully random placement using seeded Random
    val vegetation = remember {
        val rng = java.util.Random(42L) // fixed seed for deterministic layout
        val types = intArrayOf(0, 1, 2, 2, 1, 0, 2, 0) // no rocks, ferns/bush/grass only
        (0 until 55).map { i ->
            val type = types[rng.nextInt(types.size)]
            val sizeMul = when (type) {
                4 -> 2.0f + rng.nextFloat() * 1.5f   // rocks: 2.0-3.5
                1 -> 2.5f + rng.nextFloat() * 2.0f   // bushes: 2.5-4.5
                else -> 2.0f + rng.nextFloat() * 0.9f // ferns/grass: 2.0-2.9
            }
            VegetationData(
                xFraction = rng.nextFloat(),
                yFraction = 0.05f + rng.nextFloat() * 0.80f,
                type = type,
                sizeMul = sizeMul,
                phase = rng.nextFloat() * 6.28f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Block size matching the sprite's pixel resolution
        // 1 sprite pixel = SPRITE_DISPLAY_DP * density / SPRITE_SOURCE_PX
        val spritePx = SPRITE_DISPLAY_DP * density / SPRITE_SOURCE_PX
        // For detail elements we use a visible "art block" scaled 2.5x for new sprite
        val blk = spritePx * 3f * 2.5f

        // Panorama scroll: how much of the panorama is offscreen
        val panoramaWidth = w * PANORAMA_RATIO
        val maxScroll = panoramaWidth - w
        val scrollPx = scrollOffset.coerceIn(0f, 1f) * maxScroll

        // ── Layout (fractions of screen height) ──
        val skyEnd = h * 0.42f
        val mangroveY = h * 0.40f
        val waterTop = h * 0.56f
        val waterBottom = h * 0.78f
        val shoreTop = h * 0.75f

        // ── Dino feet Y in screen pixels for depth sorting ──
        val dinoFeetPx = dinoFeetScreenFrac * h

        if (!foregroundOnly) {
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
        // Draw trees sorted by scale (largest/farthest first)
        // Each sprite pixel = blk screen pixels (matches background pixel art)
        mangroveTrees.sortedByDescending { it.scale }.forEach { tree ->
            val worldX = tree.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 80 && screenX < w + blk * 80) {
                val sprite = treeSprites[tree.spriteIndex]
                // Scale per tree type: cycad (1) smaller, araucaria (0) & treefern (2) bigger
                val typeScale = when (tree.spriteIndex) {
                    1 -> 0.35f   // cycad/palmera — smaller
                    else -> 0.7f // araucaria & tree fern — bigger
                }
                val pxSize = spritePx * tree.scale * typeScale
                val drawW = sprite.width * pxSize
                val drawH = sprite.height * pxSize
                val drawX = screenX - drawW / 2
                // Anchor: tree bottom sits at water line, offset only pushes down
                val drawY = waterTop - drawH + tree.yOffset * h

                if (tree.flipH) {
                    withTransform({
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(screenX, drawY + drawH / 2))
                    }) {
                        drawImage(
                            image = sprite,
                            dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                            dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                            filterQuality = FilterQuality.None
                        )
                    }
                } else {
                    drawImage(
                        image = sprite,
                        dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                        filterQuality = FilterQuality.None
                    )
                }
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

        // (foam lines removed for cleaner water look)

        // ══════════════════════════════════════════
        // 6. REEDS along the shore
        // ══════════════════════════════════════════
        reeds.forEach { cluster ->
            val worldX = cluster.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 12 && screenX < w + blk * 12) {
                val reedH = blk * cluster.baseHeight * 1.5f
                val reedW = reedH * vegReedsSprite.width / vegReedsSprite.height
                drawImage(
                    image = vegReedsSprite,
                    dstOffset = IntOffset((screenX - reedW / 2).toInt(), (waterTop - reedH + reedH * 0.3f).toInt()),
                    dstSize = IntSize(reedW.toInt(), reedH.toInt()),
                    filterQuality = FilterQuality.None
                )
            }
        }

        // ══════════════════════════════════════════
        // 7. SHORE / TERRAIN
        // ══════════════════════════════════════════
        drawShoreTerrain(shoreTop, h, w, blk, scrollPx, panoramaWidth)
        drawShoreDetails(shoreTop, h, w, blk, scrollPx, panoramaWidth)
        } // end if (!foregroundOnly)

        // ══════════════════════════════════════════
        // 7b. VEGETATION covering terrain (organic scatter)
        //     Split by depth: behind-dino vs in-front-of-dino
        // ══════════════════════════════════════════
        val terrainH = h - shoreTop
        vegetation.forEach { veg ->
            val worldX = veg.xFraction * panoramaWidth
            val screenX = worldX - scrollPx
            if (screenX > -blk * 15 && screenX < w + blk * 15) {
                val yFrac = veg.yFraction
                val vegY = (shoreTop + terrainH * yFrac).coerceAtLeast(shoreTop + blk * 3)
                // Perspective: plants further back (lower yFrac) are smaller
                val perspScale = 0.8f + yFrac * 0.4f
                // Depth test: vegetation below dino feet = foreground
                val isForeground = vegY > dinoFeetPx
                if (isForeground == foregroundOnly) {
                    val sz = blk * veg.sizeMul * perspScale * 6f  // slightly smaller ferns
                    // All ground vegetation uses fern sprite now
                    val sprite = vegFernSprite
                    val drawH = sz
                    val drawW = drawH * sprite.width / sprite.height
                    drawImage(
                        image = sprite,
                        dstOffset = IntOffset((screenX - drawW / 2).toInt(), (vegY - drawH).toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }

        if (!foregroundOnly) {
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
        } // end if (!foregroundOnly) for dragonflies/wind
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

}

// ── Water body (smooth gradients + wave shapes) ──

private fun DrawScope.drawWaterBody(
    topY: Float, bottomY: Float, screenWidth: Float,
    wave1: Float, wave2: Float, shimmer: Float
) {
    val waterHeight = bottomY - topY

    // 9 color bands with wavy irregular edges between them
    val bandColors = listOf(
        WaterBand1, WaterBand2, WaterBand3, WaterBand4, WaterBand5,
        WaterBand6, WaterBand7, WaterBand8, WaterBand9
    )
    val bandCount = bandColors.size

    // Draw each band from top to bottom with a wavy top edge
    val waveStep = 6f
    val steps = (screenWidth / waveStep).toInt() + 1

    // First fill water area with darkest color, using wavy top edge
    val basePath = Path()
    val baseAmp1 = waterHeight * 0.015f
    val baseAmp2 = waterHeight * 0.008f
    basePath.moveTo(-10f, bottomY + 10f)
    basePath.lineTo(-10f, topY)
    for (i in 0..steps) {
        val x = i * waveStep
        val wy = sin(x * 0.009f + wave1 * 0.15f) * baseAmp1 +
                sin(x * 0.022f + 1.2f + wave2 * 0.1f) * baseAmp2
        basePath.lineTo(x, topY + wy)
    }
    basePath.lineTo(screenWidth + 10f, bottomY + 10f)
    basePath.close()
    drawPath(basePath, color = bandColors[0])

    for (band in 1 until bandCount) {
        val bandFrac = band.toFloat() / bandCount
        val baseY = topY + waterHeight * bandFrac

        // Wavy top edge — each band has unique wave pattern
        val seed1 = band * 1.7f + 0.3f
        val seed2 = band * 2.3f + 1.1f
        val seed3 = band * 0.7f + 2.5f
        val amp1 = waterHeight * 0.012f * (1f + (band % 3) * 0.3f)
        val amp2 = waterHeight * 0.007f * (1f + (band % 2) * 0.4f)
        val amp3 = waterHeight * 0.004f

        val edgePoints = mutableListOf<Offset>()
        for (i in 0..steps) {
            val x = i * waveStep
            val wy = sin(x * 0.008f + seed1 + wave1 * 0.15f) * amp1 +
                    sin(x * 0.019f + seed2 + wave2 * 0.1f) * amp2 +
                    sin(x * 0.037f + seed3) * amp3
            edgePoints.add(Offset(x, baseY + wy))
        }

        // Build path: bottom → wavy top edge → close
        val bandPath = Path()
        bandPath.moveTo(-10f, bottomY + 10f)
        bandPath.lineTo(-10f, edgePoints.first().y)
        for (pt in edgePoints) {
            bandPath.lineTo(pt.x, pt.y)
        }
        bandPath.lineTo(screenWidth + 10f, edgePoints.last().y)
        bandPath.lineTo(screenWidth + 10f, bottomY + 10f)
        bandPath.close()

        drawPath(bandPath, color = bandColors[band])
    }

    // Subtle dark mottling/texture spots scattered across water
    // Gives the murky mangrove water character
    val golden = 0.618033f
    var acc = 0.11f
    for (i in 0 until 25) {
        acc = (acc + golden) % 1f
        val spotX = acc * screenWidth
        val spotYFrac = ((i * 7 + 3) % 13) / 13f
        val spotY = topY + waterHeight * spotYFrac
        val spotR = waterHeight * (0.015f + (i % 4) * 0.008f)
        val spotAlpha = 0.04f + (i % 3) * 0.02f
        drawCircle(
            color = WaterBand1.copy(alpha = spotAlpha),
            radius = spotR,
            center = Offset(spotX, spotY)
        )
    }
}

// ── Foam lines over water ──

private fun DrawScope.drawWaveFoamLines(
    topY: Float, bottomY: Float, screenWidth: Float,
    wave1: Float, wave2: Float, blk: Float
) {
    val waterHeight = bottomY - topY

    // Scattered short foam dashes instead of continuous lines
    // Use golden ratio distribution for organic spacing
    val golden = 0.618033f
    var acc = 0.137f

    // ~30 small foam segments scattered across the water
    for (i in 0 until 30) {
        acc = (acc + golden) % 1f
        val xFrac = acc
        val yBand = (i * 7 + 3) % 5  // 0..4 distributes across water depth
        val yFrac = 0.05f + yBand * 0.18f  // 0.05..0.77

        val baseX = xFrac * screenWidth
        val baseY = topY + waterHeight * yFrac

        // Wave offset for animation
        val wy = sin(baseX * 0.015f + wave1 + i * 0.7f) * blk * 1.0f +
                sin(baseX * 0.03f + wave2 + i * 0.4f) * blk * 0.5f

        // Short dash: 3-7 blk wide, slightly wavy
        val dashLen = blk * (3f + (i * 5 + 2) % 5)
        val dashY = baseY + wy

        // Draw as a small wavy path (3-4 points)
        val foamPath = Path()
        foamPath.moveTo(baseX, dashY)
        val segs = 3
        for (s in 1..segs) {
            val sx = baseX + dashLen * s / segs
            val sy = dashY + sin(sx * 0.05f + wave1 * 2f) * blk * 0.4f
            foamPath.lineTo(sx, sy)
        }

        val alpha = 0.12f + sin(wave1 + i * 1.3f) * 0.06f  // subtle pulsing
        val thickness = blk * (0.3f + (i % 3) * 0.1f)
        drawPath(
            foamPath,
            color = WaveFoam.copy(alpha = alpha.coerceIn(0.06f, 0.22f)),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness)
        )
    }
}

// ── Lily pads ──

private fun DrawScope.drawLilyPad(
    x: Float, y: Float, radius: Float, hasFlower: Boolean
) {
    val b = radius  // block size for pixel art

    // Pixel art lily pad — flat oval shape with notch
    // Row pattern (relative to center): wider in middle, narrow at edges
    //   Row -2:      ##__##
    //   Row -1:     ########
    //   Row  0:     ####_###   (notch on right side)
    //   Row +1:      ######
    //   Row +2:       ####

    // Dark outline / base
    px(x - b * 2, y - b, b, LilyPadDark)
    px(x - b, y - b, b, LilyPadDark)
    px(x + b, y - b, b, LilyPadDark)
    px(x + b * 2, y - b, b, LilyPadDark)

    px(x - b * 3, y, b, LilyPadDark)
    px(x - b * 2, y, b, LilyPadLight)
    px(x - b, y, b, LilyPadLight)
    px(x, y, b, LilyPadLight)
    px(x + b, y, b, LilyPadDark)
    px(x + b * 2, y, b, LilyPadDark)
    px(x + b * 3, y, b, LilyPadDark)

    // Center rows with notch
    px(x - b * 3, y + b, b, LilyPadDark)
    px(x - b * 2, y + b, b, LilyPadLight)
    px(x - b, y + b, b, LilyPadLight)
    // notch gap at x, y+b
    px(x + b, y + b, b, LilyPadLight)
    px(x + b * 2, y + b, b, LilyPadDark)

    px(x - b * 2, y + b * 2, b, LilyPadDark)
    px(x - b, y + b * 2, b, LilyPadDark)
    px(x, y + b * 2, b, LilyPadDark)
    px(x + b, y + b * 2, b, LilyPadDark)

    if (hasFlower) {
        // Small pixel flower on top of pad
        val fy = y - b * 2
        px(x, fy, b, LilyFlower)
        px(x - b, fy + b * 0.5f, b * 0.8f, LilyFlower.copy(alpha = 0.8f))
        px(x + b, fy + b * 0.5f, b * 0.8f, LilyFlower.copy(alpha = 0.7f))
        px(x, fy + b, b * 0.6f, LilyCenter) // center dot
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

    // (grass tufts along shore edge removed)
}

// Shore detail items — fixed world positions, stable when scrolling
private data class ShoreDetail(
    val xFraction: Float,  // 0..1 in panorama
    val yOffset: Float,    // blk multiplier from shore top
    val size: Float,       // blk multiplier
    val type: Int          // 0=mud patch, 1=puddle, 2=stone
)

private val shoreDetails: List<ShoreDetail> = run {
    val list = mutableListOf<ShoreDetail>()
    val golden = 0.618033f
    var acc = 0.09f
    for (i in 0 until 35) {
        acc = (acc + golden) % 1f
        val yOff = 1.5f + (i * 7 + 2) % 8 * 1.2f  // 1.5..11.1 blk from shore top
        val sz = 2f + (i * 5 + 3) % 4 * 0.7f       // 2..4.1
        val type = when {
            i % 5 == 0 -> 2  // stone (20%)
            i % 3 == 0 -> 1  // puddle (27%)
            else -> 0        // mud patch (53%)
        }
        list.add(ShoreDetail(acc, yOff, sz, type))
    }
    list
}

private fun DrawScope.drawShoreDetails(
    topY: Float, screenHeight: Float, screenWidth: Float,
    blk: Float, scrollPx: Float, panoramaWidth: Float
) {
    for (detail in shoreDetails) {
        val worldX = detail.xFraction * panoramaWidth
        val screenX = worldX - scrollPx
        if (screenX < -blk * 8 || screenX > screenWidth + blk * 8) continue

        val py = topY + blk * detail.yOffset
        if (py + blk * 3 > screenHeight) continue
        val sz = blk * detail.size

        when (detail.type) {
            0 -> {
                // Mud patch
                drawOval(
                    color = MudWet.copy(alpha = 0.4f),
                    topLeft = Offset(screenX, py),
                    size = Size(sz, sz * 0.5f)
                )
            }
            1 -> {
                // Puddle
                val pw = sz * 1.2f
                val ph = sz * 0.5f
                drawOval(
                    color = PuddleColor.copy(alpha = 0.35f),
                    topLeft = Offset(screenX, py),
                    size = Size(pw, ph)
                )
                drawOval(
                    color = WaveFoam.copy(alpha = 0.1f),
                    topLeft = Offset(screenX + pw * 0.2f, py + ph * 0.1f),
                    size = Size(pw * 0.4f, ph * 0.3f)
                )
            }
            2 -> {
                // Stone — small oval with highlight
                val sw = sz * 0.8f
                val sh = sz * 0.5f
                drawOval(
                    color = Color(0xFF5A5A4A),
                    topLeft = Offset(screenX, py),
                    size = Size(sw, sh)
                )
                drawOval(
                    color = Color(0xFF7A7A6A).copy(alpha = 0.5f),
                    topLeft = Offset(screenX + sw * 0.15f, py + sh * 0.1f),
                    size = Size(sw * 0.4f, sh * 0.35f)
                )
            }
        }
    }
}

// ── Mangrove tree (individual, foreground) — thick trunk, forking branches, big dome canopy ──

// drawMangroveTree removed — now using sprite-based trees

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
