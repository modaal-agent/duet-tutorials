// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import SwiftUI

/// The home tab over the shell's view state: `appeared()` when the view
/// appears (the reducer loads once), then the list.
public struct HomeView: View {
  @ObservedObject var viewState: HomeViewState
  let shell: HomeViewShell

  public init(viewState: HomeViewState, shell: HomeViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      Text("Home")
        .font(.title)
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
      if viewState.isLoading {
        ProgressView()
          .progressViewStyle(.linear)
          .padding(.horizontal, 24)
      }
      List(viewState.items, id: \.id) { item in
        Text(item.title)
      }
      .listStyle(.plain)
    }
    .onAppear { shell.appeared() }
  }
}
