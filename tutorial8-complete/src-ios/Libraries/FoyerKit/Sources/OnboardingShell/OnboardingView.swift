// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import FoyerKit
import SwiftUI

/// The onboarding level over the shell's view state: the progress row, then
/// the step the page names. Back is an intent of the level.
public struct OnboardingView: View {
  @ObservedObject var viewState: OnboardingViewState
  let shell: OnboardingViewShell

  public init(viewState: OnboardingViewState, shell: OnboardingViewShell) {
    self.viewState = viewState
    self.shell = shell
  }

  public var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      HStack {
        if viewState.canGoBack {
          Button { shell.back() } label: { Image(systemName: "arrow.backward") }
        }
        Spacer()
      }
      .frame(height: 32)
      ProgressRowView(viewState: shell.progress.shell.viewState, shell: shell.progress.shell)
      Spacer().frame(height: 32)
      switch viewState.step {
      case .welcome(let child):
        WelcomeStepView(shell: child.shell)
      case .name(let child):
        NameStepView(viewState: child.shell.viewState, shell: child.shell)
      case .preferences(let child):
        PreferencesStepView(viewState: child.shell.viewState, shell: child.shell)
      case nil:
        EmptyView()
      }
      Spacer()
    }
    .padding(24)
  }
}

/// "Step n of 3" from the projected page, and one tick per step the seam reported ready.
struct ProgressRowView: View {
  @ObservedObject var viewState: ProgressViewState
  let shell: ProgressViewShell

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      HStack {
        Text(localizable: .stepOf(viewState.stepNumber, viewState.stepCount))
          .font(.subheadline.weight(.semibold))
        Spacer()
        ForEach(1...viewState.stepCount, id: \.self) { number in
          Image(systemName: viewState.readySteps.contains(number) ? "checkmark.circle.fill" : "circle")
            .foregroundStyle(viewState.readySteps.contains(number) ? Color.accentColor : Color.secondary)
            .accessibilityLabel(
              String(localized: .localizable(viewState.readySteps.contains(number) ? .stepReady(number) : .stepNotReady(number))))
        }
      }
      ProgressView(value: Double(viewState.stepNumber), total: Double(viewState.stepCount))
    }
    .onAppear { shell.appeared() }
  }
}

struct WelcomeStepView: View {
  let shell: WelcomeViewShell

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text(localizable: .welcomeTitle)
        .font(.title)
      Text(localizable: .welcomeBody)
        .foregroundStyle(.secondary)
      Button {
        shell.continueTapped()
      } label: {
        Text(localizable: .continueAction)
          .frame(maxWidth: .infinity)
      }
      .buttonStyle(.borderedProminent)
      .padding(.top, 24)
    }
    .onAppear { shell.appeared() }
  }
}

struct NameStepView: View {
  @ObservedObject var viewState: NameViewState
  let shell: NameViewShell

  var body: some View {
    VStack(alignment: .leading, spacing: 16) {
      Text(localizable: .nameTitle)
        .font(.title)
      TextField(
        String(localized: .localizable(.displayNamePlaceholder)),
        text: Binding(get: { viewState.draft }, set: { shell.draftChanged($0) })
      )
      .textFieldStyle(.roundedBorder)
      if let validation = viewState.validation {
        Text(localizable: validation.message)
          .font(.callout)
          .foregroundStyle(.red)
      }
      Button {
        shell.continueTapped()
      } label: {
        Text(localizable: .continueAction)
          .frame(maxWidth: .infinity)
      }
      .buttonStyle(.borderedProminent)
    }
  }
}

struct PreferencesStepView: View {
  @ObservedObject var viewState: PreferencesViewState
  let shell: PreferencesViewShell

  var body: some View {
    VStack(alignment: .leading, spacing: 16) {
      Text(localizable: .preferencesTitle)
        .font(.title)
      ForEach(viewState.rows, id: \.key) { row in
        HStack {
          label(for: row.key)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture { shell.toggle(row.key) }
          Toggle("", isOn: Binding(get: { row.isOn }, set: { _ in shell.toggle(row.key) }))
            .labelsHidden()
        }
      }
      Button {
        shell.continueTapped()
      } label: {
        Text(localizable: .finish)
          .frame(maxWidth: .infinity)
      }
      .buttonStyle(.borderedProminent)
      .disabled(!viewState.isReady)
    }
  }

  /// The toggle's label, from its key; a key the catalog does not name shows as itself.
  private func label(for key: String) -> Text {
    switch key {
    case "digest": Text(localizable: .preferenceDigest)
    case "reminders": Text(localizable: .preferenceReminders)
    case "tips": Text(localizable: .preferenceTips)
    default: Text(key)
    }
  }
}

/// The refusal's string, from its case: the reducer says why, the view says it.
extension NameValidation {
  var message: String.Localizable {
    switch onEnum(of: self) {
    case .empty: .validationEmpty
    case .tooLong: .validationTooLong
    }
  }
}
