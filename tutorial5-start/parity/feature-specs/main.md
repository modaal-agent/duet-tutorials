# Feature spec: main

The one-page description of the main level. The recordings under
`parity/fixtures/main.*` win any disagreement with this prose.

## 1. Identity & config

- No constants.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `activeTab` | `MainTab` | `home` or `profile`. Both children are mounted for the level's lifetime; the tab chooses which one is shown. |

## 3. Actions

- `tabSelected(tab)` — shell report.
- `profile(event)` — the profile tab's delegate events, received as actions.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `tabSelected(tab)` | none | `activeTab = tab` | `[]` |
| `profile(signOutRequested)` | none | none | `[notifyListener(signOutRequested)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `notifyListener(MainDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `signOutRequested` | the root's `main(event)` action; the third hop of `chain-main-signout` |

## 7. Out of feature scope (stays app-side, per platform)

- The tab bar and the two children's screens. The home tab has no delegate
  surface yet; Tutorial 5 adds `upgradeRequested`.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `main.tab-switches` | the tab value, and that switching emits nothing |
| `main.sign-out-relays` | the request climbs unchanged |
