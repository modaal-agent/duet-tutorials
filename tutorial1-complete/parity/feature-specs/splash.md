# Feature spec: splash

The one-page, platform-agnostic description of the splash feature. The
recordings under `parity/fixtures/splash.*` win any disagreement with this
prose, and every recording listed in the manifest is named here in backticks
(`tools/duet verify` checks that).

## 1. Identity & config

- `SplashConfig.SAFETY_NET_MILLIS` — 3000 ms, a module constant. The reducer
  carries the value out in the `armSafetyNet` payload, so the recordings pin
  it and a retiming shows up as a fixture diff.
- No identity is minted and the reducer reads no clock. The wait runs in the
  environment and re-enters as an action; the reducer only decides to arm it.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `isArmed` | Bool | The arming latch: the safety net is armed once per mount. Guards the inert repeat `appeared`. |

## 3. Actions

**Shell reports**

- `appeared` — the splash is on screen and the safety net should arm.
- `ceremonyFinished` — the splash animation reached its end.

**Environment reports**

- `safetyNetElapsed` — the armed delay elapsed.

The splash owns no presentation slot: the host mounts it and swaps it out on
`completed`, so its removal is the host's action, not this feature's.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | `!isArmed` | `isArmed = true` | `[armSafetyNet(3000)]` under id `splash.safetyNet` |
| `appeared` | `isArmed` (repeat) | none | `[]` — deliberately inert, pinned by `splash.repeat-appear-inert` |
| `ceremonyFinished` | none | none | `[notifyHost(completed(ceremony))]` |
| `safetyNetElapsed` | none | none | `[notifyHost(completed(safetyNet))]` |

There is no completion latch. Both paths notify the host every time they
fire, including after the other path already has; the host treats a repeat
`completed` as a no-op. For the same reason `ceremonyFinished` does not cancel
the armed net: the timer runs to completion and reports, and the host absorbs
the second notification.

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `armSafetyNet(afterMillis)` | keyed clock: one one-shot timer under id `splash.safetyNet`, so a re-arm cancels an in-flight one and teardown cancels it outright | none — the effect handler sleeps on the environment's clock and emits `safetyNetElapsed` |
| `notifyHost(SplashDelegateEvent)` | pure environment call: the delegate sink, no id | none |

The timer's temporal contract (fires after the duration and not before,
exactly once, never after teardown) is a property of the effect handler and
its clock, not of the reducer. It is tested in `SplashTestStoreTest` on the
test scheduler's virtual clock; no recording asserts elapsed time.

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `completed(path)` with `path` one of `ceremony`, `safetyNet` | the host's one completion sink; both cases land on it |

Two cases rather than one `finished`, because the paths mean different
things: the animation completed, or the animation stalled and the net
completed the splash instead. A host that never distinguishes them ignores
`path`.

No chain fixture yet: the host is the `root` feature, which Tutorial 3 adds,
and the hop is pinned there.

## 7. Out of feature scope (stays app-side, per platform)

- The splash animation itself: each app draws its own and reports
  `ceremonyFinished` when it ends.
- The mount bracket: each shell builds a store when the splash is mounted
  and tears it down when it leaves, which cancels an armed net mechanically.

## 8. Behavior recordings

Four leaves over one given (a fresh store, then `appeared`):

| Recording | Pins |
| --- | --- |
| `splash.ceremony-completes` | `appeared` arms the net; `ceremonyFinished` notifies `completed(ceremony)` |
| `splash.safety-net-fires` | `safetyNetElapsed` notifies `completed(safetyNet)`. Mutually exclusive with the row above, so it is a branch over the same given rather than a scenario of its own |
| `splash.both-paths-notify-twice` | the deliberate non-latch: `ceremonyFinished` then `safetyNetElapsed`, two notifications |
| `splash.repeat-appear-inert` | the arming guard: a second `appeared` writes nothing and emits nothing |
