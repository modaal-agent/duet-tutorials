# Feature spec: home

The one-page description of the home tab. The recordings under
`parity/fixtures/home.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The list comes from the items port; the promo asks the
  host for the upgrade flow.
- The entitlement is a slice projected from the root: it arrives as
  `entitlementChanged`, and the reducer never writes it on its own.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `items` | `[Item]` | The list, empty until loaded. |
| `isLoading` | Bool | The in-flight latch for the load. |
| `entitlement` | `Entitlement` | The slice. `free` locks the Insights card; `premium(plan)` unlocks it. |
| `presented` | `HomePresentation?` | `promo` or `insights` over the list, or none. The shell presents from this value; the route spine carries it. |

## 3. Actions

- `appeared` — shell report: the tab is on screen.
- `itemsLoaded(items)` — environment report: the items port answered.
- `entitlementChanged(entitlement)` — the root's slice arrived.
- `insightsTapped` — shell report: the Insights card.
- `upgradeTapped` — shell report: the promo's one button.
- `dismissed` — shell report: the presented screen's Back.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | `!isLoading && items.isEmpty()` | `isLoading = true` | `[loadItems]` |
| `appeared` | otherwise | none | `[]` — a tab switch or a rotation reloads nothing |
| `itemsLoaded(items)` | none | `items`, `isLoading = false` | `[]` |
| `entitlementChanged(entitlement)` | none | `entitlement` | `[]` |
| `insightsTapped` | `entitlement == free` | `presented = promo` | `[]` |
| `insightsTapped` | `entitlement == premium` | `presented = insights` | `[]` |
| `upgradeTapped` | none | `presented = null` | `[notifyListener(upgradeRequested)]` |
| `dismissed` | none | `presented = null` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `loadItems` | fire-and-forget bridge: the items port's one callback re-enters as `itemsLoaded` | `LocalItems` |
| `notifyListener(HomeDelegateEvent)` | pure environment call: the delegate sink | none |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `upgradeRequested` | the main level's `home(event)` action, which mounts the upgrade flow in its sheet slot |

## 7. Out of feature scope (stays app-side, per platform)

- The list's rendering, the card's locked and unlocked treatment, and the
  two presented screens, which are shown the same way on both platforms.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `home.loads-once` | the first appearance loads; a repeat while loading is inert |
| `home.reappear-keeps-items` | a repeat after loading is inert |
| `home.locked-card-presents-promo` | the card while `free` opens the promo; Back closes it |
| `home.unlocked-card-presents-insights` | the card while `premium` opens the summary |
| `home.promo-requests-the-upgrade` | the promo's button closes it and climbs `upgradeRequested` |
| `home.card-unlocks-from-the-stream` | the card unlocks on the slice, not on anything the flow reports |

Chains ending here: `chain-upgrade-entitlement` (the flow's Completed
clears the main level's sheet; this tab unlocks on the stream's value).
