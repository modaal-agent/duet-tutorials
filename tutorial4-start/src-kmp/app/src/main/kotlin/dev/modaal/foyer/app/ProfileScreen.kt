// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.profile.ProfileAction

/** The profile tab over its mount: the account screen when mounted, else the header and the row. */
@Composable
fun ProfileScreen(mount: ProfileMount, modifier: Modifier = Modifier) {
  val account by mount.account.collectAsState()
  account?.let { child ->
    AccountScreen(child, modifier)
    return
  }

  val state by mount.store.state.collectAsState()
  Column(modifier.fillMaxSize()) {
    Text(
      "Profile",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
    Text(
      state.displayName,
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(horizontal = 24.dp),
    )
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    ListItem(
      headlineContent = { Text("Account") },
      supportingContent = { Text("Name and sign-out") },
      modifier = Modifier.clickable { mount.store.send(ProfileAction.AccountTapped) },
    )
    HorizontalDivider()
  }
}
