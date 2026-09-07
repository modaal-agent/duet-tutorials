// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.duet.kernel.KernelClock
import dev.modaal.duet.kernel.LiveClock
import dev.modaal.foyer.ports.AccountPort
import dev.modaal.foyer.ports.AuthPort
import dev.modaal.foyer.ports.ItemsPort
import dev.modaal.foyer.ports.PurchasesPort
import kotlinx.coroutines.CoroutineScope

/**
 * The four repositories over one document. A composition root constructs
 * one of these per app and hands its members down as the ports; the scope
 * is the root's, so the purchase delay and the guest expiry end with it.
 */
class LocalBackend(file: KeyValueFile, scope: CoroutineScope, clock: KernelClock) {
  constructor(file: KeyValueFile, scope: CoroutineScope) : this(file, scope, LiveClock)

  private val storage = LocalStorage(file)
  private val localAuth = LocalAuth(storage, clock, scope)

  val auth: AuthPort = localAuth
  val purchases: PurchasesPort = LocalPurchases(storage, clock, scope)
  val items: ItemsPort = LocalItems()
  val account: AccountPort = LocalAccount(storage, localAuth)
}
