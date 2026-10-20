// Copyright (c) 2026 Modaal.dev
// Licensed under the MIT License. See LICENSE file for details.

import DuetReplay
import ReplayAdapter

// The replay-protocol endpoint: the framework's stdio server over the
// registry. No app links this executable.
ReplayServer.serve(registry: appReplayRegistry)
