// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import AccountShell
import SwiftUI

/// The profile tab over the shell's view state: the account screen when
/// mounted, else the header and the row.
public struct ProfileView: View {
  @ObservedObject var viewState: ProfileViewState
  let shell: ProfileViewShell

  public init(viewState: ProfileViewState, shell: ProfileViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    if let account = viewState.account {
      AccountView(viewState: account.shell.viewState, shell: account.shell)
    } else {
      VStack(alignment: .leading, spacing: 0) {
        Text("Profile")
          .font(.title)
          .padding(.horizontal, 24)
          .padding(.vertical, 16)
        Text(viewState.displayName)
          .font(.title2)
          .padding(.horizontal, 24)
        List {
          Button {
            shell.accountTapped()
          } label: {
            VStack(alignment: .leading, spacing: 2) {
              Text("Account")
              Text("Name and sign-out")
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
          }
          .foregroundStyle(.primary)
        }
        .listStyle(.plain)
        .padding(.top, 24)
      }
    }
  }
}
