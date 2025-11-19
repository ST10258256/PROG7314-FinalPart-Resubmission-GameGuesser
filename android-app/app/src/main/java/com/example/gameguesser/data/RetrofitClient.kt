package com.example.gameguesser.data

import com.google.gson.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

object RetrofitClient {

    private const val BASE_URL = "https://gameguesser-api.onrender.com/"

    private val gson: Gson = GsonBuilder()
        // Custom deserializer for a single Game object to handle _id as {$oid: "..."} or as a primitive string
        .registerTypeAdapter(Game::class.java, JsonDeserializer { json, _, _ ->
            try {
                val jsonObj = json.asJsonObject

                // Normalize _id into IdObject or id string
                val idElement = jsonObj.get("_id")
                var normalizedIdObj: IdObject? = null
                var normalizedIdString: String? = null

                if (idElement != null && idElement.isJsonObject) {
                    val oidElem = idElement.asJsonObject.get("\$oid")
                    if (oidElem != null && !oidElem.isJsonNull) {
                        normalizedIdObj = IdObject(oidElem.asString ?: "")
                    }
                } else if (idElement != null && idElement.isJsonPrimitive) {
                    normalizedIdString = idElement.asString
                }

                // Deserialize rest of fields using base Gson (to pick up lists, ints etc.)
                val baseGson = Gson()
                val game = baseGson.fromJson(jsonObj, Game::class.java)

                // Ensure _id and id fields are populated sensibly
                if (normalizedIdObj != null) {
                    game._id = normalizedIdObj
                    if (game.id.isBlank()) game.id = normalizedIdObj.oid
                } else if (!normalizedIdString.isNullOrBlank()) {
                    if (game.id.isBlank()) game.id = normalizedIdString
                } else {
                    // fallback: if _id not present but id empty, attempt to leave game.id (maybe server supplies "id")
                    if (game.id.isBlank()) {
                        // keep empty for now — repository will sanitize before DB insert
                    }
                }

                // Ensure lists are non-null (Gson normally does this but be safe)
                if (game.platforms == null) game.platforms = emptyList()
                if (game.keywords == null) game.keywords = emptyList()
                if (game.clues == null) game.clues = emptyList()

                game
            } catch (ex: Exception) {
                // In case something unexpected is returned, try a fallback: plain Gson parse
                Gson().fromJson(json, Game::class.java)
            }
        })
        .setLenient()
        .create()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
