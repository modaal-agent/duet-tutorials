// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.services

import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider

/**
 * The auth port as a mock service: accepts any non-empty email and any
 * guest, answers at once, and keeps no session across a relaunch. A product
 * source of the Android app, not a test double: the app runs on it until
 * Tutorial 4 replaces it with the on-device backend.
 */
class MockAuth : AuthPort {
  override fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit) {
    onOutcome(
      when (provider) {
        is SignInProvider.Email ->
          if (provider.address.isBlank()) SignInOutcome.Failed("Enter an email address.")
          else SignInOutcome.SignedIn(displayName = null)
        SignInProvider.Guest -> SignInOutcome.SignedIn(displayName = null)
      })
  }

  override fun signOut(onDone: () -> Unit) = onDone()
}
