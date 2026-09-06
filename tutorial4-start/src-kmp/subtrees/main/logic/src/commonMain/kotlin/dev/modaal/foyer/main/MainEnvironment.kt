// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.main

/** The main level's door to the platform: the delegate sink only. */
interface MainEnvironment {
  fun notifyHost(event: MainDelegateEvent)
}
