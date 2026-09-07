// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The four ports. Every operation starts the work and returns; the result
// re-enters through the callback, which is what lets a Swift class implement
// the same interface across the Apple boundary (a `suspend` member cannot be
// implemented from Swift). Each port's callback fires exactly once per call.
// Two ports also carry a stream: a sticky `StateFlow` whose current value is
// the port's answer to "what is true now", observed by a worker for the
// mount's lifetime and never by a feature directly.

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
  /**
   * Signed in; `displayName` is the account's saved name when it has one, and
   * `hasOnboarded` whether this account finished the onboarding steps.
   */
  @Serializable
  @SerialName("signedIn")
  data class SignedIn(val displayName: String?, val hasOnboarded: Boolean) : SignInOutcome

  @Serializable @SerialName("failed") data class Failed(val reason: SignInFailure) : SignInOutcome
}

/**
 * Why a sign-in was refused. A value, not a message: the shells name the
 * string in the user's language, and the recordings carry the case.
 */
@Serializable(with = SignInFailureSerializer::class)
sealed interface SignInFailure {
  /** The gate refuses an empty address before it reaches the port. */
  @Serializable @SerialName("emptyAddress") data object EmptyAddress : SignInFailure

  /** The port knows no account for the address. */
  @Serializable @SerialName("noAccount") data object NoAccount : SignInFailure
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

  @Serializable @SerialName("failed") data class Failed(val reason: PurchaseFailure) : PurchaseOutcome
}

/** Why a purchase was refused; the shells name the string. */
@Serializable(with = PurchaseFailureSerializer::class)
sealed interface PurchaseFailure {
  @Serializable @SerialName("declined") data object Declined : PurchaseFailure
}

/** One row on the home screen's list. */
@Serializable data class Item(val id: String, val title: String)

/** What the auth port's `sessions` stream carries: the session as it is now. */
sealed interface Session {
  data object SignedOut : Session

  /**
   * `displayName` is the account's saved name, or the default derived from the
   * provider; `hasOnboarded` is whether the account finished the onboarding
   * steps, the fact the root's onboarding gate reads.
   */
  data class SignedIn(val displayName: String, val hasOnboarded: Boolean) : Session
}

/** What the purchases port's `entitlements` stream carries; the paid check reads this value. */
@Serializable(with = EntitlementSerializer::class)
sealed interface Entitlement {
  @Serializable @SerialName("free") data object Free : Entitlement

  @Serializable @SerialName("premium") data class Premium(val plan: Plan) : Entitlement
}

/** The name the app shows when the account has none saved: the email's local part, or "Guest". */
fun defaultDisplayName(provider: SignInProvider?): String =
  when (provider) {
    is SignInProvider.Email -> provider.address.substringBefore('@')
    SignInProvider.Guest, null -> "Guest"
  }

// MARK: - Ports

interface AuthPort {
  /** The session, sticky: a late subscriber sees the current value first. */
  val sessions: StateFlow<Session>

  fun signIn(provider: SignInProvider, onOutcome: (SignInOutcome) -> Unit)

  fun signOut(onDone: () -> Unit)
}

interface PurchasesPort {
  /** The entitlement, sticky; the only writer of the value the paid check reads. */
  val entitlements: StateFlow<Entitlement>

  fun plans(onPlans: (List<PlanOffer>) -> Unit)

  fun purchase(plan: Plan, onOutcome: (PurchaseOutcome) -> Unit)
}

interface ItemsPort {
  fun items(onItems: (List<Item>) -> Unit)
}

interface AccountPort {
  fun saveDisplayName(name: String, onSaved: () -> Unit)

  /**
   * The onboarding steps finished: persist the name and the preferences and
   * mark the account onboarded. No callback: the session stream carries the
   * result, as it carries a saved name.
   */
  fun completeOnboarding(name: String, preferences: List<String>)
}
