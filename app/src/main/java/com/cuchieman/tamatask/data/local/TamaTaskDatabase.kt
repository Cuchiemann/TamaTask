package com.cuchieman.tamatask.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cuchieman.tamatask.data.model.Task

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class TamaTaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TamaTaskDatabase? = null

        fun getInstance(context: Context): TamaTaskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TamaTaskDatabase::class.java,
                    "tamatask_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
