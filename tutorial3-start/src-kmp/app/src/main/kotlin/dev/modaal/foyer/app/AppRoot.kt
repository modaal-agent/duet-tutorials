// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.splash.SplashCompletionPath

/**
 * The Compose render layer: one screen chosen from the host's phase. The
 * host holds the stores on the retained scope; composables collect state
 * and send actions, and hold nothing of their own.
 */
@Composable
fun AppRoot(app: AppHost) {
  val phase by app.phase.collectAsState()
  MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
  ) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        when (val current = phase) {
          AppPhase.Splash -> app.splash?.let { SplashScreen(it) }
          is AppPhase.Placeholder -> PlaceholderScreen(current.completedBy)
        }
      }
    }
  }
}

/** The screen after the splash. Tutorial 3 replaces it with the sign-in gate. */
@Composable
private fun PlaceholderScreen(completedBy: SplashCompletionPath) {
  val path =
    when (completedBy) {
      SplashCompletionPath.Ceremony -> "the ceremony"
      SplashCompletionPath.SafetyNet -> "the safety net"
    }
  Column(
    Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Foyer", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(12.dp))
    Text("Splash completed by $path.", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    Text(
      "Tutorial 3 puts the sign-in gate here.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
