// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import Duet
import DuetReplay
import SplashFeature

// The replay registry: one typed entry per feature. `tools/duet protocol-run`
// sends each recorded step through this server and compares the bytes, as it
// does through the Kotlin twin's runner.
public let appReplayRegistry = ReplayRegistry([
  ReplayFeature.entry("splash", SplashState.self, SplashAction.self) { state, action in
    splashReducer(state: &state, action: action)
  }
])
