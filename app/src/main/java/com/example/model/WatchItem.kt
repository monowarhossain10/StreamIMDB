package com.example.model

data class WatchItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val url: String,
    val imdbId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
