// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.KernelClock
import dev.modaal.duet.kernel.LiveClock
import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.splash.SplashAction
import dev.modaal.foyer.splash.SplashDelegateEvent
import dev.modaal.foyer.splash.SplashEffectPayload
import dev.modaal.foyer.splash.SplashEnvironment
import dev.modaal.foyer.splash.SplashState
import dev.modaal.foyer.splash.makeSplashStore
import kotlinx.coroutines.CoroutineScope

/** The splash store as the app tree hands it to the render layer. */
typealias SplashStore = Store<SplashState, SplashAction, SplashEffectPayload>

/**
 * The live environment: the wall clock, and the host's sink for the delegate
 * event. The interface is the feature's; this class is the app's.
 */
private class LiveSplashEnvironment(
  private val onDelegate: (SplashDelegateEvent) -> Unit,
) : SplashEnvironment {
  override val clock: KernelClock = LiveClock

  override fun notifyHost(event: SplashDelegateEvent) = onDelegate(event)
}

/**
 * Builds one splash mount: the live environment and the store, on the scope
 * the host owns. The store's lifetime belongs to the host, which is why the
 * scope is a build parameter.
 */
class SplashBuilder {
  fun buildSplash(onDelegate: (SplashDelegateEvent) -> Unit, scope: CoroutineScope): SplashStore =
    makeSplashStore(environment = LiveSplashEnvironment(onDelegate), scope = scope)
}
