// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.welcome

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object WelcomeActionSerializer :
  CanonicalSumSerializer<WelcomeAction>(
    "WelcomeAction",
    listOf(
      case(WelcomeAction.Appeared::class, WelcomeAction.Appeared.serializer()),
      case(WelcomeAction.ContinueTapped::class, WelcomeAction.ContinueTapped.serializer()),
    ))

object WelcomeDelegateEventSerializer :
  CanonicalSumSerializer<WelcomeDelegateEvent>(
    "WelcomeDelegateEvent",
    listOf(
      case(WelcomeDelegateEvent.Continued::class, WelcomeDelegateEvent.Continued.serializer()),
    ))

object WelcomeEffectPayloadSerializer :
  CanonicalSumSerializer<WelcomeEffectPayload>(
    "WelcomeEffectPayload",
    listOf(
      case(WelcomeEffectPayload.PublishReadiness::class, WelcomeEffectPayload.PublishReadiness.serializer()),
      case(WelcomeEffectPayload.NotifyHost::class, WelcomeEffectPayload.NotifyHost.serializer(), inline = true),
    ))
