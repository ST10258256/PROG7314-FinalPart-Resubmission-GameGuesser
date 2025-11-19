package com.example.gameguesser.Database

import android.content.Context

// Single access point for getting the AppDatabase instance.
// Avoid using Room.databaseBuilder directly elsewhere in the project.
object DatabaseBuilder {
    fun getInstance(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
}
