// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.services.telemetry.AnalyticsProviding
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.foyer.ports.Plan
import dev.modaal.foyer.ports.PlanOffer
import dev.modaal.foyer.ports.PurchaseOutcome
import dev.modaal.foyer.ports.PurchasesPort
import dev.modaal.foyer.upgrade.UpgradeAction
import dev.modaal.foyer.upgrade.UpgradeDelegateEvent
import dev.modaal.foyer.upgrade.UpgradeEffectPayload
import dev.modaal.foyer.upgrade.UpgradeEnvironment
import dev.modaal.foyer.upgrade.UpgradeState
import dev.modaal.foyer.upgrade.UpgradeStep
import dev.modaal.foyer.upgrade.makeUpgradeStore
import kotlinx.coroutines.CoroutineScope

typealias UpgradeStore = Store<UpgradeState, UpgradeAction, UpgradeEffectPayload>

/** What the upgrade flow consumes from its parent: the purchases port, and the analytics sink. */
interface UpgradeDependency : AnalyticsProviding {
  val purchases: PurchasesPort
}

class UpgradeComponent(dependency: UpgradeDependency) : UpgradeDependency by dependency {
  fun environment(onDelegate: (UpgradeDelegateEvent) -> Unit): UpgradeEnvironment =
    object : UpgradeEnvironment {
      override fun loadPlans(onPlans: (List<PlanOffer>) -> Unit) = purchases.plans(onPlans)

      override fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit) =
        purchases.purchase(plan, onOutcome)

      override fun notifyHost(event: UpgradeDelegateEvent) = onDelegate(event)

      override fun track(event: TrackedEvent) = analytics.track(event)
    }
}

class UpgradeBuilder(private val dependency: UpgradeDependency) {
  /** `step` is the restored route sliver, or the first step. */
  fun buildUpgrade(
    step: UpgradeStep,
    onDelegate: (UpgradeDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): UpgradeStore {
    val component = UpgradeComponent(dependency)
    return makeUpgradeStore(step = step, environment = component.environment(onDelegate), scope = scope)
  }
}
