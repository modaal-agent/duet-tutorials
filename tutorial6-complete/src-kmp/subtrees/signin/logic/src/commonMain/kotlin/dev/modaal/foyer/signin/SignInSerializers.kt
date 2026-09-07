// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object SignInActionSerializer :
  CanonicalSumSerializer<SignInAction>(
    "SignInAction",
    listOf(
      case(SignInAction.ContinueTapped::class, SignInAction.ContinueTapped.serializer()),
      case(SignInAction.SignInFinished::class, SignInAction.SignInFinished.serializer()),
    ))

object SignInDelegateEventSerializer :
  CanonicalSumSerializer<SignInDelegateEvent>(
    "SignInDelegateEvent",
    listOf(
      case(SignInDelegateEvent.Completed::class, SignInDelegateEvent.Completed.serializer()),
    ))

object SignInEffectPayloadSerializer :
  CanonicalSumSerializer<SignInEffectPayload>(
    "SignInEffectPayload",
    listOf(
      case(SignInEffectPayload.SignIn::class, SignInEffectPayload.SignIn.serializer()),
      case(
        SignInEffectPayload.NotifyHost::class,
        SignInEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
