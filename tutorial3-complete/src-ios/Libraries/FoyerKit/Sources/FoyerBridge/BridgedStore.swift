// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerKit
import Foundation

// The Kotlin `Store` is the runtime; this class is its Swift face. It exists
// for one reason: a shell must observe a reduce before `send` returns, and a
// bridged `StateFlow` delivers asynchronously (every delivery crosses a
// continuation hop). The mirror re-reads `state.value` synchronously after
// each send, which restores that ordering for the intent path; effect-fed
// actions (the safety net's tick) arrive through the collector one hop later.
//
// Feature-generic: every shell in the app uses this one class.

/// A `@Published` mirror of a Kotlin store's bridged `StateFlow`, a
/// synchronous `send`, and the runtime's teardown as a `HostedObservation`,
/// so `StateTransitions` composes over `$state` as it would over a Swift
/// store and `StoreHost` stops the Kotlin runtime with everything else.
@MainActor
public final class BridgedStore<State: Equatable, Action>: HostedObservation {
  @Published public private(set) var state: State

  private let stateFlow: SkieSwiftStateFlow<State>
  private let sendAction: (Action) -> Void
  private let teardownRuntime: () -> Void
  private var collector: Task<Void, Never>?

  /// - Parameters:
  ///   - stateFlow: the feature's typed re-expose (`splashStateFlow(store:)`).
  ///   - send: forwards to the Kotlin store's `send(action:)`; main-actor
  ///     calls only, matching the kernel's rule (the scope is `Main.immediate`).
  ///   - teardown: the runtime's full stop, `store.teardown()` then
  ///     `cancelStoreScope(scope:)` for the scope the builder made.
  public init(
    state stateFlow: SkieSwiftStateFlow<State>,
    send: @escaping (Action) -> Void,
    teardown: @escaping () -> Void
  ) {
    self.stateFlow = stateFlow
    self.state = stateFlow.value
    self.sendAction = send
    self.teardownRuntime = teardown
    collector = Task { [weak self] in
      for await newState in stateFlow {
        guard let self else { return }
        // The synchronous mirror already published most sent-action states;
        // equal redeliveries stop here instead of re-firing every sink.
        if newState != self.state {
          self.state = newState
        }
      }
    }
  }

  /// Reduces before returning: the Kotlin store reduces synchronously on
  /// this thread, and the mirror re-reads `state.value` in the same call.
  public func send(_ action: Action) {
    sendAction(action)
    let latest = stateFlow.value
    if latest != state {
      state = latest
    }
  }

  /// `StoreHost` teardown: stops the collector, then the Kotlin runtime.
  public func cancel() {
    collector?.cancel()
    collector = nil
    teardownRuntime()
  }
}
