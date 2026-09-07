// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import DuetTelemetry
import Foundation
import os

/// The vendor sink, minus the vendor: one line per event on the console, in
/// the shape a vendor SDK receives — the encoded name and the property bag.
/// A sink is a worker: the composition root adopts it, and `run()` parks for
/// the mount's lifetime. The enabled flag is the sink's own, where a vendor
/// SDK's opt-out switch would be, and a disabled sink prints nothing.
///
/// A `Working` is `Sendable`, so the flag cannot be a plain `var`; the lock
/// is the port's own advice for a sink that holds state. `line` is the
/// console; a spec passes a recorder.
public final class ConsoleAnalyticsSink: AnalyticsTrackingWorking {
  private let enabled = OSAllocatedUnfairLock(initialState: true)
  private let line: @Sendable (String) -> Void

  public init(line: @escaping @Sendable (String) -> Void = { print($0) }) {
    self.line = line
  }

  public var isEnabled: Bool { enabled.withLock { $0 } }

  public func setEnabled(_ enabled: Bool) {
    self.enabled.withLock { $0 = enabled }
  }

  public func track(event: TrackedEvent) {
    guard isEnabled else { return }
    let bag = event.encodedProperties()
    let properties =
      bag.isEmpty
      ? ""
      : " {" + bag.keys.sorted().map { "\($0)=\(bag[$0]!)" }.joined(separator: ", ") + "}"
    line("analytics: \(event.encodedName())\(properties)")
  }

  public func identify(uid: String) {
    if isEnabled { line("analytics: identify \(uid)") }
  }

  public func reset() {
    if isEnabled { line("analytics: reset") }
  }

  /// Parks until the host cancels: a console has nothing to flush.
  public func run() async {
    await untilCancelled()
  }
}
