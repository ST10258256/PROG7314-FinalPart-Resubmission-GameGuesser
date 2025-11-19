package com.example.gameguesser.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gameguesser.Class.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User)

    @Query("SELECT * FROM user_table WHERE userId = :id")
    suspend fun getUser(id: String): User?

    @Update
    suspend fun updateUser(user: User)

    // Increment keyword streak atomically. Returns number of rows updated.
    @Query("""
        UPDATE user_table
        SET streakKW = COALESCE(streakKW, 0) + 1,
            bestStreakKW = CASE WHEN (COALESCE(streakKW,0) + 1) > COALESCE(bestStreakKW,0)
                                THEN (COALESCE(streakKW,0) + 1) ELSE bestStreakKW END,
            lastPlayedKW = :now
        WHERE userId = :id
    """)
    suspend fun incrementKeywordStreak(id: String, now: Long): Int

    // If row doesn't exist, insert a new user row with keyword streak = 1.
    // Provide userName default '' so NOT NULL constraint satisfied.
    @Query("""
        INSERT OR IGNORE INTO user_table(
            userId, userName, streakKW, bestStreakKW, lastPlayedKW,
            streakCG, bestStreakCG, lastPlayedCG
        )
        VALUES(:id, '', 1, 1, :now, 0, 0, 0)
    """)
    suspend fun insertInitialUserForKeyword(id: String, now: Long)

    // Increment compare-game streak atomically. Returns number of rows updated.
    @Query("""
        UPDATE user_table
        SET streakCG = COALESCE(streakCG, 0) + 1,
            bestStreakCG = CASE WHEN (COALESCE(streakCG,0) + 1) > COALESCE(bestStreakCG,0)
                                THEN (COALESCE(streakCG,0) + 1) ELSE bestStreakCG END,
            lastPlayedCG = :now
        WHERE userId = :id
    """)
    suspend fun incrementCompareStreak(id: String, now: Long): Int

    // If row doesn't exist, insert a new user row with compare streak = 1.
    @Query("""
        INSERT OR IGNORE INTO user_table(
            userId, userName, streakKW, bestStreakKW, lastPlayedKW,
            streakCG, bestStreakCG, lastPlayedCG
        )
        VALUES(:id, '', 0, 0, 0, 1, 1, :now)
    """)
    suspend fun insertInitialUserForCompare(id: String, now: Long)
}
