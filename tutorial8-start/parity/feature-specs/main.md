# Feature spec: main

The one-page description of the main level. The recordings under
`parity/fixtures/main.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. Both tabs are mounted for the level's lifetime; the sheet
  slot mounts the upgrade flow when a tab asks for it.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `activeTab` | `MainTab` | `home` or `profile`: which mounted tab is shown. |
| `sheet` | `MainSheet?` | `upgrade` or none. The shell presents from this value in each platform's idiom: a sheet on iOS, a modal bottom sheet on Android. |

## 3. Actions

**Shell reports**

- `tabSelected(tab)`.

**Children's delegate events, received as actions**

- `home(event)` — the home tab's `upgradeRequested`.
- `profile(event)` — the profile tab's `upgradeRequested` or `signOutRequested`.
- `upgrade(event)` — the flow's `completed` or `dismissed`.

**Forwarded by the root**

- `openLink(link)` — a deep link the root forwarded down.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `tabSelected(tab)` | none | `activeTab = tab` | `[]` |
| `home(upgradeRequested)` | none | `sheet = upgrade` | `[]` |
| `profile(upgradeRequested)` | none | `sheet = upgrade` | `[]` |
| `profile(signOutRequested)` | none | none | `[notifyListener(signOutRequested)]` — relayed unchanged |
| `upgrade(completed)` / `upgrade(dismissed)` | none | `sheet = null` | `[]` — the entitlement is not this level's to write; pinned by `chain-upgrade-entitlement` |
| `openLink(upgrade)` | none | `sheet = upgrade` | `[]` |
| `openLink(profileAccount)` | none | `activeTab = profile` | `[forwardLink(profileAccount)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `notifyListener(MainDelegateEvent)` | pure environment call: the delegate sink | none |
| `forwardLink(link)` | pure environment call: the shell sends `openLink(link)` to the profile tab's store | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `signOutRequested` | the root's `main(event)` action; pinned by `chain-main-signout` |

## 7. Out of feature scope (stays app-side, per platform)

- The tab bar, and which tab's view is on screen.
- The sheet's presentation: the shell mounts the flow from `sheet` and
  presents it in the platform's idiom. The two idioms differ in manner
  only; the recordings are the same.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `main.tab-switches` | the tab follows the selection |
| `main.sign-out-relays` | the request climbs unchanged |
| `main.home-opens-the-upgrade-flow` | the home tab's request mounts the flow; its Completed clears the sheet and climbs nothing |
| `main.profile-opens-the-upgrade-flow` | the profile tab's request mounts the same slot; Dismissed clears it |
| `main.link-opens-the-upgrade-flow` | the upgrade link mounts the flow |
| `main.link-travels-to-the-profile-tab` | the account link selects the profile tab and travels on down |

Chains through here: `chain-main-signout`, `chain-upgrade-entitlement`
(the flow's Completed clears the sheet; the home tab unlocks on the stream).
