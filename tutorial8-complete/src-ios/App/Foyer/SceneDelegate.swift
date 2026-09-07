// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
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
    // The route spine a previous process saved rides the scene's restoration
    // activity; a stale payload decodes to nil and the tree starts fresh.
    let saved = session.stateRestorationActivity?.userInfo?[Self.spineKey] as? String
    let root = RootBuilder(dependency: SceneComponent()).buildRoot(restored: decodeRouteSpine(text: saved))
    window.rootViewController = UIHostingController(
      rootView: RootView(viewState: root.shell.viewState, shell: root.shell))
    window.makeKeyAndVisible()

    // Activate after the window is visible: activation runs the shell's
    // bind(), which mounts the splash and starts its Kotlin effect loop.
    root.shell.activate()

    self.window = window
    self.root = root

    // A link the app was launched with: parsed here, routed by the root.
    for context in connectionOptions.urlContexts {
      openLink(context.url)
    }
  }

  func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
    for context in URLContexts {
      openLink(context.url)
    }
  }

  /// What the scene saves before the process may die: each level's route
  /// sliver, encoded once by the Kotlin core.
  func stateRestorationActivity(for scene: UIScene) -> NSUserActivity? {
    guard let root else { return nil }
    let activity = NSUserActivity(activityType: Self.spineActivityType)
    activity.addUserInfoEntries(from: [Self.spineKey: encodeRouteSpine(spine: root.shell.routeSpine())])
    return activity
  }

  func sceneDidDisconnect(_ scene: UIScene) {
    // Explicit teardown: dropping the reference alone cancels nothing on the
    // Kotlin side. `deactivate()` unwinds the whole tree.
    root?.shell.deactivate()
    root = nil
  }

  /// The URL as a link, if it is one the app answers; the root routes it from there.
  private func openLink(_ url: URL) {
    guard let link = parseDeepLink(url: url.absoluteString) else { return }
    root?.shell.openLink(link)
  }

  private static let spineKey = "dev.modaal.foyer.routeSpine"
  private static let spineActivityType = "dev.modaal.foyer.stateRestoration"
}
