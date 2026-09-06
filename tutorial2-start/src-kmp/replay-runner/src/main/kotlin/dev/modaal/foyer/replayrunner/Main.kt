// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

package dev.modaal.foyer.replayrunner

import dev.modaal.duet.replay.ReplayFeature
import dev.modaal.duet.replay.ReplayRegistry
import dev.modaal.duet.replay.ReplayServer
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
    ))

fun main() {
  ReplayServer.serve(registry)
}
