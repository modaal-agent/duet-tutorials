// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.signin.SignInAction

/**
 * The sign-in gate over its store. The field's live text is the screen's
 * own; it reaches the store inside `ContinueTapped`, so the reducer sees the
 * address only when the user commits it.
 */
@Composable
fun SignInScreen(store: SignInStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  var address by rememberSaveable { mutableStateOf("") }

  Column(
    modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Foyer", style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(8.dp))
    Text(
      "Sign in to continue",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    OutlinedTextField(
      value = address,
      onValueChange = { address = it },
      label = { Text("Email") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
      onClick = { store.send(SignInAction.ContinueTapped(SignInProvider.Email(address))) },
      enabled = !state.isSigningIn,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Continue with email")
    }
    TextButton(
      onClick = { store.send(SignInAction.ContinueTapped(SignInProvider.Guest)) },
      enabled = !state.isSigningIn,
    ) {
      Text("Continue as guest")
    }
    Spacer(Modifier.height(16.dp))
    if (state.isSigningIn) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    state.failure?.let { failure ->
      Text(
        failure,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}
