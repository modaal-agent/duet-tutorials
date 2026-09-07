# Feature spec: progress

The one-page description of the progress row. The recordings under
`parity/fixtures/progress.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. Two values reach the row by two routes: the page, projected
  down from the onboarding level as `pageChanged`, and each step's
  readiness, arriving laterally through the seam as `readinessChanged`.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `page` | `OnboardingPage` | The level's page. The reducer reads it and never writes it on its own. |
| `readiness` | `{OnboardingPage: Bool}` | Each step's readiness as the seam last reported it. |
| `isObserving` | Bool | The observation is running; one per mount. |

## 3. Actions

- `appeared` — shell report: the row is on screen.
- `pageChanged(page)` — the level's slice arrived.
- `readinessChanged(step, ready)` — environment report: the seam delivered a value.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | `!isObserving` | `isObserving = true` | `[observeReadiness]` |
| `appeared` | otherwise | none | `[]` |
| `pageChanged(page)` | none | `page` | `[]` |
| `readinessChanged(step, ready)` | none | `readiness[step] = ready` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `observeReadiness` | sticky flag: the seam's current value per step first, then every change, each as `readinessChanged`, until the store tears down | the readiness worker, owned by the onboarding level's composition and shared with the three steps |

## 6. Delegate events

None.

## 7. Out of feature scope (stays app-side, per platform)

- The bar and the ticks: "step n of 3" from `page`, one tick per step
  whose readiness is true.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `progress.appearance-observes-once` | one subscription per mount |
| `progress.page-projects-down` | the page is written as it arrives |
| `progress.readiness-arrives-laterally` | the latest value per step is kept |
