# Feature spec: root

The one-page description of the root level, the app's spine. The recordings
under `parity/fixtures/root.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The root reads no clock and calls no port: it routes.
- The auth snapshot is seeded by the host at mount (`authChanged`); from
  Tutorial 4 the session worker sends the same action on every change.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `phase` | `RootPhase` | `splash`, `signIn` or `main`: which child the shell mounts. Exactly one child is mounted at a time. |
| `auth` | `AuthSnapshot` | `unknown`, `signedOut` or `signedIn(displayName)`. |
| `awaitingAuth` | Bool | The splash completed while `auth` was `unknown`; the next `authChanged` moves the phase. |

## 3. Actions

**Children's delegate events, received as actions**

- `splash(event)` — the splash's `completed(path)`.
- `signIn(event)` — the gate's `completed(displayName)`.
- `main(event)` — the main level's `signOutRequested`.

**Host reports**

- `authChanged(auth)` — the session changed.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `splash(completed)` | `phase == splash`, `auth == unknown` | `awaitingAuth = true` | `[]` |
| `splash(completed)` | `phase == splash`, `auth` known | `phase = main` if signed in, else `signIn` | `[]` |
| `splash(completed)` | `phase != splash` | none | `[]` — the second completion path is inert, pinned by `root.late-splash-inert` |
| `authChanged(auth)` | `awaitingAuth`, `auth != unknown` | `auth`, `phase` as above, `awaitingAuth = false` | `[]` |
| `authChanged(auth)` | otherwise | `auth` | `[]` |
| `signIn(completed(name))` | `phase == signIn` | `phase = main`, `auth = signedIn(name)` | `[]` |
| `main(signOutRequested)` | none | `phase = signIn`, `auth = signedOut` | `[]` |

## 5. Effects

None. `RootEffectPayload` has no cases yet; Tutorial 5 adds the deep-link
forward.

## 6. Delegate events

None. The root has no host.

## 7. Out of feature scope (stays app-side, per platform)

- Mounting: each shell builds the child the phase names and tears down the
  one that left, through a single-slot reconciler keyed on `phase`.
- The display name the profile tree shows is read from `auth` at the moment
  main is mounted.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `root.splash-before-auth-holds` | the latch: a completion while `auth` is `unknown` waits; `authChanged` releases it |
| `root.gate-after-splash` | signed out: splash to gate, gate to main with the session signed in |
| `root.late-splash-inert` | the safety net's completion after the ceremony's changes nothing |
| `root.signed-in-skips-gate` | signed in: splash straight to main |
| `root.sign-out-returns-to-gate` | a sign-out request from under main raises the gate |

Chains ending here: `chain-root-splash` (the splash seam),
`chain-root-signin` (the gate seam), `chain-main-signout` (the sign-out
climbing from `account` through `profile` and `main`).
