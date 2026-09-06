// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.main.MainAction
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.main.MainEffectPayload
import dev.modaal.foyer.main.MainEnvironment
import dev.modaal.foyer.main.MainState
import dev.modaal.foyer.main.makeMainStore
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.ItemsPort
import kotlinx.coroutines.CoroutineScope

typealias MainStore = Store<MainState, MainAction, MainEffectPayload>

/** What the main level consumes: the union of what its two tabs' subtrees need. */
interface MainDependency {
  val items: ItemsPort
  val auth: AuthPort
  val account: AccountPort
}

/** Forwards the Dependency and satisfies both tabs' Dependencies with the same members. */
class MainComponent(dependency: MainDependency) :
  MainDependency by dependency, HomeDependency, ProfileDependency {
  fun environment(onDelegate: (MainDelegateEvent) -> Unit): MainEnvironment =
    object : MainEnvironment {
      override fun notifyHost(event: MainDelegateEvent) = onDelegate(event)
    }
}

/**
 * What one main mount owns: its store and both tabs, mounted for the level's
 * lifetime. The tab bar chooses which one is shown; neither is rebuilt on a
 * switch.
 */
class MainMount(
  val store: MainStore,
  val home: HomeStore,
  val profile: ProfileMount,
  private val host: StoreHost,
) {
  fun teardown() = host.teardownAll()
}

class MainBuilder(private val dependency: MainDependency) {
  fun buildMain(
    displayName: String,
    onDelegate: (MainDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): MainMount {
    val component = MainComponent(dependency)
    val host = StoreHost(scope)
    val store = host.host(makeMainStore(environment = component.environment(onDelegate), scope = scope))
    val home = host.host(HomeBuilder(component).buildHome(scope))
    val profile =
      ProfileBuilder(component)
        .buildProfile(
          displayName = displayName,
          onDelegate = { store.send(MainAction.Profile(it)) },
          scope = scope,
        )
    host.adoptTeardown(profile::teardown)
    return MainMount(store, home, profile, host)
  }
}
