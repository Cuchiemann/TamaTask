package com.cuchieman.tamatask.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val fechaCreacion: Long,
    val fechaInicio: Long?,
    val fechaVencimiento: Long?,
    val fechaFin: Long?
)
