// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.root

import dev.modaal.foyer.ports.DeepLink

/**
 * The root's door to the platform: the link forward into main, and the
 * account port's onboarding write. Both are void: the shell delivers the
 * link, and the session stream carries the onboarding result.
 */
interface RootEnvironment {
  /** Hand a link down: the shell sends it to the main level's store as `OpenLink`. */
  fun forwardLink(link: DeepLink)

  fun completeOnboarding(name: String, preferences: List<String>)
}
