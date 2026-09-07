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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomePresentation
import dev.modaal.foyer.home.HomeState
import dev.modaal.foyer.ports.Entitlement

/**
 * The home tab over its store: `Appeared` when the screen enters the
 * composition (the reducer loads once), the Insights card locked or
 * unlocked from the projected entitlement, then the list. The promo and the
 * summary replace the tab's content while `presented` names them.
 */
@Composable
fun HomeScreen(store: HomeStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  LaunchedEffect(Unit) { store.send(HomeAction.Appeared) }

  when (state.presented) {
    HomePresentation.Promo -> PromoScreen(state, store, modifier)
    HomePresentation.Insights -> InsightsScreen(store, modifier)
    null ->
      Column(modifier.fillMaxSize()) {
        Text(
          "Home",
          style = MaterialTheme.typography.headlineMedium,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        InsightsCard(state.entitlement, onClick = { store.send(HomeAction.InsightsTapped) })
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
}

/** The one paid surface: a lock and "Premium" while Free, the summary's teaser once Premium. */
@Composable
private fun InsightsCard(entitlement: Entitlement, onClick: () -> Unit) {
  val locked = entitlement is Entitlement.Free
  Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Insights", style = MaterialTheme.typography.titleMedium)
        Text(
          if (locked) "Premium" else "Your week at a glance",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (locked) {
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.Lock, contentDescription = "Locked")
      }
    }
  }
}

/** The promo over a locked card: one button that buys the monthly plan. */
@Composable
private fun PromoScreen(state: HomeState, store: HomeStore, modifier: Modifier) {
  Column(
    modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Unlock Insights", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
      "Your week at a glance, every week.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(
      onClick = { store.send(HomeAction.PurchaseTapped) },
      enabled = !state.isPurchasing,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(if (state.isPurchasing) "Purchasing…" else "Monthly · $4.99")
    }
    TextButton(onClick = { store.send(HomeAction.Dismissed) }, enabled = !state.isPurchasing) {
      Text("Not now")
    }
    state.failure?.let { failure ->
      Spacer(Modifier.height(16.dp))
      Text(failure, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
  }
}

/** The summary behind an unlocked card: a static screen, the same on both platforms. */
@Composable
private fun InsightsScreen(store: HomeStore, modifier: Modifier) {
  Column(modifier.fillMaxSize()) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { store.send(HomeAction.Dismissed) }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Text("Insights", style = MaterialTheme.typography.titleLarge)
    }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
      InsightRow("Items read this week", "7")
      InsightRow("Longest streak", "4 days")
      InsightRow("Most read", "Reading list")
    }
  }
}

@Composable
private fun InsightRow(label: String, value: String) {
  Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Text(value, style = MaterialTheme.typography.titleMedium)
  }
  HorizontalDivider()
}
