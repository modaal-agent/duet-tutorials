// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.account.AccountAction
import dev.modaal.foyer.account.AccountRoute
import dev.modaal.foyer.account.AccountDelegateEvent
import dev.modaal.foyer.account.AccountEffectPayload
import dev.modaal.foyer.account.AccountEnvironment
import dev.modaal.foyer.account.AccountState
import dev.modaal.foyer.account.makeAccountStore
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias AccountStore = Store<AccountState, AccountAction, AccountEffectPayload>

/**
 * What the account screen consumes from its parent: the auth port for the
 * sign-out, and the account port its child, the name editor, saves through.
 * A level names what its subtree needs, so the parent supplies it once.
 */
interface AccountDependency {
  val auth: AuthPort
  val account: AccountPort
}

/** Forwards the Dependency and satisfies the editor's with the same members. */
class AccountComponent(dependency: AccountDependency) :
  AccountDependency by dependency, EditNameDependency {
  fun environment(onDelegate: (AccountDelegateEvent) -> Unit): AccountEnvironment =
    object : AccountEnvironment {
      override fun signOut(onDone: () -> Unit) = auth.signOut(onDone)

      override fun notifyHost(event: AccountDelegateEvent) = onDelegate(event)
    }
}

/**
 * What one account mount owns: its store, and the editor it mounts from
 * `state.child`. The reconciler builds the editor when the value appears and
 * tears it down when it clears; `editName` is what the screen renders.
 */
class AccountMount(val store: AccountStore, private val host: StoreHost) {
  private val mutableEditName = MutableStateFlow<EditNameStore?>(null)
  val editName: StateFlow<EditNameStore?> = mutableEditName

  internal fun publish(child: EditNameStore?) {
    mutableEditName.value = child
  }

  fun teardown() = host.teardownAll()
}

class AccountBuilder(private val dependency: AccountDependency) {
  fun buildAccount(
    displayName: String,
    onDelegate: (AccountDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): AccountMount {
    val component = AccountComponent(dependency)
    val host = StoreHost(scope)
    val store =
      host.host(
        makeAccountStore(
          displayName = displayName,
          environment = component.environment(onDelegate),
          scope = scope,
        ))
    val mount = AccountMount(store, host)
    // The child's delegate events route to this level's store as actions;
    // the composition holds no listener of its own.
    val editor =
      host.adopt(
        ChildSlot<AccountRoute, EditNameStore>(
          build = {
            EditNameBuilder(component)
              .buildEditName(
                currentName = store.state.value.displayName,
                onDelegate = { store.send(AccountAction.EditName(it)) },
                scope = scope,
              )
          },
          teardown = { it.teardown() },
        ))
    host.adopt(
      StateTransitions(scope, store.state) { _, state ->
        editor.reconcile(state.child)
        mount.publish(editor.activeHandle)
      })
    return mount
  }
}
