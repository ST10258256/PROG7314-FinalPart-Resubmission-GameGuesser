package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey val email: String,
    var userName: String,
    var passwordHash: String,

    // --- Mirrored fields from the User class ---

    // Daily streaks
    var streakKW: Int = 0,
    var streakCG: Int = 0,

    // Best streaks
    var bestStreakKW: Int = 0,
    var bestStreakCG: Int = 0,

    // Last played timestamps
    var lastPlayedCG: Long = 0L,
    var lastPlayedKW: Long = 0L,

    // --- NEW: Consecutive streaks ---
    var consecStreakKW: Int = 0,
    var consecStreakCG: Int = 0
)
