// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object RootPhaseSerializer :
  CanonicalSumSerializer<RootPhase>(
    "RootPhase",
    listOf(
      case(RootPhase.Splash::class, RootPhase.Splash.serializer()),
      case(RootPhase.SignIn::class, RootPhase.SignIn.serializer()),
      case(RootPhase.Main::class, RootPhase.Main.serializer()),
    ))

object AuthSnapshotSerializer :
  CanonicalSumSerializer<AuthSnapshot>(
    "AuthSnapshot",
    listOf(
      case(AuthSnapshot.Unknown::class, AuthSnapshot.Unknown.serializer()),
      case(AuthSnapshot.SignedOut::class, AuthSnapshot.SignedOut.serializer()),
      case(AuthSnapshot.SignedIn::class, AuthSnapshot.SignedIn.serializer()),
    ))

object RootActionSerializer :
  CanonicalSumSerializer<RootAction>(
    "RootAction",
    listOf(
      case(RootAction.Splash::class, RootAction.Splash.serializer()),
      case(RootAction.SignIn::class, RootAction.SignIn.serializer()),
      case(RootAction.Main::class, RootAction.Main.serializer()),
      case(RootAction.AuthChanged::class, RootAction.AuthChanged.serializer()),
    ))

/** No cases yet; the registry is the extension point Tutorial 5 fills. */
object RootEffectPayloadSerializer :
  CanonicalSumSerializer<RootEffectPayload>("RootEffectPayload", emptyList())
