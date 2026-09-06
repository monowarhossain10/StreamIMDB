package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistMovie>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: WatchlistMovie): Long

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM watchlist WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT * FROM watchlist WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): WatchlistMovie?

    @Query("SELECT COUNT(*) FROM watchlist WHERE url = :url")
    fun observeIsWatchlisted(url: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM watchlist")
    fun getWatchlistCount(): Flow<Int>
}
