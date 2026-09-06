// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.ports

import dev.modaal.duet.kernel.serialization.CanonicalSumSerializer

// The canonical coding of the ports' sum types. They appear inside feature
// state and actions, so the recordings carry them.

object SignInProviderSerializer :
  CanonicalSumSerializer<SignInProvider>(
    "SignInProvider",
    listOf(
      case(SignInProvider.Email::class, SignInProvider.Email.serializer()),
      case(SignInProvider.Guest::class, SignInProvider.Guest.serializer()),
    ))

object SignInOutcomeSerializer :
  CanonicalSumSerializer<SignInOutcome>(
    "SignInOutcome",
    listOf(
      case(SignInOutcome.SignedIn::class, SignInOutcome.SignedIn.serializer()),
      case(SignInOutcome.Failed::class, SignInOutcome.Failed.serializer()),
    ))

object PlanSerializer :
  CanonicalSumSerializer<Plan>(
    "Plan",
    listOf(
      case(Plan.Monthly::class, Plan.Monthly.serializer()),
      case(Plan.Yearly::class, Plan.Yearly.serializer()),
    ))

object PurchaseOutcomeSerializer :
  CanonicalSumSerializer<PurchaseOutcome>(
    "PurchaseOutcome",
    listOf(
      case(PurchaseOutcome.Purchased::class, PurchaseOutcome.Purchased.serializer()),
      case(PurchaseOutcome.Failed::class, PurchaseOutcome.Failed.serializer()),
    ))
