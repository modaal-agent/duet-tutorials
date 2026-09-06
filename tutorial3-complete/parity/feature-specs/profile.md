# Feature spec: profile

The one-page description of the profile tab, level 1 of the profile tree.
The recordings under `parity/fixtures/profile.*` win any disagreement with
this prose.

## 1. Identity & config

- No constants. The display name is seeded at mount from the root's auth
  snapshot; from Tutorial 4 the session stream carries it.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `displayName` | String | The header's name. |
| `child` | `ProfileRoute?` | `account` when the account screen is mounted, else none. The shell mounts from this value. |

## 3. Actions

- `accountTapped` — shell report: the Account row.
- `account(event)` — the account screen's delegate events, received as actions.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `accountTapped` | none | `child = account` | `[]` |
| `account(closed)` | none | `child = null` | `[]` |
| `account(nameChanged(name))` | none | `displayName = name` | `[]` |
| `account(signOutRequested)` | none | none | `[notifyListener(signOutRequested)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `notifyListener(ProfileDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `signOutRequested` | the main level's `profile(event)` action; the second hop of `chain-main-signout` |

The `nameChanged` climb from the account screen ends here: the header
updates and nothing climbs further, pinned by `chain-profile-editname`.

## 7. Out of feature scope (stays app-side, per platform)

- Mounting the account screen from `child`, and the tab's rendering.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `profile.account-closes` | the row mounts, `closed` dismisses |
| `profile.name-change-updates-header` | the header takes the new name |
| `profile.sign-out-relays` | the request climbs unchanged |
