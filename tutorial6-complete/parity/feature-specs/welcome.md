# Feature spec: welcome

The one-page description of the welcome step. The recordings under
`parity/fixtures/welcome.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. Nothing on this step can make it not ready.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `isReady` | Bool | Always true. |

## 3. Actions

- `appeared` — shell report: the step is on screen.
- `continueTapped` — shell report.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | none | none | `[publishReadiness(welcome, true)]` |
| `continueTapped` | none | none | `[notifyListener(continued)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `publishReadiness(step, ready)` | pure environment call: the readiness seam's `updateReadiness`; nothing re-enters | none |
| `notifyListener(WelcomeDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `continued` | the onboarding level's `welcome(event)` action |

## 7. Out of feature scope (stays app-side, per platform)

- The copy on the screen.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `welcome.appearance-publishes-readiness` | appearing publishes `true` |
| `welcome.continue-climbs` | Continue climbs `continued` |
