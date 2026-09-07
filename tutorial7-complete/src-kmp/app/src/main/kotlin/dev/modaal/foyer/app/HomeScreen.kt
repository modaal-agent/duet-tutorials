// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.compose.foundation.BorderStroke
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
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.theming.SemanticColor
import dev.modaal.foyer.theming.SemanticFont

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
  BackHandler(enabled = BackPolicy.homePresentedEnabled(state.presented)) {
    store.send(HomeAction.Dismissed)
  }

  when (state.presented) {
    HomePresentation.Promo -> PromoScreen(store, modifier)
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

/**
 * The one paid surface: a lock and "Premium" while Free, the summary's teaser
 * once Premium. Every colour and both type styles are tokens; the locked
 * state is a different surface and label token, not a different colour.
 */
@Composable
private fun InsightsCard(entitlement: Entitlement, onClick: () -> Unit) {
  val locked = entitlement is Entitlement.Free
  val surface = if (locked) SemanticColor.lockedSurface else SemanticColor.cardSurface
  val teaser = if (locked) SemanticColor.labelLocked else SemanticColor.labelSecondary
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    colors = CardDefaults.cardColors(containerColor = surface.color()),
    border = BorderStroke(1.dp, SemanticColor.cardBorder.color()),
  ) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Insights", style = SemanticFont.cardTitle.textStyle(), color = SemanticColor.labelPrimary.color())
        Text(
          if (locked) "Premium" else "Your week at a glance",
          style = SemanticFont.cardBody.textStyle(),
          color = teaser.color(),
        )
      }
      if (locked) {
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = SemanticColor.labelLocked.color())
      }
    }
  }
}

/** The promo over a locked card: one button that asks the host for the upgrade flow. */
@Composable
private fun PromoScreen(store: HomeStore, modifier: Modifier) {
  Column(
    modifier.fillMaxSize().padding(horizontal = 32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      "Unlock Insights",
      style = MaterialTheme.typography.headlineMedium,
      color = SemanticColor.labelPrimary.color(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
      "Your week at a glance, every week.",
      style = SemanticFont.cardBody.textStyle(),
      color = SemanticColor.labelSecondary.color(),
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = { store.send(HomeAction.UpgradeTapped) }, modifier = Modifier.fillMaxWidth()) {
      Text("See plans")
    }
    TextButton(onClick = { store.send(HomeAction.Dismissed) }) { Text("Not now") }
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
