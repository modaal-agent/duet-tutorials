// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object HomePresentationSerializer :
  CanonicalSumSerializer<HomePresentation>(
    "HomePresentation",
    listOf(
      case(HomePresentation.Promo::class, HomePresentation.Promo.serializer()),
      case(HomePresentation.Insights::class, HomePresentation.Insights.serializer()),
    ))

object HomeActionSerializer :
  CanonicalSumSerializer<HomeAction>(
    "HomeAction",
    listOf(
      case(HomeAction.Appeared::class, HomeAction.Appeared.serializer()),
      case(HomeAction.ItemsLoaded::class, HomeAction.ItemsLoaded.serializer()),
      case(HomeAction.EntitlementChanged::class, HomeAction.EntitlementChanged.serializer()),
      case(HomeAction.InsightsTapped::class, HomeAction.InsightsTapped.serializer()),
      case(HomeAction.PurchaseTapped::class, HomeAction.PurchaseTapped.serializer()),
      case(HomeAction.PurchaseFinished::class, HomeAction.PurchaseFinished.serializer()),
      case(HomeAction.Dismissed::class, HomeAction.Dismissed.serializer()),
    ))

object HomeEffectPayloadSerializer :
  CanonicalSumSerializer<HomeEffectPayload>(
    "HomeEffectPayload",
    listOf(
      case(HomeEffectPayload.LoadItems::class, HomeEffectPayload.LoadItems.serializer()),
      case(HomeEffectPayload.Purchase::class, HomeEffectPayload.Purchase.serializer()),
    ))
