// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object AccountRouteSerializer :
  CanonicalSumSerializer<AccountRoute>(
    "AccountRoute",
    listOf(case(AccountRoute.EditName::class, AccountRoute.EditName.serializer())))

object AccountActionSerializer :
  CanonicalSumSerializer<AccountAction>(
    "AccountAction",
    listOf(
      case(AccountAction.EditNameTapped::class, AccountAction.EditNameTapped.serializer()),
      case(AccountAction.SignOutTapped::class, AccountAction.SignOutTapped.serializer()),
      case(AccountAction.SignedOut::class, AccountAction.SignedOut.serializer()),
      case(AccountAction.CloseTapped::class, AccountAction.CloseTapped.serializer()),
      case(AccountAction.EditName::class, AccountAction.EditName.serializer()),
    ))

object AccountDelegateEventSerializer :
  CanonicalSumSerializer<AccountDelegateEvent>(
    "AccountDelegateEvent",
    listOf(
      case(AccountDelegateEvent.Closed::class, AccountDelegateEvent.Closed.serializer()),
      case(AccountDelegateEvent.NameChanged::class, AccountDelegateEvent.NameChanged.serializer()),
      case(
        AccountDelegateEvent.SignOutRequested::class,
        AccountDelegateEvent.SignOutRequested.serializer()),
    ))

object AccountEffectPayloadSerializer :
  CanonicalSumSerializer<AccountEffectPayload>(
    "AccountEffectPayload",
    listOf(
      case(AccountEffectPayload.SignOut::class, AccountEffectPayload.SignOut.serializer()),
      case(
        AccountEffectPayload.NotifyHost::class,
        AccountEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
