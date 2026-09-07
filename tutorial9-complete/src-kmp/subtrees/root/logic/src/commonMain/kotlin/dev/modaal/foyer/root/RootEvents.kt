// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedParam
import dev.modaal.duet.services.telemetry.TrackedVerb
import dev.modaal.foyer.splash.SplashCompletionPath

/**
 * The root's named events, declared from the shared grammar beside the
 * reducer that emits them. The launch is the root's to count: the splash
 * notifies its host on every completion path, and this level is where one
 * of them is acted on, so the event is minted here and not in the splash.
 */
object RootEvents {
  /** The app finished launching; `path` says which route ended the splash. */
  fun splashCompleted(path: SplashCompletionPath): TrackedEvent =
    TrackedEvent(
      "Splash",
      TrackedVerb.Completed,
      listOf(
        TrackedParam.string(
          "path",
          when (path) {
            SplashCompletionPath.Ceremony -> "ceremony"
            SplashCompletionPath.SafetyNet -> "safety_net"
          })))
}
