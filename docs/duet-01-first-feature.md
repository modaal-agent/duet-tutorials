# Duet Tutorial 1: Your First Feature

[Series overview](README.md) · [Tutorial 2: One Behavior, Two Apps →](duet-02-two-apps.md) · [On docs.modaal.dev](https://docs.modaal.dev/tutorials/duet-01-first-feature)

Trees: [`tutorial1-start/`](../tutorial1-start) is what you open, [`tutorial1-complete/`](../tutorial1-complete) is what you have at the end. Run either tree's checks with `scripts/run-tree.sh tutorial1-start`.

In this tutorial you write the splash feature of Foyer as a Duet feature in Kotlin: its state, its actions, its effects as data, and a pure reducer that turns one into the next. You describe the feature's behavior as a scenario, record the scenario into four fixture files, and verify that the reducer replays them; a test-clock suite pins the one piece of timing. There is no UI in this tutorial: [Tutorial 2](duet-02-two-apps.md) mounts the feature in a SwiftUI app and a Compose app. This page is the first of the [nine-tutorial series](README.md), which builds one app, Foyer, end to end.

> [!NOTE]
> **The manual setup below is what Modaal automates.** Every tree this tutorial
> opens and every file it asks you to write is produced by the Duet templates in
> [Modaal](https://modaal.dev), and a coding agent working in Modaal writes the
> feature, records it and runs the checks for you. This series walks the same
> ground by hand so you know what the scaffold emits and why:
> [start a Duet project in Modaal](https://docs.modaal.dev/articles/new-project)
> when you would rather skip the setup.

## What will you build?

A splash screen has one job: play an animation, then hand control to the host. The splash you write here does that with a safety net. When the splash appears it arms a timer; when the animation ends, or when the timer fires, the feature tells its host that the splash completed and by which path. Both paths notify the host every time they fire, because the host treats a repeat as a no-op, and a second `Appeared` is inert so the timer is armed once per mount. That is the whole behavior, and it fits in one state field, three actions and two effect payloads.

You will have at the end:

- `src-kmp/subtrees/splash/logic`, a Kotlin Multiplatform module with the feature's types, its reducer, its serializers, its environment interface and its effect handler.
- Four recordings under `parity/fixtures/`: `splash.ceremony-completes`, `splash.safety-net-fires`, `splash.both-paths-notify-twice` and `splash.repeat-appear-inert`.
- A green `tools/duet verify`: the four recordings replay against the reducer, and the safety net's timing holds on a virtual clock.
- A replay runner the `duet` tool drives over a protocol, which [Tutorial 6](duet-06-checks-in-ci.md#put-the-checks-in-a-workflow) puts in CI.

## Where do you start?

Clone the [duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) and open `tutorial1-start`. It is a Kotlin Multiplatform project with no feature yet: a Gradle wrapper, a version catalog pinning Duet 0.7.0 and Kotlin 2.4.10, an empty parity manifest, and `tools/duet`, which fetches the `duet` command-line tool at version 0.24.0 on first use. Every path on this page is relative to that directory. Expect about an hour; the first Gradle run downloads the wrapper's distribution and is the slowest step. Confirm the tree is green before you edit anything:

```sh
tools/duet verify
```

The last line reads `duet verify: PASS` with a note that no feature is declared. The finished tree is `tutorial1-complete` in the same repository; every code block on this page is an excerpt of it, named by its path.

## The steps

### Declare the feature module

A Duet feature is a Gradle module under `src-kmp/subtrees/<feature>/logic`. Its build file declares one JVM target, where the tests run, and three Apple targets, which compile the same sources for the iOS app in Tutorial 2. The module depends on the Duet kernel and on `kotlinx.serialization`; its tests add the kernel's test harness and the coroutines test library. Create the file:

**`src-kmp/subtrees/splash/logic/build.gradle.kts`**

```kotlin src-kmp/subtrees/splash/logic/build.gradle.kts
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

// Every feature module is a Gradle project named `logic`, so each one names
// its own archive; two jars called `logic-jvm.jar` on one classpath fail the
// replay-runner's `installDist`.
base.archivesName.set("splash-logic")

// The feature's one implementation lives in commonMain. The JVM target runs
// the host test lane (the scenario, the golden replays, the test-store suite);
// the Apple targets compile the same sources for the iOS app, which arrives in
// Tutorial 2.
kotlin {
  jvmToolchain(25)

  jvm()
  macosArm64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      api(libs.duet.kernel)
      implementation(libs.kotlinx.serialization.json)
    }
    jvmTest.dependencies {
      implementation(libs.duet.kernel.test)
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  // The golden replays read parity/fixtures/*.json at runtime. Declaring the
  // directory as a task input makes a fixtures-only change re-run the tests
  // instead of leaving the task UP-TO-DATE.
  inputs.dir(rootProject.layout.projectDirectory.dir("../parity/fixtures"))
    .withPropertyName("parityFixtures")
    .withPathSensitivity(PathSensitivity.RELATIVE)
}
```

The last block declares `parity/fixtures` as an input of the test task, so that a change to a recording re-runs the replays instead of leaving the task up to date. Then include the module in the settings file:

**`src-kmp/settings.gradle.kts`**

```kotlin src-kmp/settings.gradle.kts
include(":subtrees:splash:logic")
```

Check it with `(cd src-kmp && ./gradlew projects -q)`; the project hierarchy ends with `\--- Project ':subtrees:splash:logic'`.

### Write the state, the actions and the effect payloads

Everything the feature knows is data. The state is one field, the arming latch. The actions are the three things that can happen: two reports from the shell (the splash appeared; the animation finished) and one from the environment (the timer elapsed). The effect payloads describe what the feature asks the outside world to do, without doing it: wait on a clock, or tell the host something. The delegate event is what the host receives, and its `path` says which of the two ways completed the splash. Every type is `@Serializable`, because the recordings are JSON.

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashFeature.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
// MARK: - Configuration

object SplashConfig {
  /**
   * How long the safety net waits before it completes the splash on the
   * ceremony's behalf. The reducer carries the value out in the effect
   * payload, so the recordings pin it.
   */
  const val SAFETY_NET_MILLIS: Long = 3_000L
}

object SplashEffectIds {
  /**
   * The safety net's effect id. One net is in flight per store: a second
   * `Run` under the same id would cancel the first, and the store's teardown
   * cancels it outright.
   */
  const val SAFETY_NET: EffectId = "splash.safetyNet"
}

// MARK: - State

@Serializable
data class SplashState(
  /** The arming latch: the safety net is armed once per mount. */
  val isArmed: Boolean = false,
)

// MARK: - Actions

@Serializable(with = SplashActionSerializer::class)
sealed interface SplashAction {
  /** Shell report: the splash is on screen and the safety net should arm. */
  @Serializable @SerialName("appeared") data object Appeared : SplashAction

  /** Shell report: the splash animation reached its end. */
  @Serializable
  @SerialName("ceremonyFinished")
  data object CeremonyFinished : SplashAction

  /** Environment report: the armed delay elapsed. */
  @Serializable
  @SerialName("safetyNetElapsed")
  data object SafetyNetElapsed : SplashAction
}

// MARK: - Delegate events

/** Which of the two paths completed the splash. */
@Serializable(with = SplashCompletionPathSerializer::class)
sealed interface SplashCompletionPath {
  @Serializable @SerialName("ceremony") data object Ceremony : SplashCompletionPath

  @Serializable @SerialName("safetyNet") data object SafetyNet : SplashCompletionPath
}

/** What the splash tells its host. The host decides what happens next. */
@Serializable(with = SplashDelegateEventSerializer::class)
sealed interface SplashDelegateEvent {
  @Serializable
  @SerialName("completed")
  data class Completed(val path: SplashCompletionPath) : SplashDelegateEvent
}

// MARK: - Effect payloads

@Serializable(with = SplashEffectPayloadSerializer::class)
sealed interface SplashEffectPayload {
  /**
   * Wait `afterMillis` on the environment's clock, then report
   * `SafetyNetElapsed`. Runs under `SplashEffectIds.SAFETY_NET`.
   */
  @Serializable
  @SerialName("armSafetyNet")
  data class ArmSafetyNet(val afterMillis: Long) : SplashEffectPayload

  /** Hand a delegate event to the host. */
  @Serializable
  @SerialName("notifyListener")
  data class NotifyHost(val event: SplashDelegateEvent) : SplashEffectPayload
}
```

Two choices here matter later. The timer's duration is a module constant that the reducer copies into the payload, so the recordings pin the number and a retiming shows up as a diff in a fixture file. The effect id `splash.safetyNet` is what gives the timer cancel-in-flight semantics: the kernel's [Store](https://docs.modaal.dev/articles/duet-glossary#store) cancels a running effect when a new one starts under the same id, and cancels every running effect at teardown.

### Write the reducer

The [reducer](https://docs.modaal.dev/articles/duet-glossary#reducer) is a pure function from a state and an action to a new state and a list of effects. It never sleeps, never reads a clock and never calls the host; it only decides. The three arms below are the whole behavior: `Appeared` arms the net once, and each completion path asks for a notification.

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashFeature.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
fun splashReducer(
  state: SplashState,
  action: SplashAction,
): Reduced<SplashState, SplashEffectPayload> =
  when (action) {
    SplashAction.Appeared ->
      if (state.isArmed) {
        // The arming guard: a repeat `Appeared` writes nothing and emits nothing.
        Reduced(state)
      } else {
        Reduced(
          state.copy(isArmed = true),
          listOf(
            Effect.Run(
              SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
              id = SplashEffectIds.SAFETY_NET)))
      }

    SplashAction.CeremonyFinished ->
      Reduced(
        state,
        listOf(
          Effect.Run(
            SplashEffectPayload.NotifyHost(
              SplashDelegateEvent.Completed(SplashCompletionPath.Ceremony)))))

    SplashAction.SafetyNetElapsed ->
      Reduced(
        state,
        listOf(
          Effect.Run(
            SplashEffectPayload.NotifyHost(
              SplashDelegateEvent.Completed(SplashCompletionPath.SafetyNet)))))
  }
```

Note what the reducer does not do. `CeremonyFinished` does not cancel the net and does not set a "completed" flag: if the timer fires after the animation ended, the host is notified a second time, and the host's job is to ignore it. Keeping the rule in one place, the host, is what lets the reducer stay three arms long, and the recordings in step 7 pin that rule so nobody adds a latch later by accident.

### Register the serializers

Recordings are JSON, and every sum type on the reducer's boundary needs a canonical coding: an object with a `case` and, when the case carries data, a `value`. The kernel's `CanonicalSumSerializer` takes one registry line per case; a case you forget fails at the first encode rather than producing a fixture that quietly disagrees with the Swift side later. A case whose single payload is unlabeled, such as `NotifyHost(event)`, is marked `inline` so the payload encodes bare.

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashSerializers.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashSerializers.kt
object SplashActionSerializer :
  CanonicalSumSerializer<SplashAction>(
    "SplashAction",
    listOf(
      case(SplashAction.Appeared::class, SplashAction.Appeared.serializer()),
      case(SplashAction.CeremonyFinished::class, SplashAction.CeremonyFinished.serializer()),
      case(SplashAction.SafetyNetElapsed::class, SplashAction.SafetyNetElapsed.serializer()),
    ))
```

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashSerializers.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashSerializers.kt
object SplashEffectPayloadSerializer :
  CanonicalSumSerializer<SplashEffectPayload>(
    "SplashEffectPayload",
    listOf(
      case(SplashEffectPayload.ArmSafetyNet::class, SplashEffectPayload.ArmSafetyNet.serializer()),
      case(
        SplashEffectPayload.NotifyHost::class,
        SplashEffectPayload.NotifyHost.serializer(),
        inline = true),
    ))
```

The finished file registers `SplashCompletionPath` and `SplashDelegateEvent` the same way.

### Add the environment and the runtime

The reducer asked for two things it cannot do itself: wait, and notify the host. The environment interface is the feature's only door to the platform, and each app implements it in Tutorial 2. It has a clock rather than a `delay` call, so a test can run the wait on virtual time.

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashEnvironment.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashEnvironment.kt
interface SplashEnvironment {
  /**
   * The clock the safety net waits on. The effect handler calls `sleep` here
   * rather than `delay` directly, so a test can run the wait on virtual time.
   */
  val clock: KernelClock

  /** Hand a delegate event to the host. */
  fun notifyHost(event: SplashDelegateEvent)
}
```

The effect handler is the module's only impure code. It turns each payload into a cold flow of the actions the effect produces: `ArmSafetyNet` sleeps and then emits `SafetyNetElapsed`; `NotifyHost` calls the environment and emits nothing. Cancellation needs no code of its own: when the store cancels the effect, the sleep throws and the flow ends without emitting. The store factory below it is what both shells call.

**`src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashRuntime.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashRuntime.kt
fun splashEffectHandler(
  environment: SplashEnvironment,
): (SplashEffectPayload) -> Flow<SplashAction> = { payload ->
  flow {
    when (payload) {
      is SplashEffectPayload.ArmSafetyNet -> {
        environment.clock.sleep(payload.afterMillis * NANOS_PER_MILLI)
        emit(SplashAction.SafetyNetElapsed)
      }
      is SplashEffectPayload.NotifyHost -> environment.notifyHost(payload.event)
    }
  }
}

private const val NANOS_PER_MILLI = 1_000_000L

/** The store every shell hosts: the reducer and the handler, wired. */
fun makeSplashStore(
  environment: SplashEnvironment,
  scope: CoroutineScope,
): Store<SplashState, SplashAction, SplashEffectPayload> =
  Store(
    initialState = SplashState(),
    reducer = ::splashReducer,
    handler = splashEffectHandler(environment),
    scope = scope,
  )
```

Compile the module with `(cd src-kmp && ./gradlew :subtrees:splash:logic:compileKotlinJvm)`; the last line reads `BUILD SUCCESSFUL`.

### Declare the feature in the manifest and write its spec

`parity/manifest.yaml` is what the `duet` tool reads: which file holds the feature, which test is its scenario, and which recordings it owns. Add the feature under `features:`.

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
features:
  splash:
    kotlin: src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
    state: SplashState
    action: SplashAction
    effectPayload: SplashEffectPayload
    scenario: src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt
    fixtures:
      - splash.ceremony-completes
      - splash.safety-net-fires
      - splash.both-paths-notify-twice
      - splash.repeat-appear-inert
```

Beside it, `parity/feature-specs/splash.md` is the one-page description of the feature in prose: identity and config, state, actions, transitions, effects, delegate events, what stays app-side, and the recordings. The tool checks that every recording listed in the manifest is named in that file in backticks, so the prose cannot drift from the fixtures without a red check. Copy the finished tree's file, or write your own from its headings; its last section is the table below.

**`parity/feature-specs/splash.md`**

```markdown parity/feature-specs/splash.md
| Recording | Pins |
| --- | --- |
| `splash.ceremony-completes` | `appeared` arms the net; `ceremonyFinished` notifies `completed(ceremony)` |
| `splash.safety-net-fires` | `safetyNetElapsed` notifies `completed(safetyNet)`. Mutually exclusive with the row above, so it is a branch over the same given rather than a scenario of its own |
| `splash.both-paths-notify-twice` | the deliberate non-latch: `ceremonyFinished` then `safetyNetElapsed`, two notifications |
| `splash.repeat-appear-inert` | the arming guard: a second `appeared` writes nothing and emits nothing |
```

Run `tools/duet lint`. It fails, and the failure names the next step:

```text
✗ [splash] fixture listed but missing on disk: splash.ceremony-completes — just declared? run: tools/duet record --feature splash
```

### Describe the behavior as a scenario and record it

A [scenario](https://docs.modaal.dev/articles/duet-glossary#scenario) is a test written in the fixture-authoring language: a given state, `whenAction` steps, and `then` and `thenEffects` checks. Where two endings are mutually exclusive, they are branches over the same given, and each branch leaf becomes one recording. The splash has one given, the appeared splash, and four branches.

**`src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt
        given(SplashState())

        whenAction("the splash appears", SplashAction.Appeared)
        then("the safety net is armed") { it.isArmed }
        thenEffects("exactly the keyed safety net, carrying its duration") {
          it ==
            effectsOf<SplashEffectPayload>(
              Effect.Run(
                SplashEffectPayload.ArmSafetyNet(SplashConfig.SAFETY_NET_MILLIS),
                id = SplashEffectIds.SAFETY_NET))
        }

        branch("ceremony completes") {
          whenAction("the animation reaches its end", SplashAction.CeremonyFinished)
          then("the arming latch is untouched") { it.isArmed }
          thenEffects("the host is notified, by the ceremony path") {
            it == notified(SplashCompletionPath.Ceremony)
          }
        }

        branch("safety net fires") {
          whenAction("the armed delay elapses instead", SplashAction.SafetyNetElapsed)
          then("the arming latch is untouched") { it.isArmed }
          thenEffects("the host is notified, by the safety-net path") {
            it == notified(SplashCompletionPath.SafetyNet)
          }
        }

        branch("both paths notify twice") {
          whenAction("the animation reaches its end", SplashAction.CeremonyFinished)
          thenEffects("the host is notified, by the ceremony path") {
            it == notified(SplashCompletionPath.Ceremony)
          }
          whenAction("the armed delay elapses on top of it", SplashAction.SafetyNetElapsed)
          thenEffects("the host is notified again, by the safety-net path") {
            it == notified(SplashCompletionPath.SafetyNet)
          }
        }

        branch("repeat appear inert") {
          whenAction("the splash reports appearing a second time", SplashAction.Appeared)
          then("still armed, unchanged") { it.isArmed }
          thenEffects("nothing: the net is not re-armed") { it.isEmpty() }
        }
```

The scenario builder is wrapped in a `scenario(feature = "splash", description = …, source = …)` call, and the test ends by handing the scenario, the three serializers and the reducer to `ScenarioRunner.verifyOrRecord`. Run as an ordinary test it verifies; run by the `duet` tool with regeneration on, it records. Record it:

```sh
tools/duet record --feature splash
```

The first line reads `duet record: rewrote 4 fixture(s):` followed by the four paths. Open one. Each step holds the action, the expected state and the expected effects as canonical JSON, plus the label and the source line it came from:

**`parity/fixtures/splash.repeat-appear-inert.fixture.json`**

```json parity/fixtures/splash.repeat-appear-inert.fixture.json
  "steps": [
    {
      "action": {
        "case": "appeared"
      },
      "expectedEffects": [
        {
          "id": "splash.safetyNet",
          "kind": "run",
          "payload": {
            "case": "armSafetyNet",
            "value": {
              "afterMillis": 3000
            }
          }
        }
      ],
      "expectedState": {
        "isArmed": true
      },
      "label": "the splash appears",
      "line": 33
    },
    {
      "action": {
        "case": "appeared"
      },
      "expectedEffects": [],
      "expectedState": {
        "isArmed": true
      },
      "label": "repeat appear inert › the splash reports appearing a second time",
      "line": 71
    }
  ]
```

Fixtures are build products: review the diff like code, never edit them by hand.

### Replay the recordings

A [golden recording](https://docs.modaal.dev/articles/duet-glossary#golden-fixture) is only worth something if a test replays it. The golden test has one method per branch leaf, so a divergence in one ending fails on its own line, and the `duet` tool checks that every recording listed in the manifest reported a replay.

**`src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashGoldenTest.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashGoldenTest.kt
class SplashGoldenTest {
  @Test fun ceremonyCompletesLeaf() = replay("splash.ceremony-completes")

  @Test fun safetyNetFiresLeaf() = replay("splash.safety-net-fires")

  @Test fun bothPathsNotifyTwiceLeaf() = replay("splash.both-paths-notify-twice")

  @Test fun repeatAppearInertLeaf() = replay("splash.repeat-appear-inert")

  private fun replay(leaf: String) {
    FixtureRunner.run(
      fixture = leaf,
      stateSerializer = SplashState.serializer(),
      actionSerializer = SplashActionSerializer,
      payloadSerializer = SplashEffectPayloadSerializer,
      reducer = ::splashReducer,
    )
  }
}
```

Run the checks:

```sh
tools/duet verify
```

The output includes `duet verify: 4/4 fixture report(s) passed` and ends with `duet verify: PASS`. The verify command runs the manifest checks first, then the Kotlin lane, which is the module's `jvmTest` task; the manifest's Swift lane is skipped because no feature declares a `swift:` path. The Apple boundary lane, which replays the same recordings across the framework the iOS app consumes, arrives in [Tutorial 2](duet-02-two-apps.md#replay-the-recordings-across-the-boundary).

### Pin the safety net's timing on a test clock

The recordings pin what the reducer emits and nothing about time: the reducer never waits. The timing lives in the effect handler, so its test drives the real store through the handler on the coroutines test scheduler's virtual clock. The test double for the environment records every delegate event and waits on `LiveClock`, which under `runTest` suspends on virtual time.

**`src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/RecordingSplashEnvironment.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/RecordingSplashEnvironment.kt
class RecordingSplashEnvironment(
  override val clock: KernelClock = LiveClock,
) : SplashEnvironment {
  val notified = mutableListOf<SplashDelegateEvent>()

  override fun notifyHost(event: SplashDelegateEvent) {
    notified += event
  }
}
```

The suite has four tests: the net does not fire before its duration, fires after it, fires exactly once, and never fires after teardown. The first and last are below; every test starts from the same given, a store that has received `Appeared`.

**`src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt
  @Test
  fun safetyNetDoesNotFireBeforeItsDuration() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS - 1)
    runCurrent()

    // Teardown first: a net still sleeping at `finish()` would count as an
    // effect left in flight. Then `finish()` fails on any action that arrived
    // and was never received.
    store.teardown()
    store.finish()
    assertEquals(emptyList(), environment.notified)
  }
```

**`src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt`**

```kotlin src-kmp/subtrees/splash/logic/src/jvmTest/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt
  @Test
  fun teardownCancelsTheArmedNet() = runTest {
    val environment = RecordingSplashEnvironment()
    val store = armedStore(environment)

    store.teardown()
    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS * 3)
    runCurrent()

    store.finish()
    assertEquals(emptyList(), environment.notified)
  }
```

`TestStore` is exhaustive: `finish()` fails if an action arrived that the test never received, or if an effect is still running. That is how the "exactly once" test works with no counter: a second `SafetyNetElapsed` would be an unreceived action. Run `tools/duet verify` again; it ends with `duet verify: PASS`. Then run the regeneration gate, which CI uses to catch a scenario edited without re-recording:

```sh
tools/duet record --check
```

It prints `duet record --check: fixtures are up to date with their scenarios`.

### Add the replay runner

The last module is a small JVM application that serves the app's features over the replay protocol: the `duet` tool sends each recorded step to it and compares the bytes that come back. It is how the same recordings are checked from outside the test JVM, and [Tutorial 6](duet-06-checks-in-ci.md#put-the-checks-in-a-workflow) puts it in the CI workflow. It is a registry with one entry per feature:

**`src-kmp/replay-runner/src/main/kotlin/dev/modaal/foyer/replayrunner/Main.kt`**

```kotlin src-kmp/replay-runner/src/main/kotlin/dev/modaal/foyer/replayrunner/Main.kt
private val registry =
  ReplayRegistry(
    listOf<ReplayFeature>(
      ReplayFeature.entry(
        "splash",
        SplashState.serializer(),
        SplashActionSerializer,
        SplashEffectPayloadSerializer,
        ::splashReducer),
    ))

fun main() {
  ReplayServer.serve(registry)
}
```

Its build file applies the Kotlin JVM plugin and the `application` plugin, and depends on the kernel and on the feature module:

**`src-kmp/replay-runner/build.gradle.kts`**

```kotlin src-kmp/replay-runner/build.gradle.kts
dependencies {
  implementation(libs.duet.kernel)
  implementation(project(":subtrees:splash:logic"))
}

application {
  mainClass.set("dev.modaal.foyer.replayrunner.MainKt")
}
```

Include it in the settings file:

**`src-kmp/settings.gradle.kts`**

```kotlin src-kmp/settings.gradle.kts
// The replay-protocol endpoint `tools/duet protocol-run` drives.
include(":replay-runner")
```

Build the runner and drive it:

```sh
(cd src-kmp && ./gradlew :replay-runner:installDist -q)
tools/duet protocol-run --runner src-kmp/replay-runner/build/install/replay-runner/bin/replay-runner
```

The output includes `4 leaf + 0 chain fixture(s), 9 step(s) byte-gated CLI-side` and ends with `protocol-run: PASS`.

## What you now have

- One feature module, `src-kmp/subtrees/splash/logic`, with the behavior in four `commonMain` files: the types and the reducer, the serializers, the environment interface and the runtime.
- Four recordings under `parity/fixtures/`, each a JSON file the reducer replays byte for byte.
- Nine passing tests on the Kotlin lane: the scenario, four golden replays and four test-clock checks.
- A green `tools/duet verify`, a green `tools/duet record --check`, and a replay runner that passes `tools/duet protocol-run`.
- A manifest row and a feature spec that the tool cross-checks against the recordings.

Well done: that is a complete Duet feature, and [Tutorial 2](duet-02-two-apps.md#write-the-swift-shell) puts it on two screens without changing a line of it.

## Exercise: break the arming guard

The recording `splash.repeat-appear-inert` pins that a second `Appeared` does nothing. Delete the guard and watch the recording catch it. In `SplashFeature.kt`, replace the `Appeared` arm's `if`/`else` with the `else` branch's body alone, so every `Appeared` arms a net. Run `tools/duet verify`. The Kotlin lane fails on one recording, and the report names the step, the expected effects and what the reducer emitted instead:

```text
duet verify: 3/4 fixture report(s) passed
✗ fixture 'splash.repeat-appear-inert' step 1 'repeat appear inert › nothing: the net is not re-armed' — Then failed   [kotlin]
  at expectedEffects[0]
  expected: ∅   actual: {"id":"splash.safetyNet","kind":"run","payload":{"case":"armSafetyNet","value":{"afterMillis":3000}}}
```

Restore the guard and run `tools/duet verify` again; it ends with `duet verify: PASS`. A recording is a test you did not have to write twice: the same file fails the Apple boundary lane in [Tutorial 2](duet-02-two-apps.md#replay-the-recordings-across-the-boundary) if the framework the iOS app consumes ever disagrees.

## Common questions

<details>
<summary><b>Why does the reducer copy the duration into the payload instead of the handler knowing it?</b></summary>

So the number is in the recording. `expectedEffects` in every fixture carries `afterMillis: 3000`; a change to `SplashConfig.SAFETY_NET_MILLIS` shows up as a diff in four JSON files that a reviewer sees. A duration hidden in the handler would be invisible to the recordings and to the Apple boundary lane.

</details>

<details>
<summary><b>Why does CeremonyFinished not cancel the safety net?</b></summary>

Because the host is idempotent and the reducer stays simpler for it: three arms, one state field, no ordering between the two paths to get wrong. The recording `splash.both-paths-notify-twice` pins the second notification, so a future edit that adds a cancel or a latch fails a check instead of silently changing the contract with the host.

</details>

<details>
<summary><b>What is the difference between the scenario test and the golden test?</b></summary>

The scenario test is the source of the recordings: run with regeneration on by `tools/duet record`, it writes the fixture files; run as a plain test, it checks them. The golden test only replays what is on disk, one method per recording, and is what a second platform runs against the same files. Both stay in the tree so that a recording can be regenerated from its scenario and checked without it.

</details>

<details>
<summary><b>Why is the test double hand-written?</b></summary>

`RecordingSplashEnvironment` is nine lines and lives in test sources, which is the rule for every test double in a Duet tree. [Tutorial 3](duet-03-composing-features.md#generate-the-kotlin-test-doubles) replaces hand-written doubles with the family's generated mocks once there are several environments to double; for one interface with two members, the class is shorter than the generator wiring.

</details>

<details>
<summary><b>Why does the build file declare Apple targets when there is no iOS app yet?</b></summary>

The same `commonMain` sources compile to an Apple framework in [Tutorial 2](duet-02-two-apps.md#declare-the-apple-framework-module), and declaring the targets now means the module's shape does not change when the app arrives. Nothing on this page builds them: `tools/duet verify` runs the JVM target only.

</details>

## Sources and further reading

- [The Duet framework repository](https://github.com/modaal-agent/duet) — the kernel's `Store`, `Effect` and `Reduced` types, the `kernel-test` harness this page's tests use, and the contracts for the [store kernel](https://github.com/modaal-agent/duet/blob/main/contracts/store-kernel-contract.md) and [serialization](https://github.com/modaal-agent/duet/blob/main/contracts/serialization.md).
- [The duet-tools repository](https://github.com/modaal-agent/duet-tools) — the `duet` command-line tool and the [manifest grammar](https://github.com/modaal-agent/duet-tools/blob/main/contracts/manifest.md).
- [The duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) — `tutorial1-start` and `tutorial1-complete`, and the checks CI runs on them.
- [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) — `runTest`, `advanceTimeBy` and the virtual clock the test-clock suite runs on.
- [Kotlin serialization](https://kotlinlang.org/docs/serialization.html) — the `@Serializable` and `@SerialName` annotations on every recorded type.
- [The Duet glossary](https://docs.modaal.dev/articles/duet-glossary) — [feature](https://docs.modaal.dev/articles/duet-glossary#feature), [reducer](https://docs.modaal.dev/articles/duet-glossary#reducer), [scenario](https://docs.modaal.dev/articles/duet-glossary#scenario), [behavior recording](https://docs.modaal.dev/articles/duet-glossary#fixture) and [the checks](https://docs.modaal.dev/articles/duet-glossary#gate).

## Read next

- **[Tutorial 2: One Behavior, Two Apps](duet-02-two-apps.md)** — Build the Kotlin core into an Apple framework, then mount the splash in a SwiftUI app and a Compose app.
- **[The Duet tutorial series](README.md)** — The nine tutorials, the app they build, the prerequisites and the versions they are verified against.
- **[Duet: one shared core, two native apps](https://docs.modaal.dev/articles/duet)** — What Duet is, what you get, and the two project cards that scaffold it.
