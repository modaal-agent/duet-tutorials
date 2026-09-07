// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import MainShell
import SignInShell
import SplashShell
import SwiftUI

/// The render layer: the view of whichever child the root has mounted.
public struct RootView: View {
  @ObservedObject var viewState: RootViewState
  let shell: RootViewShell

  public init(viewState: RootViewState, shell: RootViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    switch viewState.child {
    case .splash(let child):
      SplashView(viewState: child.shell.viewState, shell: child.shell)
    case .signIn(let child):
      SignInView(viewState: child.shell.viewState, shell: child.shell)
    case .main(let child):
      MainView(viewState: child.shell.viewState, shell: child.shell)
    case nil:
      EmptyView()
    }
  }
}
