// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.signin

import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider

/**
 * The gate's door to the platform: the sign-in call and the delegate sink.
 * The interface is the feature's own, narrower than the auth port behind it;
 * each app's composition implements it over the port it holds.
 */
interface SignInEnvironment {
  /** Start a sign-in; `onOutcome` fires once with the port's answer. */
  fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit)

  /** Hand a delegate event to the host. */
  fun notifyHost(event: SignInDelegateEvent)
}
