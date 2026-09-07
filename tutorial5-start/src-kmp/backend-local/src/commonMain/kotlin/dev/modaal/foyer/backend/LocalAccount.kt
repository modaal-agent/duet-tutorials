// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.foyer.ports.AccountPort

/**
 * The account port, on device: the saved name is persisted, and a live
 * session carries it from the save on, so every reader of the session stream
 * sees the new name.
 */
class LocalAccount(private val storage: LocalStorage, private val auth: LocalAuth) : AccountPort {
  override fun saveDisplayName(name: String, onSaved: () -> Unit) {
    storage.update { it.copy(displayName = name) }
    auth.sessionRenamed(name)
    onSaved()
  }
}
