// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import RootShell
import SwiftUI
import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
  var window: UIWindow?
  // The scene retains the root mount, which holds the whole tree.
  private var root: RootChild?

  func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    guard let windowScene = scene as? UIWindowScene else { return }
    let window = UIWindow(windowScene: windowScene)
    let root = RootBuilder(dependency: SceneComponent()).buildRoot()
    window.rootViewController = UIHostingController(
      rootView: RootView(viewState: root.shell.viewState, shell: root.shell))
    window.makeKeyAndVisible()

    // Activate after the window is visible: activation runs the shell's
    // bind(), which mounts the splash and starts its Kotlin effect loop.
    root.shell.activate()

    self.window = window
    self.root = root
  }

  func sceneDidDisconnect(_ scene: UIScene) {
    // Explicit teardown: dropping the reference alone cancels nothing on the
    // Kotlin side. `deactivate()` unwinds the whole tree.
    root?.shell.deactivate()
    root = nil
  }
}
