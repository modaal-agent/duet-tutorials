// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.app.services.MockAccount
import dev.modaal.foyer.app.services.MockAuth
import dev.modaal.foyer.app.services.MockItems
import dev.modaal.foyer.app.services.MockPurchases
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.ItemsPort
import dev.modaal.foyer.ports.PurchasesPort
import dev.modaal.foyer.root.AuthSnapshot
import dev.modaal.foyer.root.RootAction
import dev.modaal.foyer.root.RootEffectPayload
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RootState
import dev.modaal.foyer.root.makeRootStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The root level's composition triple, and the one place that knows the
// whole tree. Android-free on purpose: the JVM host test drives it headless.

typealias RootStore = Store<RootState, RootAction, RootEffectPayload>

/**
 * What the root consumes from the platform: nothing yet. The Activity's
 * conformer is empty; when the tree needs an Activity-supplied object, it is
 * named here and every conformer fails to compile until it supplies it.
 */
interface RootDependency

/**
 * The root Component owns what is scoped to the app: the four services
 * behind the ports, mock services until Tutorial 4. It satisfies each
 * child's Dependency with those members, one conformance per child level.
 */
class RootComponent(dependency: RootDependency) :
  RootDependency by dependency, SignInDependency, MainDependency {
  override val auth: AuthPort = MockAuth()
  override val items: ItemsPort = MockItems()
  override val account: AccountPort = MockAccount()

  /** Not consumed yet: Tutorial 4's entitlement stream and Tutorial 5's upgrade flow read it. */
  val purchases: PurchasesPort = MockPurchases()
}

/** The child the root has mounted, as the render layer sees it. */
sealed interface RootChildMount {
  data class Splash(val store: SplashStore) : RootChildMount

  data class SignIn(val store: SignInStore) : RootChildMount

  data class Main(val mount: MainMount) : RootChildMount
}

/**
 * What the one root mount owns: the root store, the child the phase names,
 * and the teardown registry everything else hangs on.
 */
class RootMount(val store: RootStore, private val host: StoreHost) {
  private val mutableChild = MutableStateFlow<RootChildMount?>(null)
  val child: StateFlow<RootChildMount?> = mutableChild

  internal fun publish(child: RootChildMount?) {
    mutableChild.value = child
  }

  /** Logical destruction only (finish, not rotation). */
  fun teardown() = host.teardownAll()
}

class RootBuilder(private val dependency: RootDependency) {
  fun buildRoot(scope: CoroutineScope): RootMount {
    val component = RootComponent(dependency)
    val host = StoreHost(scope)
    val store = host.host(makeRootStore(scope))
    val mount = RootMount(store, host)

    // Exactly one child at a time, keyed on the phase. Each child's delegate
    // events route to the root store as actions.
    val child =
      host.adopt(
        ChildSlot<RootPhase, RootChildMount>(
          build = { phase ->
            when (phase) {
              RootPhase.Splash ->
                RootChildMount.Splash(
                  SplashBuilder()
                    .buildSplash(onDelegate = { store.send(RootAction.Splash(it)) }, scope = scope))
              RootPhase.SignIn ->
                RootChildMount.SignIn(
                  SignInBuilder(component)
                    .buildSignIn(onDelegate = { store.send(RootAction.SignIn(it)) }, scope = scope))
              RootPhase.Main ->
                RootChildMount.Main(
                  MainBuilder(component)
                    .buildMain(
                      displayName = store.state.value.auth.displayNameOrGuest,
                      onDelegate = { store.send(RootAction.Main(it)) },
                      scope = scope,
                    ))
            }
          },
          teardown = {
            when (it) {
              is RootChildMount.Splash -> it.store.teardown()
              is RootChildMount.SignIn -> it.store.teardown()
              is RootChildMount.Main -> it.mount.teardown()
            }
          },
        ))
    host.adopt(
      StateTransitions(scope, store.state) { _, state ->
        child.reconcile(state.phase)
        mount.publish(child.activeHandle)
      })

    // The auth seed: the mock keeps no session, so the root starts signed out.
    // Tutorial 4's session worker sends this action from the auth port's stream.
    store.send(RootAction.AuthChanged(AuthSnapshot.SignedOut))
    return mount
  }
}

/** The name the profile tree shows: the session's, or the guest name. */
val AuthSnapshot.displayNameOrGuest: String
  get() = (this as? AuthSnapshot.SignedIn)?.displayName ?: "Guest"
