package com.example.gameguesser.data

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class GameDeserializer : JsonDeserializer<Game> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Game {

        val obj = json.asJsonObject
        val idObj = obj["_id"]?.asJsonObject
        val oid = idObj?.get("\$oid")?.asString ?: ""

        val game = Gson().fromJson(json, Game::class.java)

        // Fix: populate primary key
        if (game.id.isBlank()) {
            game.id = oid
        }

        return game
    }
}
