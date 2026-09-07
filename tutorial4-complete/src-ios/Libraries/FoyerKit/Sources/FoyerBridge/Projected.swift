// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import Foundation

/// A slice of a parent's state, handed down to a child: the parent writes
/// it from its own projection, the child adopts an observation of it into
/// its host, and the observation ends with the host. State travels down as
/// a value; the child turns each value into one of its own actions.
///
/// Kotlin values cross the boundary as objects, so equal values are told
/// apart with `isEqual`, which the framework exports for every data class.
@MainActor
public final class Projected<Value: AnyObject> {
  private let subject: CurrentValueSubject<Value, Never>

  public init(_ initial: Value) {
    subject = CurrentValueSubject(initial)
  }

  public var value: Value { subject.value }

  /// The parent's write. An equal value is dropped, so a projection that
  /// runs on every parent state change delivers only the changes.
  public func project(_ value: Value) {
    if (subject.value as AnyObject).isEqual(value) { return }
    subject.send(value)
  }

  /// The child's read: `sink` runs with the current value now and with every
  /// change after, until the returned observation is cancelled.
  public func observe(_ sink: @escaping @MainActor (Value) -> Void) -> ProjectedObservation {
    ProjectedObservation(
      subject.sink { value in
        MainActor.assumeIsolated { sink(value) }
      })
  }
}

/// What `observe` hands back: a `HostedObservation`, so `StoreHost.adopt`
/// retains it for the mount and cancels it at teardown.
@MainActor
public final class ProjectedObservation: HostedObservation {
  private var cancellable: AnyCancellable?

  init(_ cancellable: AnyCancellable) {
    self.cancellable = cancellable
  }

  public func cancel() {
    cancellable = nil
  }
}
