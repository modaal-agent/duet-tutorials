// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

import dev.modaal.foyer.ports.DeepLink

/** The main level's door to the platform: the delegate sink, and the link forward into the profile tab. */
interface MainEnvironment {
  fun notifyHost(event: MainDelegateEvent)

  /** Hand a link down: the shell sends it to the profile tab's store as `OpenLink`. */
  fun forwardLink(link: DeepLink)
}
