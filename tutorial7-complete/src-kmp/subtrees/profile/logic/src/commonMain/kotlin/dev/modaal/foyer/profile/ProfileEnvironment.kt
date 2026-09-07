// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

/** The profile tab's door to the platform: the delegate sink only. */
interface ProfileEnvironment {
  fun notifyHost(event: ProfileDelegateEvent)
}
