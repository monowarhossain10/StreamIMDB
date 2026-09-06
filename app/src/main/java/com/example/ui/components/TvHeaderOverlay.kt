package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.tv.CursorSpeed
import com.example.tv.StreamWebController
import com.example.tv.VirtualCursorController
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold

@Composable
fun TvHeaderOverlay(
    webController: StreamWebController,
    cursorController: VirtualCursorController,
    phoneServerUrl: String,
    onOpenSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenWatchlist: () -> Unit,
    watchlistCount: Int = 0,
    onOpenPhoneRemote: () -> Unit,
    onToggleBookmark: () -> Unit,
    isCurrentBookmarked: Boolean,
    onToggleRecentlyWatched: () -> Unit = {},
    recentlyWatchedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tv_header_overlay")
    ) {
        // Loading line indicator
        if (webController.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = ImdbGold,
                trackColor = Color.Transparent
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaDarkBackground.copy(alpha = 0.94f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand & Navigation
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurface)
                                .clickable { webController.goHome() }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("brand_logo")
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_streamimdb_logo),
                                contentDescription = "StreamIMDb Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STREAM",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "IMDB",
                                color = ImdbGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { webController.goBack() },
                            modifier = Modifier.testTag("nav_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CinemaTextPrimary)
                        }

                        IconButton(
                            onClick = { webController.goForward() },
                            modifier = Modifier.testTag("nav_forward_button")
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = CinemaTextSecondary)
                        }

                        IconButton(
                            onClick = { webController.reload() },
                            modifier = Modifier.testTag("nav_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CinemaTextPrimary)
                        }

                        IconButton(
                            onClick = { webController.goHome() },
                            modifier = Modifier.testTag("nav_home_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = ImdbGold)
                        }
                    }

                    // Center Quick Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Voice Search Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaRed)
                                .clickable { onVoiceSearch() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("header_voice_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice Search",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Quick Search / IMDb ID Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .clickable { onOpenSearch() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("header_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = ImdbGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Search / IMDb ID",
                                    color = CinemaTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Watchlist (Room Database) Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (watchlistCount > 0) CinemaSurfaceVariant else CinemaSurface)
                                .clickable { onOpenWatchlist() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("header_watchlist_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = "Watchlist", tint = ImdbGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watchlist",
                                    color = CinemaTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (watchlistCount > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CinemaRed)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$watchlistCount",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Recently Watched Toggle Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .clickable { onToggleRecentlyWatched() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("header_recent_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = "Recently Watched", tint = ImdbGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recent",
                                    color = CinemaTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (recentlyWatchedCount > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CinemaSurface)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$recentlyWatchedCount",
                                            color = CinemaTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Library / History Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .clickable { onOpenBookmarks() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("header_library_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = if (isCurrentBookmarked) ImdbGold else CinemaTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Favorites",
                                    color = CinemaTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Bookmark current page toggle
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.testTag("toggle_bookmark_button")
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Save Bookmark",
                                tint = if (isCurrentBookmarked) ImdbGold else CinemaTextSecondary
                            )
                        }
                    }

                    // Right Remote & Control Tools
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Remote Mode Toggle (Cursor vs D-Pad)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (cursorController.isCursorMode) CinemaRed else CinemaSurfaceVariant)
                                .clickable { cursorController.toggleMode() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("cursor_mode_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mouse, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (cursorController.isCursorMode) "Virtual Mouse ON" else "D-Pad Direct",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Cursor Speed Toggle
                        if (cursorController.isCursorMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CinemaSurfaceVariant)
                                    .clickable { cursorController.cycleSpeed() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("cursor_speed_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = ImdbGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cursorController.cursorSpeed.label,
                                        color = CinemaTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Zoom controller (100% -> 125% -> 150%)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .clickable {
                                    val nextZoom = when (webController.textZoom) {
                                        100 -> 125
                                        125 -> 150
                                        else -> 100
                                    }
                                    webController.setZoom(nextZoom)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("zoom_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = CinemaTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${webController.textZoom}%",
                                    color = CinemaTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Phone Remote Helper Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .border(1.dp, ImdbGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onOpenPhoneRemote() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("phone_remote_header_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ImdbGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Phone Remote",
                                    color = ImdbGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Ad Shield Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinemaSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("ad_shield_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF3FB950), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${webController.blockedAdsCount} blocked",
                                    color = Color(0xFF3FB950),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tiny floating toggle bar to minimize / maximize the header
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(CinemaDarkBackground.copy(alpha = 0.85f))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .testTag("toggle_header_expand_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Hide Menu" else "Show Menu",
                tint = ImdbGold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
