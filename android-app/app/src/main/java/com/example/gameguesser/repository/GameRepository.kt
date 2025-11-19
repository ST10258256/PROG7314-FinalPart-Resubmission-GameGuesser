package com.example.gameguesser.repository

import android.content.Context
import com.example.gameguesser.DAOs.GameDAO.GameDao
import com.example.gameguesser.data.Game
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.data.ApiService
import com.example.gameguesser.models.CompareRequest
import com.example.gameguesser.models.ComparisonResponse
import com.example.gameguesser.models.GuessResponse
import com.example.gameguesser.utils.NetworkUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(
    private val dao: GameDao,
    private val api: ApiService,
    private val context: Context
) {

    private val gson = Gson()

    // Convert any API response object into a Game instance via Gson serialization/deserialization.
    // This avoids compile-time access to fields that may not exist on the response type.
    private fun convertToGame(obj: Any?): Game {
        if (obj == null) return Game() // empty-but-valid Game
        return try {
            val json = gson.toJson(obj)
            gson.fromJson(json, Game::class.java)
        } catch (ex: Exception) {
            // fallback: return an empty-but-valid Game
            Game()
        }
    }

    // Convert API response (which might be a list of mixed objects) into List<Game>
    private fun convertToGameList(obj: Any?): List<Game> {
        if (obj == null) return emptyList()
        return try {
            // If obj is already a List<*>, serialize it to JSON and then parse as List<Game>
            val json = gson.toJson(obj)
            val listType = object : TypeToken<List<Game>>() {}.type
            gson.fromJson(json, listType) ?: emptyList()
        } catch (ex: Exception) {
            emptyList()
        }
    }

    /**
     * Ensure each Game has a usable primary key (id) before inserting into Room.
     * Constructs a fresh Game instance for each input (avoids relying on copy() that may not be safe).
     */
    private fun sanitizeGames(list: List<Game>): List<Game> {
        return list.map { original ->
            val g = Game()
            g._id = original._id
            // prefer explicit id, then _id.oid, then fallback hash-based id
            g.id = when {
                !original.id.isNullOrBlank() -> original.id
                !original._id?.oid.isNullOrBlank() -> original._id!!.oid
                else -> (original.name + original.releaseYear).hashCode().toString()
            }
            g.name = original.name
            g.genre = original.genre
            g.platforms = original.platforms    ?: emptyList()
            g.releaseYear = original.releaseYear
            g.developer = original.developer
            g.publisher = original.publisher
            g.description = original.description
            g.coverImageUrl = original.coverImageUrl
            g.budget = original.budget
            g.saga = original.saga
            g.pov = original.pov
            g.clues = original.clues ?: emptyList()
            g.keywords = original.keywords ?: emptyList()
            g
        }
    }

    // Get random game (offline-safe)
    suspend fun getRandomGame(): Game? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isOnline(context)) {
                val response = api.getRandomGame().execute()
                val apiBody = response.body()
                // Convert response body to Game in a safe, generic way
                val game = convertToGame(apiBody)

                // Insert mapped Game into Room (sanitize single item)
                if (game != null) {
                    val sanitized = sanitizeGames(listOf(game))
                    dao.insertGame(sanitized.first())
                }

                return@withContext if (game != null && !game.id.isNullOrBlank()) game else dao.getAllGames().randomOrNull()
            } else {
                dao.getAllGames().randomOrNull()
            }
        } catch (ex: Exception) {
            // On any failure, fall back to local DB
            dao.getAllGames().randomOrNull()
        }
    }

    // Get game by ID (offline-safe)
    suspend fun getGameByIdOfflineSafe(id: String): Game? = withContext(Dispatchers.IO) {
        val localGame = dao.getGameById(id)
        if (localGame != null) return@withContext localGame

        if (NetworkUtils.isOnline(context)) {
            try {
                // Try to fetch a single game from API. If API doesn't provide get-by-id, we attempt random.
                // Note: keep generic conversion to avoid compile-time field assumptions.
                val response = try {
                    api.getGameById(id).execute() // if your ApiService has this endpoint, good
                } catch (_: Exception) {
                    // fallback to random if getById isn't supported by ApiService
                    api.getRandomGame().execute()
                }
                val apiBody = response.body()
                val game = convertToGame(apiBody)
                if (game != null) {
                    val sanitized = sanitizeGames(listOf(game))
                    dao.insertGame(sanitized.first())
                    return@withContext sanitized.first()
                }
                return@withContext dao.getGameById(id)
            } catch (ex: Exception) {
                return@withContext dao.getGameById(id)
            }
        } else {
            dao.getGameById(id)
        }
    }

    // Sync all games from API into local DB (offline-safe)
    suspend fun syncFromApi() = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isOnline(context)) return@withContext

        try {
            val response = api.getAllGamesFull().execute()
            val body = response.body()
            val games = convertToGameList(body)
            if (games.isNotEmpty()) {
                val sanitized = sanitizeGames(games)
                dao.insertGames(sanitized)
            }
        } catch (_: Exception) {
            // silently ignore network problems
        }
    }

    // Get all games (prefers local DB, falls back to API)
    suspend fun getAllGames(): List<Game> = withContext(Dispatchers.IO) {
        val localGames = dao.getAllGames()
        if (localGames.isNotEmpty()) return@withContext localGames

        return@withContext try {
            val response = api.getAllGamesFull().execute()
            val body = response.body()
            val games = convertToGameList(body)
            if (games.isNotEmpty()) {
                val sanitized = sanitizeGames(games)
                dao.insertGames(sanitized)
                sanitized
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Simple keyword search on local DB
    suspend fun findGamesByKeyword(keyword: String): List<Game> = withContext(Dispatchers.IO) {
        val allGames = dao.getAllGames()
        if (keyword.isBlank()) allGames
        else allGames.filter { game ->
            game.name.contains(keyword, ignoreCase = true) ||
                    (game.keywords.any { it.contains(keyword, ignoreCase = true) })
        }
    }

    // Submit guess to API (if online)
    suspend fun submitGuess(gameId: String, guess: String): GuessResponse? =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isOnline(context)) return@withContext null
            try {
                val response = api.submitGuess(gameId, guess).execute()
                if (response.isSuccessful) response.body() else null
            } catch (_: Exception) {
                null
            }
        }

    // Compare guess (online via API or offline local heuristic)
    suspend fun compareGame(request: CompareRequest): ComparisonResponse? {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val response = RetrofitClient.api.compareGame(request).execute()
                response.body()
            } else {
                val game = getGameByIdOfflineSafe(request.gameId)
                val matches = mutableMapOf<String, String>()
                game?.keywords?.forEach { keyword ->
                    matches[keyword] = if (keyword.equals(request.guessName, ignoreCase = true)) "exact" else "partial"
                }
                ComparisonResponse(
                    correct = matches.any { it.value == "exact" },
                    matches = matches
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
