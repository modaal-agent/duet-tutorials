// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.modaal.foyer.main.MainAction
import dev.modaal.foyer.main.MainTab

/**
 * The main level over its mount: a tab bar bound to `activeTab`, and the
 * tab's screen. Both tabs' stores live for the level's lifetime, so a
 * switch shows a screen whose state is still there.
 */
@Composable
fun MainScreen(mount: MainMount, modifier: Modifier = Modifier) {
  val state by mount.store.state.collectAsState()

  Scaffold(
    modifier = modifier,
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = state.activeTab == MainTab.Home,
          onClick = { mount.store.send(MainAction.TabSelected(MainTab.Home)) },
          icon = { Icon(Icons.Filled.Home, contentDescription = null) },
          label = { Text("Home") },
        )
        NavigationBarItem(
          selected = state.activeTab == MainTab.Profile,
          onClick = { mount.store.send(MainAction.TabSelected(MainTab.Profile)) },
          icon = { Icon(Icons.Filled.Person, contentDescription = null) },
          label = { Text("Profile") },
        )
      }
    },
  ) { padding ->
    when (state.activeTab) {
      MainTab.Home -> HomeScreen(mount.home, Modifier.padding(padding))
      MainTab.Profile -> ProfileScreen(mount.profile, Modifier.padding(padding))
    }
  }
}
