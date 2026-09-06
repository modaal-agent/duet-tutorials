// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object HomeActionSerializer :
  CanonicalSumSerializer<HomeAction>(
    "HomeAction",
    listOf(
      case(HomeAction.Appeared::class, HomeAction.Appeared.serializer()),
      case(HomeAction.ItemsLoaded::class, HomeAction.ItemsLoaded.serializer()),
    ))

object HomeEffectPayloadSerializer :
  CanonicalSumSerializer<HomeEffectPayload>(
    "HomeEffectPayload",
    listOf(case(HomeEffectPayload.LoadItems::class, HomeEffectPayload.LoadItems.serializer())))
