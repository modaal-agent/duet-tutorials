# Feature spec: editname

The one-page description of the name editor, level 3 of the profile tree.
The recordings under `parity/fixtures/editname.*` win any disagreement with
this prose.

## 1. Identity & config

- `EditNameConfig.MAX_LENGTH` — 40, the longest name the app accepts.
- `EditNameMessages.EMPTY`, `EditNameMessages.TOO_LONG` — the two validation
  messages.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `draft` | String | The field's text, seeded with the current name. |
| `isSaving` | Bool | The in-flight latch: one save at a time. |
| `validation` | String? | The message to show; cleared by the next edit. |

## 3. Actions

- `draftChanged(text)`, `saveTapped`, `cancelTapped` — shell reports.
- `saveFinished` — environment report: the account port saved the name.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `draftChanged(text)` | none | `draft = text`, `validation = null` | `[]` |
| `saveTapped` | `isSaving` | none | `[]` |
| `saveTapped` | trimmed draft empty | `validation = EMPTY` | `[]` |
| `saveTapped` | trimmed draft longer than 40 | `validation = TOO_LONG` | `[]` |
| `saveTapped` | otherwise | `isSaving = true`, `validation = null` | `[saveName(trimmed)]` |
| `saveFinished` | none | `isSaving = false` | `[notifyListener(saved(trimmed))]` |
| `cancelTapped` | none | none | `[notifyListener(closed)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `saveName(name)` | fire-and-forget bridge: the account port's one callback re-enters as `saveFinished` | the account port (`MockAccount` in Tutorial 3, `LocalAccount` from Tutorial 4) |
| `notifyListener(EditNameDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `saved(name)` | the account screen's `editName(event)`; the first hop of `chain-profile-editname` |
| `closed` | the account screen's `editName(event)`, which clears its child |

## 7. Out of feature scope (stays app-side, per platform)

- The text field and the two buttons.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `editname.valid-name-saves` | the trimmed save, the latch, the climb |
| `editname.empty-name-rejected` | the empty guard: no port call |
| `editname.long-name-rejected` | the length guard, and that an edit clears the message |
| `editname.cancel-closes` | `closed` climbs, the draft untouched |
