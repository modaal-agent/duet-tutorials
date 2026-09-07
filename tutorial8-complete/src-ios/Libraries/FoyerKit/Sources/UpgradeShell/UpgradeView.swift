// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import SwiftUI

/// The upgrade flow over the shell's view state: one step at a time, from
/// `step`. The parent presents this view in a sheet; the sheet's own
/// dismissal reaches the flow as `dismissed()`.
public struct UpgradeView: View {
  @ObservedObject var viewState: UpgradeViewState
  let shell: UpgradeViewShell

  public init(viewState: UpgradeViewState, shell: UpgradeViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      switch viewState.step {
      case .plans:
        Text(localizable: .choosePlan)
          .font(.title2)
        ForEach(viewState.cards, id: \.key) { card in
          Button {
            shell.selectPlan(card.plan)
          } label: {
            HStack {
              Text(localizable: card.key.name)
                .font(.headline)
              Spacer()
              Text(card.price)
                .font(.headline)
            }
            .padding(16)
            .background(Color.gray.opacity(0.15), in: RoundedRectangle(cornerRadius: 12))
          }
          .buttonStyle(.plain)
        }
        Button(String(localized: .localizable(.notNow))) { shell.back() }
          .padding(.top, 8)
      case .confirm(let plan, let price):
        Text(localizable: .confirmPlan(String(localized: .localizable(plan.name))))
          .font(.title2)
        Text(localizable: plan == .monthly ? .billedMonthly(price) : .billedYearly(price))
          .foregroundStyle(.secondary)
        if let failure = viewState.failure {
          Text(localizable: failure.message)
            .foregroundStyle(.red)
        }
        Button {
          shell.confirm()
        } label: {
          Text(localizable: viewState.isPurchasing ? .purchasing : .confirm)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(viewState.isPurchasing)
        .padding(.top, 16)
        Button(String(localized: .localizable(.back))) { shell.back() }
          .disabled(viewState.isPurchasing)
      case .done:
        Text(localizable: .premiumTitle)
          .font(.title2)
        Text(localizable: .premiumBody)
          .foregroundStyle(.secondary)
        Button {
          shell.done()
        } label: {
          Text(localizable: .done)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .padding(.top, 16)
      }
      Spacer()
    }
    .padding(24)
    .onAppear { shell.appeared() }
  }
}

/// The plan's name, from its key. The view names the string; the shell never
/// projects one, so nothing downstream can compare two names.
extension PlanKey {
  var name: String.Localizable {
    switch self {
    case .monthly: .planMonthly
    case .yearly: .planYearly
    }
  }
}

/// The refusal's string, from its case: the reducer says why, the view says it.
extension PurchaseFailure {
  var message: String.Localizable {
    switch onEnum(of: self) {
    case .declined: .failureDeclined
    }
  }
}
