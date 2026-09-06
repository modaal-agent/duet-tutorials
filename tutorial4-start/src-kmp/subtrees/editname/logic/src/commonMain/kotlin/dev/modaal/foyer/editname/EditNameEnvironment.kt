// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.editname

/** The editor's door to the platform: the save call and the delegate sink. */
interface EditNameEnvironment {
  /** Save the name; `onSaved` fires once when the account port has it. */
  fun saveName(name: String, onSaved: () -> Unit)

  fun notifyHost(event: EditNameDelegateEvent)
}
