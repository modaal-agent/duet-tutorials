// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.home

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.ports.Item
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The home tab: a list loaded once from the items port. Feature spec:
// parity/feature-specs/home.md. Recordings: parity/fixtures/home.*.

// MARK: - State

@Serializable
data class HomeState(
  val items: List<Item> = emptyList(),
  val isLoading: Boolean = false,
)

// MARK: - Actions

@Serializable(with = HomeActionSerializer::class)
sealed interface HomeAction {
  /** Shell report: the tab is on screen. */
  @Serializable @SerialName("appeared") data object Appeared : HomeAction

  /** Environment report: the items port answered. */
  @Serializable @SerialName("itemsLoaded") data class ItemsLoaded(val items: List<Item>) : HomeAction
}

// MARK: - Effect payloads

@Serializable(with = HomeEffectPayloadSerializer::class)
sealed interface HomeEffectPayload {
  /** Ask the items port for the list; the answer re-enters as `ItemsLoaded`. */
  @Serializable @SerialName("loadItems") data object LoadItems : HomeEffectPayload
}

// MARK: - Reducer

/**
 * The list loads on the first appearance and stays: a repeat `Appeared`,
 * from a tab switch or a rotation, is inert while a load is in flight or
 * once the list is there.
 */
fun homeReducer(state: HomeState, action: HomeAction): Reduced<HomeState, HomeEffectPayload> =
  when (action) {
    HomeAction.Appeared ->
      if (state.isLoading || state.items.isNotEmpty()) {
        Reduced(state)
      } else {
        Reduced(state.copy(isLoading = true), listOf(Effect.Run(HomeEffectPayload.LoadItems)))
      }

    is HomeAction.ItemsLoaded -> Reduced(state.copy(items = action.items, isLoading = false))
  }
