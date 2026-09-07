// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.duet.services.telemetry.TrackedEvent
import dev.modaal.duet.services.telemetry.TrackedParam
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.telemetry.AppVerbs

/**
 * The gate's named events. The one param is the kind of provider — never
 * the address, which is the user's: params are behavioral metadata only.
 */
object SignInEvents {
  fun signedIn(provider: SignInProvider?): TrackedEvent =
    TrackedEvent(
      "Session",
      AppVerbs.SignedIn,
      listOf(
        TrackedParam.string(
          "provider",
          when (provider) {
            is SignInProvider.Email -> "email"
            SignInProvider.Guest -> "guest"
            null -> "unknown"
          })))
}
