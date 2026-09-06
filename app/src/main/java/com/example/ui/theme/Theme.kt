package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaColorScheme = darkColorScheme(
  primary = ImdbGold,
  onPrimary = Color.Black,
  primaryContainer = CinemaRedDark,
  onPrimaryContainer = Color.White,
  secondary = CinemaAccentBlue,
  onSecondary = Color.Black,
  background = CinemaDarkBackground,
  onBackground = CinemaTextPrimary,
  surface = CinemaSurface,
  onSurface = CinemaTextPrimary,
  surfaceVariant = CinemaSurfaceVariant,
  onSurfaceVariant = CinemaTextSecondary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CinemaColorScheme,
    typography = Typography,
    content = content
  )
}

