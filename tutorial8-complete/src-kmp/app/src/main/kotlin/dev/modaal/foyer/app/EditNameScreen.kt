// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.editname.EditNameAction
import dev.modaal.foyer.editname.NameValidation
import dev.modaal.foyer.root.ProfilePath

/** The name editor over its store: the field is bound to `draft` both ways. */
@Composable
fun EditNameScreen(store: EditNameStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  BackHandler(enabled = BackPolicy.editNameEnabled(ProfilePath.EditName)) {
    store.send(EditNameAction.CancelTapped)
  }

  Column(modifier.fillMaxSize().padding(24.dp)) {
    Text(stringResource(R.string.edit_name_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
      value = state.draft,
      onValueChange = { store.send(EditNameAction.DraftChanged(it)) },
      label = { Text(stringResource(R.string.display_name)) },
      singleLine = true,
      isError = state.validation != null,
      supportingText = state.validation?.let { refusal -> { Text(stringResource(refusal.messageRes)) } },
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = { store.send(EditNameAction.CancelTapped) }) { Text(stringResource(R.string.cancel)) }
      Button(
        onClick = { store.send(EditNameAction.SaveTapped) },
        enabled = !state.isSaving,
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }
}

/** The refusal's string, from its case: the reducer says why, the screen says it. */
val NameValidation.messageRes: Int
  @StringRes
  get() =
    when (this) {
      NameValidation.Empty -> R.string.validation_empty
      NameValidation.TooLong -> R.string.validation_too_long
    }
