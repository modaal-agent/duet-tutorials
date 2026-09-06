// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.splash.SplashAction

/** How long the reveal plays. The animation is the screen's; the safety net is the feature's. */
private const val CEREMONY_MILLIS = 1_600

/**
 * The splash screen over the splash store. Every value on it comes from the
 * store's state, and every event leaves as an action: `Appeared` when the
 * screen enters the composition, `CeremonyFinished` when the reveal ends.
 * After a rotation the effect runs again; the reducer's arming guard makes
 * the second `Appeared` inert and the host ignores a second completion.
 */
@Composable
fun SplashScreen(store: SplashStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  val reveal = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    store.send(SplashAction.Appeared)
    reveal.animateTo(1f, tween(CEREMONY_MILLIS, easing = FastOutSlowInEasing))
    store.send(SplashAction.CeremonyFinished)
  }

  Column(
    modifier
      .fillMaxSize()
      .alpha(reveal.value)
      .offset(y = (16 * (1 - reveal.value)).dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Foyer", style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(
      "One core, two apps",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.isArmed) {
      Spacer(Modifier.height(24.dp))
      LinearProgressIndicator(Modifier.width(120.dp))
    }
  }
}
