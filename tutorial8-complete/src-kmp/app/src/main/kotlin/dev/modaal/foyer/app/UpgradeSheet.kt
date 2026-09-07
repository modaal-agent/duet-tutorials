// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseFailure
import dev.modaal.foyer.upgrade.UpgradeAction
import dev.modaal.foyer.upgrade.UpgradeStep

/**
 * The upgrade flow over its store, presented as a modal bottom sheet: the
 * platform's idiom for a surface over the tabs, mounted from the main
 * level's `sheet` value. The sheet handles the system back press and the
 * swipe itself and reports both through `onDismissRequest`, which leaves as
 * the flow's `DismissTapped`; the flow decides what a dismissal means on
 * each step. The Back button inside the flow walks the steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeSheet(store: UpgradeStore) {
  val state by store.state.collectAsState()
  LaunchedEffect(Unit) { store.send(UpgradeAction.Appeared) }

  ModalBottomSheet(
    onDismissRequest = { store.send(UpgradeAction.DismissTapped) },
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
      when (val step = state.step) {
        UpgradeStep.Plans -> PlansStep(state.offers, store)
        is UpgradeStep.Confirm -> ConfirmStep(step.plan, state.offers, state.isPurchasing, state.failure, store)
        UpgradeStep.Done -> DoneStep(store)
      }
    }
  }
}

@Composable
private fun PlansStep(offers: List<PlanOffer>, store: UpgradeStore) {
  Text(stringResource(R.string.upgrade_choose_plan), style = MaterialTheme.typography.headlineSmall)
  Spacer(Modifier.height(16.dp))
  offers.forEach { offer ->
    Card(
      onClick = { store.send(UpgradeAction.PlanSelected(offer.plan)) },
      modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
      Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(offer.plan.nameRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(offer.price, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
  TextButton(onClick = { store.send(UpgradeAction.Back) }) { Text(stringResource(R.string.not_now)) }
}

@Composable
private fun ConfirmStep(
  plan: Plan,
  offers: List<PlanOffer>,
  isPurchasing: Boolean,
  failure: PurchaseFailure?,
  store: UpgradeStore,
) {
  val price = offers.firstOrNull { it.plan == plan }?.price ?: ""
  Text(stringResource(R.string.upgrade_confirm_plan, stringResource(plan.nameRes)), style = MaterialTheme.typography.headlineSmall)
  Spacer(Modifier.height(8.dp))
  Text(stringResource(plan.billedRes, price), color = MaterialTheme.colorScheme.onSurfaceVariant)
  failure?.let {
    Spacer(Modifier.height(8.dp))
    Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error)
  }
  Spacer(Modifier.height(24.dp))
  Button(
    onClick = { store.send(UpgradeAction.ConfirmTapped) },
    enabled = !isPurchasing,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(stringResource(if (isPurchasing) R.string.upgrade_purchasing else R.string.upgrade_confirm))
  }
  TextButton(onClick = { store.send(UpgradeAction.Back) }, enabled = !isPurchasing) { Text(stringResource(R.string.back)) }
}

@Composable
private fun DoneStep(store: UpgradeStore) {
  Text(stringResource(R.string.upgrade_premium_title), style = MaterialTheme.typography.headlineSmall)
  Spacer(Modifier.height(8.dp))
  Text(stringResource(R.string.upgrade_premium_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(24.dp))
  Button(onClick = { store.send(UpgradeAction.DoneTapped) }, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(R.string.done))
  }
}

/** The plan's name, as a resource: the screen names it and never compares it. */
val Plan.nameRes: Int
  @StringRes
  get() =
    when (this) {
      Plan.Monthly -> R.string.plan_monthly
      Plan.Yearly -> R.string.plan_yearly
    }

/** The confirm step's billing line, with the price as its one argument. */
private val Plan.billedRes: Int
  @StringRes
  get() =
    when (this) {
      Plan.Monthly -> R.string.upgrade_billed_monthly
      Plan.Yearly -> R.string.upgrade_billed_yearly
    }

/** The refusal's string, from its case: the reducer says why, the screen says it. */
private val PurchaseFailure.messageRes: Int
  @StringRes
  get() =
    when (this) {
      PurchaseFailure.Declined -> R.string.upgrade_failure_declined
    }
