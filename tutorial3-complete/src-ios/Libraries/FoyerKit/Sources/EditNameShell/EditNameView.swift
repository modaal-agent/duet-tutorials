// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import SwiftUI

/// The name editor over the shell's view state: the field is bound to
/// `draft` both ways, through the `draftChanged` intent.
public struct EditNameView: View {
  @ObservedObject var viewState: EditNameViewState
  let shell: EditNameViewShell

  public init(viewState: EditNameViewState, shell: EditNameViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(alignment: .leading, spacing: 16) {
      Text("Edit name")
        .font(.title)
      TextField(
        "Display name",
        text: Binding(get: { viewState.draft }, set: { shell.draftChanged($0) })
      )
      .textFieldStyle(.roundedBorder)
      .padding(.top, 8)
      if let validation = viewState.validation {
        Text(validation)
          .font(.callout)
          .foregroundStyle(.red)
      }
      HStack {
        Spacer()
        Button("Cancel") { shell.cancel() }
          .buttonStyle(.borderless)
        Button("Save") { shell.save() }
          .buttonStyle(.borderedProminent)
          .disabled(viewState.isSaving)
      }
      Spacer()
    }
    .padding(24)
  }
}
