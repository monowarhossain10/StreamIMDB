package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WatchlistMovie
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchlistMenuOverlay(
    watchlist: List<WatchlistMovie>,
    currentTitle: String,
    currentUrl: String,
    isCurrentInWatchlist: Boolean,
    onSelectMovie: (String) -> Unit,
    onRemoveFromWatchlist: (Long) -> Unit,
    onAddCurrentToWatchlist: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() }
            .testTag("watchlist_menu_overlay_scrim")
    ) {
        // Menu Overlay Side Drawer
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 380.dp, max = 460.dp)
                .background(CinemaDarkBackground)
                .border(1.dp, CinemaSurfaceVariant, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
                .testTag("watchlist_menu_drawer")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ImdbGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = ImdbGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "My Watchlist",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(CinemaRed)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${watchlist.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Room Local Database",
                                color = CinemaTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CinemaSurface)
                            .testTag("close_watchlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Watchlist",
                            tint = CinemaTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar: Quick Add Current Page / Movie
                if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrentInWatchlist) CinemaSurfaceVariant else CinemaRed)
                            .clickable(enabled = !isCurrentInWatchlist) { onAddCurrentToWatchlist() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("add_current_to_watchlist_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCurrentInWatchlist) Icons.Default.Bookmark else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isCurrentInWatchlist) ImdbGold else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCurrentInWatchlist) "Already in Watchlist" else "Save Current Movie to Watchlist",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Watchlist Movies List
                if (watchlist.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = CinemaTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Watchlist is empty",
                                color = CinemaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Save movies you want to watch later by tapping 'Save Current Movie' or the bookmark icon on any movie page.",
                                color = CinemaTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(watchlist, key = { it.id }) { movie ->
                            WatchlistMovieItem(
                                movie = movie,
                                onPlay = {
                                    onSelectMovie(movie.url)
                                    onDismiss()
                                },
                                onRemove = { onRemoveFromWatchlist(movie.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistMovieItem(
    movie: WatchlistMovie,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(movie.addedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(movie.addedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlay() }
            .focusable()
            .border(1.dp, CinemaSurfaceVariant, RoundedCornerShape(10.dp))
            .testTag("watchlist_item_${movie.id}"),
        colors = CardDefaults.cardColors(containerColor = CinemaSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movie.title,
                        color = CinemaTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!movie.imdbId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ImdbGold)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = movie.imdbId,
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Added $dateStr",
                    color = CinemaTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play action button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CinemaRed)
                    .testTag("play_watchlist_item_${movie.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Movie",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete action button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CinemaSurfaceVariant)
                    .testTag("delete_watchlist_item_${movie.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Watchlist",
                    tint = CinemaTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
