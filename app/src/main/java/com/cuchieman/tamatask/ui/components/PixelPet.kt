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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cuchieman.tamatask.data.model.DinoSpec
import kotlinx.coroutines.delay

// ── Per-dino sprite config ──
private data class SpriteConfig(
    val idleFrames: Int,
    val idleFrameW: Int,
    val idleFrameH: Int,
    val idleCols: Int,        // columns in grid (1 = horizontal strip)
    val walkFrames: Int,
    val walkFrameW: Int,
    val walkFrameH: Int,
    val walkCols: Int,
    val idleDelayMs: Long,
    val walkDelayMs: Long,
    val displayH: Dp
)

private val DINO_CONFIGS = mapOf(
    "spino" to SpriteConfig(
        idleFrames = 36, idleFrameW = 717, idleFrameH = 252, idleCols = 36,
        walkFrames = 36, walkFrameW = 694, walkFrameH = 229, walkCols = 36,
        idleDelayMs = 200L, walkDelayMs = 100L, displayH = 110.dp
    ),
    "stego" to SpriteConfig(
        idleFrames = 36, idleFrameW = 711, idleFrameH = 294, idleCols = 6,
        walkFrames = 36, walkFrameW = 763, walkFrameH = 300, walkCols = 6,
        idleDelayMs = 140L, walkDelayMs = 140L, displayH = 85.dp
    )
)

/**
 * Generic animated pixel art dinosaur.
 * Backwards compatible — PixelSpinosaurus calls this with spino dino.
 */
@Composable
fun PixelDino(
    dino: DinoSpec,
    modifier: Modifier = Modifier,
    isWalking: Boolean = false,
    facingLeft: Boolean = false
) {
    val context = LocalContext.current
    val config = DINO_CONFIGS[dino.id] ?: return

    val idleFrames = remember(dino.id) {
        dino.idleStripRes?.let {
            loadSpriteGrid(context, it, config.idleFrames, config.idleFrameW, config.idleFrameH, config.idleCols)
        } ?: emptyList()
    }

    val walkFrames = remember(dino.id) {
        dino.walkStripRes?.let {
            loadSpriteGrid(context, it, config.walkFrames, config.walkFrameW, config.walkFrameH, config.walkCols)
        } ?: emptyList()
    }

    val activeFrames = if (isWalking && walkFrames.isNotEmpty()) walkFrames else idleFrames
    val activeDelay = if (isWalking) config.walkDelayMs else config.idleDelayMs

    var currentFrame by remember(isWalking, dino.id) { mutableIntStateOf(0) }

    LaunchedEffect(isWalking, dino.id, activeFrames) {
        if (activeFrames.isNotEmpty()) {
            while (true) {
                delay(activeDelay)
                currentFrame = (currentFrame + 1) % activeFrames.size
            }
        }
    }

    if (activeFrames.isNotEmpty()) {
        val frame = activeFrames[currentFrame]
        // Calculate display width preserving aspect ratio
        val frameW = if (isWalking && walkFrames.isNotEmpty()) config.walkFrameW else config.idleFrameW
        val frameH = if (isWalking && walkFrames.isNotEmpty()) config.walkFrameH else config.idleFrameH
        val aspectRatio = frameW.toFloat() / frameH
        val displayW = config.displayH * aspectRatio

        Image(
            bitmap = frame,
            contentDescription = dino.species,
            modifier = modifier
                .width(displayW)
                .height(config.displayH)
                .scale(scaleX = if (facingLeft) -1f else 1f, scaleY = 1f),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None
        )
    }
}

/**
 * Backwards compatible wrapper for Spinosaurus.
 */
@Composable
fun PixelSpinosaurus(
    modifier: Modifier = Modifier,
    isWalking: Boolean = false,
    facingLeft: Boolean = false
) {
    val spino = remember {
        com.cuchieman.tamatask.data.model.DinoCollection.all.first { it.id == "spino" }
    }
    PixelDino(dino = spino, modifier = modifier, isWalking = isWalking, facingLeft = facingLeft)
}

/**
 * Load sprite frames from a grid or horizontal strip.
 * @param cols number of columns in the grid (use totalFrames for horizontal strip)
 */
private fun loadSpriteGrid(
    context: android.content.Context,
    resourceName: String,
    totalFrames: Int,
    frameWidth: Int,
    frameHeight: Int,
    cols: Int
): List<ImageBitmap> {
    val options = BitmapFactory.Options().apply { inScaled = false }
    val resId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    if (resId == 0) return emptyList()

    val strip = BitmapFactory.decodeResource(context.resources, resId, options) ?: return emptyList()

    val frameList = mutableListOf<ImageBitmap>()
    for (i in 0 until totalFrames) {
        val col = i % cols
        val row = i / cols
        val x = col * frameWidth
        val y = row * frameHeight
        if (x + frameWidth <= strip.width && y + frameHeight <= strip.height) {
            val frameBmp = Bitmap.createBitmap(strip, x, y, frameWidth, frameHeight)
            frameList.add(frameBmp.asImageBitmap())
        }
    }
    strip.recycle()
    return frameList
}
