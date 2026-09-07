// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.SignInOutcome
import dev.modaal.foyer.ports.SignInProvider
import dev.modaal.foyer.signin.SignInAction
import dev.modaal.foyer.signin.SignInDelegateEvent
import dev.modaal.foyer.signin.SignInEffectPayload
import dev.modaal.foyer.signin.SignInEnvironment
import dev.modaal.foyer.signin.SignInState
import dev.modaal.foyer.signin.makeSignInStore
import kotlinx.coroutines.CoroutineScope

// The sign-in level's composition triple: Dependency, Component, Builder.
// One file per level, named for the Builder.

typealias SignInStore = Store<SignInState, SignInAction, SignInEffectPayload>

/**
 * What the sign-in level consumes from its parent: the auth port, and
 * nothing else. The parent's Component conforms; delete this member and the
 * root's conformance fails to compile.
 */
interface SignInDependency {
  val auth: AuthPort
}

/**
 * The Component: forwards its whole Dependency in one clause, owns what is
 * scoped to this level (nothing yet), and assembles the level's environment
 * from its own members.
 */
class SignInComponent(dependency: SignInDependency) : SignInDependency by dependency {
  fun environment(onDelegate: (SignInDelegateEvent) -> Unit): SignInEnvironment =
    object : SignInEnvironment {
      override fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit) =
        auth.signIn(provider, onOutcome)

      override fun notifyHost(event: SignInDelegateEvent) = onDelegate(event)
    }
}

/**
 * The Builder: constructs the Component once per mount, builds the store on
 * the host's scope, and resolves nothing itself.
 */
class SignInBuilder(private val dependency: SignInDependency) {
  fun buildSignIn(onDelegate: (SignInDelegateEvent) -> Unit, scope: CoroutineScope): SignInStore {
    val component = SignInComponent(dependency)
    return makeSignInStore(environment = component.environment(onDelegate), scope = scope)
  }
}
