// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias EditNameKitStore = BridgedStore<EditNameState, any EditNameAction>

@MainActor
public final class EditNameViewState: ObservableObject {
  @Published public internal(set) var draft = ""
  @Published public internal(set) var isSaving = false
  @Published public internal(set) var validation: String?

  public init() {}
}

public final class EditNameViewShell: ViewShell {
  public let viewState = EditNameViewState()
  public let store: EditNameKitStore

  public init(store: EditNameKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents

  public func draftChanged(_ text: String) {
    store.send(EditNameActionDraftChanged(text: text))
  }

  public func save() { store.send(EditNameActionSaveTapped.shared) }
  public func cancel() { store.send(EditNameActionCancelTapped.shared) }

  // MARK: - State to view state

  private func apply(_ state: EditNameState) {
    viewState.draft = state.draft
    viewState.isSaving = state.isSaving
    viewState.validation = state.validation
  }
}
