// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * The Compose render layer: the screen of whichever child the root has
 * mounted. The root mount holds the stores on the retained scope;
 * composables collect state and send actions, and hold nothing of their own.
 */
@Composable
fun AppRoot(root: RootMount) {
  val child by root.child.collectAsState()
  MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
  ) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        when (val current = child) {
          is RootChildMount.Splash -> SplashScreen(current.store)
          is RootChildMount.SignIn -> SignInScreen(current.store)
          is RootChildMount.Main -> MainScreen(current.mount)
          null -> Unit
        }
      }
    }
  }
}
