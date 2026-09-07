# Feature spec: root

The one-page description of the root level, the app's spine. The recordings
under `parity/fixtures/root.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The root reads no clock and calls two port members: the
  account port's onboarding write, and the shell's link forward.
- Two workers adopted at the root mount report the two streams into it: the
  session worker sends `authChanged` on every value of the auth port's
  `sessions`, the entitlement worker sends `entitlementChanged` on every
  value of the purchases port's `entitlements`. Both streams are sticky, so
  the first report of each arrives at mount.
- The session carries `hasOnboarded`, the backend's word on whether this
  account finished the onboarding steps; the root reads it and never
  derives it.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `phase` | `RootPhase` | `splash`, `signIn`, `onboarding` or `main`: which child the shell mounts. Exactly one child is mounted at a time. |
| `auth` | `AuthSnapshot` | `unknown`, `signedOut` or `signedIn(displayName, hasOnboarded)`. |
| `awaitingAuth` | Bool | The splash completed while `auth` was `unknown`; the next `authChanged` moves the phase. |
| `entitlement` | `Entitlement` | `free` or `premium(plan)`. Written by `entitlementChanged` only; projected to `home` and `profile` as a slice. |
| `pendingLink` | `DeepLink?` | A link that arrived before main was up; released as one `forwardLink` when main mounts. |

## 3. Actions

**Children's delegate events, received as actions**

- `splash(event)` — the splash's `completed(path)`.
- `signIn(event)` — the gate's `completed(displayName, hasOnboarded)`.
- `onboarding(event)` — the onboarding gate's `completed(name, preferences)`.
- `main(event)` — the main level's `signOutRequested`.

**Worker reports**

- `authChanged(auth)` — the session stream emitted.
- `entitlementChanged(entitlement)` — the entitlement stream emitted.

**Shell reports**

- `deepLink(link)` — the operating system handed the app `upgrade` or `profileAccount`.

## 4. Transitions

`next(auth)` below is the phase a signed-in session goes to: `main` when
`hasOnboarded`, else `onboarding`; a signed-out or unknown session goes to
`signIn`. Entering `main` with a `pendingLink` clears it and emits
`forwardLink(link)`.

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `splash(completed)` | `phase == splash`, `auth == unknown` | `awaitingAuth = true` | `[]` |
| `splash(completed)` | `phase == splash`, `auth` known | `phase = next(auth)` | `[forwardLink]` if a link was held and main is entered |
| `splash(completed)` | `phase != splash` | none | `[]` — the second completion path is inert, pinned by `root.late-splash-inert` |
| `authChanged(auth)` | `awaitingAuth`, `auth != unknown` | `auth`, `phase = next(auth)`, `awaitingAuth = false` | as above |
| `authChanged(signedOut)` | `phase` is `main` or `onboarding` | `auth`, `phase = signIn` | `[]` — the session ended under a signed-in phase (the guest expiry), pinned by `root.session-expiry-returns-to-gate` |
| `authChanged(auth)` | otherwise | `auth` | `[]` |
| `entitlementChanged(entitlement)` | none | `entitlement` | `[]` |
| `signIn(completed(name, hasOnboarded))` | `phase == signIn` | `auth = signedIn(name, hasOnboarded)`, `phase = next(auth)` | as above; pinned by `root.gate-after-splash` and `root.gate-picks-onboarding` |
| `signIn(completed(name, hasOnboarded))` | `phase != signIn` | none | `[]` — a completion after the gate is gone is inert, pinned by `root.late-sign-in-inert` |
| `onboarding(completed(name, preferences))` | `phase == onboarding` | `auth = signedIn(name, true)`, `phase = main` | `[completeOnboarding(name, preferences)]`, then `forwardLink` if a link was held |
| `main(signOutRequested)` | none | `phase = signIn`, `auth = signedOut` | `[]` |
| `deepLink(link)` | `phase == main` | none | `[forwardLink(link)]` |
| `deepLink(link)` | otherwise | `pendingLink = link` | `[]` — released when main mounts, pinned by `root.link-waits-for-main` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `forwardLink(link)` | pure environment call: the shell sends `openLink(link)` to the main level's store | none |
| `completeOnboarding(name, preferences)` | pure environment call: the account port persists the answers; the result re-enters through the session stream as `authChanged` | `LocalAccount`, and the session worker |

## 6. Delegate events

None. The root has no host.

## 7. Out of feature scope (stays app-side, per platform)

- Mounting: each shell builds the child the phase names and tears down the
  one that left, through a single-slot reconciler keyed on `phase`.
- The display name the profile tree shows is read from `auth` at the moment
  main is mounted.
- The two workers: each observes one port stream for the mount's lifetime
  and sends one action per value. They carry `WorkerTester` tests on each
  platform and no recordings; the transform from a session to an auth
  snapshot is a pure function in this module, pinned by a unit test.
- The entitlement slice: the shell publishes `entitlement` to the main
  level, which projects it into both tabs as their `entitlementChanged`.
- Deep links: each shell parses the URL it received with the ports
  module's `parseDeepLink` and sends `deepLink(link)`; the `forwardLink`
  effect can run before the shell has mounted main, so the shell holds a
  forwarded link until its main mount is published.
- The route spine: `RouteSpine` in this module is each level's route
  sliver, encoded once and decoded tolerantly; each shell saves it on
  process death and rebuilds the tree from it. The splash replays on a
  restore; the spine's `phase` says whether the slivers below it apply.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `root.splash-before-auth-holds` | the latch: a completion while `auth` is `unknown` waits; `authChanged` releases it |
| `root.gate-after-splash` | signed out: splash to gate, gate to main for an onboarded account |
| `root.gate-picks-onboarding` | the gate's completion for a new account mounts the onboarding gate |
| `root.onboarding-completes-into-main` | the onboarding gate's completion writes through the account port and mounts main |
| `root.late-splash-inert` | the safety net's completion after the ceremony's changes nothing |
| `root.late-sign-in-inert` | a sign-in completion arriving under main changes nothing |
| `root.signed-in-skips-gate` | signed in and onboarded: splash straight to main |
| `root.sign-out-returns-to-gate` | a sign-out request from under main raises the gate |
| `root.session-expiry-returns-to-gate` | the session ending while main is up raises the gate |
| `root.entitlement-changes` | the stream's value is written and nothing else moves |
| `root.link-forwards-under-main` | a link under main is forwarded at once |
| `root.link-waits-for-main` | a link during the splash is held and forwarded as main mounts |

Chains ending here: `chain-root-splash` (the splash seam),
`chain-root-signin` (the sign-in gate seam), `chain-root-onboarding` (the
onboarding gate seam), `chain-main-signout` (the sign-out climbing from
`account` through `profile` and `main`).
