# Feature spec: signin

The one-page description of the sign-in gate. The recordings under
`parity/fixtures/signin.*` win any disagreement with this prose.

## 1. Identity & config

- `SignInMessages.EMPTY_ADDRESS` — the message for an empty email address.
- The display name the gate reports is the account's saved one when the
  auth port returns it, else derived: the email's local part, or `Guest`.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `isSigningIn` | Bool | The in-flight latch: one sign-in at a time. |
| `failure` | String? | The message to show; cleared by the next attempt. |
| `pending` | `SignInProvider?` | The provider in flight, for the derived name. |

## 3. Actions

**Shell reports**

- `continueTapped(provider)` — `email(address)` or `guest`.

**Environment reports**

- `signInFinished(outcome)` — `signedIn(displayName?)` or `failed(reason)`.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `continueTapped(p)` | `isSigningIn` | none | `[]` |
| `continueTapped(email(""))` | address blank | `failure = EMPTY_ADDRESS` | `[]` — the port is never called |
| `continueTapped(p)` | otherwise | `isSigningIn = true`, `failure = null`, `pending = p` | `[signIn(p)]` |
| `signInFinished(signedIn(name?))` | none | `isSigningIn = false`, `pending = null` | `[notifyListener(completed(name ?? derived))]` |
| `signInFinished(failed(reason))` | none | `isSigningIn = false`, `pending = null`, `failure = reason` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `signIn(provider)` | fire-and-forget bridge: the auth port's one callback re-enters as `signInFinished` | the auth port (`MockAuth` in Tutorial 3, `LocalAuth` from Tutorial 4) |
| `notifyListener(SignInDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `completed(displayName)` | the root's `signIn(event)` action; pinned by `chain-root-signin` |

## 7. Out of feature scope (stays app-side, per platform)

- The email field's live text: the shell sends it with `continueTapped`.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `signin.email-signs-in` | the sign-in call, the in-flight latch, the derived name `ann` from `ann@example.com` |
| `signin.guest-signs-in` | the guest path and the `Guest` name |
| `signin.empty-address-fails` | the guard: no port call |
| `signin.port-failure-lands` | the reason lands, nothing climbs |
