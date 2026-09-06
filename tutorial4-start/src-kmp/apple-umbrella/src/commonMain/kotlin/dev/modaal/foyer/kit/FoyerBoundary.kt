// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.kit

import dev.modaal.duet.replay.BoundaryReplay
import dev.modaal.duet.replay.ReplayFeature
import dev.modaal.duet.replay.ReplayRegistry
import dev.modaal.duet.replay.ReplaySession
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

// The replay registry on the Apple side, the twin of replay-runner's: one
// entry per feature, naming the same four declarations the Kotlin lane
// replays. It is also what gives the framework a source file to compile; a
// framework over an empty commonMain produces no XCFramework at all.
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

/**
 * The surface the Swift replay suite drives across the framework: canonicalize
 * fixture JSON through the core's own writer, and open a replay session over
 * a registered feature. Swift loads the files, threads the steps and compares
 * bytes; every canonical byte on both sides is produced here.
 */
object FoyerBoundary {
  // `@Throws` is the error channel. A Kotlin exception that crosses
  // Kotlin/Native without it terminates the process instead of surfacing as
  // a Swift error, so every throwing path exported to Swift carries it.
  @Throws(IllegalArgumentException::class)
  fun canonicalize(rawJson: String): String = BoundaryReplay.canonicalize(rawJson)

  @Throws(IllegalArgumentException::class)
  fun makeSession(feature: String, initialStateJson: String): ReplaySession =
    BoundaryReplay.makeSession(registry, feature, initialStateJson)
}
