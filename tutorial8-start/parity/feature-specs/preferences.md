# Feature spec: preferences

The one-page description of the preferences step. The recordings under
`parity/fixtures/preferences.*` win any disagreement with this prose.

## 1. Identity & config

- `PreferenceKeys.ALL` — `digest`, `reminders`, `tips`, in the screen's order.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `selected` | `[String]` | The keys that are on, in `ALL`'s order. |
| `isReady` | Bool | At least one key is on. Published to the seam when it changes. |

## 3. Actions

- `preferenceToggled(key)` — shell report.
- `continueTapped` — shell report.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `preferenceToggled(key)` | `key` unknown | none | `[]` |
| `preferenceToggled(key)` | readiness changes | `selected`, `isReady` | `[publishReadiness(preferences, isReady)]` |
| `preferenceToggled(key)` | otherwise | `selected` | `[]` |
| `continueTapped` | `isReady` | none | `[notifyListener(continued(selected))]` |
| `continueTapped` | otherwise | none | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `publishReadiness(step, ready)` | pure environment call: the readiness seam's `updateReadiness`; nothing re-enters | none |
| `notifyListener(PreferencesDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `continued(preferences)` | the onboarding level's `preferences(event)` action |

## 7. Out of feature scope (stays app-side, per platform)

- The toggles' labels.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `preferences.toggle-publishes-readiness` | the selection keeps the screen's order; readiness is published when it changes |
| `preferences.continue-needs-one` | Continue with nothing on is inert; an unknown key is inert |
| `preferences.continue-climbs` | Continue climbs the selection |
