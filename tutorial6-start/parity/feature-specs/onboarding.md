# Feature spec: onboarding

The one-page description of the onboarding level. The recordings under
`parity/fixtures/onboarding.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The level owns the page and mounts one step child from it;
  the progress row is mounted beside the steps for the level's lifetime.
- The level's composition owns the readiness worker the steps and the
  progress row share, and forwards it down each child's Dependency.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `page` | `OnboardingPage` | `welcome`, `name` or `preferences`: which step is mounted. Projected to the progress row; the route spine carries it. |
| `name` | String? | What the name step handed up. |
| `preferences` | `[String]` | What the preferences step handed up. |

## 3. Actions

**Shell reports**

- `back` — Back, from the system or a button.

**Children's delegate events, received as actions**

- `welcome(event)` — the welcome step's `continued`.
- `name(event)` — the name step's `continued(name)`.
- `preferences(event)` — the preferences step's `continued(preferences)`.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `back` | `page == welcome` | none | `[]` — the gate is back-inert on its first page |
| `back` | `page == name` | `page = welcome` | `[]` |
| `back` | `page == preferences` | `page = name` | `[]` |
| `welcome(continued)` | `page == welcome` | `page = name` | `[]` |
| `name(continued(name))` | `page == name` | `page = preferences`, `name` | `[]` |
| `preferences(continued(preferences))` | `page == preferences` | `preferences` | `[notifyListener(completed(name, preferences))]` |
| a step's event on another page | — | none | `[]` — inert; the shell mounts one step at a time |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `notifyListener(OnboardingDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `completed(name, preferences)` | the root's `onboarding(event)` action, which persists the answers and mounts main; pinned by `chain-root-onboarding` |

## 7. Out of feature scope (stays app-side, per platform)

- Mounting the step the page names and tearing down the one that left.
- Projecting `page` into the progress row as its `pageChanged`.
- The readiness worker: owned by this level's composition as the lowest
  common ancestor of the steps and the progress row; a `WorkerTester`
  test on each platform pins its sticky delivery, and no recording exists
  for it.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `onboarding.steps-advance-to-completion` | each Continued moves the page; the last one climbs `completed` with both answers |
| `onboarding.back-walks-the-pages` | Back moves the page back and keeps the answers |
| `onboarding.back-on-welcome-inert` | Back on the first page changes nothing; a stray step event is inert |

Chains through here: `chain-onboarding-name` (the name step's Continued),
`chain-root-onboarding` (this level's Completed).
