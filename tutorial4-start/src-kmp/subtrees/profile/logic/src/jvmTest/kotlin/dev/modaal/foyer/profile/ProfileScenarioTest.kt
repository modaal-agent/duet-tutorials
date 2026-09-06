// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.account.AccountDelegateEvent
import kotlin.test.Test

/** The scenario the profile recordings are compiled from. */
class ProfileScenarioTest {
  @Test
  fun profileScenario() {
    val s =
      scenario<ProfileState, ProfileAction, ProfileEffectPayload>(
        feature = "profile",
        description =
          "The profile tab mounts the account screen from state, clears it on the " +
            "child's Closed, refreshes its header on NameChanged, and relays " +
            "SignOutRequested upward.",
        source =
          "src-kmp/subtrees/profile/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/profile/ProfileScenarioTest.kt",
      ) {
        given(ProfileState(displayName = "Ann"))

        whenAction("the Account row", ProfileAction.AccountTapped)
        then("the account screen is mounted") { it.child == ProfileRoute.Account }
        thenEffects("nothing: mounting is the shell's job") { it.isEmpty() }

        branch("account closes") {
          whenAction("the child closes", ProfileAction.Account(AccountDelegateEvent.Closed))
          then("the child is dismissed") { it.child == null }
        }

        branch("name change updates header") {
          whenAction(
            "the child reports a new name",
            ProfileAction.Account(AccountDelegateEvent.NameChanged("Ann B")))
          then("the header shows it, the child stays") {
            it.displayName == "Ann B" && it.child == ProfileRoute.Account
          }
          thenEffects("nothing: the name stops here") { it.isEmpty() }
        }

        branch("sign out relays") {
          whenAction(
            "the child requests a sign-out",
            ProfileAction.Account(AccountDelegateEvent.SignOutRequested))
          then("state is untouched") { it.child == ProfileRoute.Account }
          thenEffects("the request climbs unchanged") {
            it ==
              effectsOf<ProfileEffectPayload>(
                Effect.Run(ProfileEffectPayload.NotifyHost(ProfileDelegateEvent.SignOutRequested)))
          }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      ProfileState.serializer(),
      ProfileActionSerializer,
      ProfileEffectPayloadSerializer,
      ::profileReducer)
  }
}
