// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.backend

import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.ItemsPort

/** The items port, on device: the same twelve rows on both platforms, from one source. */
class LocalItems : ItemsPort {
  override fun items(onItems: (List<Item>) -> Unit) = onItems(rows)

  private companion object {
    val rows =
      listOf(
        "Welcome note",
        "Getting started",
        "Your first week",
        "Reading list",
        "Saved for later",
        "Shared with you",
        "Recently viewed",
        "Drafts",
        "Archive",
        "Highlights",
        "Notes to self",
        "Everything else",
      )
        .mapIndexed { index, title -> Item(id = (index + 1).toString(), title = title) }
  }
}
