// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import SwiftUI
import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
  var window: UIWindow?
  // The scene retains the host, which holds every mounted shell.
  private var app: AppHost?

  func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    guard let windowScene = scene as? UIWindowScene else { return }
    let window = UIWindow(windowScene: windowScene)
    let app = AppHost()
    window.rootViewController = UIHostingController(rootView: AppScreen(app: app))
    window.makeKeyAndVisible()

    // Activate after the window is visible: activation runs the shell's
    // bind(), which hosts the store and starts the projection.
    app.activate()

    self.window = window
    self.app = app
  }

  func sceneDidDisconnect(_ scene: UIScene) {
    // Explicit teardown: a store's in-flight effects stop when its shell
    // deactivates. `deactivate()` on each shell is what cancels them.
    app?.teardown()
    app = nil
  }
}
