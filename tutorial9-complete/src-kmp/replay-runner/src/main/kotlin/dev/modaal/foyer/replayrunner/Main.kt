// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.replayrunner

import dev.modaal.duet.replay.ReplayFeature
import dev.modaal.duet.replay.ReplayRegistry
import dev.modaal.duet.replay.ReplayServer
import dev.modaal.foyer.account.AccountActionSerializer
import dev.modaal.foyer.account.AccountEffectPayloadSerializer
import dev.modaal.foyer.account.AccountState
import dev.modaal.foyer.account.accountReducer
import dev.modaal.foyer.editname.EditNameActionSerializer
import dev.modaal.foyer.editname.EditNameEffectPayloadSerializer
import dev.modaal.foyer.editname.EditNameState
import dev.modaal.foyer.editname.editNameReducer
import dev.modaal.foyer.home.HomeActionSerializer
import dev.modaal.foyer.home.HomeEffectPayloadSerializer
import dev.modaal.foyer.home.HomeState
import dev.modaal.foyer.home.homeReducer
import dev.modaal.foyer.main.MainActionSerializer
import dev.modaal.foyer.main.MainEffectPayloadSerializer
import dev.modaal.foyer.main.MainState
import dev.modaal.foyer.main.mainReducer
import dev.modaal.foyer.name.NameActionSerializer
import dev.modaal.foyer.name.NameEffectPayloadSerializer
import dev.modaal.foyer.name.NameState
import dev.modaal.foyer.name.nameReducer
import dev.modaal.foyer.onboarding.OnboardingActionSerializer
import dev.modaal.foyer.onboarding.OnboardingEffectPayloadSerializer
import dev.modaal.foyer.onboarding.OnboardingState
import dev.modaal.foyer.onboarding.onboardingReducer
import dev.modaal.foyer.preferences.PreferencesActionSerializer
import dev.modaal.foyer.preferences.PreferencesEffectPayloadSerializer
import dev.modaal.foyer.preferences.PreferencesState
import dev.modaal.foyer.preferences.preferencesReducer
import dev.modaal.foyer.progress.ProgressActionSerializer
import dev.modaal.foyer.progress.ProgressEffectPayloadSerializer
import dev.modaal.foyer.progress.ProgressState
import dev.modaal.foyer.progress.progressReducer
import dev.modaal.foyer.profile.ProfileActionSerializer
import dev.modaal.foyer.profile.ProfileEffectPayloadSerializer
import dev.modaal.foyer.profile.ProfileState
import dev.modaal.foyer.profile.profileReducer
import dev.modaal.foyer.root.RootActionSerializer
import dev.modaal.foyer.root.RootEffectPayloadSerializer
import dev.modaal.foyer.root.RootState
import dev.modaal.foyer.root.rootReducer
import dev.modaal.foyer.signin.SignInActionSerializer
import dev.modaal.foyer.signin.SignInEffectPayloadSerializer
import dev.modaal.foyer.signin.SignInState
import dev.modaal.foyer.signin.signInReducer
import dev.modaal.foyer.splash.SplashActionSerializer
import dev.modaal.foyer.splash.SplashEffectPayloadSerializer
import dev.modaal.foyer.splash.SplashState
import dev.modaal.foyer.splash.splashReducer
import dev.modaal.foyer.upgrade.UpgradeActionSerializer
import dev.modaal.foyer.upgrade.UpgradeEffectPayloadSerializer
import dev.modaal.foyer.upgrade.UpgradeState
import dev.modaal.foyer.upgrade.upgradeReducer
import dev.modaal.foyer.welcome.WelcomeActionSerializer
import dev.modaal.foyer.welcome.WelcomeEffectPayloadSerializer
import dev.modaal.foyer.welcome.WelcomeState
import dev.modaal.foyer.welcome.welcomeReducer

// The replay registry: one typed entry per feature. `tools/duet protocol-run`
// sends each recorded step through this server and compares the bytes.
private val registry =
  ReplayRegistry(
    listOf<ReplayFeature>(
      ReplayFeature.entry(
        "splash",
        SplashState.serializer(),
        SplashActionSerializer,
        SplashEffectPayloadSerializer,
        ::splashReducer),
      ReplayFeature.entry(
        "root",
        RootState.serializer(),
        RootActionSerializer,
        RootEffectPayloadSerializer,
        ::rootReducer),
      ReplayFeature.entry(
        "signin",
        SignInState.serializer(),
        SignInActionSerializer,
        SignInEffectPayloadSerializer,
        ::signInReducer),
      ReplayFeature.entry(
        "main",
        MainState.serializer(),
        MainActionSerializer,
        MainEffectPayloadSerializer,
        ::mainReducer),
      ReplayFeature.entry(
        "home",
        HomeState.serializer(),
        HomeActionSerializer,
        HomeEffectPayloadSerializer,
        ::homeReducer),
      ReplayFeature.entry(
        "profile",
        ProfileState.serializer(),
        ProfileActionSerializer,
        ProfileEffectPayloadSerializer,
        ::profileReducer),
      ReplayFeature.entry(
        "account",
        AccountState.serializer(),
        AccountActionSerializer,
        AccountEffectPayloadSerializer,
        ::accountReducer),
      ReplayFeature.entry(
        "editname",
        EditNameState.serializer(),
        EditNameActionSerializer,
        EditNameEffectPayloadSerializer,
        ::editNameReducer),
      ReplayFeature.entry(
        "upgrade",
        UpgradeState.serializer(),
        UpgradeActionSerializer,
        UpgradeEffectPayloadSerializer,
        ::upgradeReducer),
      ReplayFeature.entry(
        "onboarding",
        OnboardingState.serializer(),
        OnboardingActionSerializer,
        OnboardingEffectPayloadSerializer,
        ::onboardingReducer),
      ReplayFeature.entry(
        "welcome",
        WelcomeState.serializer(),
        WelcomeActionSerializer,
        WelcomeEffectPayloadSerializer,
        ::welcomeReducer),
      ReplayFeature.entry(
        "name",
        NameState.serializer(),
        NameActionSerializer,
        NameEffectPayloadSerializer,
        ::nameReducer),
      ReplayFeature.entry(
        "preferences",
        PreferencesState.serializer(),
        PreferencesActionSerializer,
        PreferencesEffectPayloadSerializer,
        ::preferencesReducer),
      ReplayFeature.entry(
        "progress",
        ProgressState.serializer(),
        ProgressActionSerializer,
        ProgressEffectPayloadSerializer,
        ::progressReducer),
    ))

fun main() {
  ReplayServer.serve(registry)
}
