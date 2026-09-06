package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold

data class QuickSuggestion(val title: String, val imdbId: String)

val POPULAR_MOVIES = listOf(
    QuickSuggestion("Inception", "tt1375666"),
    QuickSuggestion("Interstellar", "tt0816692"),
    QuickSuggestion("The Dark Knight", "tt0468569"),
    QuickSuggestion("Oppenheimer", "tt15398776"),
    QuickSuggestion("Avatar: The Way of Water", "tt1630029"),
    QuickSuggestion("Dune: Part Two", "tt15239678"),
    QuickSuggestion("Fight Club", "tt0137523"),
    QuickSuggestion("The Matrix", "tt0133093")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSearchDialog(
    onDismiss: () -> Unit,
    onSelectUrl: (String) -> Unit,
    onStartVoiceSearch: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .testTag("quick_search_dialog"),
            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ImdbGold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "StreamIMDb Quick Watch",
                            color = CinemaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CinemaTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_query_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = CinemaTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CinemaTextPrimary,
                        unfocusedTextColor = CinemaTextPrimary,
                        focusedBorderColor = ImdbGold,
                        unfocusedBorderColor = CinemaSurfaceVariant,
                        focusedContainerColor = CinemaDarkBackground,
                        unfocusedContainerColor = CinemaDarkBackground
                    ),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onStartVoiceSearch()
                                },
                                modifier = Modifier.testTag("dialog_voice_search_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speak with TV remote",
                                    tint = ImdbGold
                                )
                            }
                            Button(
                                onClick = {
                                    if (query.isNotBlank()) {
                                        val target = if (query.lowercase().startsWith("tt")) {
                                            "https://streamimdb.ru/movie/$query"
                                        } else if (query.startsWith("http")) {
                                            query
                                        } else {
                                            "https://streamimdb.ru/?s=" + java.net.URLEncoder.encode(query, "UTF-8")
                                        }
                                        onSelectUrl(target)
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("submit_search_button")
                            ) {
                                Text("Open", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Voice search banner button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CinemaDarkBackground)
                        .border(1.dp, ImdbGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            onDismiss()
                            onStartVoiceSearch()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("dialog_voice_search_banner_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = CinemaRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Speak to Search (TV Remote Mic)",
                                    color = CinemaTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Press mic button on remote or tap here to speak movie title",
                                    color = CinemaTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "START",
                            color = ImdbGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Quick IMDb Picks (Instant Stream)",
                    color = ImdbGold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    POPULAR_MOVIES.forEach { movie ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CinemaSurfaceVariant)
                                .border(1.dp, CinemaSurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable {
                                    onSelectUrl("https://streamimdb.ru/movie/${movie.imdbId}")
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("suggestion_${movie.imdbId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = ImdbGold,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = movie.title,
                                    color = CinemaTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
