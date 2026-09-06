package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WatchItem
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold

@Composable
fun RecentlyWatchedShelf(
    history: List<WatchItem>,
    onSelectMovie: (String) -> Unit,
    onAddToWatchlist: (WatchItem) -> Unit,
    modifier: Modifier = Modifier,
    isInitiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }
    val recentTen = remember(history) { history.take(10) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recently_watched_shelf")
    ) {
        // Shelf Toggle Bar / Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinemaDarkBackground.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CinemaSurfaceVariant.copy(alpha = 0.85f))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("recently_watched_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recently Watched",
                        tint = ImdbGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recently Watched",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (recentTen.isNotEmpty()) CinemaRed else Color.Gray)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${recentTen.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = CinemaTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isExpanded && recentTen.isNotEmpty()) {
                    Text(
                        text = "Last 10 Played Movies",
                        color = CinemaTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Expandable Content: Horizontal carousel of last 10 movies
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaDarkBackground.copy(alpha = 0.98f))
                    .padding(bottom = 12.dp)
            ) {
                if (recentTen.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CinemaSurface)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = CinemaTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No movies played yet. Play any movie on StreamIMDb to quickly resume it here.",
                                color = CinemaTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentTen, key = { it.id }) { item ->
                            RecentlyWatchedCard(
                                item = item,
                                onClick = { onSelectMovie(item.url) },
                                onAddToWatchlist = { onAddToWatchlist(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyWatchedCard(
    item: WatchItem,
    onClick: () -> Unit,
    onAddToWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .border(1.dp, CinemaSurfaceVariant, RoundedCornerShape(12.dp))
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .testTag("recently_watched_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CinemaSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Movie icon + IMDb ID badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CinemaSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = ImdbGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!item.imdbId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ImdbGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.imdbId,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Movie Title
            Text(
                text = item.title,
                color = CinemaTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row: Quick Play + Add to Watchlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CinemaRed)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resume",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onAddToWatchlist,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CinemaSurfaceVariant)
                        .testTag("add_to_watchlist_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save to Watchlist",
                        tint = ImdbGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
