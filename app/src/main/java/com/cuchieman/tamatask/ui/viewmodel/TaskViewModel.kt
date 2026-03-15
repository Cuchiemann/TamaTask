package com.cuchieman.tamatask.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cuchieman.tamatask.data.model.Task
import com.cuchieman.tamatask.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTaskById(id: Long) = repository.getById(id)

    fun addTask(
        titulo: String,
        descripcion: String,
        fechaInicio: Long?,
        fechaVencimiento: Long?
    ) {
        viewModelScope.launch {
            repository.insert(
                Task(
                    titulo = titulo,
                    descripcion = descripcion,
                    fechaCreacion = System.currentTimeMillis(),
                    fechaInicio = fechaInicio,
                    fechaVencimiento = fechaVencimiento,
                    fechaFin = null
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.update(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repository) as T
        }
    }
}
