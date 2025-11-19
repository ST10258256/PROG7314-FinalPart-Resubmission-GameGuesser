package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey val email: String,
    var userName: String,
    var passwordHash: String,

    // --- Add all the fields from the User class ---
    // streak for keyword game
    var streakKW: Int = 0,
    // streak for compare game
    var streakCG: Int = 0,
    // best streak for Keyword game
    var bestStreakKW: Int = 0,
    // best streak for Compare game
    var bestStreakCG: Int = 0,
    // Last played date for Compare Game streak
    var lastPlayedCG: Long = 0L,
    // Last played date for Keyword game
    var lastPlayedKW: Long = 0L,
    // consec streak for Compare game
    var consecStreakCG: Int = 0, // Should default to 0
    // consec streak for Keyword game
    var consecStreakKW: Int = 0 // Should default to 0
)
