// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

import dev.modaal.duet.services.telemetry.TrackedEvent

/** The account screen's door to the platform: the sign-out call and the delegate sink. */
interface AccountEnvironment {
  /** End the session; `onDone` fires once when the auth port has. */
  fun signOut(onDone: () -> Unit)

  fun notifyHost(event: AccountDelegateEvent)

  /** Forward a `Track` effect's event to the app's one sink (fire-and-forget). */
  fun track(event: TrackedEvent)
}
