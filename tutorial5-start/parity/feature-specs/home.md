# Feature spec: home

The one-page description of the home tab. The recordings under
`parity/fixtures/home.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The list comes from the items port; the promo buys the
  monthly plan through the purchases port.
- The entitlement is a slice projected from the root: it arrives as
  `entitlementChanged`, and the reducer never writes it on its own.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `items` | `[Item]` | The list, empty until loaded. |
| `isLoading` | Bool | The in-flight latch for the load. |
| `entitlement` | `Entitlement` | The slice. `free` locks the Insights card; `premium(plan)` unlocks it. |
| `presented` | `HomePresentation?` | `promo` or `insights` over the list, or none. The shell presents from this value. |
| `isPurchasing` | Bool | The in-flight latch for the promo's purchase. |
| `failure` | String? | The last purchase failure to show, cleared by the next attempt. |

## 3. Actions

- `appeared` — shell report: the tab is on screen.
- `itemsLoaded(items)` — environment report: the items port answered.
- `entitlementChanged(entitlement)` — the root's slice arrived.
- `insightsTapped` — shell report: the Insights card.
- `purchaseTapped` — shell report: the promo's one button.
- `purchaseFinished(outcome)` — environment report: the purchases port answered.
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
| `purchaseTapped` | `!isPurchasing` | `isPurchasing = true`, `failure = null` | `[purchase(monthly)]` |
| `purchaseTapped` | `isPurchasing` | none | `[]` — one purchase at a time |
| `purchaseFinished(purchased)` | none | `isPurchasing = false`, `presented = null` | `[]` — the entitlement is not written here; the stream writes it |
| `purchaseFinished(failed(reason))` | none | `isPurchasing = false`, `failure = reason` | `[]` |
| `dismissed` | none | `presented = null` | `[]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `loadItems` | fire-and-forget bridge: the items port's one callback re-enters as `itemsLoaded` | `LocalItems` |
| `purchase(plan)` | fire-and-forget bridge: the purchases port's one callback re-enters as `purchaseFinished`; the entitlement re-enters separately, through the root's entitlement worker and the slice | `LocalPurchases` |

## 6. Delegate events

None yet. Tutorial 5 adds `upgradeRequested`, which replaces the promo's
one-tap purchase with the upgrade flow.

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
| `home.purchase-flips-through-the-stream` | one purchase at a time; the card unlocks on the slice, the promo closes on the answer |
| `home.purchase-failure-lands` | a refused purchase shows its reason on the promo and stays `free` |
