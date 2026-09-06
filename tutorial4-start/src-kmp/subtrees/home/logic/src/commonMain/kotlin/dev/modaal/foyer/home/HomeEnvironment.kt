// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.foyer.ports.Item

/** The home tab's door to the platform: the items call. No delegate yet. */
interface HomeEnvironment {
  /** Load the list; `onItems` fires once with the items port's answer. */
  fun loadItems(onItems: (List<Item>) -> Unit)
}
