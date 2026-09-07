// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

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
        Text("Choose a plan")
          .font(.title2)
        ForEach(viewState.cards, id: \.name) { card in
          Button {
            shell.selectPlan(card.plan)
          } label: {
            HStack {
              Text(card.name)
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
        Button("Not now") { shell.back() }
          .padding(.top, 8)
      case .confirm(let planName, let price):
        Text("Confirm \(planName)")
          .font(.title2)
        Text("\(price), billed \(planName == "Monthly" ? "every month" : "once a year").")
          .foregroundStyle(.secondary)
        if let failure = viewState.failure {
          Text(failure)
            .foregroundStyle(.red)
        }
        Button {
          shell.confirm()
        } label: {
          Text(viewState.isPurchasing ? "Purchasing…" : "Confirm")
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(viewState.isPurchasing)
        .padding(.top, 16)
        Button("Back") { shell.back() }
          .disabled(viewState.isPurchasing)
      case .done:
        Text("You're Premium")
          .font(.title2)
        Text("Insights is unlocked.")
          .foregroundStyle(.secondary)
        Button {
          shell.done()
        } label: {
          Text("Done")
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
