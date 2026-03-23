package com.cuchieman.tamatask.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.ui.theme.PixelFontFamily
import com.cuchieman.tamatask.ui.theme.SplashGradient1
import com.cuchieman.tamatask.ui.theme.SplashGradient2
import com.cuchieman.tamatask.ui.theme.TamaCyan
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.theme.TamaYellow
import com.cuchieman.tamatask.data.model.DinoCollection
import com.cuchieman.tamatask.data.model.DinoSpec

// ── Creature data (from shared DinoCollection) ──

private data class Creature(
    val name: String,
    val species: String,
    val era: String,
    val unlocked: Boolean,
    val accentColor: Color
)

private val creatures = DinoCollection.all.map { dino ->
    Creature(dino.name, dino.species, dino.era, dino.unlocked, dino.accentColor)
}

@Composable
fun CollectionScreen() {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) creatures
        else creatures.filter {
            it.species.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SplashGradient1.copy(alpha = 0.08f),
                        SplashGradient2.copy(alpha = 0.04f),
                        Color.White
                    )
                )
            )
    ) {
        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(SplashGradient1, SplashGradient2)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Coleccion",
                fontFamily = PixelFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            // Count badge
            Text(
                text = "${creatures.count { it.unlocked }}/${creatures.size}",
                fontFamily = PixelFontFamily,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // ── Search bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(2.dp, SplashGradient1.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Buscar...",
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(SplashGradient1),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Magnifying glass icon (pixel art)
            Canvas(modifier = Modifier.size(40.dp)) {
                val px = size.width / 10f
                val blue = SplashGradient1

                fun p(x: Int, y: Int) {
                    drawRect(blue, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
                }

                // Lens circle (top-left area)
                p(3, 0); p(4, 0); p(5, 0)
                p(2, 1); p(6, 1)
                p(1, 2); p(7, 2)
                p(1, 3); p(7, 3)
                p(1, 4); p(7, 4)
                p(2, 5); p(6, 5)
                p(3, 6); p(4, 6); p(5, 6)
                // Handle (diagonal)
                p(7, 6); p(8, 7); p(9, 8)
            }
        }

        // ── Grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { creature ->
                CreatureCard(creature = creature)
            }
        }
    }
}

@Composable
private fun CreatureCard(creature: Creature) {
    val borderColor = if (creature.unlocked) creature.accentColor else Color.Gray.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        // ── Sprite area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .background(
                    if (creature.unlocked)
                        Brush.verticalGradient(
                            listOf(
                                creature.accentColor.copy(alpha = 0.08f),
                                creature.accentColor.copy(alpha = 0.03f)
                            )
                        )
                    else
                        Brush.verticalGradient(
                            listOf(Color(0xFFF5F5F5), Color(0xFFEEEEEE))
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (creature.unlocked) {
                // Use first frame of the real spritestrip as thumbnail
                SpinoThumbnail()
            } else {
                // Lock icon
                Canvas(modifier = Modifier.size(40.dp)) {
                    drawPixelLock(this)
                }
            }
        }

        // ── Pixel divider ──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val px = size.height
            val count = (size.width / px).toInt()
            for (i in 0 until count) {
                val c = if (i % 2 == 0) borderColor.copy(alpha = 0.6f) else borderColor.copy(alpha = 0.3f)
                drawRect(c, Offset(i * px, 0f), Size(px, px))
            }
        }

        // ── Info area ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (creature.unlocked) creature.name else "???",
                fontFamily = PixelFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (creature.unlocked) Color.DarkGray else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = creature.species,
                fontFamily = PixelFontFamily,
                fontSize = 8.sp,
                color = if (creature.unlocked) creature.accentColor else Color.Gray.copy(alpha = 0.6f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = creature.era,
                fontFamily = PixelFontFamily,
                fontSize = 7.sp,
                color = Color.Gray.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

// ── Helpers ──

/**
 * Loads the first frame from the spino spritestrip as a thumbnail.
 */
@Composable
private fun SpinoThumbnail() {
    val context = LocalContext.current
    val thumbnail = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val strip = BitmapFactory.decodeResource(
            context.resources,
            context.resources.getIdentifier("spino_idle_strip", "drawable", context.packageName),
            options
        )
        if (strip != null) {
            val frame = Bitmap.createBitmap(strip, 0, 0, 717, 252)
            strip.recycle()
            frame.asImageBitmap()
        } else null
    }

    if (thumbnail != null) {
        Image(
            bitmap = thumbnail,
            contentDescription = "Spinosaurus",
            modifier = Modifier
                .width(170.dp)
                .height(50.dp),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None
        )
    }
}

private fun drawPixelLock(scope: DrawScope) {
    val px = scope.size.width / 11f
    val dark = Color(0xFF888888)
    val mid = Color(0xFFAAAAAA)
    val light = Color(0xFFCCCCCC)

    fun p(x: Float, y: Float, color: Color) {
        scope.drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
    }

    // Shackle (top arc)
    p(4f, 0f, dark); p(5f, 0f, dark); p(6f, 0f, dark)
    p(3f, 1f, dark); p(7f, 1f, dark)
    p(3f, 2f, dark); p(7f, 2f, dark)
    p(3f, 3f, dark); p(7f, 3f, dark)

    // Lock body
    for (x in 2..8) {
        p(x.toFloat(), 4f, dark)
    }
    for (y in 5..8) {
        p(2f, y.toFloat(), dark)
        for (x in 3..7) {
            p(x.toFloat(), y.toFloat(), mid)
        }
        p(8f, y.toFloat(), dark)
    }
    for (x in 2..8) {
        p(x.toFloat(), 9f, dark)
    }

    // Keyhole
    p(5f, 6f, dark)
    p(5f, 7f, dark)
    p(4f, 7f, light); p(6f, 7f, light)
}
