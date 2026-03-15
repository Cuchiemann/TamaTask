package com.cuchieman.tamatask.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuchieman.tamatask.ui.components.PixelButton
import com.cuchieman.tamatask.ui.theme.PixelFontFamily
import com.cuchieman.tamatask.ui.theme.SplashGradient1
import com.cuchieman.tamatask.ui.theme.SplashGradient2
import com.cuchieman.tamatask.ui.theme.TamaPurple
import com.cuchieman.tamatask.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf<Long?>(null) }
    var fechaVencimiento by remember { mutableStateOf<Long?>(null) }

    var showDatePickerInicio by remember { mutableStateOf(false) }
    var showDatePickerVencimiento by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SplashGradient1,
        unfocusedBorderColor = TamaPurple.copy(alpha = 0.3f),
        cursorColor = SplashGradient1
    )

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
            // Header with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(SplashGradient1, SplashGradient2)
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pixel back arrow
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack
                            )
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val px = size.width / 6f
                            val c = Color.White
                            drawRect(c, Offset(3f * px, 0f * px), Size(px, px))
                            drawRect(c, Offset(2f * px, 1f * px), Size(px, px))
                            drawRect(c, Offset(1f * px, 2f * px), Size(px, px))
                            drawRect(c, Offset(0f * px, 3f * px), Size(px, px))
                            drawRect(c, Offset(1f * px, 4f * px), Size(px, px))
                            drawRect(c, Offset(2f * px, 5f * px), Size(px, px))
                            // Horizontal line
                            for (i in 1..5) {
                                drawRect(c, Offset(i * px, 3f * px), Size(px, px))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Nueva Tarea",
                        fontFamily = PixelFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Titulo",
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = SplashGradient2
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Nombre de la tarea", fontFamily = PixelFontFamily, fontSize = 9.sp)
                    },
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Descripcion",
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = SplashGradient2
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text("Descripcion de la tarea", fontFamily = PixelFontFamily, fontSize = 9.sp)
                    },
                    colors = fieldColors,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fecha inicio
                Text(
                    text = "Fecha inicio",
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = SplashGradient2
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateFieldButton(
                    text = fechaInicio?.let { dateFormat.format(Date(it)) } ?: "Seleccionar fecha",
                    onClick = { showDatePickerInicio = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fecha vencimiento
                Text(
                    text = "Fecha vencimiento",
                    fontFamily = PixelFontFamily,
                    fontSize = 8.sp,
                    color = SplashGradient2
                )
                Spacer(modifier = Modifier.height(4.dp))
                DateFieldButton(
                    text = fechaVencimiento?.let { dateFormat.format(Date(it)) } ?: "Seleccionar fecha",
                    onClick = { showDatePickerVencimiento = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save button
                PixelButton(
                    text = "GUARDAR",
                    onClick = {
                        if (titulo.isNotBlank()) {
                            viewModel.addTask(
                                titulo = titulo.trim(),
                                descripcion = descripcion.trim(),
                                fechaInicio = fechaInicio,
                                fechaVencimiento = fechaVencimiento
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    bgColor = SplashGradient1
                )
            }
        }
    }

    // Date pickers
    if (showDatePickerInicio) {
        TamaDatePickerDialog(
            onDismiss = { showDatePickerInicio = false },
            onConfirm = { millis ->
                fechaInicio = millis
                showDatePickerInicio = false
            }
        )
    }

    if (showDatePickerVencimiento) {
        TamaDatePickerDialog(
            onDismiss = { showDatePickerVencimiento = false },
            onConfirm = { millis ->
                fechaVencimiento = millis
                showDatePickerVencimiento = false
            }
        )
    }
}

@Composable
private fun DateFieldButton(text: String, onClick: () -> Unit) {
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
            val px = 1f * density
            drawRect(Color.White, Offset(0f, 0f), Size(w, h))
            drawRect(TamaPurple.copy(alpha = 0.3f), Offset(0f, 0f), Size(w, px))
            drawRect(TamaPurple.copy(alpha = 0.3f), Offset(0f, h - px), Size(w, px))
            drawRect(TamaPurple.copy(alpha = 0.3f), Offset(0f, 0f), Size(px, h))
            drawRect(TamaPurple.copy(alpha = 0.3f), Offset(w - px, 0f), Size(px, h))
        }

        Text(
            text = text,
            fontFamily = PixelFontFamily,
            fontSize = 9.sp,
            color = if (text == "Seleccionar fecha") Color.Gray else Color(0xFF333333),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TamaDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text("Aceptar", fontFamily = PixelFontFamily, fontSize = 8.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontFamily = PixelFontFamily, fontSize = 8.sp)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
