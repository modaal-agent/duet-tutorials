// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation
import MainShell
import SignInShell

// The root level's composition triple, and the one place that knows the
// whole tree. Hand-written rather than generated: the root owns objects,
// and a generated Component forwards only.

/// What the root consumes from the platform: the file the backend persists
/// its document in. The app target's `SceneComponent` supplies a file under
/// the app's documents; the composition spec supplies memory.
///
/// `CreateMock` generates `RootDependencyMock` in this package's test
/// target; the composition spec builds the whole tree over it.
/// sourcery: CreateMock
public protocol RootDependency: AnyObject {
  var storage: any KeyValueFile { get }
}

/// The root Component owns what is scoped to the app: the on-device
/// backend, whose four members are the ports. It satisfies each child's
/// Dependency with those members, one conformance per child level.
final class RootComponent {
  private let dependency: RootDependency
  private let backend: LocalBackend

  init(dependency: RootDependency, scope: any Kotlinx_coroutines_coreCoroutineScope) {
    self.dependency = dependency
    backend = LocalBackend(file: dependency.storage, scope: scope)
  }

  var auth: any AuthPort { backend.auth }
  var items: any ItemsPort { backend.items }
  var account: any AccountPort { backend.account }
  var purchases: any PurchasesPort { backend.purchases }
}

extension RootComponent: SignInDependency {}
extension RootComponent: MainDependency {}
