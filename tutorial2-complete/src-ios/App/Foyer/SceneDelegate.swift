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
    // bind(), which starts its Kotlin effect loop.
    app.activate()

    self.window = window
    self.app = app
  }

  func sceneDidDisconnect(_ scene: UIScene) {
    // Explicit teardown: dropping the reference alone cancels nothing on the
    // Kotlin side. `deactivate()` on each shell is what does.
    app?.teardown()
    app = nil
  }
}
