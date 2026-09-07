// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object ProfileRouteSerializer :
  CanonicalSumSerializer<ProfileRoute>(
    "ProfileRoute",
    listOf(case(ProfileRoute.Account::class, ProfileRoute.Account.serializer())))

object ProfileActionSerializer :
  CanonicalSumSerializer<ProfileAction>(
    "ProfileAction",
    listOf(
      case(ProfileAction.AccountTapped::class, ProfileAction.AccountTapped.serializer()),
      case(ProfileAction.PlanTapped::class, ProfileAction.PlanTapped.serializer()),
      case(ProfileAction.Account::class, ProfileAction.Account.serializer()),
      case(ProfileAction.EntitlementChanged::class, ProfileAction.EntitlementChanged.serializer()),
      case(ProfileAction.OpenLink::class, ProfileAction.OpenLink.serializer()),
    ))

object ProfileDelegateEventSerializer :
  CanonicalSumSerializer<ProfileDelegateEvent>(
    "ProfileDelegateEvent",
    listOf(
      case(
        ProfileDelegateEvent.SignOutRequested::class,
        ProfileDelegateEvent.SignOutRequested.serializer()),
      case(ProfileDelegateEvent.UpgradeRequested::class, ProfileDelegateEvent.UpgradeRequested.serializer()),
    ))

object ProfileEffectPayloadSerializer :
  CanonicalSumSerializer<ProfileEffectPayload>(
    "ProfileEffectPayload",
    listOf(
      case(
        ProfileEffectPayload.NotifyHost::class,
        ProfileEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
