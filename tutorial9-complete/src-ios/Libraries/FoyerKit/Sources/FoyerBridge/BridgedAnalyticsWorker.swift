// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetShells
import DuetTelemetry
import Foundation
import FoyerKit

// The analytics crossing, and the only file in the tree that holds both
// grammars at once. A Kotlin feature's `Track` effect carries a bridged
// `TrackedEvent` — the FoyerKit framework's class, not the telemetry
// package's struct — so something has to convert before the event reaches a
// Swift sink. The telemetry artifact's fan-out is JVM-only, because the
// Apple side of a dual-language app types its sinks on the native Swift
// substrate (`DuetTelemetry`); crossing events convert app-side, here.
//
// Every other name in this file is written module-qualified on purpose: the
// two grammars spell four types the same way (`AnalyticsTracking`,
// `TrackedEvent`, `TrackedVerb`, `TrackedParam`), and confining that
// ambiguity to one file is why the root composes over the class below
// instead of importing `DuetTelemetry` itself.

/// The root's analytics object: the telemetry package's fan-out wearing the
/// bridged port, so the Apple root and the Android root fan out to the same
/// kind of sink list and take the same vendor recipe.
///
/// It is a `Working` in its own right and the root adopts it; the fan-out it
/// holds parks until teardown, and each vendor sink runs its own `run()`
/// after the root adopts it beside this one.
///
/// `NSObject` because the port is an Obj-C protocol on this side of the
/// bridge: a bridged Kotlin interface can only be implemented by an
/// Obj-C-visible class.
public final class BridgedAnalyticsWorker: NSObject, FoyerKit.AnalyticsTracking, Working {
  private let worker: AnalyticsTrackingWorker

  /// - Parameters:
  ///   - sinks: the vendor sinks to fan out to. Each is adopted by the
  ///     composition root in its own right — neither this object nor the
  ///     fan-out brackets their lifetimes. `[]` accepts and drops every event.
  ///   - isEnabled: pushed into every sink before this initializer returns,
  ///     so no sink egresses in the window between composition and the first
  ///     flip.
  public init(sinks: [any AnalyticsTrackingWorking], isEnabled: Bool = true) {
    worker = AnalyticsTrackingWorker(sinks: sinks, isEnabled: isEnabled)
  }

  public var isEnabled: Bool { worker.isEnabled }

  public func setEnabled(enabled: Bool) {
    worker.setEnabled(enabled)
  }

  public func track(event: FoyerKit.TrackedEvent) {
    worker.track(event: DuetTelemetry.TrackedEvent(bridged: event))
  }

  public func identify(uid: String) {
    worker.identify(uid: uid)
  }

  public func reset() {
    worker.reset()
  }

  public func run() async {
    await worker.run()
  }
}

extension DuetTelemetry.TrackedEvent {
  /// A Kotlin-minted event as its Swift twin. The two grammars are
  /// field-for-field twins and encode identically — the telemetry package
  /// pins that with fixtures written by the Kotlin encoders and decoded by
  /// the Swift ones — so the conversion is a subject copy, the verb token
  /// verbatim, and the param map below. Nothing here decides taxonomy: the
  /// verb is spelled once, in the Kotlin declaration that minted it.
  public init(bridged: FoyerKit.TrackedEvent) {
    self.init(
      subject: bridged.subject,
      verb: DuetTelemetry.TrackedVerb(bridged.verb.rendered),
      params: bridged.params.map { DuetTelemetry.TrackedParam(bridged: $0) })
  }
}

extension DuetTelemetry.TrackedParam {
  /// The four-arm param map. `onEnum(of:)` is SKIE's projection of the
  /// Kotlin sealed interface: the switch is exhaustive, so a fifth param kind
  /// added to the grammar fails this file to compile rather than dropping a
  /// value.
  public init(bridged: any FoyerKit.TrackedParam) {
    switch onEnum(of: bridged) {
    case .stringParam(let param):
      self = .string(key: param.key, value: param.value)
    case .intParam(let param):
      // The Kotlin `Int` bridges as `Int32`; the Swift grammar's case holds
      // `Int`, which is wider on every platform this package builds for.
      self = .int(key: param.key, value: Int(param.value))
    case .boolParam(let param):
      self = .bool(key: param.key, value: param.value)
    case .doubleParam(let param):
      self = .double(key: param.key, value: param.value)
    }
  }
}
