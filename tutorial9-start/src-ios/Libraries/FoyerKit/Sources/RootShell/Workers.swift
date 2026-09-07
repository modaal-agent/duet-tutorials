// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import FoyerKit
import Foundation

// The two workers adopted at the root mount. Each observes one port stream
// for the mount's lifetime and reports every value into the root store
// through the relay; `run()` is the whole life, and the host's cancellation
// ends the `for await`. Main-bound, so the class is `@MainActor` and its
// state needs no lock; the transform is the root module's pure function.
// The streams are read through the ports module's `sessionsFlow` and
// `entitlementsFlow`, whose return types cross the boundary as Swift async
// sequences.

/// The auth port's session stream, as the root's `AuthChanged`.
@MainActor
final class SessionWorker: Working {
  private let auth: any AuthPort
  private let relay: Relay<any RootAction>

  init(auth: any AuthPort, relay: Relay<any RootAction>) {
    self.auth = auth
    self.relay = relay
  }

  func run() async {
    for await session in sessionsFlow(auth: auth) {
      relay.send(RootActionAuthChanged(auth: authSnapshot(session: session)))
    }
  }
}

/// The purchases port's entitlement stream, as the root's `EntitlementChanged`.
@MainActor
final class EntitlementWorker: Working {
  private let purchases: any PurchasesPort
  private let relay: Relay<any RootAction>

  init(purchases: any PurchasesPort, relay: Relay<any RootAction>) {
    self.purchases = purchases
    self.relay = relay
  }

  func run() async {
    for await entitlement in entitlementsFlow(purchases: purchases) {
      relay.send(RootActionEntitlementChanged(entitlement: entitlement))
    }
  }
}
