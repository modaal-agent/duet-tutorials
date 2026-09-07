# Feature spec: account

The one-page description of the account screen, level 2 of the profile
tree. The recordings under `parity/fixtures/account.*` win any disagreement
with this prose.

## 1. Identity & config

- No constants.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `displayName` | String | Seeded at mount by the parent. |
| `child` | `AccountRoute?` | `editName` when the editor is mounted, else none. |
| `isSigningOut` | Bool | The in-flight latch: one sign-out at a time. |

## 3. Actions

**Shell reports**

- `editNameTapped`, `signOutTapped`, `closeTapped`.

**Environment reports**

- `signedOut` — the auth port ended the session.

**The child's delegate events, received as actions**

- `editName(event)` — the editor's `saved(name)` or `closed`.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `editNameTapped` | none | `child = editName` | `[]` |
| `editName(saved(name))` | none | `displayName = name`, `child = null` | `[notifyListener(nameChanged(name))]` |
| `editName(closed)` | none | `child = null` | `[]` |
| `signOutTapped` | `!isSigningOut` | `isSigningOut = true` | `[signOut]` |
| `signOutTapped` | `isSigningOut` | none | `[]` |
| `signedOut` | none | `isSigningOut = false` | `[notifyListener(signOutRequested)]` |
| `closeTapped` | none | none | `[notifyListener(closed)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `signOut` | fire-and-forget bridge: the auth port's one callback re-enters as `signedOut` | the auth port |
| `notifyListener(AccountDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `closed` | the profile tab's `account(event)`, which clears its child |
| `nameChanged(name)` | the profile tab's `account(event)`; the second hop of `chain-profile-editname` |
| `signOutRequested` | the profile tab's `account(event)`; the first hop of `chain-main-signout` |

## 7. Out of feature scope (stays app-side, per platform)

- Mounting the editor from `child`, and the screen's rendering.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `account.edit-name-saves-and-climbs` | the editor mounts; `saved` is taken and climbs as `nameChanged` |
| `account.edit-name-cancels` | `closed` dismisses the editor, the name untouched |
| `account.sign-out-requests-once` | one port call, the latch, the climb on confirmation |
| `account.close-climbs` | `closeTapped` climbs as `closed` |
