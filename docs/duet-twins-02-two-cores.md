# Duet Twins Tutorial 2: One Behavior, Two Cores

[Series overview](README.md) · [← Swift Tutorial 1: Your First Feature](duet-swift-01-first-feature.md) · [On docs.modaal.dev](https://docs.modaal.dev/tutorials/duet-twins-02-two-cores)

Trees: [`tutorial2-twins-start/`](../tutorial2-twins-start) is what you open, [`tutorial2-twins-complete/`](../tutorial2-twins-complete) is what you have at the end. Run either tree's checks with `scripts/run-tree.sh tutorial2-twins-start`.

In this tutorial you give the splash feature from [Swift Tutorial 1](duet-swift-01-first-feature.md) a Kotlin twin and put the feature on two screens. The Kotlin twin is a plain Kotlin/JVM module with the same state, actions, effects and reducer as the Swift one, and it replays the four recordings the Swift scenario wrote, so the reducer the Android app runs is checked against the behavior the iPhone app's reducer recorded. Then you write the two shells: a Swift shell and a SwiftUI view over the Swift core in the iOS app, and a Compose screen over the Kotlin core in the Android app. Each shell does three things: it turns intents into actions, projects state into what the view renders, and brackets the store's lifetime. At the end each app runs its own core and both land on the same placeholder screen. This page is the first in the series where each app runs its own core; [Tutorial 2](duet-02-two-apps.md) is the shared-core version of the same step, where one Kotlin core runs in both apps.

> [!NOTE]
> Modaal does not scaffold Duet Twins yet. This tutorial builds the shape by hand with the open `duet` CLI (0.34.0 or later) and the published Duet packages.

## What will you build?

The splash feature exists twice at the end. The Swift twin is Swift Tutorial 1's package, `src-ios/Subtrees/Splash/SplashFeature`; this page adds one test to it, in the exercise. The Kotlin twin is `src-android/subtrees/splash/logic`, a `kotlin.jvm` module with no Apple target, written from [Tutorial 1](duet-01-first-feature.md)'s Kotlin files. The Swift scenario stays the one writer of the four recordings under `parity/fixtures/`; on the Kotlin side a golden test replays them and a second scenario checks them without writing. The four recordings at the end are byte-identical to Swift Tutorial 1's; the one `record` on this page is in the last step, and `git checkout` undoes it.

The iOS app links Swift packages only, and the Android app compiles the Kotlin twin. Neither app contains a line of feature logic. The splash reveals the app's name over 1.6 seconds, reports the end of the reveal to its store, and the host swaps in a placeholder that names which path completed it.

You will have at the end:

- `src-android/subtrees/splash/logic`: the Kotlin twin, with nine passing tests: four golden replays of the Swift recordings, a scenario that checks them, and four test-clock tests.
- `src-android/replay-runner`: the Kotlin replay runner, driven beside the Swift one by `tools/duet protocol-run --platform both`.
- `src-ios/Subtrees/Splash/SplashNode`: the shell, the builder and the SwiftUI view over the Swift twin, with four passing shell tests on virtual time.
- `src-ios/App`: the iOS app, generated from an XcodeGen spec and built with Xcode alone.
- `src-android/app`: the Android app, with two passing host tests on the JVM.
- `tools/duet verify` reporting eight fixture reports, four per lane, and `tools/duet doctor` reporting two target declarations.

## Where do you start?

Open `tutorial2-twins-start` from the [duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials). It is [Swift Tutorial 1](duet-swift-01-first-feature.md)'s finished tree plus one failing test, the closing exercise: the splash feature in Swift in `src-ios/Subtrees/Splash/SplashFeature`, its Swift scenario as the writer of four recordings, a generated `SplashEnvironmentMock`, and the Swift replay runner in `src-ios/Libraries/Replay`. It has no Kotlin and no UI. It pins Duet 0.7.0, duet-tools 0.34.0 and swift-sourcery-templates 0.10.2. You need Xcode 26.6 or later, a JDK 25, XcodeGen (`brew install xcodegen`) and the Android SDK with platform 36; the [series index](README.md#what-do-you-need-installed) lists them. Run the checks once before you edit anything:

```sh
tools/duet verify
```

The Swift lane runs 6 tests with one failure, `Tutorial2ExerciseMountBracketTests.testASecondMountArmsAFreshNet`, reported as `failed - Tutorial 2 exercise: mount a second store and pin the fresh net`; that is the exercise, and everything else is green. `TUTORIAL_SKIP_STUBS=1 tools/duet verify` skips the stub, which reports `Test skipped - the closing exercise; unset TUTORIAL_SKIP_STUBS to run it`, and ends with `duet verify: PASS`. The steps below run `verify` and `record` with `TUTORIAL_SKIP_STUBS=1` until the exercise is done: a bare `tools/duet record` runs the Swift package's tests, fails on the stub, and ends with `duet record: FAIL — fixtures not (fully) regenerated`.

## The steps

### Add the Kotlin plane

The Kotlin twin gets a Gradle root of its own, `src-android/`, beside `src-ios/`. Every Kotlin module of the tree goes in it: the splash module in this step, the replay runner in step 6 and the Android app in step 10. Copy the Gradle wrapper and its settings from Tutorial 1's tree, `tutorial1-complete/src-kmp/` in the same repository: `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `gradle/gradle-daemon-jvm.properties` and `gradle.properties`. Copy the version catalog, `gradle/libs.versions.toml`, from `tutorial2-complete/src-kmp/`, and delete its `skie` and `kotlin-multiplatform` entries. Copy the root build file from `tutorial2-complete/src-kmp/build.gradle.kts` and delete its `kotlin.multiplatform` and `skie` plugin lines; it then registers `kotlin.jvm`, the serialization plugin, KSP, the Android application plugin and the Compose compiler plugin once each with `apply false`, and its `subprojects` block leaves out every test class named `Tutorial*Exercise*` when `TUTORIAL_SKIP_STUBS=1` is set. Write the settings file with Tutorial 1's `pluginManagement` and `dependencyResolutionManagement` blocks, which declare the Duet family's Maven host, then the root name and one module:

**`src-android/settings.gradle.kts`**

```kotlin src-android/settings.gradle.kts
rootProject.name = "foyer-android"

// Feature modules live under subtrees/<feature>/logic and are included here
// one line per feature.
include(":subtrees:splash:logic")
```

The splash module applies the `kotlin.jvm` plugin where Tutorial 1's applied `kotlin.multiplatform` with a JVM target and three Apple targets. It compiles for the JVM only, so its dependencies sit in the plain `dependencies` block, with the test libraries under `testImplementation`:

**`src-android/subtrees/splash/logic/build.gradle.kts`**

```kotlin src-android/subtrees/splash/logic/build.gradle.kts
// The Kotlin twin of the splash, as a plain JVM module. The iPhone app runs
// the Swift twin under src-ios/Subtrees/Splash, so nothing here compiles for
// an Apple target. The Android app and the replay-runner depend on this
// module, and its tests are the Kotlin lane.
kotlin {
  jvmToolchain(25)
}

dependencies {
  api(libs.duet.kernel)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.duet.kernel.test)
  testImplementation(kotlin("test"))
  testImplementation(libs.kotlinx.coroutines.test)
}
```

The file starts with the `kotlin.jvm` and serialization plugins and ends with Tutorial 1's test-task block, which declares `parity/fixtures` as a test input so that a changed recording re-runs the tests. Declare the Kotlin plane in the manifest, below `replayRunner:`:

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
# Every Kotlin module in this tree is a plain JVM module (src/main, src/test):
# the iPhone app runs the Swift twin and links no Kotlin.
kotlinCore: jvm
```

With `kotlinCore: jvm`, `lint` accepts a Kotlin path under `src/main` and refuses one that names a multiplatform source set such as `commonMain`. Add Gradle's outputs to `.gitignore`:

**`.gitignore`**

```gitignore .gitignore
# Gradle / JVM
.gradle/
build/
local.properties
.kotlin/
```

Check the plane:

```sh
./src-android/gradlew -p src-android :subtrees:splash:logic:compileKotlin
tools/duet lint
```

Gradle ends with `BUILD SUCCESSFUL`; the module has no source yet. `lint` prints `lockstep-lint: OK (1 feature(s), 4 fixture(s), ledger: 0 waiver(s) / 0 island(s), mocks: 1 generator(s))`.

### Write the Kotlin twin

The Kotlin twin's two declaration files are Tutorial 1's, unchanged: copy `SplashFeature.kt` and `SplashSerializers.kt` from `tutorial1-complete/src-kmp/subtrees/splash/logic/src/commonMain/kotlin/dev/modaal/foyer/splash/` to `src/main/kotlin/dev/modaal/foyer/splash/` in the new module. `SplashFeature.kt` and `SplashFeature.swift` declare the same things in the same order: configuration, effect ids, state, actions, delegate events, effect payloads and the reducer. Kotlin spells each case as a PascalCase class with a `@SerialName` that equals the Swift case name. One case uses a different word on each side: Swift names the delegate payload `notifyListener`, and Kotlin names the class `NotifyHost` and gives it the serial name `notifyListener`, which is the name the recordings carry:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
public enum SplashEffectPayload: Equatable, Sendable, CanonicalSumCodable {
  /// Wait `afterMillis` on the environment's clock, then report
  /// `.safetyNetElapsed`. Runs under `SplashEffectIds.safetyNet`.
  case armSafetyNet(afterMillis: Int64)
  /// Hand a delegate event to the host.
  case notifyListener(SplashDelegateEvent)
}
```

**`src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashFeature.kt`**

```kotlin src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
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

Each reducer has its platform's shape. The Swift reducer mutates `inout` state and returns the effects; the Kotlin reducer returns a `Reduced` value that carries the new state and the effects. Both arm the safety net once, and both notify the host every time a completion path fires. Swift Tutorial 1 generated the Swift coders with `tools/duet canonical-sum`; the Kotlin twin registers each sum case by hand in `SplashSerializers.kt`, where `inline = true` marks `NotifyHost`, whose single value encodes bare like the unlabeled Swift case `notifyListener(SplashDelegateEvent)`. Name the Kotlin twin in the manifest row, below the Swift one:

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
features:
  splash:
    swift: src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
    kotlin: src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
```

With both `swift:` and `kotlin:` declared, `lint` compares the twins' declarations: the state's property names, and the action and effect-payload cases by wire name. A Kotlin case's wire name is its `@SerialName`, so `NotifyHost` matches the Swift `notifyListener`. `lint` also checks that both twins sit in the same subtree layout, `Subtrees/Splash/` on the Swift side and `subtrees/splash/` on the Kotlin side. Run `./src-android/gradlew -p src-android :subtrees:splash:logic:compileKotlin`, then `tools/duet lint`; Gradle ends with `BUILD SUCCESSFUL`, and `lint` prints the same `lockstep-lint: OK` line as in step 1.

### Replay the Swift recordings on the Kotlin core

The recordings under `parity/fixtures/` were written by the Swift scenario in Swift Tutorial 1. Kotlin's `FixtureRunner` replays a recording against the pure reducer, with no store and no effect handler: it feeds each recorded action to the reducer from the recorded starting state and compares the state and effects the reducer returns with the recorded ones, in canonical bytes. The golden test is Tutorial 1's file, unchanged; copy it to `src/test/kotlin/dev/modaal/foyer/splash/` in the module:

**`src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashGoldenTest.kt`**

```kotlin src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashGoldenTest.kt
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

Run the module's tests, then the checks across both lanes:

```sh
./src-android/gradlew -p src-android :subtrees:splash:logic:test
TUTORIAL_SKIP_STUBS=1 tools/duet verify
```

Gradle ends with `BUILD SUCCESSFUL`, and the report under `src-android/subtrees/splash/logic/build/reports/tests/test/` lists four tests and no failures: the Kotlin reducer reproduces every state and effect in four files that Swift wrote. `verify` now runs two lanes, the Swift package's tests and the Kotlin module's tests, expects one report per recording from each, and prints `duet verify: 8/8 fixture report(s) passed` before `duet verify: PASS`.

### Write the Kotlin scenario beside the Swift one

A scenario on each side describes the behavior in the same words: the same description, the same given, the same root steps, and four branches with the same labels. The Kotlin file is Tutorial 1's `SplashScenarioTest.kt` in `src/test`, with three changes: its doc comment, its `source` path under `src-android/`, and its last call. One branch on each side:

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift
  Branch("ceremony completes") {
    When("the animation reaches its end", .ceremonyFinished)
    Then("the arming latch is untouched") { $0.isArmed }
    ThenEffects("the host is notified, by the ceremony path") { $0 == notified(.ceremony) }
  }
```

**`src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt`**

```kotlin src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt
        branch("ceremony completes") {
          whenAction("the animation reaches its end", SplashAction.CeremonyFinished)
          then("the arming latch is untouched") { it.isArmed }
          thenEffects("the host is notified, by the ceremony path") {
            it == notified(SplashCompletionPath.Ceremony)
          }
        }
```

The last call is where the two scenarios differ. The Swift scenario ends in `ScenarioRunner.verifyOrRecord`: it writes one fixture per branch when `tools/duet record` runs it, and checks the committed fixtures on every other run. The Kotlin scenario ends in `ScenarioRunner.verify`, which checks the committed fixtures against the Kotlin reducer and writes nothing, even when Gradle runs with `-PregenFixtures=1`:

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift
final class SplashScenarioTests: XCTestCase {
  func testSplashScenario() throws {
    try ScenarioRunner.verifyOrRecord(splashScenario, reducer: splashReducer)
  }
}
```

**`src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt`**

```kotlin src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashScenarioTest.kt
    ScenarioRunner.verify(
      s,
      SplashState.serializer(),
      SplashActionSerializer,
      SplashEffectPayloadSerializer,
      ::splashReducer)
```

The manifest's `scenario:` key still names the Swift file, so the Swift scenario stays the one writer of record. Run `./src-android/gradlew -p src-android :subtrees:splash:logic:test` again; the report lists five tests and no failures. `TUTORIAL_SKIP_STUBS=1 tools/duet verify` still prints `duet verify: 8/8 fixture report(s) passed` and `duet verify: PASS`.

### Write the Kotlin runtime half

The runtime half is the environment interface, the effect handler and the store factory. Swift Tutorial 1 wrote the Swift half. The Kotlin half is Tutorial 1's `SplashEnvironment.kt` and `SplashRuntime.kt`, in `src/main`. The environment is the feature's door to the platform on both sides: a clock to wait on and a sink for the delegate event.

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashEnvironment.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashEnvironment.swift
public protocol SplashEnvironment {
  /// The clock the safety net waits on. The effect handler calls `sleep` here
  /// rather than `Task.sleep` directly, so a test can run the wait on virtual
  /// time.
  var clock: any KernelClock { get }

  /// Hand a delegate event to the host.
  func notifyHost(_ event: SplashDelegateEvent)
}
```

**`src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashEnvironment.kt`**

```kotlin src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashEnvironment.kt
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

The effect handler turns `armSafetyNet` into a sleep on the environment's clock followed by `safetyNetElapsed`, and `notifyListener` into a call on the environment. The Swift handler returns an `AsyncStream`, the Kotlin handler a `Flow`. The two factories differ in one parameter. The Swift `Store` runs on the main actor and owns its effect tasks. The Kotlin `Store` runs its effects on a `CoroutineScope` the caller passes in, which is how the Android host owns the store's lifetime in step 10. Tutorial 1's `SplashRuntime.kt` also declares `splashStateFlow`, which Tutorial 2's Apple framework exported to the SwiftUI shell; leave it out, because no Swift code reads the Kotlin store in this tree.

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashRuntime.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashRuntime.swift
/// The store a shell hosts: the reducer and the handler, wired.
@MainActor
public func makeSplashStore(
  environment: SplashEnvironment
) -> Store<SplashState, SplashAction, SplashEffectPayload> {
  Store(
    initialState: SplashState(),
    reducer: splashReducer,
    handler: splashEffectHandler(environment: environment))
}
```

**`src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashRuntime.kt`**

```kotlin src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashRuntime.kt
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

The test-clock suites are twins as well. Both drive the real effect handler through a test store on virtual time and pin the same four facts: the net does not fire before 3,000 ms, fires at 3,000 ms, fires once, and never fires after teardown. The Swift tests use the generated `SplashEnvironmentMock` over a `TestClock`. The Kotlin tests use `LiveClock`, which under `runTest` sleeps on the test scheduler's virtual time:

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashTestStoreTests.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashTestStoreTests.swift
  func testTeardownCancelsTheArmedNet() async {
    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let store = armedStore(environment)

    store.teardown()
    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis * 3))

    await store.finish()
    XCTAssertEqual(environment.notifyHostArgs, [])
  }
```

**`src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt`**

```kotlin src-android/subtrees/splash/logic/src/test/kotlin/dev/modaal/foyer/splash/SplashTestStoreTest.kt
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

The Kotlin twin's test double is `RecordingSplashEnvironment`, Tutorial 1's class in the test sources, which records every delegate event in `notified`. Copy `RecordingSplashEnvironment.kt` and `SplashTestStoreTest.kt` from Tutorial 1's `jvmTest` to `src/test`, then run `./src-android/gradlew -p src-android :subtrees:splash:logic:test`; the report lists nine tests and no failures.

### Add the Kotlin replay runner and run both protocol lanes

`tools/duet verify` checks each twin through its own test framework. The protocol lane checks both reducers through one driver. A replay runner is a small executable with one operation: it decodes a state and an action, reduces, and encodes the result. `tools/duet protocol-run` sends every recorded step through a runner and compares the bytes it returns with the recording. Swift Tutorial 1 added the Swift runner in `src-ios/Libraries/Replay`. The Kotlin runner is Tutorial 1's `replay-runner` module, unchanged: copy it from `tutorial1-complete/src-kmp/replay-runner/` to `src-android/replay-runner/`. It applies `kotlin.jvm` and `application`, depends on the kernel and the splash module, and is included in the settings file:

**`src-android/settings.gradle.kts`**

```kotlin src-android/settings.gradle.kts
// The replay-protocol endpoint `tools/duet protocol-run` drives.
include(":replay-runner")
```

Each runner registers the feature once, with the types it decodes and the reducer it runs:

**`src-ios/Libraries/Replay/Sources/ReplayAdapter/ReplayAdapter.swift`**

```swift src-ios/Libraries/Replay/Sources/ReplayAdapter/ReplayAdapter.swift
public let appReplayRegistry = ReplayRegistry([
  ReplayFeature.entry("splash", SplashState.self, SplashAction.self) { state, action in
    splashReducer(state: &state, action: action)
  }
])
```

**`src-android/replay-runner/src/main/kotlin/dev/modaal/foyer/replayrunner/Main.kt`**

```kotlin src-android/replay-runner/src/main/kotlin/dev/modaal/foyer/replayrunner/Main.kt
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
```

Run both lanes:

```sh
tools/duet protocol-run --platform both
```

`--platform both` builds and drives the Swift runner, then builds the Kotlin runner with Gradle's `:replay-runner:installDist` and drives it, and fails if either side fails:

```text
protocol-run [swift]: duet-replay v1, 1 feature(s) advertised by …/src-ios/Libraries/Replay/.build/debug/replay-runner
  4 leaf + 0 chain fixture(s), 9 step(s) byte-gated CLI-side (hop-derivation authoring checks stay in-process)
protocol-run [kotlin]: duet-replay v1, 1 feature(s) advertised by …/src-android/replay-runner/build/install/replay-runner/bin/replay-runner
  4 leaf + 0 chain fixture(s), 9 step(s) byte-gated CLI-side (hop-derivation authoring checks stay in-process)
protocol-run: PASS
```

The block above shortens the two runner paths and leaves out the timing line each side prints after its first line.

### Write the Swift shell and the SwiftUI view

The app-side Swift code goes in a sibling package, `src-ios/Subtrees/Splash/SplashNode`, so the feature package keeps resolving the Duet family and nothing else. `SplashNode` depends on the feature package by path and on the Duet `DuetShells` product for `ViewShell`, `StoreHost` and `StateTransitions`:

**`src-ios/Subtrees/Splash/SplashNode/Package.swift`**

```swift src-ios/Subtrees/Splash/SplashNode/Package.swift
  targets: [
    .target(
      name: "SplashNode",
      dependencies: [
        .product(name: "Duet", package: "duet"),
        .product(name: "DuetShells", package: "duet"),
        .product(name: "SplashFeature", package: "SplashFeature"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashNodeTests",
      dependencies: [
        "SplashNode",
        .product(name: "DuetTesting", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
```

The shell, `SplashViewShell`, subclasses `ViewShell`; it owns a `SplashViewState` with one published field, `isArmed`, and two intents, `appeared()` and `ceremonyFinished()`, each of which sends one action. Its `bind()` runs at `activate()`. It hosts the store first, which registers the store's `teardown()` with the shell's `StoreHost`, and then adopts a `StateTransitions` observation over `store.$state` that projects each state into the view state. `StoreHost` unwinds in reverse registration order, so `deactivate()` stops the projection and then cancels the store's effects. The store is Duet's Swift `Store`: `send` reduces synchronously on the main actor and publishes the new state before it returns, so the shell observes the store itself.

**`src-ios/Subtrees/Splash/SplashNode/Sources/SplashNode/SplashViewShell.swift`**

```swift src-ios/Subtrees/Splash/SplashNode/Sources/SplashNode/SplashViewShell.swift
  override public func bind() {
    // Registered first so it unwinds last: the projection below stops before
    // the store's teardown cancels its effects.
    host.host(store)

    // State to view state. `StateTransitions` replays the current value on
    // subscription, so this adoption is also the initial projection.
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }
```

`SplashBuilder.swift` holds the live environment, a struct over `LiveClock` and a closure to the host, and the builder, which wires a mount from two parts:

**`src-ios/Subtrees/Splash/SplashNode/Sources/SplashNode/SplashBuilder.swift`**

```swift src-ios/Subtrees/Splash/SplashNode/Sources/SplashNode/SplashBuilder.swift
  @MainActor
  public func buildSplash(
    onDelegate: @escaping (SplashDelegateEvent) -> Void
  ) -> SplashChild {
    let store = makeSplashStore(environment: LiveSplashEnvironment(onDelegate: onDelegate))
    return SplashChild(shell: SplashViewShell(store: store))
  }
```

The view, `SplashView.swift`, is Tutorial 2's with the subtitle "One behavior, two cores". It reads the shell's view state, calls `appeared()` from `onAppear`, owns the 1.6-second reveal because the animation is presentation, and calls `ceremonyFinished()` from the animation's completion. Create the empty directory `Tests/SplashNodeTests/` before you build: `Package.swift` declares the test target, and without its directory `swift build` stops with `error: 'splashnode': target 'SplashNodeTests' has overlapping sources`. Then build the package with `(cd src-ios/Subtrees/Splash/SplashNode && swift build)`; it ends with `Build complete!`.

### Test the shell on virtual time

The shell's tests use the same generated double as the feature's tests. A generated mock goes into the test target that uses it, one manifest row per target, so add a second row to the manifest's `mocks:` section that writes `SplashEnvironmentMock` into `SplashNodeTests`:

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
    splashnode_mocks:
      output: src-ios/Subtrees/Splash/SplashNode/Tests/SplashNodeTests/Generated/SplashNodeMocks.swift
      template: Mocks.swifttemplate
      sources:
        - src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature
      args:
        - import=Duet
        - import=SplashFeature
```

Run `tools/duet mocks`; it regenerates every row's file and ends with one line per row:

```text
  SplashFeatureMocks.swift — 1 types, 57 lines
  SplashNodeMocks.swift — 1 types, 57 lines
```

`SplashViewShellSpec` drives the shell with a `SplashEnvironmentMock` whose clock is a `TestClock`, so the safety net's sleep ends only when the test advances that clock: first by 2,999 ms, then by 1 ms:

**`src-ios/Subtrees/Splash/SplashNode/Tests/SplashNodeTests/SplashViewShellSpec.swift`**

```swift src-ios/Subtrees/Splash/SplashNode/Tests/SplashNodeTests/SplashViewShellSpec.swift
  /// The safety net, through the shell: three seconds of virtual time after
  /// `appeared()`, the host hears the safety-net path.
  func testTheSafetyNetNotifiesTheHostOnVirtualTime() async {
    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let shell = SplashViewShell(store: makeSplashStore(environment: environment))
    shell.activate()
    defer { shell.deactivate() }

    shell.appeared()
    await clock.advance(byNanoseconds: 2_999_000_000)
    XCTAssertEqual(environment.notifyHostArgs, [], "the net waits its full duration")

    await clock.advance(byNanoseconds: 1_000_000)
    await Task.megaYield()
    XCTAssertEqual(environment.notifyHostArgs, [.completed(path: .safetyNet)])
  }
```

The other three rows check that `appeared()` projects back before it returns, that `ceremonyFinished()` reaches the environment with the ceremony path, and that the armed net never fires after `deactivate()`. Run `(cd src-ios/Subtrees/Splash/SplashNode && swift test)`; it executes 4 tests with 0 failures. Tutorial 2's shell tests run on real time, because no test dispatcher crosses the Kotlin/Native boundary; this store is Swift, so a `TestClock` stands where the builder puts the wall clock. `tools/duet lint` now ends its line with `mocks: 2 generator(s))`.

### Build the iOS app

The app target has no feature code. It links one package, `SplashNode`, which brings the Swift twin and the Duet packages with it. The XcodeGen spec declares the package and the dependency, with no pre-action and no build script:

**`src-ios/App/xcodegen.yml`**

```yaml src-ios/App/xcodegen.yml
packages:
  # The splash's shell package; it brings the Swift twin with it. The app
  # links Swift packages only: no framework is assembled before the build.
  SplashNode:
    path: ../Subtrees/Splash/SplashNode
    group: ''
```

**`src-ios/App/xcodegen.yml`**

```yaml src-ios/App/xcodegen.yml
    dependencies:
    - package: SplashNode
      product: SplashNode
```

A host object mounts the splash through the builder, receives its delegate event, tears the splash down and switches the phase. Both completion paths notify the host, and the host acts on the first only. `SplashDelegateEvent` is a Swift enum, so the host matches it directly:

**`src-ios/App/Foyer/AppHost.swift`**

```swift src-ios/App/Foyer/AppHost.swift
  private func splashCompleted(_ event: SplashDelegateEvent) {
    // Both completion paths notify every time; the first notification moves
    // the app on and the rest arrive after the splash is gone.
    guard case .splash = phase,
      case .completed(let path) = event
    else { return }
    splash?.shell.deactivate()
    splash = nil
    phase = .placeholder(completedBy: path)
  }
```

`AppScreen.swift` renders the splash while the phase is `.splash` and the placeholder after; it is Tutorial 2's file with `SplashFeature` and `SplashNode` imported in place of `FoyerKit` and `SplashShell`, a direct `switch` on `SplashCompletionPath`, and no Tutorial 3 line on the placeholder. `SceneDelegate.swift` builds the host, shows the window, activates the host after the window is visible, and calls `teardown()` in `sceneDidDisconnect`; it and `AppDelegate.swift` are Tutorial 2's files apart from comments. Add `DerivedData/`, `*.xcodeproj`, `*.xcuserdata/` and the generated `src-ios/App/Foyer/Info.plist` to `.gitignore`, then generate and build:

```sh
(cd src-ios/App && xcodegen generate --spec xcodegen.yml && xcodebuild build -project Foyer.xcodeproj -scheme Foyer -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO | tail -1)
```

The last line is `** BUILD SUCCEEDED **`. The build runs no Gradle task: its log has no line naming `gradlew` or `assemble_kit`. The command also needs no `ARCHS=arm64`; Tutorial 2 set it because its prebuilt framework has no x86_64 slice, and this app links no prebuilt binary.

### Build the Compose app

The Android app is Tutorial 2's `src-kmp/app` module at `src-android/app`. Apart from comments, it differs in two places: the splash subtitle reads "One behavior, two cores", and the placeholder has no Tutorial 3 line. Include it in the settings file:

**`src-android/settings.gradle.kts`**

```kotlin src-android/settings.gradle.kts
// The Android app: the Compose shells and the Activity, over the Kotlin twin.
include(":app")
```

The module depends on the splash module directly, on the kernel, and on the Duet `shells-compose` artifact for `StoreHost` and `RetainedRoot`:

**`src-android/app/build.gradle.kts`**

```kotlin src-android/app/build.gradle.kts
  implementation(project(":subtrees:splash:logic"))
  implementation(libs.duet.kernel)
  // StoreHost and RetainedRoot: the teardown registry and the retained
  // carrier the Activity keeps the app on across rotation.
  implementation(libs.duet.shells.compose)
  implementation(libs.essenty.instance.keeper)
```

`SplashBuilder.kt` is the Swift builder's counterpart: a live environment over `LiveClock`, and the module's `makeSplashStore` on a scope the host passes in. `AppHost.kt` registers the store in a `StoreHost`, tears it down on the first completion and switches the phase; it is Android-free, so a JVM test drives it in step 11. On Android the composable is the shell: it collects the store's state, and its launched effect sends `Appeared`, plays the reveal, then sends `CeremonyFinished`. After a rotation the effect runs again; the reducer's arming guard makes the second `Appeared` inert, and the host ignores a second completion.

**`src-android/app/src/main/kotlin/dev/modaal/foyer/app/SplashScreen.kt`**

```kotlin src-android/app/src/main/kotlin/dev/modaal/foyer/app/SplashScreen.kt
@Composable
fun SplashScreen(store: SplashStore, modifier: Modifier = Modifier) {
  val state by store.state.collectAsState()
  val reveal = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    store.send(SplashAction.Appeared)
    reveal.animateTo(1f, tween(CEREMONY_MILLIS, easing = FastOutSlowInEasing))
    store.send(SplashAction.CeremonyFinished)
  }
```

`MainActivity` keeps the host on a retained scope through `RetainedRoot`, so rotation hands back the same host with its running store, and finishing the Activity is the one teardown. `AppRoot` collects the phase and shows `SplashScreen` or the placeholder. [Tutorial 2](duet-02-two-apps.md#build-the-compose-app) walks through each file. Build with `./src-android/gradlew -p src-android :app:assembleDebug`; it ends with `BUILD SUCCESSFUL`.

### Test the host on the JVM

The host test is Tutorial 2's `AppHostTest.kt`, in `src-android/app/src/test/kotlin/dev/modaal/foyer/app/`. The store runs on the test scope, so the safety net waits on virtual time. The first test drives the ceremony path and checks that the torn-down store's net never fires; the second advances the clock past the net's duration and lands on the safety-net placeholder:

**`src-android/app/src/test/kotlin/dev/modaal/foyer/app/AppHostTest.kt`**

```kotlin src-android/app/src/test/kotlin/dev/modaal/foyer/app/AppHostTest.kt
  @Test
  fun theSafetyNetPathLandsOnThePlaceholder() = runTest {
    val app = AppHost(backgroundScope)
    assertNotNull(app.splash).send(SplashAction.Appeared)

    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()

    assertEquals(AppPhase.Placeholder(SplashCompletionPath.SafetyNet), app.phase.value)
    assertNull(app.splash)
    app.teardown()
  }
```

Run `./src-android/gradlew -p src-android :app:testDebugUnitTest`; it ends with `BUILD SUCCESSFUL`, and the report under `src-android/app/build/reports/tests/testDebugUnitTest/` lists two tests.

### Declare the targets and run both apps from the command line

`tools/duet doctor` reads the tree's declaration file and cross-checks it against what is on disk. Declare the two app targets, paired under one name. The file's top-level `template` and its `duet` block name the template `duet-twins` and the flavor `twins`:

**`.modaal/project.json`**

```json .modaal/project.json
  "targets": {
    "Foyer": {
      "platform": "iOS",
      "architecture": "duet",
      "template": "duet-twins",
      "pair": "Foyer",
      "deviceTypes": ["iPhone"],
      "iphoneOrientations": ["UIInterfaceOrientationPortrait"]
    },
    "FoyerAndroid": {
      "platform": "Android",
      "architecture": "duet",
      "template": "duet-twins",
      "pair": "Foyer",
      "androidOrientations": ["Portrait"]
    }
  },
```

Run `tools/duet doctor`; it ends with `duet doctor: PASS — 2 target declaration(s), 16 Swift source file(s) scanned`.

Then run both apps from the command line. Boot an iPhone 17 simulator (`xcrun simctl boot "iPhone 17"`, then `open -a Simulator` to show it), build into a derived-data directory inside the app folder, install the app and launch it:

```sh
(cd src-ios/App && xcodegen generate --spec xcodegen.yml && xcodebuild build -project Foyer.xcodeproj -scheme Foyer -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build CODE_SIGNING_ALLOWED=NO)
xcrun simctl install booted src-ios/App/build/Build/Products/Debug-iphonesimulator/Foyer.app
xcrun simctl launch booted dev.modaal.foyer
```

Start an emulator at API 36, then install the debug build and start its Activity. The application id is `dev.modaal.foyer`, and the Activity's class is in the namespace `dev.modaal.foyer.app`:

```sh
./src-android/gradlew -p src-android :app:installDebug
adb shell am start -n dev.modaal.foyer/dev.modaal.foyer.app.MainActivity
```

Both apps reveal the name over 1.6 seconds and then show the placeholder with the line `Splash completed by the ceremony.`

![Two phone screens side by side, an iPhone simulator on the left and an Android emulator on the right. Each shows the word Foyer in large type, the line One behavior, two cores under it, and a thin progress line below, all partly faded in during the reveal animation.](images/duet-tutorial-twins-2-splash-pair.png)

*The splash mid-reveal, 0.8 seconds after launch: an iPhone 17 simulator on iOS 27.0 running the Swift core on the left, and a Pixel 8 emulator at API 36 running the Kotlin core on the right, from tutorial2-twins-complete at Duet 0.7.0 and duet-tools 0.34.0.*

### Change one behavior, in Swift first

A behavior change edits both twins. This step changes the safety net's duration from 3,000 ms to 2,500 ms, records it from the Swift scenario, and watches the Kotlin lane fail until the Kotlin twin changes too. Commit your work first, so that one `git checkout` undoes the step. The duration is a constant in each twin:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
public enum SplashConfig {
  /// How long the safety net waits before it completes the splash on the
  /// ceremony's behalf. The reducer carries the value out in the effect
  /// payload, so the recordings pin it.
  public static let safetyNetMillis: Int64 = 3_000
}
```

**`src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashFeature.kt`**

```kotlin src-android/subtrees/splash/logic/src/main/kotlin/dev/modaal/foyer/splash/SplashFeature.kt
object SplashConfig {
  /**
   * How long the safety net waits before it completes the splash on the
   * ceremony's behalf. The reducer carries the value out in the effect
   * payload, so the recordings pin it.
   */
  const val SAFETY_NET_MILLIS: Long = 3_000L
}
```

Set `safetyNetMillis` to `2_500` in `SplashFeature.swift`, then record:

```sh
TUTORIAL_SKIP_STUBS=1 tools/duet record
```

The reducer carries the duration out in the `armSafetyNet` payload, and every branch starts from the root step that arms the net, so the Swift scenario rewrites all four recordings. No line carries the `(metadata-only)` tag, because the behavioral fields changed:

```text
duet record: rewrote 4 fixture(s):
  parity/fixtures/splash.both-paths-notify-twice.fixture.json
  parity/fixtures/splash.ceremony-completes.fixture.json
  parity/fixtures/splash.repeat-appear-inert.fixture.json
  parity/fixtures/splash.safety-net-fires.fixture.json
review the diff (git diff -- parity/fixtures), then run `duet verify` for both lanes
```

Run `TUTORIAL_SKIP_STUBS=1 tools/duet verify`. The Swift lane passes on the recordings it just wrote. The Kotlin lane fails on each of the four recordings at the same field, the duration in the first step's effect:

```text
duet verify: 4/8 fixture report(s) passed
✗ fixture 'splash.both-paths-notify-twice' step 0 'the splash appears' — effects diverged   [kotlin]
  at expectedEffects[0].payload.value.afterMillis
  expected: 2500   actual: 3000
```

Set `SAFETY_NET_MILLIS` to `2_500L` in `SplashFeature.kt` and run `TUTORIAL_SKIP_STUBS=1 tools/duet verify` again; it prints `duet verify: 8/8 fixture report(s) passed` and `duet verify: PASS`. Restore the tree with `git checkout -- src-ios src-android parity/fixtures`.

## What you now have

- A Swift twin and a Kotlin twin of the splash, each with its scenario and its test-clock suite, checked against one set of four recordings written by the Swift scenario.
- The Kotlin twin on a plain `kotlin.jvm` module under `src-android/`, declared with `kotlinCore: jvm`, replaying the Swift recordings in its golden test.
- Both replay runners passing `tools/duet protocol-run --platform both`.
- A Swift shell with its builder, view and four virtual-time tests in `src-ios/Subtrees/Splash/SplashNode`, and a Compose screen with its builder, host and two tests in `src-android/app`.
- Two apps, each on its own core, that land on the same placeholder, both built and run from the command line.
- `TUTORIAL_SKIP_STUBS=1 tools/duet verify` passing with eight fixture reports, and a doctor report with two targets.

## Exercise: finish the mount-bracket test

`tutorial2-twins-start` carries `Tutorial2ExerciseMountBracketTests.swift` in the Swift feature package's tests. Its given is written: one test store over a `SplashEnvironmentMock` on a `TestClock`, armed, then torn down. Delete its `XCTSkipIf` call and finish it so it pins that a second store over the same environment arms a fresh net, and that after advancing the clock by the net's duration `environment.notifyHostArgs` holds exactly one `.completed(path: .safetyNet)`, the second store's. The first store's net was cancelled at teardown, and `first.finish()` fails on any action that arrived there. Then write the same test for the Kotlin twin in the splash module's `src/test`, on the Kotlin `TestStore` under `runTest` with a `RecordingSplashEnvironment`. Run the checks without `TUTORIAL_SKIP_STUBS`: `swift test` in `src-ios/Subtrees/Splash/SplashFeature` executes 6 tests with 0 failures, the Kotlin module's report lists 10 tests, and `tools/duet verify` prints `duet verify: 8/8 fixture report(s) passed` and `duet verify: PASS`. `tutorial2-twins-complete` carries the finished tests as `SplashMountBracketTests.swift` and `SplashMountBracketTest.kt`.

## Common questions

<details>
<summary><b>Which scenario writes the recordings, and why only one?</b></summary>

The manifest's `scenario:` key names the Swift file, so the Swift scenario writes. One writer means each recording's bytes come from one runtime and its `scenario.source` names one file. The Kotlin scenario calls `ScenarioRunner.verify`, which never writes. `tools/duet record --feature splash --platform kotlin` refuses, and names the command to use instead:

```text
duet record: REFUSED — --platform kotlin --feature splash: the feature's scenario is Swift (src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift), and the scenario is the writer of its fixtures. Record through it: `duet record --feature splash --platform swift`. The Kotlin twin replays the recordings in `duet verify --feature splash --kotlin-only`.
```

</details>

<details>
<summary><b>What happens if I add a branch to one scenario only?</b></summary>

Added to the Swift scenario, the branch becomes a fifth recording at the next `tools/duet record`. `lint` fails until the manifest's `fixtures:` list names it (`fixture on disk but not in manifest`), `verify` fails until `parity/feature-specs/splash.md` mentions it, and then `verify` fails on the Kotlin lane, which ran and did not replay it:

```text
✗ missing fixture report(s) — lane ran but did not replay these:
    splash.appear-after-ceremony-inert [kotlin]
```

The fix the message names is one more replay method in `SplashGoldenTest.kt`. Added to the Kotlin scenario only, the branch has no recording, and `verify` fails on the Kotlin lane with `fixture missing or unreadable`. The recording has to come from the Swift scenario, the writer, so the branch goes there too.

</details>

<details>
<summary><b>Why is the Kotlin module not multiplatform?</b></summary>

The iPhone app runs the Swift twin and links no Kotlin, so no Apple target needs to compile the module. A `kotlin.jvm` module builds with the JDK and Gradle alone, without the Kotlin/Native toolchain Tutorial 2's framework downloads on its first build. `kotlinCore: jvm` in the manifest declares this for every Kotlin module of the tree, and `lint` then refuses a Kotlin path in a multiplatform source set such as `commonMain`.

</details>

<details>
<summary><b>Why does the Swift shell hold the store directly?</b></summary>

Duet's Swift `Store` is a main-actor `ObservableObject`. `send` reduces synchronously, and the new state is published through its `@Published` `state` before `send` returns, which is the ordering `StateTransitions` and the other shell helpers assume. Tutorial 2's shell needs the `BridgedStore` mirror because a Kotlin `StateFlow` bridged into Swift delivers every value one continuation hop later.

</details>

<details>
<summary><b>What does the iPhone app link?</b></summary>

Swift packages only: `SplashNode`, which brings `SplashFeature` and the Duet Swift packages. The build runs no Gradle task. A Release build's executable has 2,310 symbols, none with Kotlin/Native's `kfun:` prefix, and the app bundle has no `Frameworks` directory. [Kotlin/Native GC and Swift ARC](https://docs.modaal.dev/articles/kotlin-native-gc-swift-arc) describes what the shared-core version links into the iPhone app.

</details>

<details>
<summary><b>What does a behavior change cost?</b></summary>

Two reducer edits, one per language, and one `record` from the Swift scenario. The last step shows the order: the Swift edit and the new recordings first, then the Kotlin lane fails until the Kotlin edit lands. The shared-core version edits one reducer; [What does sharing the logic cost?](https://docs.modaal.dev/articles/share-code-ios-android#what-does-sharing-the-logic-cost) gives Modaal's measured cost of a change shipped to both apps on that version.

</details>

<details>
<summary><b>Does Modaal create this tree?</b></summary>

Not yet. Modaal scaffolds the Swift flavor and the Kotlin Multiplatform flavor; this tutorial builds the twin-core tree by hand with the `duet` command-line tool. [Duet Twins](https://docs.modaal.dev/articles/duet-twins#what-will-ship) lists what Modaal adds for this shape.

</details>

## Sources and further reading

- [The Duet framework repository](https://github.com/modaal-agent/duet) — the Swift `Store`, the `DuetShells` package this page's shell subclasses (`ViewShell`, `StoreHost`, `StateTransitions`), `DuetTesting` (`ScenarioRunner`, `TestStore`, `TestClock`), the Kotlin `kernel-test` artifact (`FixtureRunner`, `ScenarioRunner.verify`, `TestStore`), and two contracts: `contracts/store-kernel-contract.md` for the reducer and store shape on each platform, and `contracts/serialization.md` for the fixture format both twins replay.
- [The duet-tools repository](https://github.com/modaal-agent/duet-tools) — the `duet` command-line tool, and `contracts/manifest.md`, the manifest grammar: `kotlinCore`, the declaration-parity check across twins, and the `mocks:` rows.
- [swift-sourcery-templates](https://github.com/modaal-agent/swift-sourcery-templates) — the `Mocks` template `tools/duet mocks` runs to generate `SplashEnvironmentMock`.
- [The duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) — `tutorial2-twins-start` and `tutorial2-twins-complete`, and the checks CI runs on them.
- [Duet Twins](https://docs.modaal.dev/articles/duet-twins) — the architecture this tutorial builds, compared with the Kotlin Multiplatform flavor.
- [Tutorial 2: One Behavior, Two Apps](duet-02-two-apps.md) — the same step with one Kotlin core in both apps.
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) — the project spec format `src-ios/App/xcodegen.yml` uses.
- [The Duet glossary](https://docs.modaal.dev/articles/duet-glossary) — [twin](https://docs.modaal.dev/articles/duet-glossary#twin), [scenario](https://docs.modaal.dev/articles/duet-glossary#scenario), [replay](https://docs.modaal.dev/articles/duet-glossary#replay), [shell](https://docs.modaal.dev/articles/duet-glossary#shell) and [the Swift and Kotlin lanes](https://docs.modaal.dev/articles/duet-glossary#swift-and-kotlin-lanes).

## Read next

- **[Tutorial 2: One Behavior, Two Apps](duet-02-two-apps.md)** — The shared-core version of this step: one Kotlin core built into an Apple framework and mounted in a SwiftUI app and a Compose app.
- **[Duet Twins](https://docs.modaal.dev/articles/duet-twins)** — What the twin-core shape costs, who it is for, and what Modaal adds for it.
- **[Tutorial 3: Composing Features](duet-03-composing-features.md)** — The root, the sign-in gate and the profile tree. It continues from Tutorial 2's finished tree, on the shared Kotlin core.
- **[The Duet tutorial series](README.md)** — The nine tutorials, the Swift-first track, the prerequisites and the versions they are verified against.
