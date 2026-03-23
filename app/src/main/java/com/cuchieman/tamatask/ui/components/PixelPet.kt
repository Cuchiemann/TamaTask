package com.cuchieman.tamatask.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.delay

// ── Idle animation ──
private const val IDLE_FRAMES = 36
private const val IDLE_FRAME_W = 717   // px per frame in strip
private const val IDLE_FRAME_H = 252   // px height
private const val IDLE_DELAY_MS = 200L // ~5 FPS

// ── Walk animation ──
private const val WALK_FRAMES = 36
private const val WALK_FRAME_W = 694   // px per frame in strip
private const val WALK_FRAME_H = 229   // px height
private const val WALK_DELAY_MS = 100L // ~10 FPS — snappier walk cycle

// Display sizes (dp) — unified height so idle/walk dino looks same size
// Both use 110dp height; width preserves aspect ratio
// Idle: 717×252 → 313×110 dp (0.4365 dp/px)
// Walk: 694×229 → 333×110 dp (0.4803 dp/px)
private val IDLE_DISPLAY_W = 313.dp
private val IDLE_DISPLAY_H = 110.dp
private val WALK_DISPLAY_W = 333.dp
private val WALK_DISPLAY_H = 110.dp

/**
 * Animated pixel art Spinosaurus mirabilis.
 * Supports idle and walk animations with directional flipping.
 *
 * @param isWalking true = walk animation, false = idle animation
 * @param facingLeft true = sprite flipped horizontally (walking left)
 */
@Composable
fun PixelSpinosaurus(
    modifier: Modifier = Modifier,
    isWalking: Boolean = false,
    facingLeft: Boolean = false
) {
    val context = LocalContext.current

    // Load idle spritestrip
    val idleFrames = remember {
        loadSpriteFrames(context, "spino_idle_strip", IDLE_FRAMES, IDLE_FRAME_W, IDLE_FRAME_H)
    }

    // Load walk spritestrip
    val walkFrames = remember {
        loadSpriteFrames(context, "spino_walk_strip", WALK_FRAMES, WALK_FRAME_W, WALK_FRAME_H)
    }

    val activeFrames = if (isWalking && walkFrames.isNotEmpty()) walkFrames else idleFrames
    val activeDelay = if (isWalking) WALK_DELAY_MS else IDLE_DELAY_MS

    // Current frame index — reset when switching animation
    var currentFrame by remember(isWalking) { mutableIntStateOf(0) }

    // Animate through frames
    LaunchedEffect(isWalking, activeFrames) {
        if (activeFrames.isNotEmpty()) {
            while (true) {
                delay(activeDelay)
                currentFrame = (currentFrame + 1) % activeFrames.size
            }
        }
    }

    // Display size depends on animation
    val displayW = if (isWalking && walkFrames.isNotEmpty()) WALK_DISPLAY_W else IDLE_DISPLAY_W
    val displayH = if (isWalking && walkFrames.isNotEmpty()) WALK_DISPLAY_H else IDLE_DISPLAY_H

    // Draw current frame
    if (activeFrames.isNotEmpty()) {
        Image(
            bitmap = activeFrames[currentFrame],
            contentDescription = "Spinosaurus",
            modifier = modifier
                .width(displayW)
                .height(displayH)
                // Flip horizontally when facing left
                .scale(scaleX = if (facingLeft) -1f else 1f, scaleY = 1f),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None // Nearest-neighbor for crisp pixels
        )
    }
}

/**
 * Load a horizontal spritestrip from drawable resources and split into individual frames.
 */
private fun loadSpriteFrames(
    context: android.content.Context,
    resourceName: String,
    totalFrames: Int,
    frameWidth: Int,
    frameHeight: Int
): List<ImageBitmap> {
    val options = BitmapFactory.Options().apply {
        inScaled = false
    }
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    if (resId == 0) return emptyList()

    val strip = BitmapFactory.decodeResource(context.resources, resId, options) ?: return emptyList()

    val frameList = mutableListOf<ImageBitmap>()
    for (i in 0 until totalFrames) {
        val x = i * frameWidth
        if (x + frameWidth <= strip.width) {
            val frameBmp = Bitmap.createBitmap(strip, x, 0, frameWidth, frameHeight)
            frameList.add(frameBmp.asImageBitmap())
        }
    }
    strip.recycle()
    return frameList
}
