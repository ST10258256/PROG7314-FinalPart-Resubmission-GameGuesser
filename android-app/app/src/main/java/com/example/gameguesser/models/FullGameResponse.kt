package com.example.gameguesser.models

data class FullGameResponse(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val coverImageUrl: String?,
    val genre: String?,
    val platforms: List<String>?,
    val releaseYear: Int?,
    val developer: String?,
    val publisher: String?,
    val description: String?,
    val budget: String?,
    val saga: String?,
    val pov: String?,
    val clues: List<String>?
)
