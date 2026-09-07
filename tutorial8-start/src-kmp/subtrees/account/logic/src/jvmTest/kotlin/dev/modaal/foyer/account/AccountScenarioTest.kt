// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.test.*
import dev.modaal.foyer.editname.EditNameDelegateEvent
import kotlin.test.Test

/** The scenario the account recordings are compiled from. */
class AccountScenarioTest {
  @Test
  fun accountScenario() {
    val s =
      scenario<AccountState, AccountAction, AccountEffectPayload>(
        feature = "account",
        description =
          "The account screen mounts the name editor from state, takes the editor's " +
            "Saved as its own name and climbs it as NameChanged, signs out through " +
            "the auth port once, and climbs Closed when the user leaves.",
        source =
          "src-kmp/subtrees/account/logic/src/jvmTest/kotlin/" +
            "dev/modaal/foyer/account/AccountScenarioTest.kt",
      ) {
        given(AccountState(displayName = "Ann"))

        branch("edit name saves and climbs") {
          whenAction("the Edit name row", AccountAction.EditNameTapped)
          then("the editor is mounted") { it.child == AccountRoute.EditName }
          thenEffects("nothing: mounting is the shell's job") { it.isEmpty() }
          whenAction(
            "the editor saves", AccountAction.EditName(EditNameDelegateEvent.Saved("Ann B")))
          then("the name is taken, the editor dismissed") {
            it.displayName == "Ann B" && it.child == null
          }
          thenEffects("the parent hears the new name") {
            it == notified(AccountDelegateEvent.NameChanged("Ann B"))
          }
        }

        branch("edit name cancels") {
          whenAction("the Edit name row", AccountAction.EditNameTapped)
          whenAction("the editor closes", AccountAction.EditName(EditNameDelegateEvent.Closed))
          then("the editor is dismissed, the name untouched") {
            it.child == null && it.displayName == "Ann"
          }
          thenEffects("nothing") { it.isEmpty() }
        }

        branch("sign out requests once") {
          whenAction("the Sign out row", AccountAction.SignOutTapped)
          then("signing out") { it.isSigningOut }
          thenEffects("exactly the port call") {
            it == effectsOf<AccountEffectPayload>(Effect.Run(AccountEffectPayload.SignOut))
          }
          whenAction("a second tap while in flight", AccountAction.SignOutTapped)
          thenEffects("nothing: one sign-out at a time") { it.isEmpty() }
          whenAction("the port confirms", AccountAction.SignedOut)
          then("the latch is released") { !it.isSigningOut }
          thenEffects("the request climbs") {
            it == notified(AccountDelegateEvent.SignOutRequested)
          }
        }

        branch("close climbs") {
          whenAction("the back control", AccountAction.CloseTapped)
          thenEffects("the parent hears Closed") { it == notified(AccountDelegateEvent.Closed) }
        }
      }

    ScenarioRunner.verifyOrRecord(
      s,
      AccountState.serializer(),
      AccountActionSerializer,
      AccountEffectPayloadSerializer,
      ::accountReducer)
  }
}

private fun notified(event: AccountDelegateEvent): List<Effect<AccountEffectPayload>> =
  effectsOf(Effect.Run(AccountEffectPayload.NotifyHost(event)))
