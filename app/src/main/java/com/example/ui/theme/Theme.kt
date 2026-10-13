package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
  primary = PrimaryAction,
  onPrimary = PureWhite,
  primaryContainer = SurfaceNeutral,
  onPrimaryContainer = TextPrimary,
  secondary = AccentSage,
  onSecondary = PureWhite,
  secondaryContainer = SurfaceNeutral,
  onSecondaryContainer = TextPrimary,
  tertiary = StatusGreen,
  onTertiary = PureWhite,
  background = MainBackground,
  onBackground = TextPrimary,
  surface = CardBackground,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceNeutral,
  onSurfaceVariant = TextSecondary,
  outline = BorderSubtle,
  outlineVariant = BorderSubtle,
  error = StatusRed,
  onError = PureWhite,
  errorContainer = StatusRedBg,
  onErrorContainer = StatusRed
)

@Composable
fun HisabTripTheme(
  content: @Composable () -> Unit
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = MainBackground.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
      }
    }
  }

  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}
