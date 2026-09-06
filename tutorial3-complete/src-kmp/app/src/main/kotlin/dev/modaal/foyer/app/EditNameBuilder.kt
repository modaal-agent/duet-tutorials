// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app

import dev.modaal.duet.kernel.Store
import dev.modaal.foyer.editname.EditNameAction
import dev.modaal.foyer.editname.EditNameDelegateEvent
import dev.modaal.foyer.editname.EditNameEffectPayload
import dev.modaal.foyer.editname.EditNameEnvironment
import dev.modaal.foyer.editname.EditNameState
import dev.modaal.foyer.editname.makeEditNameStore
import dev.modaal.foyer.ports.AccountPort
import kotlinx.coroutines.CoroutineScope

typealias EditNameStore = Store<EditNameState, EditNameAction, EditNameEffectPayload>

/** What the name editor consumes from its parent: the account port. */
interface EditNameDependency {
  val account: AccountPort
}

class EditNameComponent(dependency: EditNameDependency) : EditNameDependency by dependency {
  fun environment(onDelegate: (EditNameDelegateEvent) -> Unit): EditNameEnvironment =
    object : EditNameEnvironment {
      override fun saveName(name: String, onSaved: () -> Unit) =
        account.saveDisplayName(name, onSaved)

      override fun notifyHost(event: EditNameDelegateEvent) = onDelegate(event)
    }
}

class EditNameBuilder(private val dependency: EditNameDependency) {
  fun buildEditName(
    currentName: String,
    onDelegate: (EditNameDelegateEvent) -> Unit,
    scope: CoroutineScope,
  ): EditNameStore {
    val component = EditNameComponent(dependency)
    return makeEditNameStore(
      currentName = currentName,
      environment = component.environment(onDelegate),
      scope = scope,
    )
  }
}
