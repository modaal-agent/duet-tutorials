# Feature spec: name

The one-page description of the name step. The recordings under
`parity/fixtures/name.*` win any disagreement with this prose.

## 1. Identity & config

- The validation is the editor's `validateDisplayName`: non-empty after
  trimming, at most 40 characters, with the editor's two messages.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `draft` | String | The field's text. |
| `isReady` | Bool | The draft passes validation. Published to the seam when it changes. |
| `validation` | String? | The message after a refused Continue, cleared by the next edit. |

## 3. Actions

- `draftChanged(text)` — shell report.
- `continueTapped` — shell report.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `draftChanged(text)` | readiness changes | `draft`, `isReady`, `validation = null` | `[publishReadiness(name, isReady)]` |
| `draftChanged(text)` | otherwise | `draft`, `validation = null` | `[]` |
| `continueTapped` | the draft is valid | none | `[notifyListener(continued(draft.trim()))]` |
| `continueTapped` | otherwise | `validation = message` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `publishReadiness(step, ready)` | pure environment call: the readiness seam's `updateReadiness`; nothing re-enters | none |
| `notifyListener(NameDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `continued(name)` | the onboarding level's `name(event)` action; pinned by `chain-onboarding-name` |

## 7. Out of feature scope (stays app-side, per platform)

- The text field and its live text: the shell sends every edit.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `name.valid-draft-publishes-readiness` | readiness is published when it changes and only then |
| `name.empty-name-rejected` | the editor's message; the next edit clears it |
| `name.continue-climbs` | Continue climbs the trimmed name |
