// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import FoyerServices
import Foundation
import MainShell
import SignInShell

// The root level's composition triple, and the one place that knows the
// whole tree. Hand-written rather than generated: the root owns objects,
// and a generated Component forwards only.

/// What the root consumes from the platform: nothing yet. The app target's
/// `SceneComponent` is its only conformer; when the tree needs a
/// scene-supplied object, it is named here and every conformer fails to
/// compile until it supplies it.
///
/// `CreateMock` generates `RootDependencyMock` in this package's test
/// target; the composition spec builds the whole tree over it.
/// sourcery: CreateMock
public protocol RootDependency: AnyObject {}

/// The root Component owns what is scoped to the app: the four services
/// behind the ports, mock services until Tutorial 4. It satisfies each
/// child's Dependency with those members, one conformance per child level.
final class RootComponent {
  private let dependency: RootDependency

  init(dependency: RootDependency) {
    self.dependency = dependency
  }

  let auth: any AuthPort = MockAuth()
  let items: any ItemsPort = MockItems()
  let account: any AccountPort = MockAccount()

  /// Not consumed yet: Tutorial 4's entitlement stream and Tutorial 5's
  /// upgrade flow read it.
  let purchases: any PurchasesPort = MockPurchases()
}

extension RootComponent: SignInDependency {}
extension RootComponent: MainDependency {}
