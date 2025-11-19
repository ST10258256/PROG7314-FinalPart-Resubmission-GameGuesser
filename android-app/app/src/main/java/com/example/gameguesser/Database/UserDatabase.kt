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

@Database(entities = [User::class, LocalUser::class], version = 4, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        // --- THIS MIGRATION IS NOW FULLY CORRECTED ---
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Actions for the 'users' table (originally part of this migration)
                // Note: The table for the User class is 'users'
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedKW INTEGER NOT NULL DEFAULT 0")

                // Correctly create the 'local_users' table WITHOUT the old 'streak' column
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_users (
                        email TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL,
                        passwordHash TEXT NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakKW INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add all the new columns to the `local_users` table
                db.execSQL("ALTER TABLE local_users ADD COLUMN streakKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN streakCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN bestStreakKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN bestStreakCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN lastPlayedKW INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN consecStreakCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_users ADD COLUMN consecStreakKW INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
