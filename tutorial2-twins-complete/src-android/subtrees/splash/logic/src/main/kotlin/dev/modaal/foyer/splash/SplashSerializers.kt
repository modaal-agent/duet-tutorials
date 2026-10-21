// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.splash

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

// The canonical `{"case": …, "value": …}` coding of every sum type, one
// registry line per case. A case missing here fails at the first encode.
// `inline = true` marks a case whose single unlabeled payload encodes bare;
// `ArmSafetyNet` and `Completed` carry labeled payloads and encode as objects.

object SplashActionSerializer :
  CanonicalSumSerializer<SplashAction>(
    "SplashAction",
    listOf(
      case(SplashAction.Appeared::class, SplashAction.Appeared.serializer()),
      case(SplashAction.CeremonyFinished::class, SplashAction.CeremonyFinished.serializer()),
      case(SplashAction.SafetyNetElapsed::class, SplashAction.SafetyNetElapsed.serializer()),
    ))

object SplashCompletionPathSerializer :
  CanonicalSumSerializer<SplashCompletionPath>(
    "SplashCompletionPath",
    listOf(
      case(SplashCompletionPath.Ceremony::class, SplashCompletionPath.Ceremony.serializer()),
      case(SplashCompletionPath.SafetyNet::class, SplashCompletionPath.SafetyNet.serializer()),
    ))

object SplashDelegateEventSerializer :
  CanonicalSumSerializer<SplashDelegateEvent>(
    "SplashDelegateEvent",
    listOf(
      case(SplashDelegateEvent.Completed::class, SplashDelegateEvent.Completed.serializer()),
    ))

object SplashEffectPayloadSerializer :
  CanonicalSumSerializer<SplashEffectPayload>(
    "SplashEffectPayload",
    listOf(
      case(SplashEffectPayload.ArmSafetyNet::class, SplashEffectPayload.ArmSafetyNet.serializer()),
      case(
        SplashEffectPayload.NotifyHost::class,
        SplashEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
