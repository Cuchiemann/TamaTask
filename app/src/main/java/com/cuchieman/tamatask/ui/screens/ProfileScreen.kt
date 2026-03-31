package com.cuchieman.tamatask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.data.DebugPrefs
import com.cuchieman.tamatask.data.rememberDebugPref
import com.cuchieman.tamatask.ui.theme.PixelFontFamily

@Composable
fun ProfileScreen() {
    var unlockAllSites by rememberDebugPref(
        "sites",
        DebugPrefs::getUnlockAllSites,
        DebugPrefs::setUnlockAllSites
    )
    var unlockAllDinos by rememberDebugPref(
        "dinos",
        DebugPrefs::getUnlockAllDinos,
        DebugPrefs::setUnlockAllDinos
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Perfil",
            fontFamily = PixelFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Debug section
        Text(
            text = "Desarrollo",
            fontFamily = PixelFontFamily,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Unlock all excavation sites
        DebugToggleRow(
            label = "Desbloquear excavaciones",
            checked = unlockAllSites,
            onCheckedChange = { unlockAllSites = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Unlock all dinosaurs
        DebugToggleRow(
            label = "Desbloquear dinosaurios",
            checked = unlockAllDinos,
            onCheckedChange = { unlockAllDinos = it }
        )
    }
}

@Composable
private fun DebugToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = PixelFontFamily,
            fontSize = 14.sp,
            color = Color.White
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CAF50),
                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.4f)
            )
        )
    }
}
