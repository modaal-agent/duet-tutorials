// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome
import dev.modaal.foyer.ports.awaitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

fun upgradeEffectHandler(
  environment: UpgradeEnvironment,
): (UpgradeEffectPayload) -> Flow<UpgradeAction> = { payload ->
  flow {
    when (payload) {
      UpgradeEffectPayload.LoadPlans -> {
        val offers = awaitCallback<List<PlanOffer>> { onPlans -> environment.loadPlans(onPlans) }
        emit(UpgradeAction.PlansLoaded(offers))
      }
      is UpgradeEffectPayload.Purchase -> {
        val outcome =
          awaitCallback<PurchaseOutcome> { onOutcome -> environment.purchase(payload.plan, onOutcome) }
        emit(UpgradeAction.PurchaseFinished(outcome))
      }
      is UpgradeEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

/** The store a shell hosts; `step` is the restored route sliver, or the first step. */
fun makeUpgradeStore(
  step: UpgradeStep,
  environment: UpgradeEnvironment,
  scope: CoroutineScope,
): Store<UpgradeState, UpgradeAction, UpgradeEffectPayload> =
  Store(
    initialState = UpgradeState(step = step),
    reducer = ::upgradeReducer,
    handler = upgradeEffectHandler(environment),
    scope = scope,
  )

fun upgradeStateFlow(
  store: Store<UpgradeState, UpgradeAction, UpgradeEffectPayload>,
): StateFlow<UpgradeState> = store.state
