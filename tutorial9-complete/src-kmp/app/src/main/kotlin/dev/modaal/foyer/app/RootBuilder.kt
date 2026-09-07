// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.duet.services.telemetry.AnalyticsProviding
import dev.modaal.duet.services.telemetry.AnalyticsTrackingWorker
import dev.modaal.duet.services.telemetry.AnalyticsTrackingWorking
import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.shells.ChildSlot
import dev.modaal.duet.shells.Relay
import dev.modaal.duet.shells.StateTransitions
import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.app.workers.ConsoleAnalyticsSink
import dev.modaal.foyer.app.workers.EntitlementWorker
import dev.modaal.foyer.app.workers.SessionWorker
import dev.modaal.foyer.backend.KeyValueFile
import dev.modaal.foyer.backend.LocalBackend
import dev.modaal.foyer.main.MainAction
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import dev.modaal.foyer.ports.ItemsPort
import dev.modaal.foyer.ports.PurchasesPort
import dev.modaal.foyer.root.AuthSnapshot
import dev.modaal.foyer.root.RootAction
import dev.modaal.foyer.root.RootEffectPayload
import dev.modaal.foyer.root.RootEnvironment
import dev.modaal.foyer.root.RootPhase
import dev.modaal.foyer.root.RootState
import dev.modaal.foyer.root.RouteSpine
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
 * whose four members are the ports, and the analytics sink every feature's
 * `Track` effect reaches. It satisfies each child's Dependency with those
 * members, one conformance per child level; `AnalyticsProviding` is the
 * conformance every level's Dependency extends.
 */
class RootComponent(dependency: RootDependency, scope: CoroutineScope) :
  RootDependency by dependency,
  AnalyticsProviding,
  SignInDependency,
  OnboardingDependency,
  MainDependency {
  private val backend = LocalBackend(storage, scope)

  override val auth: AuthPort = backend.auth
  override val items: ItemsPort = backend.items
  override val account: AccountPort = backend.account
  override val purchases: PurchasesPort = backend.purchases

  /**
   * The one vendor sink this app ships: the console. Picking a real vendor is
   * one more class implementing [AnalyticsTrackingWorking] — the only file
   * that imports the SDK — added to the list below and adopted in its own
   * right.
   */
  val consoleSink: AnalyticsTrackingWorking = ConsoleAnalyticsSink()

  /**
   * The grammar-typed sink [AnalyticsProviding]'s one member names, and a
   * worker the root adopts: the artifact's fan-out over the sink list. It
   * holds no flag of its own; the seed reaches every sink in the constructor,
   * before any event can.
   */
  override val analytics: AnalyticsTrackingWorking =
    AnalyticsTrackingWorker(sinks = listOf(consoleSink), isEnabled = true)

  /** The root's environment: the void calls the reducer's effects make. */
  fun environment(mount: () -> RootMount): RootEnvironment =
    object : RootEnvironment {
      override fun forwardLink(link: DeepLink) = mount().openLink(link)

      override fun completeOnboarding(name: String, preferences: List<String>) =
        account.completeOnboarding(name, preferences)

      override fun track(event: TrackedEvent) = analytics.track(event)
    }
}

/** The child the root has mounted, as the render layer sees it. */
sealed interface RootChildMount {
  data class Splash(val store: SplashStore) : RootChildMount

  data class SignIn(val store: SignInStore) : RootChildMount

  data class Onboarding(val mount: OnboardingMount) : RootChildMount

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

  /** A forwarded link that arrived before this mount published main. */
  private var heldLink: DeepLink? = null

  internal fun publish(child: RootChildMount?) {
    mutableChild.value = child
    val link = heldLink ?: return
    if (child is RootChildMount.Main) {
      heldLink = null
      child.mount.store.send(MainAction.OpenLink(link))
    }
  }

  internal fun project(entitlement: Entitlement) {
    mutableEntitlement.value = entitlement
  }

  /**
   * The root's `forwardLink` effect lands here. The effect can run before
   * the phase change that mounts main has been published, so a link with no
   * main to receive it waits for the next publish.
   */
  internal fun openLink(link: DeepLink) {
    when (val current = child.value) {
      is RootChildMount.Main -> current.mount.store.send(MainAction.OpenLink(link))
      else -> heldLink = link
    }
  }

  /** Each level's route sliver, read from the live stores: what the Activity saves on process death. */
  fun routeSpine(): RouteSpine =
    when (val current = child.value) {
      is RootChildMount.Main -> current.mount.routeSpine()
      is RootChildMount.Onboarding ->
        RouteSpine(phase = RootPhase.Onboarding, onboardingPage = current.mount.store.state.value.page)
      else -> RouteSpine(phase = store.state.value.phase)
    }

  /** Logical destruction only (finish, not rotation). */
  fun teardown() = host.teardownAll()
}

class RootBuilder(private val dependency: RootDependency) {
  /**
   * `restored` is the spine a previous process saved, or null for a fresh
   * start. The splash replays either way; the slivers below the phase apply
   * once, when the child they belong to mounts.
   */
  fun buildRoot(scope: CoroutineScope, restored: RouteSpine? = null): RootMount {
    val component = RootComponent(dependency, scope)
    val host = StoreHost(scope)
    lateinit var mount: RootMount
    val store = host.host(makeRootStore(component.environment { mount }, scope))
    mount = RootMount(store, host)
    var pendingRestore = restored

    // The sink and the fan-out over it, adopted before any child exists: a
    // `Track` effect is fire-and-forget, so a sink that is not yet running
    // would hear nothing. Each is a worker in its own right — the fan-out
    // does not bracket the sinks' lifetimes.
    host.adopt(component.consoleSink)
    host.adopt(component.analytics)

    // Exactly one child at a time, keyed on the phase. Each child's delegate
    // events route to the root store as actions.
    val child =
      host.adopt(
        ChildSlot<RootPhase, RootChildMount>(
          build = { phase ->
            // The spine applies to the first child after the splash, and only
            // when that child is the one the spine names; otherwise it is dropped.
            var restoreFor: RouteSpine? = null
            if (phase != RootPhase.Splash) {
              restoreFor = pendingRestore?.takeIf { it.phase == phase }
              pendingRestore = null
            }
            when (phase) {
              RootPhase.Splash ->
                RootChildMount.Splash(
                  SplashBuilder()
                    .buildSplash(onDelegate = { store.send(RootAction.Splash(it)) }, scope = scope))
              RootPhase.SignIn ->
                RootChildMount.SignIn(
                  SignInBuilder(component)
                    .buildSignIn(onDelegate = { store.send(RootAction.SignIn(it)) }, scope = scope))
              RootPhase.Onboarding ->
                RootChildMount.Onboarding(
                  OnboardingBuilder(component)
                    .buildOnboarding(
                      page = restoreFor?.onboardingPage,
                      onDelegate = { store.send(RootAction.Onboarding(it)) },
                      scope = scope,
                    ))
              RootPhase.Main ->
                RootChildMount.Main(
                  MainBuilder(component)
                    .buildMain(
                      displayName = store.state.value.auth.displayNameOrGuest,
                      entitlement = mount.entitlement,
                      restored = restoreFor,
                      onDelegate = { store.send(RootAction.Main(it)) },
                      scope = scope,
                    ))
            }
          },
          teardown = {
            when (it) {
              is RootChildMount.Splash -> it.store.teardown()
              is RootChildMount.SignIn -> it.store.teardown()
              is RootChildMount.Onboarding -> it.mount.teardown()
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
