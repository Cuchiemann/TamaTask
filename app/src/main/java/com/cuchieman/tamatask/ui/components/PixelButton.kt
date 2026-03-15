package com.cuchieman.tamatask.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.ui.theme.PixelFontFamily

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF667EEA),
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val px = 3f * density

            // Shadow
            val shadow = Color.Black.copy(alpha = 0.3f)
            drawRect(shadow, Offset(px, px), Size(w, h))

            // Main body (no rounded corners - pixel style)
            drawRect(bgColor, Offset(0f, 0f), Size(w, h))

            // Highlight top edge
            drawRect(Color.White.copy(alpha = 0.3f), Offset(0f, 0f), Size(w, px))
            // Highlight left edge
            drawRect(Color.White.copy(alpha = 0.2f), Offset(0f, 0f), Size(px, h))
            // Dark bottom edge
            drawRect(Color.Black.copy(alpha = 0.2f), Offset(0f, h - px), Size(w, px))
            // Dark right edge
            drawRect(Color.Black.copy(alpha = 0.15f), Offset(w - px, 0f), Size(px, h))
        }

        Text(
            text = text,
            fontFamily = PixelFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
