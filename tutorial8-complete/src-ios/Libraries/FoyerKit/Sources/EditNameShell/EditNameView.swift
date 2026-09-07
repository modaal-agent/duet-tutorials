// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
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
      Text(localizable: .title)
        .font(.title)
      TextField(
        String(localized: .localizable(.displayNamePlaceholder)),
        text: Binding(get: { viewState.draft }, set: { shell.draftChanged($0) })
      )
      .textFieldStyle(.roundedBorder)
      .padding(.top, 8)
      if let validation = viewState.validation {
        Text(localizable: validation.message)
          .font(.callout)
          .foregroundStyle(.red)
      }
      HStack {
        Spacer()
        Button(String(localized: .localizable(.cancel))) { shell.cancel() }
          .buttonStyle(.borderless)
        Button(String(localized: .localizable(.save))) { shell.save() }
          .buttonStyle(.borderedProminent)
          .disabled(viewState.isSaving)
      }
      Spacer()
    }
    .padding(24)
  }
}

/// The refusal's string, from its case: the reducer says why, the view says it.
extension NameValidation {
  var message: String.Localizable {
    switch onEnum(of: self) {
    case .empty: .validationEmpty
    case .tooLong: .validationTooLong
    }
  }
}
