package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String, // Google user ID
    var userName: String,           // Display name
    // streak for keyword game
    val streakKW: Int = 0,             // Initial streak
    //streak for compare game
    val streakCG: Int = 0             // Initial streak
)
