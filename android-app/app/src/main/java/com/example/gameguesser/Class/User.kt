package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

// Must match the table name used by migrations & DAO
@Entity(tableName = "user_table")
data class User(
    @PrimaryKey val userId: String,           // Google user ID (PK)
    var userName: String = "",                // NOT NULL (default empty)
    var streakKW: Int = 0,                    // Keyword streak
    var streakCG: Int = 0,                    // Compare-game streak
    var bestStreakKW: Int = 0,                // Best keyword streak
    var bestStreakCG: Int = 0,                // Best compare streak
    var lastPlayedCG: Long = 0L,              // Last played compare (ms)
    var lastPlayedKW: Long = 0L               // Last played keyword (ms)
)
