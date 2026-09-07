// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.profile

import dev.modaal.duet.kernel.Effect
import dev.modaal.duet.kernel.Reduced
import dev.modaal.foyer.account.AccountDelegateEvent
import dev.modaal.foyer.ports.DeepLink
import dev.modaal.foyer.ports.Entitlement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The profile tab, level 1 of the profile tree: a header with the display
// name and the plan row, and the account screen it mounts. Feature spec:
// parity/feature-specs/profile.md. Recordings: parity/fixtures/profile.*.

// MARK: - State

/** The one child this level can mount. */
@Serializable(with = ProfileRouteSerializer::class)
sealed interface ProfileRoute {
  @Serializable @SerialName("account") data object Account : ProfileRoute
}

@Serializable
data class ProfileState(
  /** The header's name. Seeded at mount from the root's auth snapshot. */
  val displayName: String = "",
  /** The mounted child, or none. The shell mounts from this value. */
  val child: ProfileRoute? = null,
  /** The slice the root projects down; the plan row shows it. */
  val entitlement: Entitlement = Entitlement.Free,
)

// MARK: - Actions

@Serializable(with = ProfileActionSerializer::class)
sealed interface ProfileAction {
  @Serializable @SerialName("accountTapped") data object AccountTapped : ProfileAction

  /** Shell report: the plan row. */
  @Serializable @SerialName("planTapped") data object PlanTapped : ProfileAction

  /** The child's delegate events, received as this level's actions. */
  @Serializable
  @SerialName("account")
  data class Account(val event: AccountDelegateEvent) : ProfileAction

  /** The root's slice arrived: the entitlement worker wrote a new value. */
  @Serializable
  @SerialName("entitlementChanged")
  data class EntitlementChanged(val entitlement: Entitlement) : ProfileAction

  /** The main level forwarded a deep link into this tab. */
  @Serializable @SerialName("openLink") data class OpenLink(val link: DeepLink) : ProfileAction
}

// MARK: - Delegate events

@Serializable(with = ProfileDelegateEventSerializer::class)
sealed interface ProfileDelegateEvent {
  /** Relayed from the account screen; keeps climbing to the root. */
  @Serializable @SerialName("signOutRequested") data object SignOutRequested : ProfileDelegateEvent

  /** The plan row: the main level mounts the upgrade flow. */
  @Serializable @SerialName("upgradeRequested") data object UpgradeRequested : ProfileDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = ProfileEffectPayloadSerializer::class)
sealed interface ProfileEffectPayload {
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: ProfileDelegateEvent) : ProfileEffectPayload
}

// MARK: - Reducer

/**
 * The row mounts the account screen; the child's `Closed` clears it, its
 * `NameChanged` refreshes the header, and its `SignOutRequested` is relayed
 * upward unchanged. The plan row follows the projected entitlement and asks
 * the host for the upgrade flow. A forwarded link to the account screen
 * mounts it exactly as the row does.
 */
fun profileReducer(
  state: ProfileState,
  action: ProfileAction,
): Reduced<ProfileState, ProfileEffectPayload> =
  when (action) {
    ProfileAction.AccountTapped -> Reduced(state.copy(child = ProfileRoute.Account))

    ProfileAction.PlanTapped ->
      Reduced(
        state,
        listOf(
          Effect.Run(ProfileEffectPayload.NotifyHost(ProfileDelegateEvent.UpgradeRequested))))

    is ProfileAction.EntitlementChanged -> Reduced(state.copy(entitlement = action.entitlement))

    is ProfileAction.OpenLink ->
      when (action.link) {
        DeepLink.ProfileAccount -> Reduced(state.copy(child = ProfileRoute.Account))
        DeepLink.Upgrade -> Reduced(state)
      }

    is ProfileAction.Account ->
      when (val event = action.event) {
        AccountDelegateEvent.Closed -> Reduced(state.copy(child = null))
        is AccountDelegateEvent.NameChanged -> Reduced(state.copy(displayName = event.name))
        AccountDelegateEvent.SignOutRequested ->
          Reduced(
            state,
            listOf(
              Effect.Run(
                ProfileEffectPayload.NotifyHost(ProfileDelegateEvent.SignOutRequested))))
      }
  }
