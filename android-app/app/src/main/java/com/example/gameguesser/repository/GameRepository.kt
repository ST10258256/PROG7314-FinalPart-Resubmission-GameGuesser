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

    private fun convertToGame(obj: Any?): Game {
        if (obj == null) return Game()
        return try {
            val json = gson.toJson(obj)
            gson.fromJson(json, Game::class.java)
        } catch (ex: Exception) {
            Game()
        }
    }

    private fun convertToGameList(obj: Any?): List<Game> {
        if (obj == null) return emptyList()
        return try {
            val json = gson.toJson(obj)
            val listType = object : TypeToken<List<Game>>() {}.type
            gson.fromJson(json, listType) ?: emptyList()
        } catch (ex: Exception) {
            emptyList()
        }
    }

    private fun sanitizeGames(list: List<Game>): List<Game> {
        return list.map { original ->
            val g = Game()
            g._id = original._id
            g.id = when {
                !original.id.isNullOrBlank() -> original.id
                !original._id?.oid.isNullOrBlank() -> original._id!!.oid
                else -> (original.name + original.releaseYear).hashCode().toString()
            }
            g.name = original.name
            g.genre = original.genre
            g.platforms = original.platforms ?: emptyList()
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

    suspend fun getRandomGame(): Game? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isOnline(context)) {
                val response = api.getRandomGame().execute()
                val apiBody = response.body()
                val game = convertToGame(apiBody)
                if (game != null) {
                    val sanitized = sanitizeGames(listOf(game))
                    dao.insertGame(sanitized.first())
                }
                return@withContext if (game != null && !game.id.isNullOrBlank()) game else dao.getAllGames().randomOrNull()
            } else {
                dao.getAllGames().randomOrNull()
            }
        } catch (_: Exception) {
            dao.getAllGames().randomOrNull()
        }
    }

    suspend fun getGameByIdOfflineSafe(id: String): Game? = withContext(Dispatchers.IO) {
        val localGame = dao.getGameById(id)
        if (localGame != null) return@withContext localGame

        if (NetworkUtils.isOnline(context)) {
            try {
                val response = try {
                    api.getGameById(id).execute()
                } catch (_: Exception) {
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
            } catch (_: Exception) {
                return@withContext dao.getGameById(id)
            }
        } else {
            dao.getGameById(id)
        }
    }

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
        } catch (_: Exception) { }
    }

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

    suspend fun findGamesByKeyword(keyword: String): List<Game> = withContext(Dispatchers.IO) {
        val allGames = dao.getAllGames()
        if (keyword.isBlank()) allGames
        else allGames.filter { game ->
            game.name.contains(keyword, ignoreCase = true) ||
                    (game.keywords.any { it.contains(keyword, ignoreCase = true) })
        }
    }

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

    /**
     * Compare guess online, or fallback to offline heuristic:
     * - matches map: only keywords that are exact or partial (contain/contained) are included
     * - correct = exact keyword match OR guess equals game.name (case-insensitive)
     */
    suspend fun compareGame(request: CompareRequest): ComparisonResponse? {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val response = RetrofitClient.api.compareGame(request).execute()
                response.body()
            } else {
                // offline fallback: use local DB game data to compute matches
                val game = getGameByIdOfflineSafe(request.gameId)
                val matches = mutableMapOf<String, String>()
                val guessNormalized = request.guessName.trim()
                game?.keywords?.forEach { keyword ->
                    val k = keyword.trim()
                    if (k.equals(guessNormalized, ignoreCase = true)) {
                        matches[k] = "exact"
                    } else if (k.contains(guessNormalized, ignoreCase = true) ||
                        guessNormalized.contains(k, ignoreCase = true)
                    ) {
                        matches[k] = "partial"
                    }
                }

                val correct = (game?.name?.trim()?.equals(guessNormalized, ignoreCase = true) == true)
                        || matches.any { it.value == "exact" }

                ComparisonResponse(
                    correct = correct,
                    matches = matches
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
