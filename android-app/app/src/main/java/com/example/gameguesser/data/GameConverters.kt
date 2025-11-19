package com.example.gameguesser.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        // store empty list as JSON array "[]", avoid storing literal "null"
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        } catch (ex: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIdObject(idObject: IdObject?): String? {
        return idObject?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toIdObject(value: String?): IdObject? {
        return value?.let { Gson().fromJson(it, IdObject::class.java) }
    }

}
