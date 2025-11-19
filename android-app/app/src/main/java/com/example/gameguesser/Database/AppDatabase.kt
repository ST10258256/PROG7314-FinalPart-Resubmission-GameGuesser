package com.example.gameguesser.Database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameguesser.DAOs.GameDAO.GameDao
import com.example.gameguesser.data.Game
import com.example.gameguesser.data.GameConverters
import java.io.File

@Database(entities = [Game::class], version = 3, exportSchema = false)
@TypeConverters(GameConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "gameguesser_db"

        // Migration 2 -> 3: recreate the `games` table (destructive).
        // Adjust SQL if you know the exact previous column types and want a non-destructive migration.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop old table if it exists and recreate using current schema.
                database.execSQL("DROP TABLE IF EXISTS `games`")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `games` (
                      `_id` TEXT,
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `name` TEXT NOT NULL,
                      `genre` TEXT NOT NULL,
                      `platforms` TEXT NOT NULL,
                      `releaseYear` INTEGER NOT NULL,
                      `developer` TEXT NOT NULL,
                      `publisher` TEXT NOT NULL,
                      `description` TEXT NOT NULL,
                      `coverImageUrl` TEXT NOT NULL,
                      `budget` TEXT NOT NULL,
                      `saga` TEXT NOT NULL,
                      `pov` TEXT NOT NULL,
                      `clues` TEXT NOT NULL,
                      `keywords` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            INSTANCE?.let { return it }

            return synchronized(this) {
                try {
                    val instance = buildDatabase(context)
                    INSTANCE = instance
                    instance
                } catch (firstEx: Exception) {
                    Log.w("AppDatabase", "First DB open failed: ${firstEx.message}", firstEx)

                    // Try deleting legacy/corrupt DB files then recreate
                    try {
                        deleteDatabaseFiles(context)
                        Log.w("AppDatabase", "Deleted database files; retrying creation.")
                    } catch (deleteEx: Exception) {
                        Log.e("AppDatabase", "Failed to delete DB files: ${deleteEx.message}", deleteEx)
                    }

                    try {
                        val instance2 = buildDatabase(context)
                        INSTANCE = instance2
                        instance2
                    } catch (secondEx: Exception) {
                        Log.e("AppDatabase", "Failed to recreate DB after deletion: ${secondEx.message}", secondEx)
                        throw secondEx
                    }
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                // Add explicit migration 2->3
                .addMigrations(MIGRATION_2_3)
                // Keep destructive fallback as a last resort
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        private fun deleteDatabaseFiles(context: Context) {
            val legacyNames = listOf("gameguessr_database", DB_NAME)
            for (name in legacyNames) {
                try {
                    context.deleteDatabase(name)
                    val dbFile = context.getDatabasePath(name)
                    val parent = dbFile.parentFile
                    if (parent != null && parent.exists()) {
                        val main = File(parent, name)
                        val wal = File(parent, "$name-wal")
                        val shm = File(parent, "$name-shm")
                        if (main.exists()) main.delete()
                        if (wal.exists()) wal.delete()
                        if (shm.exists()) shm.delete()
                    }
                } catch (ex: Exception) {
                    Log.w("AppDatabase", "Unable to delete DB file $name: ${ex.message}")
                }
            }
        }
    }
}
