// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object MainTabSerializer :
  CanonicalSumSerializer<MainTab>(
    "MainTab",
    listOf(
      case(MainTab.Home::class, MainTab.Home.serializer()),
      case(MainTab.Profile::class, MainTab.Profile.serializer()),
    ))

object MainSheetSerializer :
  CanonicalSumSerializer<MainSheet>(
    "MainSheet",
    listOf(
      case(MainSheet.Upgrade::class, MainSheet.Upgrade.serializer()),
    ))

object MainActionSerializer :
  CanonicalSumSerializer<MainAction>(
    "MainAction",
    listOf(
      case(MainAction.TabSelected::class, MainAction.TabSelected.serializer()),
      case(MainAction.Home::class, MainAction.Home.serializer()),
      case(MainAction.Profile::class, MainAction.Profile.serializer()),
      case(MainAction.Upgrade::class, MainAction.Upgrade.serializer()),
      case(MainAction.OpenLink::class, MainAction.OpenLink.serializer()),
    ))

object MainDelegateEventSerializer :
  CanonicalSumSerializer<MainDelegateEvent>(
    "MainDelegateEvent",
    listOf(
      case(MainDelegateEvent.SignOutRequested::class, MainDelegateEvent.SignOutRequested.serializer()),
    ))

object MainEffectPayloadSerializer :
  CanonicalSumSerializer<MainEffectPayload>(
    "MainEffectPayload",
    listOf(
      case(MainEffectPayload.NotifyHost::class, MainEffectPayload.NotifyHost.serializer(), inline = true),
      case(MainEffectPayload.ForwardLink::class, MainEffectPayload.ForwardLink.serializer()),
    ))
