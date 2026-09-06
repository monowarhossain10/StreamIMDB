package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.WatchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class WatchRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("streamimdb_tv_prefs", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<WatchItem>>(emptyList())
    val history: StateFlow<List<WatchItem>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<WatchItem>>(emptyList())
    val bookmarks: StateFlow<List<WatchItem>> = _bookmarks.asStateFlow()

    init {
        loadHistory()
        loadBookmarks()
        if (_bookmarks.value.isEmpty()) {
            // Seed with quick popular StreamIMDb links
            seedDefaultBookmarks()
        }
    }

    private fun seedDefaultBookmarks() {
        val defaults = listOf(
            WatchItem(title = "StreamIMDb Home", url = "https://streamimdb.ru/", isFavorite = true),
            WatchItem(title = "Inception", url = "https://streamimdb.ru/movie/tt1375666", imdbId = "tt1375666", isFavorite = true),
            WatchItem(title = "Interstellar", url = "https://streamimdb.ru/movie/tt0816692", imdbId = "tt0816692", isFavorite = true),
            WatchItem(title = "The Dark Knight", url = "https://streamimdb.ru/movie/tt0468569", imdbId = "tt0468569", isFavorite = true),
            WatchItem(title = "Oppenheimer", url = "https://streamimdb.ru/movie/tt15398776", imdbId = "tt15398776", isFavorite = true)
        )
        _bookmarks.value = defaults
        saveBookmarks(defaults)
    }

    fun addHistory(title: String, url: String, imdbId: String? = null) {
        if (url.isBlank() || url == "about:blank") return
        val current = _history.value.toMutableList()
        current.removeAll { it.url == url }
        current.add(0, WatchItem(title = title.ifBlank { "StreamIMDb Movie" }, url = url, imdbId = imdbId))
        val trimmed = current.take(30)
        _history.value = trimmed
        saveHistory(trimmed)
    }

    fun toggleBookmark(title: String, url: String, imdbId: String? = null): Boolean {
        val current = _bookmarks.value.toMutableList()
        val exists = current.any { it.url == url }
        if (exists) {
            current.removeAll { it.url == url }
            _bookmarks.value = current
            saveBookmarks(current)
            return false
        } else {
            current.add(0, WatchItem(title = title.ifBlank { "Movie" }, url = url, imdbId = imdbId, isFavorite = true))
            _bookmarks.value = current
            saveBookmarks(current)
            return true
        }
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarks.value.any { it.url == url }
    }

    private fun loadHistory() {
        val raw = prefs.getString("watch_history", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<WatchItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WatchItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        imdbId = if (obj.has("imdbId")) obj.getString("imdbId") else null,
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
            _history.value = list
        } catch (_: Exception) {}
    }

    private fun saveHistory(list: List<WatchItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                if (item.imdbId != null) put("imdbId", item.imdbId)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("watch_history", array.toString()).apply()
    }

    private fun loadBookmarks() {
        val raw = prefs.getString("bookmarks", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<WatchItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WatchItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        imdbId = if (obj.has("imdbId")) obj.getString("imdbId") else null,
                        timestamp = obj.optLong("timestamp"),
                        isFavorite = true
                    )
                )
            }
            _bookmarks.value = list
        } catch (_: Exception) {}
    }

    private fun saveBookmarks(list: List<WatchItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                if (item.imdbId != null) put("imdbId", item.imdbId)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("bookmarks", array.toString()).apply()
    }
}
