// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.profile.ProfileAction
import dev.modaal.foyer.profile.ProfileRoute
import dev.modaal.foyer.profile.ProfileDelegateEvent
import dev.modaal.foyer.profile.ProfileEffectPayload
import dev.modaal.foyer.profile.ProfileEnvironment
import dev.modaal.foyer.profile.ProfileState
import dev.modaal.foyer.profile.makeProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias ProfileStore = Store<ProfileState, ProfileAction, ProfileEffectPayload>

/** What the profile tab consumes: nothing of its own; both members are its subtree's. */
interface ProfileDependency {
  val auth: AuthPort
  val account: AccountPort
}

class ProfileComponent(dependency: ProfileDependency) :
  ProfileDependency by dependency, AccountDependency {
  fun environment(onDelegate: (ProfileDelegateEvent) -> Unit): ProfileEnvironment =
    object : ProfileEnvironment {
      override fun notifyHost(event: ProfileDelegateEvent) = onDelegate(event)
    }
}

/** What one profile mount owns: its store and the account screen it mounts from `state.child`. */
class ProfileMount(val store: ProfileStore, private val host: StoreHost) {
  private val mutableAccount = MutableStateFlow<AccountMount?>(null)
  val account: StateFlow<AccountMount?> = mutableAccount

  internal fun publish(child: AccountMount?) {
    mutableAccount.value = child
  }

  fun teardown() = host.teardownAll()
}

class ProfileBuilder(private val dependency: ProfileDependency) {
  fun buildProfile(
    displayName: String,
    onDelegate: (ProfileDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): ProfileMount {
    val component = ProfileComponent(dependency)
    val host = StoreHost(scope)
    val store =
      host.host(
        makeProfileStore(
          displayName = displayName,
          environment = component.environment(onDelegate),
          scope = scope,
        ))
    val mount = ProfileMount(store, host)
    val accountSlot =
      host.adopt(
        ChildSlot<ProfileRoute, AccountMount>(
          build = {
            AccountBuilder(component)
              .buildAccount(
                displayName = store.state.value.displayName,
                onDelegate = { store.send(ProfileAction.Account(it)) },
                scope = scope,
              )
          },
          teardown = { it.teardown() },
        ))
    host.adopt(
      StateTransitions(scope, store.state) { _, state ->
        accountSlot.reconcile(state.child)
        mount.publish(accountSlot.activeHandle)
      })
    return mount
  }
}
