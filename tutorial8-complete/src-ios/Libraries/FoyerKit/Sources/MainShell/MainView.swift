// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import HomeShell
import ProfileShell
import SwiftUI
import UpgradeShell

/// The main level over the shell's view state: a tab bar bound to
/// `activeTab`, the tab's view, and the upgrade flow in a sheet while the
/// slot names it. Both tabs' stores live for the level's lifetime, so a
/// switch shows a view whose state is still there. The sheet is the
/// platform's idiom for a surface over the tabs; its interactive dismissal
/// reaches the flow as `dismissed()`.
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
          .tabItem { Label(String(localized: .localizable(.tabHome)), systemImage: "house.fill") }
          .tag(MainTabKey.home)
      }
      if let profile = shell.profile {
        ProfileView(viewState: profile.shell.viewState, shell: profile.shell)
          .tabItem { Label(String(localized: .localizable(.tabProfile)), systemImage: "person.fill") }
          .tag(MainTabKey.profile)
      }
    }
    .sheet(
      isPresented: Binding(
        get: { viewState.upgrade != nil },
        set: { presented in
          if !presented { viewState.upgrade?.shell.dismissed() }
        })
    ) {
      if let upgrade = viewState.upgrade {
        UpgradeView(viewState: upgrade.shell.viewState, shell: upgrade.shell)
      }
    }
  }
}
