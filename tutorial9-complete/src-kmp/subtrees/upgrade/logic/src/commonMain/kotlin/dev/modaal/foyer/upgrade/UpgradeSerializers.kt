// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.upgrade

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object UpgradeStepSerializer :
  CanonicalSumSerializer<UpgradeStep>(
    "UpgradeStep",
    listOf(
      case(UpgradeStep.Plans::class, UpgradeStep.Plans.serializer()),
      case(UpgradeStep.Confirm::class, UpgradeStep.Confirm.serializer()),
      case(UpgradeStep.Done::class, UpgradeStep.Done.serializer()),
    ))

object UpgradeActionSerializer :
  CanonicalSumSerializer<UpgradeAction>(
    "UpgradeAction",
    listOf(
      case(UpgradeAction.Appeared::class, UpgradeAction.Appeared.serializer()),
      case(UpgradeAction.PlansLoaded::class, UpgradeAction.PlansLoaded.serializer()),
      case(UpgradeAction.PlanSelected::class, UpgradeAction.PlanSelected.serializer()),
      case(UpgradeAction.ConfirmTapped::class, UpgradeAction.ConfirmTapped.serializer()),
      case(UpgradeAction.PurchaseFinished::class, UpgradeAction.PurchaseFinished.serializer()),
      case(UpgradeAction.Back::class, UpgradeAction.Back.serializer()),
      case(UpgradeAction.DoneTapped::class, UpgradeAction.DoneTapped.serializer()),
      case(UpgradeAction.DismissTapped::class, UpgradeAction.DismissTapped.serializer()),
    ))

object UpgradeDelegateEventSerializer :
  CanonicalSumSerializer<UpgradeDelegateEvent>(
    "UpgradeDelegateEvent",
    listOf(
      case(UpgradeDelegateEvent.Completed::class, UpgradeDelegateEvent.Completed.serializer()),
      case(UpgradeDelegateEvent.Dismissed::class, UpgradeDelegateEvent.Dismissed.serializer()),
    ))

object UpgradeEffectPayloadSerializer :
  CanonicalSumSerializer<UpgradeEffectPayload>(
    "UpgradeEffectPayload",
    listOf(
      case(UpgradeEffectPayload.LoadPlans::class, UpgradeEffectPayload.LoadPlans.serializer()),
      case(UpgradeEffectPayload.Purchase::class, UpgradeEffectPayload.Purchase.serializer()),
      case(UpgradeEffectPayload.NotifyHost::class, UpgradeEffectPayload.NotifyHost.serializer(), inline = true),
      case(UpgradeEffectPayload.Track::class, UpgradeEffectPayload.Track.serializer()),
    ))
