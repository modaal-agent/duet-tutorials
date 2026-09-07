// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.preferences

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object PreferencesActionSerializer :
  CanonicalSumSerializer<PreferencesAction>(
    "PreferencesAction",
    listOf(
      case(PreferencesAction.PreferenceToggled::class, PreferencesAction.PreferenceToggled.serializer()),
      case(PreferencesAction.ContinueTapped::class, PreferencesAction.ContinueTapped.serializer()),
    ))

object PreferencesDelegateEventSerializer :
  CanonicalSumSerializer<PreferencesDelegateEvent>(
    "PreferencesDelegateEvent",
    listOf(
      case(PreferencesDelegateEvent.Continued::class, PreferencesDelegateEvent.Continued.serializer()),
    ))

object PreferencesEffectPayloadSerializer :
  CanonicalSumSerializer<PreferencesEffectPayload>(
    "PreferencesEffectPayload",
    listOf(
      case(
        PreferencesEffectPayload.PublishReadiness::class,
        PreferencesEffectPayload.PublishReadiness.serializer()),
      case(
        PreferencesEffectPayload.NotifyHost::class,
        PreferencesEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
