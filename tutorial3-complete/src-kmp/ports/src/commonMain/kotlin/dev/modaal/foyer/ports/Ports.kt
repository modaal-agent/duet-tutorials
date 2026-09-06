// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The four ports. Every operation starts the work and returns; the result
// re-enters through the callback, which is what lets a Swift class implement
// the same interface across the Apple boundary (a `suspend` member cannot be
// implemented from Swift). Each port's callback fires exactly once per call.

// MARK: - Value types

/** How a user signs in. */
@Serializable(with = SignInProviderSerializer::class)
sealed interface SignInProvider {
  @Serializable @SerialName("email") data class Email(val address: String) : SignInProvider

  @Serializable @SerialName("guest") data object Guest : SignInProvider
}

/** What the auth port answers a sign-in with. */
@Serializable(with = SignInOutcomeSerializer::class)
sealed interface SignInOutcome {
  /** Signed in; `displayName` is the account's saved name when it has one. */
  @Serializable
  @SerialName("signedIn")
  data class SignedIn(val displayName: String?) : SignInOutcome

  @Serializable @SerialName("failed") data class Failed(val reason: String) : SignInOutcome
}

/** The two plans the app sells. */
@Serializable(with = PlanSerializer::class)
sealed interface Plan {
  @Serializable @SerialName("monthly") data object Monthly : Plan

  @Serializable @SerialName("yearly") data object Yearly : Plan
}

/** A plan with the price the purchases port displays for it. */
@Serializable data class PlanOffer(val plan: Plan, val price: String)

@Serializable(with = PurchaseOutcomeSerializer::class)
sealed interface PurchaseOutcome {
  @Serializable @SerialName("purchased") data class Purchased(val plan: Plan) : PurchaseOutcome

  @Serializable @SerialName("failed") data class Failed(val reason: String) : PurchaseOutcome
}

/** One row on the home screen's list. */
@Serializable data class Item(val id: String, val title: String)

// MARK: - Ports

interface AuthPort {
  fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit)

  fun signOut(onDone: () -> Unit)
}

interface PurchasesPort {
  fun plans(onPlans: (List<PlanOffer>) -> Unit)

  fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit)
}

interface ItemsPort {
  fun items(onItems: (List<Item>) -> Unit)
}

interface AccountPort {
  fun saveDisplayName(name: String, onSaved: () -> Unit)
}
