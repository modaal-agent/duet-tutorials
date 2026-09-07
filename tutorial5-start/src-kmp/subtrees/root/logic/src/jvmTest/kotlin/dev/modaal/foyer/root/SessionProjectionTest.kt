// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.foyer.ports.Session
import kotlin.test.Test
import kotlin.test.assertEquals

/** The session worker's transform, pinned once for both native workers. */
class SessionProjectionTest {
  @Test
  fun aSignedOutSessionIsTheSignedOutSnapshot() {
    assertEquals(AuthSnapshot.SignedOut, authSnapshot(Session.SignedOut))
  }

  @Test
  fun aSignedInSessionCarriesItsName() {
    assertEquals(AuthSnapshot.SignedIn("ann"), authSnapshot(Session.SignedIn("ann")))
  }
}
