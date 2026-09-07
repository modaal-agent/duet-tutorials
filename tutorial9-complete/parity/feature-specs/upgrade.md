# Feature spec: upgrade

The one-page description of the upgrade flow. The recordings under
`parity/fixtures/upgrade.*` win any disagreement with this prose.

## 1. Identity & config

- No constants. The plans and their prices come from the purchases port.
- The flow never writes an entitlement. The purchase's answer moves the
  step; the card unlocks from the entitlement stream through the root.
- Events (`UpgradeEvents`): `Upgrade Completed` with one param, `plan`
  (`monthly` / `yearly`), from the purchase outcome — emitted when the port
  answers, not when the user leaves the last step.

## 2. State

| Field | Type | Notes |
| --- | --- | --- |
| `step` | `UpgradeStep` | `plans`, `confirm(plan)` or `done`: the route. The shell renders exactly one step; the route spine carries it. |
| `offers` | `[PlanOffer]` | The plans on offer, empty until loaded. |
| `isPurchasing` | Bool | The in-flight latch: one purchase at a time. |
| `failure` | String? | The last refusal to show on the confirm step, cleared by the next attempt. |

## 3. Actions

**Shell reports**

- `appeared` — the flow is on screen.
- `planSelected(plan)` — a plan card on the first step.
- `confirmTapped` — the confirm step's button.
- `back` — Back, on whichever step.
- `doneTapped` — the last step's button.
- `dismissTapped` — the sheet was dismissed from outside the flow.

**Environment reports**

- `plansLoaded(offers)` — the purchases port listed its plans.
- `purchaseFinished(outcome)` — `purchased(plan)` or `failed(reason)`.

## 4. Transitions

| Action | Guard | State writes | Effects |
| --- | --- | --- | --- |
| `appeared` | `offers.isEmpty()` | none | `[loadPlans]` |
| `appeared` | otherwise | none | `[]` |
| `plansLoaded(offers)` | none | `offers` | `[]` |
| `planSelected(plan)` | `step == plans` | `step = confirm(plan)`, `failure = null` | `[]` |
| `confirmTapped` | `step == confirm(plan)`, `!isPurchasing` | `isPurchasing = true`, `failure = null` | `[purchase(plan)]` |
| `confirmTapped` | otherwise | none | `[]` — one purchase at a time |
| `purchaseFinished(purchased(plan))` | none | `isPurchasing = false`, `step = done` | `[track(Upgrade Completed, plan)]` — no entitlement is written here |
| `purchaseFinished(failed(reason))` | none | `isPurchasing = false`, `failure = reason` | `[]` |
| `back` | `step == confirm`, `!isPurchasing` | `step = plans`, `failure = null` | `[]` |
| `back` | `step == plans` | none | `[notifyListener(dismissed)]` |
| `back` | `step == done` | none | `[notifyListener(completed)]` |
| `back` | `step == confirm`, `isPurchasing` | none | `[]` |
| `doneTapped` | `step == done` | none | `[notifyListener(completed)]` |
| `dismissTapped` | `step == done` | none | `[notifyListener(completed)]` |
| `dismissTapped` | otherwise | none | `[notifyListener(dismissed)]` |

## 5. Effects

| Effect | Ingress shape | Backing worker(s) |
| --- | --- | --- |
| `loadPlans` | fire-and-forget bridge: the purchases port's one callback re-enters as `plansLoaded` | `LocalPurchases` |
| `purchase(plan)` | fire-and-forget bridge: the purchases port's one callback re-enters as `purchaseFinished`; the entitlement re-enters separately, through the root's entitlement worker and the slice | `LocalPurchases` |
| `notifyListener(UpgradeDelegateEvent)` | pure environment call: the delegate sink | none |
| `track(event)` | pure environment call: the app's one analytics sink, fire-and-forget; consent gates the sink, never the emission | the console sink worker (`ConsoleAnalyticsSink` on both platforms), behind the app's fan-out |

## 6. Delegate events

| Case | Host seam |
| --- | --- |
| `completed` | the main level's `upgrade(event)` action, which clears the sheet; pinned by `chain-upgrade-entitlement` |
| `dismissed` | the same action; the sheet clears the same way |

## 7. Out of feature scope (stays app-side, per platform)

- The presentation: a sheet on iOS, a modal bottom sheet on Android, both
  mounted from the main level's `sheet` value.
- The plan cards' layout and the confirm step's copy.

## 8. Behavior recordings

| Recording | Pins |
| --- | --- |
| `upgrade.plans-load-once` | the first appearance loads; a repeat is inert |
| `upgrade.select-confirm-purchase-done` | the three steps as route state; one purchase at a time; Done climbs `completed`; the event with `plan = yearly` when the port answers, none on Done |
| `upgrade.back-walks-the-steps` | Back inside the flow is a state change; Back on the first step climbs `dismissed` |
| `upgrade.purchase-failure-lands` | a refusal shows its reason on the confirm step |
| `upgrade.back-on-done-completes` | leaving the last step by Back completes the flow |
| `upgrade.dismiss-from-outside` | a dismissal from outside the flow climbs `dismissed` |

Chains starting here: `chain-upgrade-entitlement`.
