// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import EditNameShell
import FoyerBridge
import FoyerKit
import Foundation

public typealias AccountKitStore = BridgedStore<AccountState, any AccountAction>

/// How the shell mounts its child: the Builder supplies this over the level's Component.
public typealias EditNameMount =
  @MainActor (_ currentName: String, _ onDelegate: @escaping (EditNameDelegateEvent) -> Void)
    -> EditNameChild

@MainActor
public final class AccountViewState: ObservableObject {
  @Published public internal(set) var displayName = ""
  @Published public internal(set) var isSigningOut = false
  /// The mounted editor, or none. The view renders it when present.
  @Published public internal(set) var editor: EditNameChild?

  public init() {}
}

/// The account screen's shell: intents in, view state out, and the fourth
/// duty a level with children carries: mounting the child `state.child`
/// names and tearing it down when the value clears.
public final class AccountViewShell: ViewShell {
  public let viewState = AccountViewState()
  public let store: AccountKitStore
  private let mountEditor: EditNameMount
  private var editor: ChildSlot<EditorKey, EditNameChild>?
  /// The state being applied. The mirror publishes before it assigns, so a
  /// child built during a projection reads this, not `store.state`.
  private var applying: AccountState?

  private enum EditorKey: Hashable {
    case editName
  }

  public init(store: AccountKitStore, mountEditor: @escaping EditNameMount) {
    self.store = store
    self.mountEditor = mountEditor
    super.init()
  }

  override public func bind() {
    // Registration order is teardown order reversed: the projection stops
    // first, the child is torn down next, the store's effects last.
    host.adopt(store)
    editor = host.adopt(
      ChildSlot<EditorKey, EditNameChild>(
        build: { [weak self] _ in self?.buildEditor() },
        teardown: { $0.shell.deactivate() }))
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  /// The child's delegate events route to this level's store as actions;
  /// the composition holds no listener of its own.
  private func buildEditor() -> EditNameChild {
    let child = mountEditor((applying ?? store.state).displayName) { [weak self] event in
      self?.store.send(AccountActionEditName(event: event))
    }
    child.shell.activate()
    return child
  }

  // MARK: - Intents

  public func editNameTapped() { store.send(AccountActionEditNameTapped.shared) }
  public func signOutTapped() { store.send(AccountActionSignOutTapped.shared) }
  public func closeTapped() { store.send(AccountActionCloseTapped.shared) }

  // MARK: - State to view state, and the child mount

  private func apply(_ state: AccountState) {
    applying = state
    defer { applying = nil }
    viewState.displayName = state.displayName
    viewState.isSigningOut = state.isSigningOut
    editor?.reconcile(key: state.child == nil ? nil : .editName)
    viewState.editor = editor?.activeHandle
  }
}
