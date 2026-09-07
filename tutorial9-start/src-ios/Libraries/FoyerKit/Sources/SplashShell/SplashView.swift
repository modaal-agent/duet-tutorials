// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import SwiftUI

/// How long the reveal plays. The animation is the view's; the safety net is the feature's.
private let ceremonySeconds = 1.6

/// The splash screen over the shell's view state. Every value on it comes
/// from the store the shell drives, and every event leaves as an intent:
/// `appeared()` when the view appears, `ceremonyFinished()` when the reveal
/// animation completes.
public struct SplashView: View {
  @ObservedObject var viewState: SplashViewState
  let shell: SplashViewShell
  @State private var revealed = false

  public init(viewState: SplashViewState, shell: SplashViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(spacing: 12) {
      Text("Foyer")
        .font(.system(size: 45, weight: .medium, design: .default))
      Text("One core, two apps")
        .font(.body)
        .foregroundStyle(.secondary)
      if viewState.isArmed {
        ProgressView()
          .progressViewStyle(.linear)
          .frame(width: 120)
          .padding(.top, 24)
      }
    }
    .opacity(revealed ? 1 : 0)
    .offset(y: revealed ? 0 : 16)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onAppear {
      shell.appeared()
      withAnimation(.easeOut(duration: ceremonySeconds)) {
        revealed = true
      } completion: {
        shell.ceremonyFinished()
      }
    }
  }
}
