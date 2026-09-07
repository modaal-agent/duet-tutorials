// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

object EditNameActionSerializer :
  CanonicalSumSerializer<EditNameAction>(
    "EditNameAction",
    listOf(
      case(EditNameAction.DraftChanged::class, EditNameAction.DraftChanged.serializer()),
      case(EditNameAction.SaveTapped::class, EditNameAction.SaveTapped.serializer()),
      case(EditNameAction.SaveFinished::class, EditNameAction.SaveFinished.serializer()),
      case(EditNameAction.CancelTapped::class, EditNameAction.CancelTapped.serializer()),
    ))

object EditNameDelegateEventSerializer :
  CanonicalSumSerializer<EditNameDelegateEvent>(
    "EditNameDelegateEvent",
    listOf(
      case(EditNameDelegateEvent.Saved::class, EditNameDelegateEvent.Saved.serializer()),
      case(EditNameDelegateEvent.Closed::class, EditNameDelegateEvent.Closed.serializer()),
    ))

object EditNameEffectPayloadSerializer :
  CanonicalSumSerializer<EditNameEffectPayload>(
    "EditNameEffectPayload",
    listOf(
      case(EditNameEffectPayload.SaveName::class, EditNameEffectPayload.SaveName.serializer()),
      case(
        EditNameEffectPayload.NotifyHost::class,
        EditNameEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
