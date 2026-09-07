// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.name

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object NameActionSerializer :
  CanonicalSumSerializer<NameAction>(
    "NameAction",
    listOf(
      case(NameAction.DraftChanged::class, NameAction.DraftChanged.serializer()),
      case(NameAction.ContinueTapped::class, NameAction.ContinueTapped.serializer()),
    ))

object NameDelegateEventSerializer :
  CanonicalSumSerializer<NameDelegateEvent>(
    "NameDelegateEvent",
    listOf(
      case(NameDelegateEvent.Continued::class, NameDelegateEvent.Continued.serializer()),
    ))

object NameEffectPayloadSerializer :
  CanonicalSumSerializer<NameEffectPayload>(
    "NameEffectPayload",
    listOf(
      case(NameEffectPayload.PublishReadiness::class, NameEffectPayload.PublishReadiness.serializer()),
      case(NameEffectPayload.NotifyHost::class, NameEffectPayload.NotifyHost.serializer(), inline = true),
    ))
