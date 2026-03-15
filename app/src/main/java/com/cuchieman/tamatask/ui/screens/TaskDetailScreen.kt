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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.cuchieman.tamatask.ui.theme.TamaCyan
import com.cuchieman.tamatask.ui.theme.TamaGreen
import com.cuchieman.tamatask.ui.theme.TamaOrange
import com.cuchieman.tamatask.ui.theme.TamaRed
import com.cuchieman.tamatask.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val task by viewModel.getTaskById(taskId).collectAsState(initial = null)
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SplashGradient1,
        unfocusedBorderColor = TamaPurple.copy(alpha = 0.3f),
        cursorColor = SplashGradient1
    )

    // Edit state
    var editTitulo by remember(task) { mutableStateOf(task?.titulo ?: "") }
    var editDescripcion by remember(task) { mutableStateOf(task?.descripcion ?: "") }
    var editFechaInicio by remember(task) { mutableStateOf(task?.fechaInicio) }
    var editFechaVencimiento by remember(task) { mutableStateOf(task?.fechaVencimiento) }
    var editFechaFin by remember(task) { mutableStateOf(task?.fechaFin) }

    var showDatePickerInicio by remember { mutableStateOf(false) }
    var showDatePickerVencimiento by remember { mutableStateOf(false) }
    var showDatePickerFin by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Back arrow
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
                                for (i in 1..5) {
                                    drawRect(c, Offset(i * px, 3f * px), Size(px, px))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isEditing) "Editar Tarea" else "Detalle",
                            fontFamily = PixelFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (!isEditing) {
                        Row {
                            // Edit button
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { isEditing = true }
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "EDITAR",
                                    fontFamily = PixelFontFamily,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            // Delete button
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showDeleteDialog = true }
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "ELIMINAR",
                                    fontFamily = PixelFontFamily,
                                    fontSize = 10.sp,
                                    color = TamaRed.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // Content
            task?.let { currentTask ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (isEditing) {
                        // Edit mode
                        FieldLabel("Titulo")
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editTitulo,
                            onValueChange = { editTitulo = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FieldLabel("Descripcion")
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editDescripcion,
                            onValueChange = { editDescripcion = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = fieldColors,
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FieldLabel("Fecha inicio")
                        Spacer(modifier = Modifier.height(4.dp))
                        DateFieldButtonDetail(
                            text = editFechaInicio?.let { dateFormat.format(Date(it)) } ?: "Sin fecha",
                            onClick = { showDatePickerInicio = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FieldLabel("Fecha vencimiento")
                        Spacer(modifier = Modifier.height(4.dp))
                        DateFieldButtonDetail(
                            text = editFechaVencimiento?.let { dateFormat.format(Date(it)) } ?: "Sin fecha",
                            onClick = { showDatePickerVencimiento = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FieldLabel("Fecha fin")
                        Spacer(modifier = Modifier.height(4.dp))
                        DateFieldButtonDetail(
                            text = editFechaFin?.let { dateFormat.format(Date(it)) } ?: "Sin fecha",
                            onClick = { showDatePickerFin = true }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelButton(
                                text = "CANCELAR",
                                onClick = {
                                    isEditing = false
                                    editTitulo = currentTask.titulo
                                    editDescripcion = currentTask.descripcion
                                    editFechaInicio = currentTask.fechaInicio
                                    editFechaVencimiento = currentTask.fechaVencimiento
                                    editFechaFin = currentTask.fechaFin
                                },
                                modifier = Modifier.weight(1f),
                                bgColor = Color.Gray
                            )

                            PixelButton(
                                text = "GUARDAR",
                                onClick = {
                                    if (editTitulo.isNotBlank()) {
                                        viewModel.updateTask(
                                            currentTask.copy(
                                                titulo = editTitulo.trim(),
                                                descripcion = editDescripcion.trim(),
                                                fechaInicio = editFechaInicio,
                                                fechaVencimiento = editFechaVencimiento,
                                                fechaFin = editFechaFin
                                            )
                                        )
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                bgColor = SplashGradient1
                            )
                        }
                    } else {
                        // Read-only mode
                        ReadOnlyField("Titulo", currentTask.titulo)
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Descripcion", currentTask.descripcion.ifBlank { "Sin descripcion" })
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Fecha creacion", dateFormat.format(Date(currentTask.fechaCreacion)))
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Fecha inicio", currentTask.fechaInicio?.let { dateFormat.format(Date(it)) } ?: "Sin fecha")
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Fecha vencimiento", currentTask.fechaVencimiento?.let { dateFormat.format(Date(it)) } ?: "Sin fecha")
                        Spacer(modifier = Modifier.height(16.dp))
                        ReadOnlyField("Fecha fin", currentTask.fechaFin?.let { dateFormat.format(Date(it)) } ?: "Sin fecha")

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botón Finalizar / Reabrir
                        val isCompleted = currentTask.fechaFin != null
                        PixelButton(
                            text = if (isCompleted) "REABRIR" else "FINALIZAR",
                            onClick = {
                                if (isCompleted) {
                                    // Reabrir: quitar fecha fin
                                    viewModel.updateTask(currentTask.copy(fechaFin = null))
                                } else {
                                    // Finalizar: poner fecha fin a ahora
                                    viewModel.updateTask(
                                        currentTask.copy(fechaFin = System.currentTimeMillis())
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            bgColor = if (isCompleted) TamaOrange else TamaGreen
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Eliminar tarea",
                    fontFamily = PixelFontFamily,
                    fontSize = 12.sp
                )
            },
            text = {
                Text(
                    "Seguro que quieres eliminar esta tarea?",
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    task?.let { viewModel.deleteTask(it) }
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Eliminar", fontFamily = PixelFontFamily, fontSize = 10.sp, color = TamaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", fontFamily = PixelFontFamily, fontSize = 10.sp)
                }
            }
        )
    }

    // Date pickers for edit mode
    if (showDatePickerInicio) {
        EditDatePickerDialog(
            onDismiss = { showDatePickerInicio = false },
            onConfirm = { editFechaInicio = it; showDatePickerInicio = false }
        )
    }
    if (showDatePickerVencimiento) {
        EditDatePickerDialog(
            onDismiss = { showDatePickerVencimiento = false },
            onConfirm = { editFechaVencimiento = it; showDatePickerVencimiento = false }
        )
    }
    if (showDatePickerFin) {
        EditDatePickerDialog(
            onDismiss = { showDatePickerFin = false },
            onConfirm = { editFechaFin = it; showDatePickerFin = false }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = PixelFontFamily,
        fontSize = 12.sp,
        color = SplashGradient2
    )
}

@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontFamily = PixelFontFamily,
            fontSize = 12.sp,
            color = SplashGradient2.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawRect(Color.White, Offset(0f, 0f), Size(w, h))
                val px = 1f * density
                drawRect(TamaPurple.copy(alpha = 0.15f), Offset(0f, h - px), Size(w, px))
            }

            Text(
                text = value,
                fontFamily = PixelFontFamily,
                fontSize = 13.sp,
                color = Color(0xFF333333),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun DateFieldButtonDetail(text: String, onClick: () -> Unit) {
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
            fontSize = 11.sp,
            color = if (text == "Sin fecha") Color.Gray else Color(0xFF333333),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                Text("Aceptar", fontFamily = PixelFontFamily, fontSize = 10.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontFamily = PixelFontFamily, fontSize = 10.sp)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
