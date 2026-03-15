package com.cuchieman.tamatask.data.repository

import com.cuchieman.tamatask.data.local.TaskDao
import com.cuchieman.tamatask.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getAll(): Flow<List<Task>> = taskDao.getAll()
    fun getById(id: Long): Flow<Task?> = taskDao.getById(id)
    suspend fun insert(task: Task) = taskDao.insert(task)
    suspend fun update(task: Task) = taskDao.update(task)
    suspend fun delete(task: Task) = taskDao.delete(task)
}
