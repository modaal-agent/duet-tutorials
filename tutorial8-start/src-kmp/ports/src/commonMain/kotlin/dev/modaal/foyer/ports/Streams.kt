// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlinx.coroutines.flow.StateFlow

// The two streams, re-exposed as functions for the Swift workers. A stream
// declared on an interface crosses the Apple boundary as the raw Kotlin
// flow type; a top-level function's return type crosses as the Swift async
// sequence the workers iterate. The Kotlin workers read the members.

fun sessionsFlow(auth: AuthPort): StateFlow<Session> = auth.sessions

fun entitlementsFlow(purchases: PurchasesPort): StateFlow<Entitlement> = purchases.entitlements
