// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The onboarding steps' lateral seam. Three step features publish whether
// they are ready to continue; a sibling, the progress row, observes the
// three values. Neither side names the other: each sees one narrow port, and
// one worker, owned by the level that mounts them all, conforms to both.

/** The onboarding level's pages, in order. The same value names a step to the seam. */
@Serializable
enum class OnboardingPage {
  @SerialName("welcome") Welcome,
  @SerialName("name") Name,
  @SerialName("preferences") Preferences,
}

/** One step's readiness, as the seam carries it. */
data class StepReadiness(val step: OnboardingPage, val ready: Boolean)

/** The producer's side: a step publishes its readiness. Void; the stream carries the result. */
interface ReadinessUpdating {
  fun updateReadiness(step: OnboardingPage, ready: Boolean)
}

/**
 * The consumer's side: the progress row observes every step's readiness.
 * Sticky: `onChange` fires now with each step's current value, then on every
 * change, until the returned subscription is cancelled.
 */
interface ReadinessObserving {
  fun observeReadiness(onChange: (StepReadiness) -> Unit): ReadinessSubscription
}

fun interface ReadinessSubscription {
  fun cancel()
}
