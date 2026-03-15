package com.cuchieman.tamatask.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.data.model.Task
import com.cuchieman.tamatask.ui.theme.PixelFontFamily
import com.cuchieman.tamatask.ui.theme.SplashGradient1
import com.cuchieman.tamatask.ui.theme.SplashGradient2
import com.cuchieman.tamatask.ui.theme.TamaCyan
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.theme.TamaRed
import com.cuchieman.tamatask.ui.theme.TamaYellow
import com.cuchieman.tamatask.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Long) -> Unit,
    onCreateClick: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SplashGradient1.copy(alpha = 0.15f),
                        SplashGradient2.copy(alpha = 0.1f),
                        Color(0xFFF5F5F5)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(SplashGradient1, SplashGradient2)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Mis Tareas",
                    fontFamily = PixelFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (tasks.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pixel art clipboard icon
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val px = size.width / 16f
                        val c = TamaPurple

                        // Clipboard body
                        for (row in 3..14) {
                            for (col in 2..13) {
                                drawRect(c.copy(alpha = 0.3f), Offset(col * px, row * px), Size(px, px))
                            }
                        }
                        // Clipboard top clip
                        for (col in 5..10) {
                            drawRect(c, Offset(col * px, 1f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 2f * px), Size(px, px))
                        }
                        // Border
                        for (col in 2..13) {
                            drawRect(c, Offset(col * px, 3f * px), Size(px, px * 0.5f))
                            drawRect(c, Offset(col * px, 14f * px + px * 0.5f), Size(px, px * 0.5f))
                        }
                        for (row in 3..14) {
                            drawRect(c, Offset(2f * px, row * px), Size(px * 0.5f, px))
                            drawRect(c, Offset(13f * px + px * 0.5f, row * px), Size(px * 0.5f, px))
                        }
                        // Lines on clipboard
                        for (row in listOf(6, 8, 10, 12)) {
                            for (col in 4..11) {
                                drawRect(c.copy(alpha = 0.4f), Offset(col * px, row * px), Size(px, px * 0.4f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "No hay tareas",
                        fontFamily = PixelFontFamily,
                        fontSize = 12.sp,
                        color = SplashGradient2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Toca + para crear una",
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Task list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                    // Space for FAB
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        // FAB - Pixel art "+" button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCreateClick
                )
        ) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val w = size.width
                val h = size.height
                val px = 3f * density

                // Shadow
                drawRect(Color.Black.copy(alpha = 0.3f), Offset(px, px), Size(w, h))
                // Body
                drawRect(TamaPink, Offset(0f, 0f), Size(w, h))
                // Highlight edges
                drawRect(Color.White.copy(alpha = 0.3f), Offset(0f, 0f), Size(w, px))
                drawRect(Color.White.copy(alpha = 0.2f), Offset(0f, 0f), Size(px, h))
                drawRect(Color.Black.copy(alpha = 0.2f), Offset(0f, h - px), Size(w, px))
                drawRect(Color.Black.copy(alpha = 0.15f), Offset(w - px, 0f), Size(px, h))

                // Plus sign
                val plusPx = w / 10f
                val cx = w / 2f
                val cy = h / 2f
                // Horizontal
                for (i in -2..2) {
                    drawRect(Color.White, Offset(cx + i * plusPx - plusPx / 2, cy - plusPx / 2), Size(plusPx, plusPx))
                }
                // Vertical
                for (i in -2..2) {
                    drawRect(Color.White, Offset(cx - plusPx / 2, cy + i * plusPx - plusPx / 2), Size(plusPx, plusPx))
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit
) {
    val accentColor = getTaskAccentColor(task)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val px = 2f * density

            // Shadow
            drawRect(Color.Black.copy(alpha = 0.15f), Offset(px, px), Size(w, h))
            // Card bg
            drawRect(Color.White, Offset(0f, 0f), Size(w, h))
            // Accent left strip
            drawRect(accentColor, Offset(0f, 0f), Size(px * 3, h))
            // Border
            drawRect(accentColor.copy(alpha = 0.4f), Offset(0f, 0f), Size(w, px * 0.5f))
            drawRect(accentColor.copy(alpha = 0.3f), Offset(0f, h - px * 0.5f), Size(w, px * 0.5f))
            drawRect(accentColor.copy(alpha = 0.2f), Offset(w - px * 0.5f, 0f), Size(px * 0.5f, h))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.titulo,
                    fontFamily = PixelFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.descripcion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.descripcion,
                        fontFamily = PixelFontFamily,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.fechaVencimiento != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vence: ${dateFormat.format(Date(task.fechaVencimiento))}",
                        fontFamily = PixelFontFamily,
                        fontSize = 10.sp,
                        color = accentColor
                    )
                }
            }

            // Small pixel arrow indicator
            Canvas(modifier = Modifier.size(12.dp)) {
                val px = size.width / 6f
                val c = Color.Gray.copy(alpha = 0.5f)
                drawRect(c, Offset(1f * px, 0f * px), Size(px, px))
                drawRect(c, Offset(2f * px, 1f * px), Size(px, px))
                drawRect(c, Offset(3f * px, 2f * px), Size(px, px))
                drawRect(c, Offset(2f * px, 3f * px), Size(px, px))
                drawRect(c, Offset(1f * px, 4f * px), Size(px, px))
            }
        }
    }
}

private fun getTaskAccentColor(task: Task): Color {
    if (task.fechaFin != null) return TamaCyan
    val due = task.fechaVencimiento ?: return TamaPurple
    val now = System.currentTimeMillis()
    val daysLeft = TimeUnit.MILLISECONDS.toDays(due - now)
    return when {
        daysLeft < 0 -> TamaRed
        daysLeft == 0L -> TamaOrange
        daysLeft <= 2 -> TamaYellow
        else -> TamaGreen
    }
}
