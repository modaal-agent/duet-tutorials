// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.shells.StoreHost
import dev.modaal.foyer.splash.SplashCompletionPath
import dev.modaal.foyer.splash.SplashDelegateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which screen the app shows. Tutorial 3 replaces this with the root feature's state. */
sealed interface AppPhase {
  data object Splash : AppPhase

  data class Placeholder(val completedBy: SplashCompletionPath) : AppPhase
}

/**
 * The app's host at this step: it mounts the splash store on the retained
 * scope, receives the splash's delegate event, tears the splash down and
 * switches to the placeholder. Android-free, so the JVM test drives it.
 * Tutorial 3 replaces it with the root feature and its Builder.
 */
class AppHost(scope: CoroutineScope) {
  /** The teardown registry: what the host builds registers here and unwinds in reverse. */
  val host = StoreHost(scope)

  private val mutablePhase = MutableStateFlow<AppPhase>(AppPhase.Splash)
  val phase: StateFlow<AppPhase> = mutablePhase

  /** The splash store: mounted here, torn down when the splash completes. */
  var splash: SplashStore? =
    host.host(SplashBuilder().buildSplash(onDelegate = ::splashCompleted, scope = scope))
    private set

  private fun splashCompleted(event: SplashDelegateEvent) {
    // Both completion paths notify every time; the first notification moves
    // the app on and the rest arrive after the splash is gone.
    if (mutablePhase.value !is AppPhase.Splash) return
    val completed = event as SplashDelegateEvent.Completed
    splash?.teardown()
    splash = null
    mutablePhase.value = AppPhase.Placeholder(completedBy = completed.path)
  }

  /** Logical destruction only (finish, not rotation). */
  fun teardown() = host.teardownAll()
}
