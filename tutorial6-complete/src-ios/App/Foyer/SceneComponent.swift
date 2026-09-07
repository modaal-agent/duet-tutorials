// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation
import RootShell

/// The app target's conformer to the root's Dependency: the backend's
/// document, a JSON file under the app's documents directory. The tree
/// consumes nothing else from outside the framework it imports.
final class SceneComponent: RootDependency {
  let storage: any KeyValueFile

  init() {
    let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    storage = JsonFile(path: documents.appendingPathComponent("foyer.json").path)
  }
}
