// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.name.NameAction
import dev.modaal.foyer.onboarding.OnboardingAction
import dev.modaal.foyer.ports.OnboardingPage
import dev.modaal.foyer.preferences.PreferenceKeys
import dev.modaal.foyer.preferences.PreferencesAction
import dev.modaal.foyer.progress.ProgressAction
import dev.modaal.foyer.progress.STEP_COUNT
import dev.modaal.foyer.progress.stepNumber
import dev.modaal.foyer.welcome.WelcomeAction

/**
 * The onboarding level over its mount: the progress row, then the step the
 * page names. Back is an action of the level; the handler is enabled by the
 * back policy's predicate, so the first page leaves the app.
 */
@Composable
fun OnboardingScreen(mount: OnboardingMount, modifier: Modifier = Modifier) {
  val state by mount.store.state.collectAsState()
  val step by mount.step.collectAsState()
  BackHandler(enabled = BackPolicy.onboardingEnabled(state.page)) {
    mount.store.send(OnboardingAction.Back)
  }

  Column(modifier.fillMaxSize().padding(24.dp)) {
    ProgressRow(mount.progress)
    Spacer(Modifier.height(32.dp))
    when (val current = step) {
      is OnboardingStepMount.Welcome -> WelcomeStep(current.store)
      is OnboardingStepMount.Name -> NameStep(current.store)
      is OnboardingStepMount.Preferences -> PreferencesStep(current.store)
      null -> Unit
    }
  }
}

/** "Step n of 3" from the projected page, and one tick per step the seam reported ready. */
@Composable
private fun ProgressRow(store: ProgressStore) {
  val state by store.state.collectAsState()
  LaunchedEffect(Unit) { store.send(ProgressAction.Appeared) }
  Column {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Step ${state.page.stepNumber} of $STEP_COUNT",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.weight(1f),
      )
      OnboardingPage.entries.forEach { page ->
        val ready = state.readiness[page] == true
        Icon(
          if (ready) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
          contentDescription = if (ready) "${page.name} ready" else "${page.name} not ready",
          tint =
            if (ready) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
      progress = { state.page.stepNumber.toFloat() / STEP_COUNT },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun WelcomeStep(store: WelcomeStore) {
  LaunchedEffect(Unit) { store.send(WelcomeAction.Appeared) }
  Column(Modifier.fillMaxSize()) {
    Text("Welcome to Foyer", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
      "Three short steps and you are in.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = { store.send(WelcomeAction.ContinueTapped) }, modifier = Modifier.fillMaxWidth()) {
      Text("Continue")
    }
  }
}

@Composable
private fun NameStep(store: NameStore) {
  val state by store.state.collectAsState()
  Column(Modifier.fillMaxSize()) {
    Text("What should we call you?", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
      value = state.draft,
      onValueChange = { store.send(NameAction.DraftChanged(it)) },
      label = { Text("Display name") },
      singleLine = true,
      isError = state.validation != null,
      supportingText = state.validation?.let { message -> { Text(message) } },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = { store.send(NameAction.ContinueTapped) }, modifier = Modifier.fillMaxWidth()) {
      Text("Continue")
    }
  }
}

@Composable
private fun PreferencesStep(store: PreferencesStore) {
  val state by store.state.collectAsState()
  Column(Modifier.fillMaxSize()) {
    Text("Pick at least one", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    PreferenceKeys.ALL.forEach { key ->
      Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(key.preferenceLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Switch(
          checked = key in state.selected,
          onCheckedChange = { store.send(PreferencesAction.PreferenceToggled(key)) },
        )
      }
    }
    Spacer(Modifier.height(24.dp))
    Button(
      onClick = { store.send(PreferencesAction.ContinueTapped) },
      enabled = state.isReady,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Finish")
    }
  }
}

/** The toggle's label, from its key. */
val String.preferenceLabel: String
  get() =
    when (this) {
      "digest" -> "Weekly digest"
      "reminders" -> "Reminders"
      "tips" -> "Tips and tricks"
      else -> this
    }
