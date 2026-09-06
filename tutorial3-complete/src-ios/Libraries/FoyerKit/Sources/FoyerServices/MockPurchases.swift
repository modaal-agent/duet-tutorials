// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import Foundation

/// The purchases port as a mock service: two plans with fixed prices, and a
/// purchase that succeeds at once. Nothing consumes it yet; Tutorial 4's
/// entitlement stream and Tutorial 5's upgrade flow do.
public final class MockPurchases: NSObject, PurchasesPort {
  public override init() {}

  public func plans(onPlans: @escaping ([PlanOffer]) -> Void) {
    onPlans([
      PlanOffer(plan: PlanMonthly.shared, price: "$4.99"),
      PlanOffer(plan: PlanYearly.shared, price: "$39.99"),
    ])
  }

  public func purchase(plan: Plan, onOutcome: @escaping (PurchaseOutcome) -> Void) {
    onOutcome(PurchaseOutcomePurchased(plan: plan))
  }
}
