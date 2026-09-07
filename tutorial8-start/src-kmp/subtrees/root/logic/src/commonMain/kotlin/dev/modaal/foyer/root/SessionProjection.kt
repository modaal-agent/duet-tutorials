// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.foyer.ports.Session

/**
 * The session worker's transform: a value from the auth port's stream, as
 * the root's auth snapshot. Pure and common, so the two native workers
 * (Kotlin over a Flow, Swift over an async sequence) share it and a test
 * pins it once.
 */
fun authSnapshot(session: Session): AuthSnapshot =
  when (session) {
    Session.SignedOut -> AuthSnapshot.SignedOut
    is Session.SignedIn -> AuthSnapshot.SignedIn(session.displayName, session.hasOnboarded)
  }
