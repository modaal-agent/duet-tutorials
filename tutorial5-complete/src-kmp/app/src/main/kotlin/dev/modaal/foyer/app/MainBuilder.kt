// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.home.HomeAction
import dev.modaal.foyer.main.MainAction
import dev.modaal.foyer.main.MainDelegateEvent
import dev.modaal.foyer.main.MainEffectPayload
import dev.modaal.foyer.main.MainEnvironment
import dev.modaal.foyer.main.MainSheet
import dev.modaal.foyer.main.MainState
import dev.modaal.foyer.main.MainTab
import dev.modaal.foyer.main.makeMainStore
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.ItemsPort
import dev.modaal.foyer.ports.PurchasesPort
import dev.modaal.foyer.profile.ProfileAction
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RouteSpine
import dev.modaal.foyer.upgrade.UpgradeStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias MainStore = Store<MainState, MainAction, MainEffectPayload>

/** What the main level consumes: the union of what its two tabs' subtrees and the flow need. */
interface MainDependency {
  val items: ItemsPort
  val purchases: PurchasesPort
  val auth: AuthPort
  val account: AccountPort
}

/** Forwards the Dependency and satisfies the tabs' and the flow's Dependencies with the same members. */
class MainComponent(dependency: MainDependency) :
  MainDependency by dependency, HomeDependency, ProfileDependency, UpgradeDependency {
  fun environment(onDelegate: (MainDelegateEvent) -> Unit, forwardLink: (DeepLink) -> Unit): MainEnvironment =
    object : MainEnvironment {
      override fun notifyHost(event: MainDelegateEvent) = onDelegate(event)

      override fun forwardLink(link: DeepLink) = forwardLink(link)
    }
}

/**
 * What one main mount owns: its store, both tabs, mounted for the level's
 * lifetime, and the flow the sheet slot mounts from `state.sheet`. The tab
 * bar chooses which tab is shown; neither is rebuilt on a switch.
 */
class MainMount(
  val store: MainStore,
  val home: HomeStore,
  val profile: ProfileMount,
  private val host: StoreHost,
) {
  private val mutableUpgrade = MutableStateFlow<UpgradeStore?>(null)
  val upgrade: StateFlow<UpgradeStore?> = mutableUpgrade

  internal fun publish(upgrade: UpgradeStore?) {
    mutableUpgrade.value = upgrade
  }

  /** This level's slivers and its children's, for the route spine. */
  fun routeSpine(): RouteSpine =
    RouteSpine(
      phase = RootPhase.Main,
      activeTab = store.state.value.activeTab,
      profilePath = profile.routePath(),
      homePresented = home.state.value.presented,
      upgradeStep = upgrade.value?.state?.value?.step,
    )

  fun teardown() = host.teardownAll()
}

class MainBuilder(private val dependency: MainDependency) {
  /** `restored` is the spine's main-level slivers, applied as each store's initial state, or null. */
  fun buildMain(
    displayName: String,
    entitlement: StateFlow<Entitlement>,
    restored: RouteSpine?,
    onDelegate: (MainDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): MainMount {
    val component = MainComponent(dependency)
    val host = StoreHost(scope)
    lateinit var mount: MainMount
    val store =
      host.host(
        makeMainStore(
          environment =
            component.environment(
              onDelegate = onDelegate,
              // The link travels on down as the profile tab's own action.
              forwardLink = { link -> mount.profile.store.send(ProfileAction.OpenLink(link)) },
            ),
          scope = scope,
          initialState =
            MainState(
              activeTab = restored?.activeTab ?: MainTab.Home,
              sheet = restored?.upgradeStep?.let { MainSheet.Upgrade },
            ),
        ))
    val home =
      host.host(
        HomeBuilder(component)
          .buildHome(
            presented = restored?.homePresented,
            onDelegate = { store.send(MainAction.Home(it)) },
            scope = scope,
          ))
    val profile =
      ProfileBuilder(component)
        .buildProfile(
          displayName = displayName,
          restoredPath = restored?.profilePath,
          onDelegate = { store.send(MainAction.Profile(it)) },
          scope = scope,
        )
    host.adoptTeardown(profile::teardown)
    mount = MainMount(store, home, profile, host)

    // The sheet slot: the flow is built when `sheet` names it and torn down
    // when the value clears. A restored step seeds the first build only.
    var restoredStep: UpgradeStep? = restored?.upgradeStep
    val sheet =
      host.adopt(
        ChildSlot<MainSheet, UpgradeStore>(
          build = {
            val step = restoredStep ?: UpgradeStep.Plans
            restoredStep = null
            UpgradeBuilder(component)
              .buildUpgrade(
                step = step,
                onDelegate = { store.send(MainAction.Upgrade(it)) },
                scope = scope,
              )
          },
          teardown = { it.teardown() },
        ))
    host.adopt(
      StateTransitions(scope, store.state) { _, state ->
        sheet.reconcile(state.sheet)
        mount.publish(sheet.activeHandle)
      })
    // State down: the root's slice reaches both tabs as an action of their
    // own. The first delivery is the current value, at mount.
    host.adopt(
      StateTransitions(scope, entitlement) { _, value ->
        home.send(HomeAction.EntitlementChanged(value))
        profile.store.send(ProfileAction.EntitlementChanged(value))
      })
    return mount
  }
}
