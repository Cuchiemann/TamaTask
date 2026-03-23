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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cuchieman.tamatask.navigation.BottomNavItem
import com.cuchieman.tamatask.ui.theme.SplashGradient1
import com.cuchieman.tamatask.ui.theme.SplashGradient2
import com.cuchieman.tamatask.ui.theme.SplashGradient3
import com.cuchieman.tamatask.ui.theme.SplashGradient4
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaPink
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.theme.TamaYellow

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Pet,
        BottomNavItem.Excavate,
        BottomNavItem.Collection,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if we're on a tab route (show bottom bar) or minigame route (hide it)
    val tabRoutes = items.map { it.route }
    var excavationActive by remember { mutableStateOf(false) }
    // Reset excavation state when leaving the Excavate tab
    if (currentRoute != BottomNavItem.Excavate.route) {
        excavationActive = false
    }
    val showBottomBar = currentRoute in tabRoutes && !excavationActive

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TamaBottomBar(
                    items = items,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Pet.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Pet.route) {
                PetScreen()
            }
            composable(BottomNavItem.Excavate.route) {
                MinigameScreen(
                    onBack = { navController.popBackStack() },
                    onShowBottomBar = { show -> excavationActive = !show }
                )
            }
            composable(BottomNavItem.Collection.route) {
                CollectionScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun TamaBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── Pixel art top border (thick, like Tamagotchi frame edge) ──
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            val w = size.width
            val px = w / 60f // pixel block size

            // Single row of alternating colored pixels
            val decoColors = listOf(TamaYellow, TamaPink, Color.White, TamaPink, TamaYellow)
            for (col in 0..(w / px).toInt()) {
                    drawRect(
                        color = decoColors[col % decoColors.size],
                        topLeft = Offset(col * px, 0f),
                        size = Size(px, px)
                    )
                }
        }

        // ── Bar content ──
        Box(modifier = Modifier.fillMaxWidth()) {
            // Layer 1: Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SplashGradient1,
                                SplashGradient2,
                                SplashGradient3,
                                SplashGradient4
                            )
                        )
                    )
            )

            // Layer 2: Pixel grid checkerboard overlay
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .alpha(0.08f)
            ) {
                val gridSize = 16f * density
                val cols = (size.width / gridSize).toInt() + 1
                val rows = (size.height / gridSize).toInt() + 1
                for (row in 0..rows) {
                    for (col in 0..cols) {
                        if ((row + col) % 2 == 0) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(col * gridSize, row * gridSize),
                                size = Size(gridSize, gridSize)
                            )
                        }
                    }
                }
            }

            // Nav items row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    TamaNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TamaNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Each tab gets its own color when selected
    val selectedColor = when (item) {
        is BottomNavItem.Pet -> TamaGreen
        is BottomNavItem.Excavate -> TamaYellow
        is BottomNavItem.Collection -> TamaOrange
        is BottomNavItem.Profile -> TamaPurple
    }
    val iconColor = if (selected) selectedColor else Color.White.copy(alpha = 0.6f)
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Pixel art selection frame behind content
        if (selected) {
            Canvas(
                modifier = Modifier.size(width = 68.dp, height = 58.dp)
            ) {
                val w = size.width
                val h = size.height
                val px = w / 17f

                val bgColor = Color.White.copy(alpha = 0.18f)
                val borderColor = selectedColor.copy(alpha = 0.7f)

                // Fill: pixel rounded rect with notched corners
                for (row in 1 until (h / px).toInt() - 1) {
                    for (col in 0 until (w / px).toInt()) {
                        drawRect(bgColor, Offset(col * px, row * px), Size(px, px))
                    }
                }
                for (col in 1 until (w / px).toInt() - 1) {
                    drawRect(bgColor, Offset(col * px, 0f), Size(px, px))
                    drawRect(bgColor, Offset(col * px, ((h / px).toInt() - 1) * px), Size(px, px))
                }

                // Border: top & bottom pixel edges
                val maxCol = (w / px).toInt() - 1
                val maxRow = (h / px).toInt() - 1
                for (col in 1 until maxCol) {
                    drawRect(borderColor, Offset(col * px, 0f), Size(px, px * 0.6f))
                    drawRect(borderColor, Offset(col * px, maxRow * px + px * 0.4f), Size(px, px * 0.6f))
                }
                // Border: left & right pixel edges
                for (row in 1 until maxRow) {
                    drawRect(borderColor, Offset(0f, row * px), Size(px * 0.6f, px))
                    drawRect(borderColor, Offset(maxCol * px + px * 0.4f, row * px), Size(px * 0.6f, px))
                }

                // Pixel sparkle decorations around selected item
                val sparkle = selectedColor
                val sparkle2 = TamaPink.copy(alpha = 0.8f)
                // Top-left star
                drawRect(sparkle, Offset(-px * 0.5f, px * 0.5f), Size(px * 0.7f, px * 0.7f))
                drawRect(sparkle2, Offset(px * 0.3f, -px * 0.3f), Size(px * 0.5f, px * 0.5f))
                // Top-right star
                drawRect(sparkle, Offset(w - px * 0.2f, px * 0.8f), Size(px * 0.7f, px * 0.7f))
                drawRect(sparkle2, Offset(w - px * 1f, -px * 0.2f), Size(px * 0.5f, px * 0.5f))
                // Bottom diamond
                drawRect(sparkle, Offset(w / 2 - px * 0.3f, h - px * 0.1f), Size(px * 0.6f, px * 0.6f))
            }
        }

        // Content on top
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Pixel art icon drawn with Canvas
            val iconSize = if (selected) 28.dp else 24.dp
            Canvas(modifier = Modifier.size(iconSize)) {
                val px = size.width / 9f
                val c = iconColor
                when (item) {
                    is BottomNavItem.Pet -> {
                        // Pixel egg (tamagotchi egg)
                        // Top of egg (narrow)
                        for (col in 3..5) drawRect(c, Offset(col * px, 0f * px), Size(px, px))
                        // Widen
                        for (col in 2..6) drawRect(c, Offset(col * px, 1f * px), Size(px, px))
                        // Full width rows
                        for (col in 1..7) drawRect(c, Offset(col * px, 2f * px), Size(px, px))
                        for (col in 1..7) drawRect(c, Offset(col * px, 3f * px), Size(px, px))
                        for (col in 1..7) drawRect(c, Offset(col * px, 4f * px), Size(px, px))
                        for (col in 1..7) drawRect(c, Offset(col * px, 5f * px), Size(px, px))
                        for (col in 1..7) drawRect(c, Offset(col * px, 6f * px), Size(px, px))
                        // Narrow bottom
                        for (col in 2..6) drawRect(c, Offset(col * px, 7f * px), Size(px, px))
                        for (col in 3..5) drawRect(c, Offset(col * px, 8f * px), Size(px, px))
                        // Zigzag crack pattern across middle
                        val crackColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
                        drawRect(crackColor, Offset(1f * px, 4f * px), Size(px, px))
                        drawRect(crackColor, Offset(2f * px, 3f * px), Size(px, px))
                        drawRect(crackColor, Offset(3f * px, 4f * px), Size(px, px))
                        drawRect(crackColor, Offset(4f * px, 3f * px), Size(px, px))
                        drawRect(crackColor, Offset(5f * px, 4f * px), Size(px, px))
                        drawRect(crackColor, Offset(6f * px, 3f * px), Size(px, px))
                        drawRect(crackColor, Offset(7f * px, 4f * px), Size(px, px))
                    }
                    is BottomNavItem.Excavate -> {
                        // Simple X icon
                        drawRect(c, Offset(1f * px, 1f * px), Size(px, px))
                        drawRect(c, Offset(2f * px, 2f * px), Size(px, px))
                        drawRect(c, Offset(3f * px, 3f * px), Size(px, px))
                        drawRect(c, Offset(4f * px, 4f * px), Size(px, px))
                        drawRect(c, Offset(5f * px, 5f * px), Size(px, px))
                        drawRect(c, Offset(6f * px, 6f * px), Size(px, px))
                        drawRect(c, Offset(7f * px, 7f * px), Size(px, px))
                        drawRect(c, Offset(7f * px, 1f * px), Size(px, px))
                        drawRect(c, Offset(6f * px, 2f * px), Size(px, px))
                        drawRect(c, Offset(5f * px, 3f * px), Size(px, px))
                        drawRect(c, Offset(3f * px, 5f * px), Size(px, px))
                        drawRect(c, Offset(2f * px, 6f * px), Size(px, px))
                        drawRect(c, Offset(1f * px, 7f * px), Size(px, px))
                    }
                    is BottomNavItem.Collection -> {
                        // Pixel book / collection
                        // Spine
                        for (row in 0..8) drawRect(c, Offset(1f * px, row * px), Size(px, px))
                        // Top & bottom cover
                        for (col in 1..7) {
                            drawRect(c, Offset(col * px, 0f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 4f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 8f * px), Size(px, px))
                        }
                        // Right edge
                        for (row in 0..8) drawRect(c, Offset(7f * px, row * px), Size(px, px))
                        // Page lines
                        for (col in 3..6) {
                            drawRect(c, Offset(col * px, 2f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 6f * px), Size(px, px))
                        }
                        // Star on cover
                        drawRect(c, Offset(4f * px, 1f * px), Size(px, px))
                        drawRect(c, Offset(3f * px, 2f * px), Size(px, px))
                        drawRect(c, Offset(5f * px, 2f * px), Size(px, px))
                        drawRect(c, Offset(4f * px, 3f * px), Size(px, px))
                    }
                    is BottomNavItem.Profile -> {
                        // Pixel person
                        // Head
                        for (col in 3..5) {
                            drawRect(c, Offset(col * px, 0f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 1f * px), Size(px, px))
                            drawRect(c, Offset(col * px, 2f * px), Size(px, px))
                        }
                        // Neck
                        drawRect(c, Offset(4f * px, 3f * px), Size(px, px))
                        // Shoulders & body
                        for (col in 1..7) drawRect(c, Offset(col * px, 4f * px), Size(px, px))
                        for (col in 1..7) drawRect(c, Offset(col * px, 5f * px), Size(px, px))
                        for (col in 2..6) drawRect(c, Offset(col * px, 6f * px), Size(px, px))
                        for (col in 2..6) drawRect(c, Offset(col * px, 7f * px), Size(px, px))
                        for (col in 2..6) drawRect(c, Offset(col * px, 8f * px), Size(px, px))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.title,
                fontSize = if (selected) 10.sp else 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )

            // Pixel dots indicator under selected label
            if (selected) {
                Spacer(modifier = Modifier.height(2.dp))
                Canvas(modifier = Modifier.size(width = 18.dp, height = 3.dp)) {
                    val dotSize = size.height
                    val totalWidth = size.width
                    val count = 3
                    val spacing = (totalWidth - count * dotSize) / (count + 1)
                    for (i in 0 until count) {
                        drawRect(
                            color = selectedColor,
                            topLeft = Offset(spacing + i * (dotSize + spacing), 0f),
                            size = Size(dotSize, dotSize)
                        )
                    }
                }
            }
        }
    }
}
