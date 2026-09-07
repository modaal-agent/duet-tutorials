// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import SwiftUI

/// The home tab over the shell's view state: `appeared()` when the view
/// appears (the reducer loads once), the Insights card locked or unlocked
/// from the projected entitlement, then the list. The promo and the summary
/// replace the tab's content while `presented` names them.
public struct HomeView: View {
  @ObservedObject var viewState: HomeViewState
  let shell: HomeViewShell

  public init(viewState: HomeViewState, shell: HomeViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    Group {
      switch viewState.presented {
      case .promo:
        PromoView(viewState: viewState, shell: shell)
      case .insights:
        InsightsView(shell: shell)
      case nil:
        VStack(alignment: .leading, spacing: 0) {
          Text(localizable: .title)
            .font(.title)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
          InsightsCard(locked: viewState.isInsightsLocked) { shell.insightsTapped() }
            .padding(.horizontal, 24)
          if viewState.isLoading {
            ProgressView()
              .progressViewStyle(.linear)
              .padding(.horizontal, 24)
          }
          List(viewState.items, id: \.id) { item in
            Text(item.title)
          }
          .listStyle(.plain)
        }
      }
    }
    .onAppear { shell.appeared() }
  }
}

/// The one paid surface: a lock and "Premium" while locked, the summary's teaser once unlocked.
struct InsightsCard: View {
  let locked: Bool
  let onTap: () -> Void

  var body: some View {
    Button(action: onTap) {
      HStack {
        VStack(alignment: .leading, spacing: 4) {
          Text(localizable: .insightsTitle)
            .font(.headline)
          Text(localizable: locked ? .insightsLocked : .insightsTeaser)
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }
        Spacer()
        if locked {
          Image(systemName: "lock.fill")
        }
      }
      .padding(16)
      .background(Color.gray.opacity(0.15), in: RoundedRectangle(cornerRadius: 12))
    }
    .buttonStyle(.plain)
  }
}

/// The promo over a locked card: one button that asks the host for the upgrade flow.
struct PromoView: View {
  @ObservedObject var viewState: HomeViewState
  let shell: HomeViewShell

  var body: some View {
    VStack(spacing: 8) {
      Text(localizable: .promoTitle)
        .font(.title)
      Text(localizable: .promoBody)
        .foregroundStyle(.secondary)
      Button {
        shell.upgradeTapped()
      } label: {
        Text(localizable: .seePlans)
          .frame(maxWidth: .infinity)
      }
      .buttonStyle(.borderedProminent)
      .padding(.top, 24)
      Button(String(localized: .localizable(.notNow))) { shell.dismissed() }
    }
    .padding(.horizontal, 32)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}

/// The summary behind an unlocked card: a static screen, the same on both platforms.
struct InsightsView: View {
  let shell: HomeViewShell

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      HStack {
        Button { shell.dismissed() } label: { Image(systemName: "arrow.backward") }
          .padding(.horizontal, 8)
        Text(localizable: .insightsTitle)
          .font(.title2)
      }
      .padding(.vertical, 4)
      VStack(spacing: 0) {
        InsightRow(label: String(localized: .localizable(.itemsReadThisWeek)), value: "7")
        InsightRow(label: String(localized: .localizable(.longestStreak)), value: String(localized: .localizable(.streakDays(4))))
        InsightRow(label: String(localized: .localizable(.mostRead)), value: String(localized: .localizable(.readingList)))
      }
      .padding(.horizontal, 24)
      .padding(.vertical, 16)
      Spacer()
    }
  }
}

struct InsightRow: View {
  let label: String
  let value: String

  var body: some View {
    VStack(spacing: 0) {
      HStack {
        Text(label)
        Spacer()
        Text(value)
          .font(.headline)
      }
      .padding(.vertical, 12)
      Divider()
    }
  }
}
