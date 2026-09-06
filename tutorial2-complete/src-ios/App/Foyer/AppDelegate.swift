// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
  func application(
    _ application: UIApplication,
    configurationForConnecting connectingSceneSession: UISceneSession,
    options: UIScene.ConnectionOptions
  ) -> UISceneConfiguration {
    // The scene manifest in Info.plist names SceneDelegate; the app's host
    // lives there, and the app delegate carries process-level duties only.
    UISceneConfiguration(
      name: "Default Configuration", sessionRole: connectingSceneSession.role)
  }
}
