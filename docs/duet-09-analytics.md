# Duet Tutorial 9: Adding Analytics

[Series overview](README.md) · [← Tutorial 8: Localizing the App](duet-08-localization.md) · [On docs.modaal.dev](https://docs.modaal.dev/tutorials/duet-09-analytics)

Trees: [`tutorial9-start/`](../tutorial9-start) is what you open, [`tutorial9-complete/`](../tutorial9-complete) is what you have at the end. Run either tree's checks with `scripts/run-tree.sh tutorial9-start`.

In this tutorial you add analytics to Foyer without calling an analytics SDK from anywhere in the app. Seven things happen in Foyer that a product team would want counted: the app finishes launching, a user signs in, finishes onboarding, opens the promo, buys a plan, edits their name, signs out. Each is a transition a reducer already decides, so each becomes one more effect that reducer emits, typed on a shared event grammar, and the recordings pin it beside the state change it belongs to. The sink that receives the events is a worker at the edge, one per platform, and the one this tutorial ships prints to the console. This is the ninth and last page of the [nine-tutorial series](README.md), and it starts from [Tutorial 6](duet-06-checks-in-ci.md)'s finished tree, as Tutorials 7 and 8 do.

> [!NOTE]
> **The manual setup below is what Modaal automates.** Every tree this tutorial
> opens and every file it asks you to write is produced by the Duet templates in
> [Modaal](https://modaal.dev), and a coding agent working in Modaal writes the
> feature, records it and runs the checks for you. This series walks the same
> ground by hand so you know what the scaffold emits and why:
> [start a Duet project in Modaal](https://docs.modaal.dev/articles/new-project)
> when you would rather skip the setup.

## What will you build?

An event taxonomy of seven names, spelled the way a dashboard reads them, declared once in Kotlin and reaching a Swift sink and a Kotlin sink unchanged. The grammar comes from the family's telemetry artifact: an event is a subject, a verb and a list of primitive parameters, and the artifact ships ten starter verbs. Foyer adds one verb of its own, `Signed In`, and reaches for the starter vocabulary for the other six. Each event is emitted as a `Track` effect from the reducer that owns the transition, so `tools/duet record` writes it into the recordings and `tools/duet verify` fails when it moves. A fan-out worker at each composition root holds the sink list; the console sink behind it is a worker in its own right, adopted for the mount's lifetime, and a test on each platform drives it through the worker harness. Expect about an hour and a half.

You will have at the end:

- A `src-kmp/telemetry` module with the app's one verb and a test that pins how it renders; seven `<Feature>Events` objects, one beside each reducer that emits.
- A `Track` effect on seven features, asserted in their scenarios and in five chains, with 26 recordings re-recorded to carry the event envelope.
- A console sink worker on Android and on iOS behind the artifact's fan-out, each adopted at the root, each with a test through `WorkerTester`.
- Two tests that read every event envelope back out of the recordings, one with the Kotlin grammar and one with its Swift twin, and hold the set of names to the seven declared.

## Where do you start?

Open `tutorial9-start` from the [duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials). It is Tutorial 6's finished tree plus one failing test, the closing exercise, and it resolves Duet 0.7.0, duet-tools 0.24.0, duet-services 0.11.1 and the KSP mock processor 0.2.1 with Tutorial 6's toolchain. Tutorials 7, 8 and 9 all open this same tree. Run the checks once before you edit anything:

```sh
tools/duet verify
```

The Kotlin lane reports 73 recordings passing and one test failing, `Tutorial9ExerciseTrackedEventsTest` in the sign-in module. That test is the exercise at the end of this page: the sign-in event is the one you emit yourself. The six others are the steps.

## The steps

### Link the grammar

The event grammar is the family artifact `dev.modaal.duet.services:telemetry`, already in the version catalog since [Tutorial 6](duet-06-checks-in-ci.md#put-the-checks-in-a-workflow) as `duet-services-telemetry`. Its `commonMain` carries `TrackedEvent`, `TrackedVerb`, `TrackedParam`, the one encoding rule that turns an event into a vendor-facing name, and the port `AnalyticsTracking`; its JVM half adds the worker-typed sink port `AnalyticsTrackingWorking` and the fan-out `AnalyticsTrackingWorker`. The app links it through one module of its own, `src-kmp/telemetry`, which re-exports the artifact and holds what only this app can say:

**`src-kmp/telemetry/build.gradle.kts`**

```kotlin src-kmp/telemetry/build.gradle.kts
  sourceSets {
    commonMain.dependencies {
      api(libs.duet.services.telemetry)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
      // The recordings are JSON; the taxonomy test reads the envelopes back.
      implementation(libs.kotlinx.serialization.json)
    }
  }
```

Register the module, and export it from the Apple umbrella so Swift can name a `TrackedEvent`:

**`src-kmp/settings.gradle.kts`**

```kotlin src-kmp/settings.gradle.kts
// The telemetry grammar: the app's own event verbs over the family's
// telemetry artifact. Reducers emit `Track` effects on its types, so every
// feature module that emits one depends on it.
include(":telemetry")
```

**`src-kmp/apple-umbrella/build.gradle.kts`**

```kotlin src-kmp/apple-umbrella/build.gradle.kts
      // The event grammar the `Track` effects carry: the app's verbs, and
      // the artifact whose types they are, so Swift can name a TrackedEvent.
      export(dependencies.project(":telemetry"))
      export(libs.duet.services.telemetry)
```

Every feature module that will emit an event depends on the grammar module, and on nothing else new. The dependency is what makes a `TrackedEvent` nameable in a reducer; the Kotlin lane admits it because the module's own dependencies are family artifacts only:

**`src-kmp/subtrees/home/logic/build.gradle.kts`**

```kotlin src-kmp/subtrees/home/logic/build.gradle.kts
      // The event grammar the `Track` effects carry.
      api(project(":telemetry"))
```

The Android app links the module and the artifact too, for the fan-out and the sink port; `src-kmp/app/build.gradle.kts` gains the same two lines under `implementation`.

### Mint the app's verb

A `TrackedVerb` is a token, not an enum case: the artifact declares `Viewed`, `Opened`, `Started`, `Completed`, `Failed`, `Created`, `Edited`, `Deleted`, `Toggled` and `Signed Out` as companion values, and an app that needs another declares it in one line. The token is the vendor-facing word: `encodedName()` splices it after the subject, so `TrackedVerb("Signed In")` on the `Session` subject renders as `Session Signed In` on every platform and in every sink. Foyer needs one verb the artifact does not ship:

**`src-kmp/telemetry/src/commonMain/kotlin/dev/modaal/foyer/telemetry/AppVerbs.kt`**

```kotlin src-kmp/telemetry/src/commonMain/kotlin/dev/modaal/foyer/telemetry/AppVerbs.kt
object AppVerbs {
  /** The starter vocabulary has `Signed Out` and no `Signed In`; this is its pair. */
  val SignedIn = TrackedVerb("Signed In")
}
```

The test beside it is the module's lane; `tools/duet verify` never reaches it, because the module is not a manifest feature, and step 8 puts it into the workflow:

**`src-kmp/telemetry/src/jvmTest/kotlin/dev/modaal/foyer/telemetry/AppVerbsTest.kt`**

```kotlin src-kmp/telemetry/src/jvmTest/kotlin/dev/modaal/foyer/telemetry/AppVerbsTest.kt
  fun theAppsVerbRendersAsAuthored() {
    assertEquals("Session Signed In", TrackedEvent("Session", AppVerbs.SignedIn).encodedName())
  }
```

Now write the taxonomy down before writing any code, because every row is a name a dashboard will key on for years:

| Event | Owner | Emitted when | Parameters |
| --- | --- | --- | --- |
| `Splash Completed` | `root` | the root acts on the splash's completion | `path`: `ceremony` or `safety_net` |
| `Session Signed In` | `signin` | the auth port signs the account in | `provider`: `email` or `guest` |
| `Onboarding Completed` | `onboarding` | the last step continues | `preference_count` |
| `Promo Viewed` | `home` | a locked card opens the offer | none |
| `Upgrade Completed` | `upgrade` | the purchases port answers `purchased` | `plan`: `monthly` or `yearly` |
| `Name Edited` | `editname` | the account port confirms the save | none |
| `Session Signed Out` | `account` | the auth port confirms the sign-out | none |

Two rows read differently from how you might first name them. The promo is `Viewed`, not `Shown`, and the name is `Edited`, not `Changed`, because both acts are in the starter vocabulary already: a second spelling of the same act is a second column on every dashboard. The one verb the app mints is the one the vocabulary lacks. The parameters follow the grammar's privacy rule: primitives that describe behavior, never content. `Name Edited` carries nothing because the name is the user's; `Session Signed In` carries which kind of provider, never the address; `Onboarding Completed` carries how many preferences were picked, not which.

### Declare the events beside the reducers

Each feature that emits declares its events in an object next to its reducer, one builder per named event, from the grammar's types. The root's event takes the completion path, a sealed value in the splash module, and spells it as a string the dashboard can group on:

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootEvents.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootEvents.kt
object RootEvents {
  /** The app finished launching; `path` says which route ended the splash. */
  fun splashCompleted(path: SplashCompletionPath): TrackedEvent =
    TrackedEvent(
      "Splash",
      TrackedVerb.Completed,
      listOf(
        TrackedParam.string(
          "path",
          when (path) {
            SplashCompletionPath.Ceremony -> "ceremony"
            SplashCompletionPath.SafetyNet -> "safety_net"
          })))
}
```

The editor's has no builder function because it has no parameter:

**`src-kmp/subtrees/editname/logic/src/commonMain/kotlin/dev/modaal/foyer/editname/EditNameEvents.kt`**

```kotlin src-kmp/subtrees/editname/logic/src/commonMain/kotlin/dev/modaal/foyer/editname/EditNameEvents.kt
object EditNameEvents {
  val nameEdited: TrackedEvent = TrackedEvent("Name", TrackedVerb.Edited)
}
```

`OnboardingEvents.completed(preferenceCount:)`, `HomeEvents.promoViewed`, `UpgradeEvents.completed(plan:)` and `AccountEvents.signedOut` follow the same two shapes; `SignInEvents` is the exercise. The objects are Kotlin and only Kotlin: the Swift side never spells an event, because the recordings and the framework carry the declaration across.

### Emit them as Track effects

An event leaves a reducer the way every other side effect does: as a value in the effect list. Each of the seven features gains one effect payload case, one serializer case, one environment member and one handler arm; the shape is the same in every module, so the root's four are shown once:

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt
  @Serializable @SerialName("track") data class Track(val event: TrackedEvent) : RootEffectPayload
```

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootSerializers.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootSerializers.kt
      case(RootEffectPayload.Track::class, RootEffectPayload.Track.serializer()),
```

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootEnvironment.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootEnvironment.kt
  /** Forward a `Track` effect's event to the app's one sink (fire-and-forget). */
  fun track(event: TrackedEvent)
```

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootRuntime.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootRuntime.kt
        is RootEffectPayload.Track -> environment.track(payload.event)
```

The handler arm is fire-and-forget, like `notifyListener`: the flow calls the environment and emits no action. The environment member is the one line the generated mock grows too, so `RootEnvironmentMock` has a `trackArgs` list on the next test compile.

Then the reducer arms. The root is the interesting one, because the splash notifies its host on every completion path and [Tutorial 1](duet-01-first-feature.md#describe-the-behavior-as-a-scenario-and-record-it) pinned that deliberately in `splash.both-paths-notify-twice`. The launch is counted where one completion is acted on, and a second completion, late or while the first waits for auth, writes nothing and tracks nothing:

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt
    is RootAction.Splash ->
      when (val event = action.event) {
        is SplashDelegateEvent.Completed ->
          when {
            state.phase != RootPhase.Splash -> Reduced(state)
            // The other path completing while the first waits for auth: the
            // launch was counted once, so nothing is written and nothing tracked.
            state.awaitingAuth -> Reduced(state)
            state.auth == AuthSnapshot.Unknown ->
              Reduced(state.copy(awaitingAuth = true), listOf(splashCompleted(event.path)))
            else -> {
              val entered = enter(state, phaseAfterSplash(state.auth))
              Reduced(entered.state, listOf(splashCompleted(event.path)) + entered.effects)
            }
          }
      }
```

**`src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt`**

```kotlin src-kmp/subtrees/root/logic/src/commonMain/kotlin/dev/modaal/foyer/root/RootFeature.kt
/** The launch event: emitted once, by whichever completion this level acted on. */
private fun splashCompleted(path: SplashCompletionPath): Effect<RootEffectPayload> =
  Effect.Run(RootEffectPayload.Track(RootEvents.splashCompleted(path)))
```

The `awaitingAuth` guard is new: before this step a second completion while waiting re-wrote the same flag, which no recording could see. Now it would emit a second event, so the guard makes the case explicit and the scenario pins it in the next step. The home tab tracks the promo and not the summary, the flow tracks the purchase when the port answers and not when the user leaves the last step, and the account screen tracks the sign-out when the port confirms:

**`src-kmp/subtrees/home/logic/src/commonMain/kotlin/dev/modaal/foyer/home/HomeFeature.kt`**

```kotlin src-kmp/subtrees/home/logic/src/commonMain/kotlin/dev/modaal/foyer/home/HomeFeature.kt
    HomeAction.InsightsTapped ->
      if (state.entitlement is Entitlement.Premium) {
        Reduced(state.copy(presented = HomePresentation.Insights))
      } else {
        // The one screen product counts: a locked card tapped, the offer shown.
        Reduced(
          state.copy(presented = HomePresentation.Promo),
          listOf(Effect.Run(HomeEffectPayload.Track(HomeEvents.promoViewed))))
      }

    HomeAction.UpgradeTapped ->
      Reduced(
        state.copy(presented = null),
        listOf(Effect.Run(HomeEffectPayload.NotifyHost(HomeDelegateEvent.UpgradeRequested))))

    HomeAction.Dismissed -> Reduced(state.copy(presented = null))
  }
```

**`src-kmp/subtrees/upgrade/logic/src/commonMain/kotlin/dev/modaal/foyer/upgrade/UpgradeFeature.kt`**

```kotlin src-kmp/subtrees/upgrade/logic/src/commonMain/kotlin/dev/modaal/foyer/upgrade/UpgradeFeature.kt
        is PurchaseOutcome.Purchased ->
          Reduced(
            state.copy(isPurchasing = false, step = UpgradeStep.Done),
            listOf(Effect.Run(UpgradeEffectPayload.Track(UpgradeEvents.completed(outcome.plan)))))
```

**`src-kmp/subtrees/account/logic/src/commonMain/kotlin/dev/modaal/foyer/account/AccountFeature.kt`**

```kotlin src-kmp/subtrees/account/logic/src/commonMain/kotlin/dev/modaal/foyer/account/AccountFeature.kt
    AccountAction.SignedOut ->
      Reduced(
        state.copy(isSigningOut = false),
        listOf(
          // The event first: the delegate below climbs to the root, which
          // raises the gate and tears this screen down.
          Effect.Run(AccountEffectPayload.Track(AccountEvents.signedOut)),
          Effect.Run(AccountEffectPayload.NotifyHost(AccountDelegateEvent.SignOutRequested))))
```

Where an event rides beside a delegate event, it comes first in the list, and the order is load-bearing. A delegate event can end the mount that emitted it: the sign-in gate's `Completed` moves the root's phase, and the root shell tears the gate down; the editor's `Saved` clears the account screen's child. Effects run in list order, and the iOS root shell applies a phase change synchronously inside the delegate call, so an effect listed after the delegate is cancelled with the store before it runs. On Android the transition observer runs on a later hop and the second effect gets through, which is the kind of platform difference a recording cannot see and a console can, and step 8 shows the line that found it. The editor's `SaveFinished`, the onboarding level's last `Continued` and the account screen's `SignedOut` take the same shape: event, then delegate. Nothing in any arm asks whether analytics is enabled. Consent gates the sink, never the emission, and that is what keeps every recording deterministic.

### Assert them in the scenarios and re-record

A `Track` effect is asserted where every effect is: in the scenario's `thenEffects`, as the exact list. The root's first branch grows a step for the guard, and the branch that already pinned a late completion now pins that it tracks nothing either:

**`src-kmp/subtrees/root/logic/src/jvmTest/kotlin/dev/modaal/foyer/root/RootScenarioTest.kt`**

```kotlin src-kmp/subtrees/root/logic/src/jvmTest/kotlin/dev/modaal/foyer/root/RootScenarioTest.kt
        branch("splash before auth holds") {
          whenAction("the splash completes while auth is unknown", splashCompleted)
          then("the latch is set, the phase stays") {
            it.awaitingAuth && it.phase == RootPhase.Splash
          }
          thenEffects("the launch is counted, by the ceremony path") {
            it == effectsOf<RootEffectPayload>(launched(SplashCompletionPath.Ceremony))
          }
          whenAction("the safety net completes it again while auth is still unknown", safetyNetCompleted)
          then("nothing changed") { it.awaitingAuth && it.phase == RootPhase.Splash }
          thenEffects("nothing: the launch was counted once") { it.isEmpty() }
          whenAction("the host reports a signed-out session", signedOut)
          then("the latch releases into the gate") {
            !it.awaitingAuth && it.phase == RootPhase.SignIn && it.auth == AuthSnapshot.SignedOut
          }
        }
```

The flow's step that used to say "nothing: the stream carries the entitlement" now says exactly the event, with the plan the port answered with:

**`src-kmp/subtrees/upgrade/logic/src/jvmTest/kotlin/dev/modaal/foyer/upgrade/UpgradeScenarioTest.kt`**

```kotlin src-kmp/subtrees/upgrade/logic/src/jvmTest/kotlin/dev/modaal/foyer/upgrade/UpgradeScenarioTest.kt
          then("the done step; no entitlement anywhere in this state") {
            it.step == UpgradeStep.Done && !it.isPurchasing
          }
          thenEffects("exactly the event with the plan: the stream carries the entitlement") {
            it ==
              effectsOf<UpgradeEffectPayload>(
                Effect.Run(UpgradeEffectPayload.Track(UpgradeEvents.completed(Plan.Yearly))))
          }
```

Five chains cross a step that now emits, and the chain scenario asserts the emitting node's effects the same way; the splash seam, for one, ends by naming the launch event the root emitted after the hop:

**`src-kmp/subtrees/root/logic/src/jvmTest/kotlin/dev/modaal/foyer/root/RootChainsTest.kt`**

```kotlin src-kmp/subtrees/root/logic/src/jvmTest/kotlin/dev/modaal/foyer/root/RootChainsTest.kt
        thenEffects(root, "the launch event, by the ceremony path") {
          it ==
            effectsOf<RootEffectPayload>(
              Effect.Run(
                RootEffectPayload.Track(RootEvents.splashCompleted(SplashCompletionPath.Ceremony))))
        }
```

Re-record each feature and each chain that changed, then verify:

```sh
for f in root onboarding home upgrade editname account; do tools/duet record --feature $f; done
for c in chain-root-splash chain-main-signout chain-profile-editname chain-root-onboarding; do tools/duet record --chain $c; done
tools/duet verify
```

Thirty-nine recordings change: 26 carry a new effect, and 13 differ only in the line numbers the scenario's steps moved to. Every one is a diff you can read; this is what the flow's purchase step records now:

**`parity/fixtures/upgrade.select-confirm-purchase-done.fixture.json`**

```json parity/fixtures/upgrade.select-confirm-purchase-done.fixture.json
            "case": "track",
            "value": {
              "event": {
                "params": [
                  {
                    "case": "string",
                    "value": {
                      "key": "plan",
                      "value": "yearly"
                    }
                  }
                ],
                "subject": "Upgrade",
                "verb": "Completed"
```

That object is the event envelope: the effect's `case`, the event's subject and verb, and each parameter in the grammar's canonical form, `case` naming the primitive kind. The Kotlin encoders wrote it, and step 8 reads it back with both languages' decoders. `tools/duet verify` reports 73 of 73 and `tools/duet record --check` reports every feature and chain up to date; the mutation drill from [Tutorial 6](duet-06-checks-in-ci.md#write-the-first-mutation-rows) gains nothing here, because a deleted emission fails the recording that pins it.

### Wire the sink on Android

Everything so far ran on the Kotlin lane with a generated mock as the environment. On a device, a `Track` effect has to reach a sink. The artifact's fan-out, `AnalyticsTrackingWorker`, holds a list of sinks and forwards every call to each; a sink implements `AnalyticsTrackingWorking`, which is the tracking port plus `Working`, so the composition root adopts it like the session and entitlement workers from [Tutorial 4](duet-04-workers.md#write-the-workers-on-android). Foyer's one sink prints:

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/workers/ConsoleAnalyticsSink.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/workers/ConsoleAnalyticsSink.kt
class ConsoleAnalyticsSink(
  private val line: (String) -> Unit = ::println,
) : AnalyticsTrackingWorking {
  @Volatile private var enabled = true

  override val isEnabled: Boolean
    get() = enabled

  override fun setEnabled(enabled: Boolean) {
    this.enabled = enabled
  }

  override fun track(event: TrackedEvent) {
    if (!enabled) return
    val bag = event.encodedProperties()
    val properties =
      if (bag.isEmpty()) "" else bag.entries.joinToString(", ", " {", "}") { "${it.key}=${it.value}" }
    line("analytics: ${event.encodedName()}$properties")
  }

  override fun identify(uid: String) {
    if (enabled) line("analytics: identify $uid")
  }

  override fun reset() {
    if (enabled) line("analytics: reset")
  }

  /** Parks until the host cancels: a console has nothing to flush. */
  override suspend fun run() = untilCancelled()
}
```

The root Component owns the sink and the fan-out, and conforms to `AnalyticsProviding`, the artifact's one-member Dependency interface, so that every level's Dependency can extend it and be satisfied by the same member:

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/RootBuilder.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/RootBuilder.kt
   * The one vendor sink this app ships: the console. Picking a real vendor is
   * one more class implementing [AnalyticsTrackingWorking] — the only file
   * that imports the SDK — added to the list below and adopted in its own
   * right.
   */
  val consoleSink: AnalyticsTrackingWorking = ConsoleAnalyticsSink()

  /**
   * The grammar-typed sink [AnalyticsProviding]'s one member names, and a
   * worker the root adopts: the artifact's fan-out over the sink list. It
   * holds no flag of its own; the seed reaches every sink in the constructor,
   * before any event can.
   */
  override val analytics: AnalyticsTrackingWorking =
    AnalyticsTrackingWorker(sinks = listOf(consoleSink), isEnabled = true)
```

The root Builder adopts both before it builds the child slot, so no child can emit into a sink that is not yet running; the fan-out does not bracket the sinks' lifetimes, so each is adopted on its own:

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/RootBuilder.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/RootBuilder.kt
    host.adopt(component.consoleSink)
    host.adopt(component.analytics)
```

Each feature's Dependency extends `AnalyticsProviding` and its live environment forwards the effect; the compiler walks the chain from the root down, which is the same discipline [Tutorial 3](duet-03-composing-features.md#compose-the-tree-on-android) set for the ports:

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/HomeBuilder.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/HomeBuilder.kt
interface HomeDependency : AnalyticsProviding {
  val items: ItemsPort
}

class HomeComponent(dependency: HomeDependency) : HomeDependency by dependency {
  fun environment(onDelegate: (HomeDelegateEvent) -> Unit): HomeEnvironment =
    object : HomeEnvironment {
      override fun loadItems(onItems: (List<Item>) -> Unit) = items.items(onItems)

      override fun notifyHost(event: HomeDelegateEvent) = onDelegate(event)

      override fun track(event: TrackedEvent) = analytics.track(event)
```

`Main` and `Profile` extend it too, though their reducers emit nothing: a level's Dependency is the union of its subtree's needs. The sink's test goes through `WorkerTester`, as the two stream workers' do, and one of its three cases pins the fan-out's seed, the reason a disabled sink never egresses in the gap between composition and the first consent flip:

**`src-kmp/app/src/test/kotlin/dev/modaal/foyer/app/workers/ConsoleAnalyticsSinkTest.kt`**

```kotlin src-kmp/app/src/test/kotlin/dev/modaal/foyer/app/workers/ConsoleAnalyticsSinkTest.kt
  fun theFanOutSeedsTheSinkBeforeAnyEventReachesIt() = runTest {
    val lines = mutableListOf<String>()
    val sink = ConsoleAnalyticsSink(lines::add)
    val analytics = AnalyticsTrackingWorker(sinks = listOf(sink), isEnabled = false)

    // The seed landed in the constructor: the sink reports it, and drops the event.
    assertFalse(sink.isEnabled)
    analytics.track(signedIn)
    assertTrue(lines.isEmpty())

    analytics.setEnabled(true)
    analytics.track(signedIn)
    assertEquals(listOf("analytics: Session Signed In {provider=email}"), lines)
  }
```

`./gradlew :app:testDebugUnitTest` runs it beside the app's other suites; `RootFlowTest`, the headless walk over the whole tree, needs no change, because the root Component builds its own sink.

### Wire the sink on iOS

The Apple side types its sinks on the Swift half of the grammar, `DuetTelemetry` in the shared-services package: the artifact's fan-out is JVM-only, and a Kotlin `Track` effect carries the framework's bridged `TrackedEvent` class, not the Swift package's struct. So the tree gains the package, and one file that holds both grammars and converts between them. It lives in `FoyerBridge`, the target every shell and every spec already shares:

**`src-ios/Libraries/FoyerKit/Package.swift`**

```swift src-ios/Libraries/FoyerKit/Package.swift
    // The shared-services package, for DuetTelemetry: the Swift twin of the
    // event grammar the Kotlin core emits on, and the fan-out worker the
    // console sink sits behind. Built against the same duet release; the
    // Kotlin half pins the same version as `duetServices`.
    .package(url: "https://github.com/modaal-agent/duet-services.git", exact: "0.11.1"),
```

`BridgedAnalyticsWorker` wears the bridged port, holds the package's `AnalyticsTrackingWorker`, and converts each crossing event before fanning it out. The conversion copies the subject, carries the verb token verbatim, and maps the four parameter kinds; SKIE projects the Kotlin sealed interface as an enum, so the switch is exhaustive:

**`src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedAnalyticsWorker.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedAnalyticsWorker.swift
  public func track(event: FoyerKit.TrackedEvent) {
    worker.track(event: DuetTelemetry.TrackedEvent(bridged: event))
  }
```

**`src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedAnalyticsWorker.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedAnalyticsWorker.swift
  public init(bridged: any FoyerKit.TrackedParam) {
    switch onEnum(of: bridged) {
    case .stringParam(let param):
      self = .string(key: param.key, value: param.value)
    case .intParam(let param):
      // The Kotlin `Int` bridges as `Int32`; the Swift grammar's case holds
      // `Int`, which is wider on every platform this package builds for.
      self = .int(key: param.key, value: Int(param.value))
    case .boolParam(let param):
      self = .bool(key: param.key, value: param.value)
    case .doubleParam(let param):
      self = .double(key: param.key, value: param.value)
    }
  }
```

The console sink is the Android one's twin on the Swift port. A `Working` is `Sendable`, so its enabled flag lives behind a lock rather than in a plain property:

**`src-ios/Libraries/FoyerKit/Sources/RootShell/ConsoleAnalyticsSink.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/RootShell/ConsoleAnalyticsSink.swift
  public func track(event: TrackedEvent) {
    guard isEnabled else { return }
    let bag = event.encodedProperties()
    let properties =
      bag.isEmpty
      ? ""
      : " {" + bag.keys.sorted().map { "\($0)=\(bag[$0]!)" }.joined(separator: ", ") + "}"
    line("analytics: \(event.encodedName())\(properties)")
  }
```

The root Component owns the sink and the bridged fan-out, and exposes the fan-out under the bridged port's type as `analytics`, the member every level's Dependency names; the shell adopts the sink and the fan-out before the child slot exists:

**`src-ios/Libraries/FoyerKit/Sources/RootShell/RootComposition.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/RootShell/RootComposition.swift
  let consoleSink = ConsoleAnalyticsSink()

  /// The fan-out over the sink list, wearing the bridged port so a Kotlin
  /// `Track` effect lands on it unchanged; a worker the root adopts. The
  /// seed reaches every sink in the initializer, before any event can.
  let analyticsWorker: BridgedAnalyticsWorker

  init(dependency: RootDependency, scope: any Kotlinx_coroutines_coreCoroutineScope) {
    self.dependency = dependency
    backend = LocalBackend(file: dependency.storage, scope: scope)
    analyticsWorker = BridgedAnalyticsWorker(sinks: [consoleSink], isEnabled: true)
  }

  var auth: any AuthPort { backend.auth }
  var items: any ItemsPort { backend.items }
  var account: any AccountPort { backend.account }
  var purchases: any PurchasesPort { backend.purchases }
  var analytics: any AnalyticsTracking { analyticsWorker }
```

**`src-ios/Libraries/FoyerKit/Sources/RootShell/RootViewShell.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/RootShell/RootViewShell.swift
    // The sinks and the fan-out over them, adopted before any child exists:
    // a `Track` effect is fire-and-forget, so a sink not yet running would
    // hear nothing. The fan-out does not bracket the sinks' lifetimes.
    for sink in workers.sinks { host.adopt(sink) }
    host.adopt(workers.analytics)
```

Each feature's Dependency gains the member and its live environment forwards the effect, the mirror of the Kotlin side:

**`src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
public protocol HomeDependency: AnyObject {
  var items: any ItemsPort { get }
  var analytics: any AnalyticsTracking { get }
}
```

**`src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/HomeShell/HomeBuilder.swift
  func track(event: TrackedEvent) {
    analytics.track(event: event)
  }
```

The Dependency protocols are what `tools/duet mocks` generates the Components and the test doubles from, so regenerate; the generated `HomeDependencyMock` now takes `analytics:` in its initializer, and every shell spec passes the working default, a fan-out over no sink:

```sh
tools/duet mocks
```

**`src-ios/Libraries/FoyerKit/Tests/HomeShellTests/HomeViewShellSpec.swift`**

```swift src-ios/Libraries/FoyerKit/Tests/HomeShellTests/HomeViewShellSpec.swift
    HomeBuilder(dependency: HomeDependencyMock(analytics: BridgedAnalyticsWorker(sinks: []), items: backend.items))
```

The composition spec counted two live workers on the root after a sign-out; it counts four now. The crossing gets a spec of its own, because the package's twin fixtures pin the two grammars' encodings and not this app's converter:

**`src-ios/Libraries/FoyerKit/Tests/RootShellTests/BridgedAnalyticsWorkerSpec.swift`**

```swift src-ios/Libraries/FoyerKit/Tests/RootShellTests/BridgedAnalyticsWorkerSpec.swift
  func testABridgedEventReachesTheSinkAsItsSwiftTwin() {
    let sink = RecordingSink()
    let analytics = BridgedAnalyticsWorker(sinks: [sink], isEnabled: true)

    analytics.track(
      event: FoyerKit.TrackedEvent(
        subject: "Session",
        verb: FoyerKit.TrackedVerb(rendered: "Signed In"),
        params: [
          TrackedParamStringParam(key: "provider", value: "email"),
          TrackedParamIntParam(key: "attempt", value: 2),
          TrackedParamBoolParam(key: "cached", value: true),
          TrackedParamDoubleParam(key: "elapsed_s", value: 1.5),
        ]))

    XCTAssertEqual(sink.recorded.count, 1)
    // The vendor-facing name is the grammar's one encoding rule, and the verb
    // token crosses verbatim — a converter that re-derived it would spell the
    // event differently on the two platforms.
    XCTAssertEqual(sink.recorded.first?.encodedName(), "Session Signed In")
    XCTAssertEqual(
      sink.recorded.first?.params,
      [
        .string(key: "provider", value: "email"),
        .int(key: "attempt", value: 2),
        .bool(key: "cached", value: true),
        .double(key: "elapsed_s", value: 1.5),
      ])
  }
```

`ConsoleAnalyticsSinkSpec` is the Android test's twin, through the Swift `WorkerTester`. The Apple lane runs both after it assembles the core: `parity/scripts/apple-boundary-lane.sh` replays 67 recordings across the framework, `Track` envelopes included, and runs the shells' 41 specs.

### Take the receipt

The receipt is the taxonomy, read back out of the recordings by both languages. On the Kotlin side a test in the grammar module walks every fixture, decodes each `track` envelope with the grammar's own serializer, and holds the set of encoded names to the seven declared; it lives in `:telemetry` and not in a feature module on purpose, because `tools/duet record` runs a feature module's whole test task, and this test cannot pass until the recording it reads exists:

**`src-kmp/telemetry/src/jvmTest/kotlin/dev/modaal/foyer/telemetry/TrackedEventsInRecordingsTest.kt`**

```kotlin src-kmp/telemetry/src/jvmTest/kotlin/dev/modaal/foyer/telemetry/TrackedEventsInRecordingsTest.kt
    val DECLARED =
      setOf(
        "Splash Completed",
        "Session Signed In",
        "Onboarding Completed",
        "Promo Viewed",
        "Upgrade Completed",
        "Name Edited",
        "Session Signed Out",
      )
```

On the Swift side a spec does the same with the Swift twin's `Codable`, and re-encodes each event to check it writes back what Kotlin wrote:

**`src-ios/Libraries/FoyerKit/Tests/RootShellTests/TrackedEventEnvelopesSpec.swift`**

```swift src-ios/Libraries/FoyerKit/Tests/RootShellTests/TrackedEventEnvelopesSpec.swift
        let again = try JSONSerialization.jsonObject(with: JSONEncoder().encode(event))
        XCTAssertEqual(
          again as? NSDictionary, envelope as? NSDictionary, "\(file): the Swift twin re-encodes the envelope unchanged")
      }
    }
    XCTAssertGreaterThan(envelopes, 0)
    XCTAssertEqual(names, Self.declared, "the names in the recordings")
```

An eighth event, wherever it is emitted, fails both until its name is added to both lists: adding an event is a taxonomy decision, and this is where it is made. The grammar module's suite joins the workflow as one step, beside the backend's:

**`.github/workflows/parity.yml`**

```yaml .github/workflows/parity.yml
      # The telemetry grammar module is not a manifest feature either. Its
      # suite pins the app's own verb and reads every `track` envelope back
      # out of the recordings; the encoding rule and the fan-out belong to the
      # family artifact and are gated by its suite.
      - name: telemetry grammar tests
        run: (cd src-kmp && ./gradlew :telemetry:jvmTest --console=plain -q)
```

Then run the apps and watch the console. On Android the sink prints through `println`, which reaches logcat under the `System.out` tag; on iOS it prints to the process's standard output, which `xcrun simctl launch --console-pty` shows. Launch each, sign in as a guest, and the first two lines are the same on both:

```text
analytics: Splash Completed {path=ceremony}
analytics: Session Signed In {provider=guest}
```

Each line is the sink's rendering of `encodedName()` and `encodedProperties()`, the two things a vendor SDK is handed. This is also the check that found the ordering rule in step 4: with the event listed after the delegate, Android printed both lines and the iOS console stopped after the first, because the gate was torn down before its second effect ran. Swap the console sink for a vendor's and nothing above the composition root changes.

## What you now have

- Seven events with dashboard-ready names, declared once in Kotlin beside the reducers that emit them, reaching a Swift sink and a Kotlin sink unchanged; one verb of the app's own, and a test that pins how it renders.
- Every emission pinned as an effect in a recording, with its parameters, and asserted in the scenario and in the chains that cross it; a repeated or late completion pinned as tracking nothing.
- A console sink worker on each platform behind the artifact's fan-out, adopted at the root before any child can emit, with a test through the worker harness on each side.
- The taxonomy read back out of the recordings by both languages' decoders, so an eighth event is a deliberate edit in two lists, and a grammar module suite that runs as one step of the workflow.

## Exercise: emit the sign-in event

`tutorial9-start` carries `Tutorial9ExerciseTrackedEventsTest`, a failing placeholder in the sign-in module. The taxonomy's second row is still empty on the tree: emit `Session Signed In` from `signInReducer` when the auth port answers `signedIn`, with one parameter, `provider`, whose value is `email` or `guest` from the provider held in `pending`. The steps above give you the four shape edits and the `SignInEvents` object to write; the verb is `AppVerbs.SignedIn`. Assert the event in `signin.email-signs-in` and `signin.guest-signs-in`, re-record the feature and `chain-root-signin`, which crosses the emitting step, and replace the placeholder with `SignInTrackedEventsTest`: a `TestStore` over the generated environment mock that checks the event reached `trackArgs` once with the name and the bag a vendor would see, and that a refused sign-in reached it not at all.

**`src-kmp/subtrees/signin/logic/src/jvmTest/kotlin/dev/modaal/foyer/signin/SignInTrackedEventsTest.kt`**

```kotlin src-kmp/subtrees/signin/logic/src/jvmTest/kotlin/dev/modaal/foyer/signin/SignInTrackedEventsTest.kt
    val tracked: List<TrackedEvent> = environment.trackArgs
    assertEquals(1, tracked.size)
    assertEquals("Session Signed In", tracked.single().encodedName())
    // The kind, never the address: the property bag is what a vendor sees.
    assertEquals(mapOf<String, Any>("provider" to "email"), tracked.single().encodedProperties())
```

```sh
tools/duet verify
```

`tutorial9-complete` carries the finished event and test. The address never enters the event: `SignInEvents.signedIn` maps the provider to its kind, and the test's last assertion is the property bag with one key.

## Common questions

<details>
<summary><b>Why does the root emit the launch event, and not the splash?</b></summary>

The splash notifies its host on both completion paths by design, and [Tutorial 1](duet-01-first-feature.md#describe-the-behavior-as-a-scenario-and-record-it) pinned that in `splash.both-paths-notify-twice`: a completion latch in the splash would hide a bug the safety net exists to catch. A `Track` there would fire twice per launch. The root is where one of the two completions is acted on and the other is inert, so the root owns the count, and its recordings pin that a late or repeated completion tracks nothing. An event belongs to the reducer that decides the transition, which is not always the one that first sees the input.

</details>
<details>
<summary><b>Why Promo Viewed and Name Edited, when the plan said Shown and Changed?</b></summary>

Because `Viewed` and `Edited` are in the artifact's starter vocabulary and describe the same acts. A verb is a taxonomy decision: every event with that verb becomes a family of dashboard names, and two spellings of one act split a chart in two for as long as both exist. The rule is to reach for a starter verb first and mint one only for an act the vocabulary lacks, which is why Foyer mints exactly `Signed In`, the pair of the starter `Signed Out`.

</details>
<details>
<summary><b>Why is the emission unconditional, and where does consent go?</b></summary>

A reducer that checked a consent flag before emitting would need the flag in its state, and then two recordings per branch. Consent gates the sink instead: `setEnabled(false)` on the fan-out reaches every sink, and each sink drops what it receives while disabled, which is where a vendor SDK's own opt-out switch lives anyway. The fan-out pushes the seed into every sink in its constructor, so nothing egresses before the app's persisted choice is applied. Foyer seeds `true` and has no Settings row; an app that ships a vendor adds the row, persists the choice, and forwards it to `setEnabled`. `identify(uid:)` and `reset()` are the same shape: composition-root calls on the session stream, never reducer effects.

</details>
<details>
<summary><b>Why a console sink and not a real vendor?</b></summary>

A vendor SDK breaks the tree's hermeticity and needs a credential, so the tutorial ships the sink that needs neither. The shape is the vendor's: one class conforming to `AnalyticsTrackingWorking`, the only file that imports an SDK, added to the sink list at the root and adopted in its own right. Everything above the root, and every recording, is identical with a real sink in that list.

</details>
<details>
<summary><b>What about events the reducers never see, like a screen appearing?</b></summary>

Lifecycle and system events are shell-side: a view shell calls `analytics.track` directly on the same grammar types, with no `Track` effect, because no reducer decided anything. Nothing in Foyer needs one today; the seven events are all transitions. The split rule is the one from the grammar's own documentation: reducer-driven events are effects and fixture-gated, lifecycle events are shell calls and are not.

</details>
<details>
<summary><b>What do the package's twin fixtures pin, and what do the app's?</b></summary>

The shared-services package commits five fixtures written by its Kotlin encoders and decoded by its Swift suite, which pins that the two grammars encode identically, verb tokens included. That is what lets the converter in `BridgedAnalyticsWorker` copy fields instead of translating. The app's envelope tests pin something the package cannot: that this app's seven events, as recorded, decode with both grammars and name exactly the taxonomy. Step 8's two tests are the app's contribution to the same contract.

</details>
<details>
<summary><b>Why do 13 recordings change when nothing in them moved?</b></summary>

A recording carries the line number of each step in its scenario source, so an assertion added above a branch shifts the lines of every branch below it. `tools/duet record` marks those rewrites as metadata-only, and the drift check treats them the same as any other change: a recording is what the scenario produces today, line numbers included.

</details>

## Sources and further reading

- [duet-services](https://github.com/modaal-agent/duet-services) — `DuetTelemetry` and its Kotlin twin `dev.modaal.duet.services:telemetry`: the grammar, the encoding rule, the sink port, the fan-out worker, and the twin fixtures under `contracts/telemetry-twin`.
- [Tutorial 4: Workers](duet-04-workers.md#test-the-workers-with-the-harness) — the worker seam and `WorkerTester`, which the two sink tests use.
- [Tutorial 6: The Checks in CI](duet-06-checks-in-ci.md#put-the-checks-in-a-workflow) — the workflow the grammar module's step joins.
- [The Duet glossary](https://docs.modaal.dev/articles/duet-glossary) — [effect](https://docs.modaal.dev/articles/duet-glossary#effect), [worker](https://docs.modaal.dev/articles/duet-glossary#worker) and [golden recording](https://docs.modaal.dev/articles/duet-glossary#golden-fixture).

## Read next

- **[Tutorial 6: The Checks in CI](duet-06-checks-in-ci.md)** — The tree this page opens, and the workflow the grammar module's step joins.
- **[Tutorial 7: Theming with Design Tokens](duet-07-theming.md)** — The same tree, a second theme over the cards, and no feature-code diff.
- **[Tutorial 8: Localizing the App](duet-08-localization.md)** — The same tree, every string into catalogs and resources, and a second language that changes no recording.
- **[The Duet tutorial series](README.md)** — The nine tutorials, the app they build, the prerequisites and the versions they are verified against.
