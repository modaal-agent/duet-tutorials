// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.Relay
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.app.workers.EntitlementWorker
import dev.modaal.foyer.app.workers.SessionWorker
import dev.modaal.foyer.backend.KeyValueFile
import dev.modaal.foyer.backend.LocalBackend
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.Entitlement
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
 * What the root consumes from the platform: the file the backend persists
 * its document in. The Activity supplies a file under its private storage;
 * the host test supplies memory.
 */
interface RootDependency {
  val storage: KeyValueFile
}

/**
 * The root Component owns what is scoped to the app: the on-device backend,
 * whose four members are the ports. It satisfies each child's Dependency
 * with those members, one conformance per child level.
 */
class RootComponent(dependency: RootDependency, scope: CoroutineScope) :
  RootDependency by dependency, SignInDependency, MainDependency {
  private val backend = LocalBackend(storage, scope)

  override val auth: AuthPort = backend.auth
  override val items: ItemsPort = backend.items
  override val account: AccountPort = backend.account
  override val purchases: PurchasesPort = backend.purchases
}

/** The child the root has mounted, as the render layer sees it. */
sealed interface RootChildMount {
  data class Splash(val store: SplashStore) : RootChildMount

  data class SignIn(val store: SignInStore) : RootChildMount

  data class Main(val mount: MainMount) : RootChildMount
}

/**
 * What the one root mount owns: the root store, the child the phase names,
 * the entitlement slice the main level projects into its tabs, and the
 * teardown registry everything else hangs on.
 */
class RootMount(val store: RootStore, private val host: StoreHost) {
  private val mutableChild = MutableStateFlow<RootChildMount?>(null)
  val child: StateFlow<RootChildMount?> = mutableChild

  private val mutableEntitlement = MutableStateFlow<Entitlement>(Entitlement.Free)
  /** The slice: root state, narrowed to the one value the tabs read. */
  val entitlement: StateFlow<Entitlement> = mutableEntitlement

  internal fun publish(child: RootChildMount?) {
    mutableChild.value = child
  }

  internal fun project(entitlement: Entitlement) {
    mutableEntitlement.value = entitlement
  }

  /** Logical destruction only (finish, not rotation). */
  fun teardown() = host.teardownAll()
}

class RootBuilder(private val dependency: RootDependency) {
  fun buildRoot(scope: CoroutineScope): RootMount {
    val component = RootComponent(dependency, scope)
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
                      entitlement = mount.entitlement,
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
        // The slice first, so a main level built below reads the current value.
        mount.project(state.entitlement)
        child.reconcile(state.phase)
        mount.publish(child.activeHandle)
      })

    // The two workers, adopted for the mount's lifetime: each observes one
    // port stream and reports every value into the root store through the
    // relay. The streams are sticky, so the auth seed is their first report.
    val relay = Relay<RootAction>()
    relay.sink = store::send
    host.adopt(SessionWorker(component.auth, relay))
    host.adopt(EntitlementWorker(component.purchases, relay))
    return mount
  }
}

/** The name the profile tree shows: the session's, or the guest name. */
val AuthSnapshot.displayNameOrGuest: String
  get() = (this as? AuthSnapshot.SignedIn)?.displayName ?: "Guest"
