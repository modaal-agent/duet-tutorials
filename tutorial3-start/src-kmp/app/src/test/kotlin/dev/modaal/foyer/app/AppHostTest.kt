// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.foyer.splash.SplashAction
import dev.modaal.foyer.splash.SplashCompletionPath
import dev.modaal.foyer.splash.SplashConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The host, headless: no Compose and no Android in the loop. The splash
 * store runs on the test scope, so the safety net waits on virtual time.
 */
class AppHostTest {

  @Test
  fun theCeremonyPathLandsOnThePlaceholderAndTearsTheSplashDown() = runTest {
    val app = AppHost(backgroundScope)
    val splash = assertNotNull(app.splash)
    assertEquals(AppPhase.Splash, app.phase.value)

    splash.send(SplashAction.Appeared)
    splash.send(SplashAction.CeremonyFinished)
    runCurrent()

    assertEquals(AppPhase.Placeholder(SplashCompletionPath.Ceremony), app.phase.value)
    assertNull(app.splash, "the completed splash is torn down")

    // The torn-down store's net never fires: past the deadline, nothing changes.
    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()
    assertEquals(AppPhase.Placeholder(SplashCompletionPath.Ceremony), app.phase.value)
    app.teardown()
  }

  @Test
  fun theSafetyNetPathLandsOnThePlaceholder() = runTest {
    val app = AppHost(backgroundScope)
    assertNotNull(app.splash).send(SplashAction.Appeared)

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()

    assertEquals(AppPhase.Placeholder(SplashCompletionPath.SafetyNet), app.phase.value)
    assertNull(app.splash)
    app.teardown()
  }
}
