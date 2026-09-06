# Feature spec: home

The one-page description of the home tab. The recordings under
`parity/fixtures/home.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The list comes from the items port.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `items` | `[Item]` | The list, empty until loaded. |
| `isLoading` | Bool | The in-flight latch. |

## 3. Actions

- `appeared` — shell report: the tab is on screen.
- `itemsLoaded(items)` — environment report: the items port answered.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | `!isLoading && items.isEmpty()` | `isLoading = true` | `[loadItems]` |
| `appeared` | otherwise | none | `[]` — a tab switch or a rotation reloads nothing |
| `itemsLoaded(items)` | none | `items`, `isLoading = false` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `loadItems` | fire-and-forget bridge: the items port's one callback re-enters as `itemsLoaded` | the items port (`MockItems` in Tutorial 3, `LocalItems` from Tutorial 4) |

## 6. Delegate events

None yet. Tutorial 5 adds `upgradeRequested`.

## 7. Out of feature scope (stays app-side, per platform)

- The list's rendering.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `home.loads-once` | the first appearance loads; a repeat while loading is inert |
| `home.reappear-keeps-items` | a repeat after loading is inert |
