// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Combine
import DuetShells
import FoyerBridge
import FoyerKit
import Foundation

public typealias UpgradeKitStore = BridgedStore<UpgradeState, any UpgradeAction>

/// The step, as the view switches on it.
public enum UpgradeStepKey: Equatable {
  case plans
  case confirm(planName: String, price: String)
  case done
}

/// One plan card.
public struct PlanCard {
  public let plan: Plan
  public let name: String
  public let price: String
}

@MainActor
public final class UpgradeViewState: ObservableObject {
  @Published public internal(set) var step: UpgradeStepKey = .plans
  @Published public internal(set) var cards: [PlanCard] = []
  @Published public internal(set) var isPurchasing = false
  @Published public internal(set) var failure: String?

  public init() {}
}

/// The flow's shell: the intents from every step, and the step projection.
/// Presentation is the parent's: the main level mounts this child from its
/// `sheet` value and shows it in the platform's idiom.
public final class UpgradeViewShell: ViewShell {
  public let viewState = UpgradeViewState()
  public let store: UpgradeKitStore

  public init(store: UpgradeKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    host.adopt(store)
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents

  public func appeared() { store.send(UpgradeActionAppeared.shared) }

  public func selectPlan(_ plan: Plan) { store.send(UpgradeActionPlanSelected(plan: plan)) }

  public func confirm() { store.send(UpgradeActionConfirmTapped.shared) }

  public func back() { store.send(UpgradeActionBack.shared) }

  public func done() { store.send(UpgradeActionDoneTapped.shared) }

  /// The sheet was dismissed from outside the flow: a swipe, the close button.
  public func dismissed() { store.send(UpgradeActionDismissTapped.shared) }

  // MARK: - State to view state

  private func apply(_ state: UpgradeState) {
    viewState.cards = state.offers.map { offer in
      PlanCard(plan: offer.plan, name: planName(offer.plan), price: offer.price)
    }
    viewState.step =
      switch onEnum(of: state.step) {
      case .plans: .plans
      case .confirm(let confirm):
        .confirm(
          planName: planName(confirm.plan),
          price: state.offers.first { ($0.plan as AnyObject).isEqual(confirm.plan) }?.price ?? "")
      case .done: .done
      }
    viewState.isPurchasing = state.isPurchasing
    viewState.failure = state.failure
  }

  private func planName(_ plan: Plan) -> String {
    switch onEnum(of: plan) {
    case .monthly: "Monthly"
    case .yearly: "Yearly"
    }
  }
}
