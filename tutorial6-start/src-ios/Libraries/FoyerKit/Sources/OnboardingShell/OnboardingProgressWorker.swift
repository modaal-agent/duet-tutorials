// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerKit
import Foundation

/// The lateral seam between the onboarding steps and the progress row, as a
/// worker: it holds the seam for the level's lifetime, `run()` parks, and
/// the host's cancellation ends it. Main-bound, like every worker in this
/// app. No recording exists for this class; `OnboardingProgressWorkerSpec`
/// pins the seam's sticky delivery.
@MainActor
final class OnboardingProgressWorker: Working {
  /// The two ports the steps and the row hold. One object conforms to both.
  let seam = ReadinessSeam()

  func run() async {
    await untilCancelled()
  }
}

/// One sticky value per step behind a `CurrentValueSubject`. The steps write
/// through `updateReadiness`, a void call; the row reads through
/// `observeReadiness`, which delivers each step's current value first and
/// every change after. Every call arrives from a store on the main-immediate
/// scope; the Kotlin interfaces carry no isolation of their own.
final class ReadinessSeam: NSObject, ReadinessUpdating, ReadinessObserving {
  private let readiness = CurrentValueSubject<[OnboardingPage: Bool], Never>([:])

  func updateReadiness(step: OnboardingPage, ready: Bool) {
    var current = readiness.value
    current[step] = ready
    readiness.send(current)
  }

  func observeReadiness(onChange: @escaping (StepReadiness) -> Void) -> any ReadinessSubscription {
    var delivered: [OnboardingPage: Bool] = [:]
    let cancellable = readiness.sink { current in
      for (step, ready) in current where delivered[step] != ready {
        delivered[step] = ready
        onChange(StepReadiness(step: step, ready: ready))
      }
    }
    return ReadinessObservation(cancellable)
  }
}

/// What `observeReadiness` hands back: the seam's own cancel handle.
final class ReadinessObservation: NSObject, ReadinessSubscription {
  private var cancellable: AnyCancellable?

  init(_ cancellable: AnyCancellable) {
    self.cancellable = cancellable
  }

  func cancel() {
    cancellable = nil
  }
}
