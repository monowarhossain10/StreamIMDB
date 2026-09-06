package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistMovie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WatchlistDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WatchlistDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.watchlistDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveWatchlistMovie() = runBlocking {
        val movie = WatchlistMovie(
            title = "Inception",
            url = "https://www.imdb.com/title/tt1375666/",
            imdbId = "tt1375666"
        )
        dao.insert(movie)

        val allMovies = dao.getAllWatchlist().first()
        assertEquals(1, allMovies.size)
        assertEquals("Inception", allMovies[0].title)
        assertEquals("tt1375666", allMovies[0].imdbId)

        val count = dao.getWatchlistCount().first()
        assertEquals(1, count)

        val countByUrl = dao.observeIsWatchlisted("https://www.imdb.com/title/tt1375666/").first()
        assertTrue(countByUrl > 0)
    }

    @Test
    fun removeWatchlistMovie() = runBlocking {
        val movie = WatchlistMovie(
            title = "The Dark Knight",
            url = "https://www.imdb.com/title/tt0468569/",
            imdbId = "tt0468569"
        )
        val id = dao.insert(movie)

        val beforeDelete = dao.getAllWatchlist().first()
        assertEquals(1, beforeDelete.size)

        dao.deleteById(id)

        val afterDelete = dao.getAllWatchlist().first()
        assertEquals(0, afterDelete.size)

        val countByUrl = dao.observeIsWatchlisted("https://www.imdb.com/title/tt0468569/").first()
        assertFalse(countByUrl > 0)
    }
}
