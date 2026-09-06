// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.home.HomeEffectPayload
import dev.modaal.foyer.home.HomeEnvironment
import dev.modaal.foyer.home.HomeState
import dev.modaal.foyer.home.makeHomeStore
import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.ItemsPort
import kotlinx.coroutines.CoroutineScope

typealias HomeStore = Store<HomeState, HomeAction, HomeEffectPayload>

/** What the home tab consumes from its parent: the items port. */
interface HomeDependency {
  val items: ItemsPort
}

class HomeComponent(dependency: HomeDependency) : HomeDependency by dependency {
  fun environment(): HomeEnvironment =
    object : HomeEnvironment {
      override fun loadItems(onItems: (List<Item>) -> Unit) = items.items(onItems)
    }
}

class HomeBuilder(private val dependency: HomeDependency) {
  fun buildHome(scope: CoroutineScope): HomeStore {
    val component = HomeComponent(dependency)
    return makeHomeStore(environment = component.environment(), scope = scope)
  }
}
