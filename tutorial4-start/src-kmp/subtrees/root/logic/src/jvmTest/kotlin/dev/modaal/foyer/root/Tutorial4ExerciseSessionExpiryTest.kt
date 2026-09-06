// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import kotlin.test.Test
import kotlin.test.fail

/**
 * Tutorial 4's closing exercise, deliberately failing until you finish it:
 * a session that expires while main is up must raise the gate. Record the
 * leaf `root.session-expiry-returns-to-gate` from the root scenario, list it
 * in the manifest, and replace this class with the golden that replays it.
 */
class Tutorial4ExerciseSessionExpiryTest {
  @Test
  fun recordTheSessionExpiryLeaf() {
    fail(
      "Tutorial 4 exercise: record root.session-expiry-returns-to-gate — " +
        "AuthChanged(SignedOut) while the phase is Main raises the gate")
  }
}
