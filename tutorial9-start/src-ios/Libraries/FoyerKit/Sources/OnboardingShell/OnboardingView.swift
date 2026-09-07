// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

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
        Text("Step \(viewState.stepNumber) of \(viewState.stepCount)")
          .font(.subheadline.weight(.semibold))
        Spacer()
        ForEach(1...viewState.stepCount, id: \.self) { number in
          Image(systemName: viewState.readySteps.contains(number) ? "checkmark.circle.fill" : "circle")
            .foregroundStyle(viewState.readySteps.contains(number) ? Color.accentColor : Color.secondary)
            .accessibilityLabel(viewState.readySteps.contains(number) ? "Step \(number) ready" : "Step \(number) not ready")
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
      Text("Welcome to Foyer")
        .font(.title)
      Text("Three short steps and you are in.")
        .foregroundStyle(.secondary)
      Button {
        shell.continueTapped()
      } label: {
        Text("Continue")
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
      Text("What should we call you?")
        .font(.title)
      TextField(
        "Display name",
        text: Binding(get: { viewState.draft }, set: { shell.draftChanged($0) })
      )
      .textFieldStyle(.roundedBorder)
      if let validation = viewState.validation {
        Text(validation)
          .font(.callout)
          .foregroundStyle(.red)
      }
      Button {
        shell.continueTapped()
      } label: {
        Text("Continue")
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
      Text("Pick at least one")
        .font(.title)
      ForEach(viewState.rows, id: \.key) { row in
        HStack {
          Text(row.label)
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
        Text("Finish")
          .frame(maxWidth: .infinity)
      }
      .buttonStyle(.borderedProminent)
      .disabled(!viewState.isReady)
    }
  }
}
