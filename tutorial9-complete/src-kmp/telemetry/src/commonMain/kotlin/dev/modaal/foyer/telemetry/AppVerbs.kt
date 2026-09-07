// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.telemetry

import dev.modaal.duet.services.telemetry.TrackedVerb

/**
 * The verbs this app declares beyond the artifact's starter vocabulary.
 *
 * `TrackedVerb` is an open token, not a closed enum: the artifact ships the
 * verbs an app is likeliest to need on day one (`Viewed`, `Opened`,
 * `Started`, `Completed`, `Failed`, `Created`, `Edited`, `Deleted`,
 * `Toggled`, `SignedOut`), and an app that needs another declares it here.
 * Reach for a starter verb first: a second spelling of the same act splits a
 * dashboard in two.
 *
 * A verb is a taxonomy decision. Spell it exactly as a dashboard should read
 * it — Title Case, `TrackedVerb("Signed In")` — because the token IS the
 * vendor-facing word: `encodedName()` splices it after the subject, and both
 * platforms read this one declaration rather than deriving from it.
 */
object AppVerbs {
  /** The starter vocabulary has `Signed Out` and no `Signed In`; this is its pair. */
  val SignedIn = TrackedVerb("Signed In")
}
