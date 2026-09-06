// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until the port's callback fires. An effect handler calls a port
 * through this, so the port's one callback becomes the value the handler
 * turns into an action.
 */
suspend fun <T> awaitCallback(start: (onResult: (T) -> Unit) -> Unit): T =
  suspendCancellableCoroutine { continuation -> start { continuation.resume(it) } }
