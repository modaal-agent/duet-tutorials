// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.foyer.ports.AccountPort

/**
 * The account port, on device: the saved name and the onboarding result are
 * persisted, and a live session carries both from the write on, so every
 * reader of the session stream sees the new value.
 */
class LocalAccount(private val storage: LocalStorage, private val auth: LocalAuth) : AccountPort {
  override fun saveDisplayName(name: String, onSaved: () -> Unit) {
    storage.update { it.copy(displayName = name) }
    auth.sessionRenamed(name)
    onSaved()
  }

  override fun completeOnboarding(name: String, preferences: List<String>) {
    storage.update { it.copy(displayName = name, hasOnboarded = true, preferences = preferences) }
    auth.sessionRenamed(name)
    auth.sessionOnboarded()
  }
}
