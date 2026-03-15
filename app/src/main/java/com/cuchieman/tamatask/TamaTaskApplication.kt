package com.cuchieman.tamatask

import android.app.Application
import com.cuchieman.tamatask.data.local.TamaTaskDatabase
import com.cuchieman.tamatask.data.repository.TaskRepository

class TamaTaskApplication : Application() {
    val database by lazy { TamaTaskDatabase.getInstance(this) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
}
