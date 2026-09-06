// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import EditNameShell
import SwiftUI

/// The account screen over the shell's view state: the editor when the
/// shell has mounted one, else the name and the two rows.
public struct AccountView: View {
  @ObservedObject var viewState: AccountViewState
  let shell: AccountViewShell

  public init(viewState: AccountViewState, shell: AccountViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    if let editor = viewState.editor {
      EditNameView(viewState: editor.shell.viewState, shell: editor.shell)
    } else {
      VStack(alignment: .leading, spacing: 0) {
        HStack(spacing: 8) {
          Button {
            shell.closeTapped()
          } label: {
            Image(systemName: "chevron.backward")
              .font(.title3)
          }
          .buttonStyle(.borderless)
          .padding(8)
          Text("Account")
            .font(.title2)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        Text(viewState.displayName)
          .font(.title)
          .padding(.horizontal, 24)
          .padding(.vertical, 16)
        List {
          Button("Edit name") { shell.editNameTapped() }
            .foregroundStyle(.primary)
          Button(viewState.isSigningOut ? "Signing out…" : "Sign out") {
            shell.signOutTapped()
          }
          .foregroundStyle(.red)
          .disabled(viewState.isSigningOut)
        }
        .listStyle(.plain)
      }
    }
  }
}
