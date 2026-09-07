// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.root.ProfilePath

/**
 * The account screen over its mount: the editor when the mount has one,
 * else the name and the two rows. Every tap leaves as an action; the
 * editor's appearance is a consequence of `state.child`, never of the tap.
 */
@Composable
fun AccountScreen(mount: AccountMount, modifier: Modifier = Modifier) {
  val editor by mount.editName.collectAsState()
  // Composed before the editor's handler, so the editor's wins while it is up.
  BackHandler(enabled = BackPolicy.accountEnabled(ProfilePath.Account)) {
    mount.store.send(AccountAction.CloseTapped)
  }
  editor?.let { store ->
    EditNameScreen(store, modifier)
    return
  }

  val state by mount.store.state.collectAsState()
  Column(modifier.fillMaxSize()) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { mount.store.send(AccountAction.CloseTapped) }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
      }
      Text(stringResource(R.string.account_title), style = MaterialTheme.typography.titleLarge)
    }
    Text(
      state.displayName,
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    ListItem(
      headlineContent = { Text(stringResource(R.string.account_edit_name)) },
      modifier = Modifier.clickable { mount.store.send(AccountAction.EditNameTapped) },
    )
    HorizontalDivider()
    ListItem(
      headlineContent = {
        Text(
          stringResource(if (state.isSigningOut) R.string.signing_out else R.string.sign_out),
          color = MaterialTheme.colorScheme.error,
        )
      },
      modifier =
        Modifier.clickable(enabled = !state.isSigningOut) {
          mount.store.send(AccountAction.SignOutTapped)
        },
    )
    HorizontalDivider()
  }
}
