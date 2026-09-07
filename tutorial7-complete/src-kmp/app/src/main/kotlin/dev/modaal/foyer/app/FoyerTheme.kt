// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import android.app.UiModeManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.modaal.duet.services.theming.FontFamilyToken
import dev.modaal.duet.services.theming.FontToken
import dev.modaal.duet.services.theming.ResolvedAppearance
import dev.modaal.foyer.theming.HighContrastTheme
import dev.modaal.foyer.theming.MainTheme
import dev.modaal.foyer.theming.SemanticColor
import dev.modaal.foyer.theming.SemanticFont
import dev.modaal.foyer.theming.Theme

/** The theme in effect below the enclosing [FoyerTheme]. */
val LocalTheme = staticCompositionLocalOf<Theme> { MainTheme }

/** The appearance the enclosing [FoyerTheme] resolved. */
val LocalAppearance = staticCompositionLocalOf { ResolvedAppearance.Light }

/**
 * Wraps [content] in the theme the system's settings select: the main theme,
 * or the high-contrast theme while the contrast setting is raised, at the
 * light or dark appearance the system is in. Material's own components keep
 * their default scheme; the surfaces this app draws itself read the tokens.
 */
@Composable
fun FoyerTheme(content: @Composable () -> Unit) {
  val dark = isSystemInDarkTheme()
  val theme = if (rememberSystemContrast() > 0f) HighContrastTheme else MainTheme
  CompositionLocalProvider(
    LocalTheme provides theme,
    LocalAppearance provides if (dark) ResolvedAppearance.Dark else ResolvedAppearance.Light,
  ) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
  }
}

/** A colour token's value under the theme in effect, at the resolved appearance. */
@Composable
@ReadOnlyComposable
fun SemanticColor.color(): Color = Color(LocalTheme.current.color(this).value(LocalAppearance.current))

/** A type token as a text style, in the platform's face for its family. */
@Composable
@ReadOnlyComposable
fun SemanticFont.textStyle(): TextStyle = LocalTheme.current.font(this).textStyle()

private fun FontToken.textStyle(): TextStyle =
  TextStyle(
    fontFamily =
      when (family) {
        FontFamilyToken.Serif -> FontFamily.Serif
        FontFamilyToken.Sans -> FontFamily.SansSerif
        FontFamilyToken.Mono -> FontFamily.Monospace
      },
    fontWeight = FontWeight(weight),
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = trackingEm.em,
  )

/**
 * The system's contrast setting, `0` (standard) to `1` (high), observed for
 * as long as the composable is up. The setting exists from Android 14; an
 * earlier release reads as standard.
 */
@Composable
private fun rememberSystemContrast(): Float {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return 0f
  val context = LocalContext.current
  val manager = remember(context) { context.getSystemService(UiModeManager::class.java) }
  var contrast by remember(manager) { mutableFloatStateOf(manager.contrast) }
  DisposableEffect(manager) {
    val listener = UiModeManager.ContrastChangeListener { contrast = it }
    manager.addContrastChangeListener(context.mainExecutor, listener)
    onDispose { manager.removeContrastChangeListener(listener) }
  }
  return contrast
}
