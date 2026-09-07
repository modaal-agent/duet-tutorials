// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import SwiftUI

/// The sign-in gate over the shell's view state. The field's live text is
/// the view's own; it reaches the store inside `continueWithEmail`, so the
/// reducer sees the address only when the user commits it.
public struct SignInView: View {
  @ObservedObject var viewState: SignInViewState
  let shell: SignInViewShell
  @State private var address = ""

  public init(viewState: SignInViewState, shell: SignInViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(spacing: 8) {
      Text("Foyer")
        .font(.system(size: 45, weight: .medium))
      Text("Sign in to continue")
        .font(.body)
        .foregroundStyle(.secondary)
      TextField("Email", text: $address)
        .textFieldStyle(.roundedBorder)
        .emailKeyboard()
        .padding(.top, 32)
      Button("Continue with email") {
        shell.continueWithEmail(address)
      }
      .buttonStyle(.borderedProminent)
      .frame(maxWidth: .infinity)
      .disabled(viewState.isSigningIn)
      .padding(.top, 8)
      Button("Continue as guest") {
        shell.continueAsGuest()
      }
      .buttonStyle(.borderless)
      .disabled(viewState.isSigningIn)
      if viewState.isSigningIn {
        ProgressView()
          .progressViewStyle(.linear)
          .padding(.top, 16)
      }
      if let failure = viewState.failure {
        Text(failure)
          .font(.callout)
          .foregroundStyle(.red)
          .padding(.top, 16)
      }
    }
    .padding(.horizontal, 32)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}

extension View {
  /// The email keyboard is an iOS setting; the package also builds for macOS.
  @ViewBuilder
  fileprivate func emailKeyboard() -> some View {
    #if os(iOS)
    self.keyboardType(.emailAddress)
      .textInputAutocapitalization(.never)
      .autocorrectionDisabled()
    #else
    self
    #endif
  }
}
