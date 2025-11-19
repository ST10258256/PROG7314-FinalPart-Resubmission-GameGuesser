package com.example.gameguesser.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.Class.User
import com.example.gameguesser.DAOs.LocalUserDao
import com.example.gameguesser.DAOs.UserDao

// 1. UPDATE THE VERSION NUMBER TO 5
@Database(entities = [User::class, LocalUser::class], version = 5, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        // --- Existing Migrations (assumed from previous context) ---
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_users (
                        email TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL,
                        passwordHash TEXT NOT NULL
                    )
                """)
            }
        }

        // This migration was empty in the previous step, so we merge its changes into MIGRATION_4_5
        // We will add it back to the builder for users who might be on version 2 or 3.
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This migration is now intentionally left blank as its logic is handled below,
                // but it's kept to maintain the migration path.
            }
        }

        // This migration was also empty.
        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Intentionally blank for the same reason.
            }
        }

        // 2. DEFINE THE NEW MIGRATION FROM 4 TO 5
        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new columns to the 'users' table
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakCG INTEGER NOT NULL DEFAULT 0")

                // Add the new columns to the 'local_users' table
                db.execSQL("ALTER TABLE local_users ADD COLUMN consecStreakKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN consecStreakCG INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    // 3. ADD THE NEW MIGRATION TO THE BUILDER
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
