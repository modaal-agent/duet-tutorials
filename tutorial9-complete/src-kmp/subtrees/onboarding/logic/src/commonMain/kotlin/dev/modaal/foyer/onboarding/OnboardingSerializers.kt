// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.onboarding

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object OnboardingActionSerializer :
  CanonicalSumSerializer<OnboardingAction>(
    "OnboardingAction",
    listOf(
      case(OnboardingAction.Back::class, OnboardingAction.Back.serializer()),
      case(OnboardingAction.Welcome::class, OnboardingAction.Welcome.serializer()),
      case(OnboardingAction.Name::class, OnboardingAction.Name.serializer()),
      case(OnboardingAction.Preferences::class, OnboardingAction.Preferences.serializer()),
    ))

object OnboardingDelegateEventSerializer :
  CanonicalSumSerializer<OnboardingDelegateEvent>(
    "OnboardingDelegateEvent",
    listOf(
      case(OnboardingDelegateEvent.Completed::class, OnboardingDelegateEvent.Completed.serializer()),
    ))

object OnboardingEffectPayloadSerializer :
  CanonicalSumSerializer<OnboardingEffectPayload>(
    "OnboardingEffectPayload",
    listOf(
      case(
        OnboardingEffectPayload.NotifyHost::class,
        OnboardingEffectPayload.NotifyHost.serializer(),
        inline = true),
      case(OnboardingEffectPayload.Track::class, OnboardingEffectPayload.Track.serializer()),
    ))
