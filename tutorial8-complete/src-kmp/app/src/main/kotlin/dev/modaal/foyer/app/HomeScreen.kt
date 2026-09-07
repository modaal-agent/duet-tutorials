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
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomePresentation
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
  BackHandler(enabled = BackPolicy.homePresentedEnabled(state.presented)) {
    store.send(HomeAction.Dismissed)
  }

  when (state.presented) {
    HomePresentation.Promo -> PromoScreen(store, modifier)
    HomePresentation.Insights -> InsightsScreen(store, modifier)
    null ->
      Column(modifier.fillMaxSize()) {
        Text(
          stringResource(R.string.home_title),
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
        Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleMedium)
        Text(
          stringResource(if (locked) R.string.insights_locked else R.string.insights_teaser),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (locked) {
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.insights_locked_description))
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
    Text(stringResource(R.string.promo_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
      stringResource(R.string.promo_body),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = { store.send(HomeAction.UpgradeTapped) }, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.promo_see_plans))
    }
    TextButton(onClick = { store.send(HomeAction.Dismissed) }) { Text(stringResource(R.string.not_now)) }
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
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
      }
      Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.titleLarge)
    }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
      InsightRow(stringResource(R.string.insight_items_read), "7")
      InsightRow(stringResource(R.string.insight_longest_streak), pluralStringResource(R.plurals.streak_days, 4, 4))
      InsightRow(stringResource(R.string.insight_most_read), stringResource(R.string.insight_reading_list))
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
