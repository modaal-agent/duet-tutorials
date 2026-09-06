// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.app.services

import dev.modaal.foyer.ports.Item
import dev.modaal.foyer.ports.ItemsPort

/**
 * The items port as a mock service: a fixed list of twelve rows. The Swift
 * mock carries the same twelve; the two lists are duplicated on purpose for
 * the mocks' short life.
 */
class MockItems : ItemsPort {
  override fun items(onItems: (List<Item>) -> Unit) = onItems(canned)

  private companion object {
    val canned =
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
