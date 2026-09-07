// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.progress

import dev.modaal.foyer.ports.ReadinessSubscription
import dev.modaal.foyer.ports.StepReadiness

/**
 * The row's door to the platform: the readiness seam's consumer side. Sticky:
 * `onChange` fires now with each step's current value, then on every change,
 * until the subscription is cancelled.
 */
interface ProgressEnvironment {
  fun observeReadiness(onChange: (StepReadiness) -> Unit): ReadinessSubscription
}
