package com.example.gameguesser.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.Class.User
import com.example.gameguesser.DAOs.LocalUserDao
import com.example.gameguesser.DAOs.UserDao

@Database(entities = [User::class, LocalUser::class], version = 3, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null
        private const val DB_NAME = "user_database"

        // Migration 1 -> 2 (and safe create if missing). This will:
        // - Ensure user_table exists with exact columns Room expects.
        // - Add lastPlayedCG column if table already existed but was missing it.
        // - Create local_users table if missing.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create user_table if it doesn't exist (fields and types must exactly match the entity)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_table (
                        userId TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL DEFAULT '',
                        streakKW INTEGER NOT NULL DEFAULT 0,
                        streakCG INTEGER NOT NULL DEFAULT 0,
                        bestStreakKW INTEGER NOT NULL DEFAULT 0,
                        bestStreakCG INTEGER NOT NULL DEFAULT 0,
                        lastPlayedCG INTEGER NOT NULL DEFAULT 0,
                        lastPlayedKW INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // If old table existed under a different name (e.g. "users" or "user_table_old") you could copy data here.
                // Also ensure local_users table exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_users (
                        email TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        streak INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Add lastPlayedCG column if it does not exist (defensive)
                try {
                    db.execSQL("ALTER TABLE user_table ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")
                } catch (ex: Exception) {
                    // ignore if column already exists or ALTER fails (we already created table with column)
                    Log.i("UserDB", "lastPlayedCG column add skipped: ${ex.message}")
                }
            }
        }

        // Migration 2 -> 3: ensure table still exists (safe no-op or adjust as needed)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes required; keep as no-op to let Room validate.
                // If you ever change entity fields, update this accordingly.
            }
        }

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // If something still mismatches, destructive fallback will recreate DB.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
