package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WatchlistRepository(context: Context) {

    private val dao: WatchlistDao = AppDatabase.getInstance(context).watchlistDao()

    val watchlist: Flow<List<WatchlistMovie>> = dao.getAllWatchlist()
    val count: Flow<Int> = dao.getWatchlistCount()

    fun observeIsWatchlisted(url: String): Flow<Boolean> {
        return dao.observeIsWatchlisted(url).map { it > 0 }
    }

    suspend fun isWatchlisted(url: String): Boolean = withContext(Dispatchers.IO) {
        dao.findByUrl(url) != null
    }

    suspend fun addToWatchlist(title: String, url: String, imdbId: String? = null): Long = withContext(Dispatchers.IO) {
        val effectiveTitle = title.ifBlank { "StreamIMDb Movie" }
        dao.insert(WatchlistMovie(title = effectiveTitle, url = url, imdbId = imdbId))
    }

    suspend fun removeFromWatchlist(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun removeFromWatchlistByUrl(url: String) = withContext(Dispatchers.IO) {
        dao.deleteByUrl(url)
    }

    suspend fun toggleWatchlist(title: String, url: String, imdbId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.findByUrl(url)
        if (existing != null) {
            dao.deleteById(existing.id)
            false
        } else {
            val effectiveTitle = title.ifBlank { "StreamIMDb Movie" }
            dao.insert(WatchlistMovie(title = effectiveTitle, url = url, imdbId = imdbId))
            true
        }
    }
}
