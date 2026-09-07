// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.workers

import dev.modaal.duet.shells.Relay
import dev.modaal.duet.shells.Working
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.root.RootAction
import dev.modaal.foyer.root.authSnapshot

/**
 * Observes the auth port's session stream for the root mount's lifetime and
 * reports each value as the root's `AuthChanged`. The stream is sticky, so
 * the first report is the current session; the transform is the root
 * module's pure function. `run()` is the whole life: the host adopts the
 * worker at mount and cancels it at teardown, and `collect` returns on that
 * cancellation. The worker decides nothing: it reads a stream and sends.
 */
class SessionWorker(
  private val auth: AuthPort,
  private val relay: Relay<RootAction>,
) : Working {
  override suspend fun run() {
    auth.sessions.collect { session -> relay.send(RootAction.AuthChanged(authSnapshot(session))) }
  }
}
