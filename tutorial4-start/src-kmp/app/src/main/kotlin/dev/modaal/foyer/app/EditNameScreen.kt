// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.editname.EditNameAction

/** The name editor over its store: the field is bound to `draft` both ways. */
@Composable
fun EditNameScreen(store: EditNameStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()

  Column(modifier.fillMaxSize().padding(24.dp)) {
    Text("Edit name", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
      value = state.draft,
      onValueChange = { store.send(EditNameAction.DraftChanged(it)) },
      label = { Text("Display name") },
      singleLine = true,
      isError = state.validation != null,
      supportingText = state.validation?.let { message -> { Text(message) } },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = { store.send(EditNameAction.CancelTapped) }) { Text("Cancel") }
      Button(
        onClick = { store.send(EditNameAction.SaveTapped) },
        enabled = !state.isSaving,
      ) {
        Text("Save")
      }
    }
  }
}
