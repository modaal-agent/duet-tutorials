// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import HomeShell
import ProfileShell
import SwiftUI

/// The main level over the shell's view state: a tab bar bound to
/// `activeTab`, and the tab's view. Both tabs' stores live for the level's
/// lifetime, so a switch shows a view whose state is still there.
public struct MainView: View {
  @ObservedObject var viewState: MainViewState
  let shell: MainViewShell

  public init(viewState: MainViewState, shell: MainViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    TabView(selection: Binding(get: { viewState.activeTab }, set: { shell.selectTab($0) })) {
      if let home = shell.home {
        HomeView(viewState: home.shell.viewState, shell: home.shell)
          .tabItem { Label("Home", systemImage: "house.fill") }
          .tag(MainTabKey.home)
      }
      if let profile = shell.profile {
        ProfileView(viewState: profile.shell.viewState, shell: profile.shell)
          .tabItem { Label("Profile", systemImage: "person.fill") }
          .tag(MainTabKey.profile)
      }
    }
  }
}
