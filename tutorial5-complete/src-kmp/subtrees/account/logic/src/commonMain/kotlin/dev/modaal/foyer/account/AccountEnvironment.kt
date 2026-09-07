// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.account

/** The account screen's door to the platform: the sign-out call and the delegate sink. */
interface AccountEnvironment {
  /** End the session; `onDone` fires once when the auth port has. */
  fun signOut(onDone: () -> Unit)

  fun notifyHost(event: AccountDelegateEvent)
}
