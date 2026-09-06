// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation

/// The auth port as a mock service: accepts any non-empty email and any
/// guest, answers at once, and keeps no session across a relaunch. A product
/// source of the iOS app, not a test double: the app runs on it until
/// Tutorial 4 replaces it with the on-device backend. The framework exports
/// the port as an Objective-C protocol, so an `NSObject` subclass conforms.
public final class MockAuth: NSObject, AuthPort {
  public override init() {}

  public func signIn(
    provider: SignInProvider, onOutcome: @escaping (SignInOutcome) -> Void
  ) {
    switch onEnum(of: provider) {
    case .email(let email):
      if email.address.trimmingCharacters(in: .whitespaces).isEmpty {
        onOutcome(SignInOutcomeFailed(reason: "Enter an email address."))
      } else {
        onOutcome(SignInOutcomeSignedIn(displayName: nil))
      }
    case .guest:
      onOutcome(SignInOutcomeSignedIn(displayName: nil))
    }
  }

  public func signOut(onDone: @escaping () -> Void) {
    onDone()
  }
}
