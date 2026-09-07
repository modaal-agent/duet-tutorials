// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object ProgressActionSerializer :
  CanonicalSumSerializer<ProgressAction>(
    "ProgressAction",
    listOf(
      case(ProgressAction.Appeared::class, ProgressAction.Appeared.serializer()),
      case(ProgressAction.PageChanged::class, ProgressAction.PageChanged.serializer()),
      case(ProgressAction.ReadinessChanged::class, ProgressAction.ReadinessChanged.serializer()),
    ))

object ProgressEffectPayloadSerializer :
  CanonicalSumSerializer<ProgressEffectPayload>(
    "ProgressEffectPayload",
    listOf(
      case(ProgressEffectPayload.ObserveReadiness::class, ProgressEffectPayload.ObserveReadiness.serializer()),
    ))
