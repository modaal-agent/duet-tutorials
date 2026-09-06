// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.home.HomeAction

/**
 * The home tab over its store: `Appeared` when the screen enters the
 * composition (the reducer loads once), then the list.
 */
@Composable
fun HomeScreen(store: HomeStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  LaunchedEffect(Unit) { store.send(HomeAction.Appeared) }

  Column(modifier.fillMaxSize()) {
    Text(
      "Home",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
    if (state.isLoading) {
      LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    }
    LazyColumn {
      items(state.items, key = { it.id }) { item ->
        ListItem(headlineContent = { Text(item.title) })
        HorizontalDivider()
      }
    }
  }
}
