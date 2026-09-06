// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.account.AccountActionSerializer
import dev.modaal.foyer.account.AccountRoute
import dev.modaal.foyer.account.AccountDelegateEvent
import dev.modaal.foyer.account.AccountDelegateEventSerializer
import dev.modaal.foyer.account.AccountEffectPayload
import dev.modaal.foyer.account.AccountEffectPayloadSerializer
import dev.modaal.foyer.account.AccountState
import dev.modaal.foyer.account.accountReducer
import dev.modaal.foyer.editname.EditNameAction
import dev.modaal.foyer.editname.EditNameActionSerializer
import dev.modaal.foyer.editname.EditNameDelegateEvent
import dev.modaal.foyer.editname.EditNameDelegateEventSerializer
import dev.modaal.foyer.editname.EditNameEffectPayload
import dev.modaal.foyer.editname.EditNameEffectPayloadSerializer
import dev.modaal.foyer.editname.EditNameState
import dev.modaal.foyer.editname.editNameReducer
import kotlin.test.Test

/**
 * The chain scenario for `chain-profile-editname`: a saved name climbs two
 * levels, from the editor to the account screen (which takes it and climbs
 * it on as `NameChanged`) to the profile header. Each hop is the
 * delegate-to-action forwarding the shells perform in production; the
 * recording marks the emitting step, and verify re-derives the mapping from
 * the replayed payload. Tutorial 3's closing exercise is this file.
 */
class ProfileEditNameChainTest {

  private val editName =
    ChainNode(
      "editname",
      EditNameState(draft = "Ann B", isSaving = true),
      EditNameState.serializer(),
      EditNameActionSerializer,
      EditNameEffectPayloadSerializer,
      ::editNameReducer)

  private val account =
    ChainNode(
      "account",
      AccountState(displayName = "Ann", child = AccountRoute.EditName),
      AccountState.serializer(),
      AccountActionSerializer,
      AccountEffectPayloadSerializer,
      ::accountReducer)

  private val profile =
    ChainNode(
      "profile",
      ProfileState(displayName = "Ann", child = ProfileRoute.Account),
      ProfileState.serializer(),
      ProfileActionSerializer,
      ProfileEffectPayloadSerializer,
      ::profileReducer)

  @Test
  fun aSavedNameClimbsToTheHeader() {
    val chain =
      chainScenario(
        chain = "profile-editname",
        fixture = "chain-profile-editname",
        description =
          "The editor's Saved crosses into the account screen as EditName(Saved), " +
            "which takes the name and climbs NameChanged into the profile tab, " +
            "whose header shows it.",
        source =
          "src-kmp/subtrees/profile/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/profile/ProfileEditNameChainTest.kt",
      ) {
        whenAction(editName, "the account port confirms the save", EditNameAction.SaveFinished)
        thenEffects(editName, "exactly the Saved delegate") {
          it ==
            effectsOf<EditNameEffectPayload>(
              Effect.Run(EditNameEffectPayload.NotifyHost(EditNameDelegateEvent.Saved("Ann B"))))
        }
        hop(
          "the editor's delegate is the account screen's action",
          from = editName,
          to = account,
          delegateSerializer = EditNameDelegateEventSerializer,
        ) { event ->
          AccountAction.EditName(event)
        }
        then(account, "the account screen took the name and dismissed the editor") {
          it.displayName == "Ann B" && it.child == null
        }
        thenEffects(account, "exactly the NameChanged delegate") {
          it ==
            effectsOf<AccountEffectPayload>(
              Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.NameChanged("Ann B"))))
        }
        hop(
          "the account screen's delegate is the profile tab's action",
          from = account,
          to = profile,
          delegateSerializer = AccountDelegateEventSerializer,
        ) { event ->
          ProfileAction.Account(event)
        }
        then(profile, "the header shows the new name") { it.displayName == "Ann B" }
        thenEffects(profile, "nothing climbs further") { it.isEmpty() }
      }

    ChainScenarioRunner.verifyOrRecord(chain)
  }
}
