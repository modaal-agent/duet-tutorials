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
    ))

fun main() {
  ReplayServer.serve(registry)
}
