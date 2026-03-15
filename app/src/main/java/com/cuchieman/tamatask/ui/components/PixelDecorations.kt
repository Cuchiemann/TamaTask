package com.cuchieman.tamatask.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.cuchieman.tamatask.ui.theme.TamaCyan
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.theme.TamaYellow

/**
 * Data class for a pixel decoration element
 */
data class PixelDecoration(
    val xFraction: Float,   // 0..1 position relative to screen width
    val yFraction: Float,   // 0..1 position relative to screen height
    val type: DecorationType,
    val color: Color,
    val sizePx: Float = 12f,
    val alpha: Float = 0.7f
)

enum class DecorationType {
    STAR, HEART, DIAMOND, SMALL_SQUARE, CROSS
}

/**
 * Draws pixel art floating decorations across the screen.
 * Stars, hearts, diamonds in vibrant Tamagotchi colors.
 */
@Composable
fun PixelDecorationsOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val decorations = remember {
        listOf(
            // Top area scattered
            PixelDecoration(0.08f, 0.06f, DecorationType.STAR, TamaYellow, 14f, 0.8f),
            PixelDecoration(0.85f, 0.04f, DecorationType.HEART, TamaPink, 12f, 0.6f),
            PixelDecoration(0.45f, 0.08f, DecorationType.DIAMOND, TamaCyan, 10f, 0.5f),
            PixelDecoration(0.22f, 0.12f, DecorationType.SMALL_SQUARE, TamaPurple, 8f, 0.4f),
            PixelDecoration(0.72f, 0.10f, DecorationType.CROSS, TamaOrange, 10f, 0.5f),

            // Upper-mid area
            PixelDecoration(0.92f, 0.18f, DecorationType.STAR, TamaGreen, 10f, 0.6f),
            PixelDecoration(0.05f, 0.22f, DecorationType.DIAMOND, TamaOrange, 12f, 0.5f),
            PixelDecoration(0.55f, 0.15f, DecorationType.SMALL_SQUARE, TamaPink, 6f, 0.3f),
            PixelDecoration(0.35f, 0.20f, DecorationType.HEART, TamaYellow, 8f, 0.4f),

            // Mid area
            PixelDecoration(0.12f, 0.40f, DecorationType.STAR, TamaCyan, 12f, 0.5f),
            PixelDecoration(0.90f, 0.38f, DecorationType.HEART, TamaPurple, 10f, 0.4f),
            PixelDecoration(0.75f, 0.45f, DecorationType.SMALL_SQUARE, TamaGreen, 8f, 0.3f),

            // Lower-mid
            PixelDecoration(0.08f, 0.60f, DecorationType.CROSS, TamaPink, 10f, 0.4f),
            PixelDecoration(0.88f, 0.58f, DecorationType.DIAMOND, TamaYellow, 10f, 0.5f),
            PixelDecoration(0.50f, 0.65f, DecorationType.STAR, TamaOrange, 8f, 0.3f),

            // Bottom area
            PixelDecoration(0.15f, 0.80f, DecorationType.HEART, TamaCyan, 12f, 0.5f),
            PixelDecoration(0.78f, 0.78f, DecorationType.STAR, TamaPink, 14f, 0.6f),
            PixelDecoration(0.42f, 0.85f, DecorationType.DIAMOND, TamaGreen, 10f, 0.4f),
            PixelDecoration(0.92f, 0.82f, DecorationType.SMALL_SQUARE, TamaPurple, 8f, 0.3f),
            PixelDecoration(0.25f, 0.90f, DecorationType.CROSS, TamaYellow, 10f, 0.4f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        decorations.forEach { dec ->
            val cx = dec.xFraction * w
            val cy = dec.yFraction * h
            val s = dec.sizePx * density
            val color = dec.color.copy(alpha = dec.alpha * alpha)

            when (dec.type) {
                DecorationType.STAR -> drawPixelStar(cx, cy, s, color)
                DecorationType.HEART -> drawPixelHeart(cx, cy, s, color)
                DecorationType.DIAMOND -> drawPixelDiamond(cx, cy, s, color)
                DecorationType.SMALL_SQUARE -> drawPixelSquare(cx, cy, s, color)
                DecorationType.CROSS -> drawPixelCross(cx, cy, s, color)
            }
        }
    }
}

// === Pixel art shape drawing functions ===

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBlock(
    x: Float, y: Float, blockSize: Float, color: Color
) {
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(blockSize, blockSize)
    )
}

/**
 * Pixel star: a plus/cross shape with center filled
 *   X
 *  XXX
 *   X
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelStar(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val b = size / 3f
    // Center
    drawPixelBlock(cx - b / 2, cy - b / 2, b, color)
    // Top
    drawPixelBlock(cx - b / 2, cy - b / 2 - b, b, color)
    // Bottom
    drawPixelBlock(cx - b / 2, cy - b / 2 + b, b, color)
    // Left
    drawPixelBlock(cx - b / 2 - b, cy - b / 2, b, color)
    // Right
    drawPixelBlock(cx - b / 2 + b, cy - b / 2, b, color)
}

/**
 * Pixel heart:
 *  X X
 * XXXXX
 *  XXX
 *   X
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelHeart(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val b = size / 5f
    // Top bumps
    drawPixelBlock(cx - b * 1.5f, cy - b, b, color)
    drawPixelBlock(cx + b * 0.5f, cy - b, b, color)
    // Middle row full
    for (i in -2..2) {
        drawPixelBlock(cx + i * b - b / 2, cy, b, color)
    }
    // Lower row
    for (i in -1..1) {
        drawPixelBlock(cx + i * b - b / 2, cy + b, b, color)
    }
    // Bottom point
    drawPixelBlock(cx - b / 2, cy + b * 2, b, color)
}

/**
 * Pixel diamond:
 *   X
 *  XXX
 * XXXXX
 *  XXX
 *   X
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelDiamond(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val b = size / 5f
    // Top
    drawPixelBlock(cx - b / 2, cy - b * 2, b, color)
    // Row 2
    for (i in -1..1) drawPixelBlock(cx + i * b - b / 2, cy - b, b, color)
    // Center row
    for (i in -2..2) drawPixelBlock(cx + i * b - b / 2, cy, b, color)
    // Row 4
    for (i in -1..1) drawPixelBlock(cx + i * b - b / 2, cy + b, b, color)
    // Bottom
    drawPixelBlock(cx - b / 2, cy + b * 2, b, color)
}

/**
 * Simple pixel square 2x2
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelSquare(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val b = size / 2f
    drawPixelBlock(cx - b, cy - b, b, color)
    drawPixelBlock(cx, cy - b, b, color)
    drawPixelBlock(cx - b, cy, b, color)
    drawPixelBlock(cx, cy, b, color)
}

/**
 * Pixel cross / plus shape
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelCross(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val b = size / 3f
    drawPixelBlock(cx - b / 2, cy - b * 1.5f, b, color)
    drawPixelBlock(cx - b * 1.5f, cy - b / 2, b, color)
    drawPixelBlock(cx - b / 2, cy - b / 2, b, color)
    drawPixelBlock(cx + b / 2, cy - b / 2, b, color)
    drawPixelBlock(cx - b / 2, cy + b / 2, b, color)
}
