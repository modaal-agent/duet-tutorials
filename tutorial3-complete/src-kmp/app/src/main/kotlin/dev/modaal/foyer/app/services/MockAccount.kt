// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.services

import dev.modaal.foyer.ports.AccountPort

/** The account port as a mock service: the saved name is held in memory. */
class MockAccount : AccountPort {
  var displayName: String? = null
    private set

  override fun saveDisplayName(name: String, onSaved: () -> Unit) {
    displayName = name
    onSaved()
  }
}
