# Duet Swift Tutorial 1: Your First Feature

[Series overview](README.md) · [Duet Twins Tutorial 2: One Behavior, Two Cores →](duet-twins-02-two-cores.md) · [On docs.modaal.dev](https://docs.modaal.dev/tutorials/duet-swift-01-first-feature)

Trees: [`tutorial1-swift-start/`](../tutorial1-swift-start) is what you open, [`tutorial1-swift-complete/`](../tutorial1-swift-complete) is what you have at the end. Run either tree's checks with `scripts/run-tree.sh tutorial1-swift-start`.

In this tutorial you write the splash feature of Foyer as a Duet feature in Swift: its state, its actions, its effects as data, and a pure reducer that turns one into the next. You describe the feature's behavior as a scenario, record the scenario into four fixture files, and compare them with the four files Tutorial 1's Kotlin core records. A generated test double and a test-clock suite pin the one piece of timing. There is no UI in this tutorial: [Duet Twins Tutorial 2](duet-twins-02-two-cores.md) starts from the tree this page ends with, adds a Kotlin twin of the feature, and mounts the splash in a SwiftUI app and a Compose app. [Tutorial 1](duet-01-first-feature.md), the first page of the [nine-tutorial series](README.md), is the same tutorial in Kotlin Multiplatform.

> [!NOTE]
> **Modaal's Swift flavor emits this package layout.** A Duet project created in
> [Modaal](https://modaal.dev) with the Swift flavor puts each feature in a
> package at `src-ios/Subtrees/<Name>/<Name>Feature` and the replay runner at
> `src-ios/Libraries/Replay`, the layout this page builds. The trees this
> tutorial opens are written by hand with the open-source `duet` command-line
> tool (0.34.0 or later) and the published Duet packages, so you can follow
> every step without Modaal.

## What will you build?

A splash screen has one job: play an animation, then hand control to the host. The splash you write here does that with a safety net. When the splash appears it arms a timer; when the animation ends, or when the timer fires, the feature tells its host that the splash completed and by which path. Both paths notify the host every time they fire, because the host treats a repeat as a no-op, and a second `.appeared` is inert so the timer is armed once per mount. That is the whole behavior, and it fits in one state field, three actions and two effect payloads.

You will have at the end:

- `src-ios/Subtrees/Splash/SplashFeature`, a Swift package with the feature's types, its reducer, its generated coders, its environment protocol and its effect handler.
- Four recordings under `parity/fixtures/`: `splash.ceremony-completes`, `splash.safety-net-fires`, `splash.both-paths-notify-twice` and `splash.repeat-appear-inert`.
- `SplashEnvironmentMock`, a test double that `tools/duet mocks` generates from the environment protocol, and a test-clock suite that uses it.
- A green `tools/duet verify`: the scenario replays the four recordings against the reducer, and the safety net's timing holds on a virtual clock.
- `src-ios/Libraries/Replay`, a replay runner the `duet` tool drives over a protocol.

## Where do you start?

Clone the [duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) and open `tutorial1-swift-start`. It is a Swift project with no feature yet: an empty parity manifest, empty `parity/feature-specs/`, `parity/fixtures/` and `src-ios/Subtrees/` directories, and `tools/duet`, which fetches the `duet` command-line tool at version 0.34.0 on first use. Two more versions are pinned as you go: the package you write in step 1 depends on the Duet framework at 0.7.0, and the generator row you add in step 8 pins swift-sourcery-templates 0.10.2, the bundle that generates the test double. You need Xcode 26.6 or later on macOS; this tutorial needs no JDK and no Android SDK. Every path on this page is relative to `tutorial1-swift-start`. Confirm the tree is green before you edit anything:

```sh
tools/duet verify
```

The output includes a note that no features are declared, and its last line starts with `duet verify: PASS`. The finished tree is `tutorial1-swift-complete` in the same repository; every code block on this page that names a path is an excerpt of it. [Tutorial 1](duet-01-first-feature.md) is the Kotlin version of this tutorial: it builds the same feature in Kotlin Multiplatform from `tutorial1-start`, and step 7 compares its recordings with the ones you write here.

## The steps

### Declare the feature package and its manifest row

A Duet feature in Swift is a Swift package under `src-ios/Subtrees/<Feature>/<Feature>Feature`. Its library target holds the feature's types, its reducer and its runtime; its test target holds the scenario and the store tests. The package depends on the Duet framework and nothing else: the library uses the `Duet` product, and the tests add `DuetTesting`. Create `src-ios/Subtrees/Splash/SplashFeature/Package.swift`. It starts with `// swift-tools-version:5.9` and `import PackageDescription`, and defines one setting that both targets use, `strictConcurrency`, which is `[.enableExperimentalFeature("StrictConcurrency")]`: complete concurrency checking under the Swift 5 language mode. The package declaration follows:

**`src-ios/Subtrees/Splash/SplashFeature/Package.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Package.swift
let package = Package(
  name: "SplashFeature",
  platforms: [
    // iOS for the app; macOS so the Swift lane runs as plain `swift test`.
    .iOS(.v17),
    .macOS(.v14),
  ],
  products: [
    .library(name: "SplashFeature", targets: ["SplashFeature"])
  ],
  dependencies: [
    // The Duet framework, pinned exactly: a newer release is a deliberate
    // re-pin, made in every manifest of the tree at once.
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0")
  ],
  targets: [
    .target(
      name: "SplashFeature",
      dependencies: [
        .product(name: "Duet", package: "duet")
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashFeatureTests",
      dependencies: [
        "SplashFeature",
        .product(name: "DuetTesting", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
  ]
)
```

The macOS platform is what lets `tools/duet verify` run the tests as plain `swift test` on your Mac, with no simulator. Create the two directories the targets name: `Sources/SplashFeature`, holding a `SplashFeature.swift` that contains only `import Duet` for now, and an empty `Tests/SplashFeatureTests`. Then build the package:

```sh
(cd src-ios/Subtrees/Splash/SplashFeature && swift build)
```

The first build resolves and compiles the Duet package. It warns that the test target has no source files yet, and its last line starts with `Build complete!`:

```text
warning: 'splashfeature': Source files for target SplashFeatureTests should be located under 'Tests/SplashFeatureTests', 'Tests', or a custom sources path can be set with the 'path' property in Package.swift
```

`parity/manifest.yaml` is what the `duet` tool reads: which file holds the feature, which types are its state, action and effect payload, which test is its scenario, and which recordings it owns. Replace the empty `features:` key with the feature's row, and delete the comment lines above it that describe the empty key:

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
features:
  splash:
    swift: src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
    state: SplashState
    action: SplashAction
    effectPayload: SplashEffectPayload
    scenario: src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift
    fixtures:
      - splash.ceremony-completes
      - splash.safety-net-fires
      - splash.both-paths-notify-twice
      - splash.repeat-appear-inert
```

A row that declares `swift:` and no `kotlin:` runs the Swift lane only, rooted at the package that contains the `swift:` file. Run `tools/duet lint`. It fails, and the failure names what the next steps write, the scenario file and the four recordings:

```text
lockstep-lint: FAIL
  ✗ [splash] scenario file missing on disk: src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift
  ✗ [splash] fixture listed but missing on disk: splash.ceremony-completes — just declared? run: tools/duet record --feature splash
  ✗ [splash] fixture listed but missing on disk: splash.safety-net-fires — just declared? run: tools/duet record --feature splash
  ✗ [splash] fixture listed but missing on disk: splash.both-paths-notify-twice — just declared? run: tools/duet record --feature splash
  ✗ [splash] fixture listed but missing on disk: splash.repeat-appear-inert — just declared? run: tools/duet record --feature splash
```

### Write the state, the actions and the effect payloads

Everything the feature knows is data. The state is one field, the arming latch. The actions are the three things that can happen: two reports from the shell (the splash appeared; the animation finished) and one from the environment (the timer elapsed). The effect payloads describe what the feature asks the outside world to do, without doing it: wait on a clock, or tell the host something. The delegate event is what the host receives, and its `path` says which of the two ways completed the splash. The state is a `Codable` struct; the actions, the delegate events and the effect payloads are enums that conform to `CanonicalSumCodable`, because the recordings are JSON and every sum type on the reducer's boundary needs one fixed coding. Write them in `SplashFeature.swift`, below the import:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
// MARK: - Configuration

public enum SplashConfig {
  /// How long the safety net waits before it completes the splash on the
  /// ceremony's behalf. The reducer carries the value out in the effect
  /// payload, so the recordings pin it.
  public static let safetyNetMillis: Int64 = 3_000
}

public enum SplashEffectIds {
  /// The safety net's effect id. One net is in flight per store: a second
  /// `.run` under the same id cancels the first, and the store's teardown
  /// cancels it outright.
  public static let safetyNet: EffectID = "splash.safetyNet"
}

// MARK: - State

public struct SplashState: Equatable, Codable, Sendable {
  /// The arming latch: the safety net is armed once per mount.
  public var isArmed: Bool

  public init(isArmed: Bool = false) {
    self.isArmed = isArmed
  }
}

// MARK: - Actions

public enum SplashAction: Equatable, Sendable, CanonicalSumCodable {
  /// Shell report: the splash is on screen and the safety net should arm.
  case appeared
  /// Shell report: the splash animation reached its end.
  case ceremonyFinished
  /// Environment report: the armed delay elapsed.
  case safetyNetElapsed
}

// MARK: - Delegate events

/// Which of the two paths completed the splash.
public enum SplashCompletionPath: Equatable, Sendable, CanonicalSumCodable {
  case ceremony
  case safetyNet
}

/// What the splash tells its host. The host decides what happens next.
public enum SplashDelegateEvent: Equatable, Sendable, CanonicalSumCodable {
  case completed(path: SplashCompletionPath)
}

// MARK: - Effect payloads

public enum SplashEffectPayload: Equatable, Sendable, CanonicalSumCodable {
  /// Wait `afterMillis` on the environment's clock, then report
  /// `.safetyNetElapsed`. Runs under `SplashEffectIds.safetyNet`.
  case armSafetyNet(afterMillis: Int64)
  /// Hand a delegate event to the host.
  case notifyListener(SplashDelegateEvent)
}
```

Two choices here matter later. The timer's duration is a constant that the reducer copies into the payload, so the recordings pin the number and a retiming shows up as a diff in a fixture file. The effect id `splash.safetyNet` gives the timer cancel-in-flight semantics: the kernel's [Store](https://docs.modaal.dev/articles/duet-glossary#store) cancels a running effect when a new one starts under the same id, and cancels every running effect at teardown. The payload case is named `notifyListener` because Duet's chain runner, which [Tutorial 3](duet-03-composing-features.md) uses, decodes a delegate event from the `notifyListener` payload at each hop between features; the environment method that carries it out, in step 4, is `notifyHost`.

`CanonicalSumCodable` comes with no coding of its own. Until the coders exist, `swift build` fails on each enum with `type 'SplashAction' does not conform to protocol 'CanonicalSumCodable'`. Generate them:

```sh
tools/duet canonical-sum
```

```text
duet canonical-sum: wrote 1 coder file(s):
  src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashSerialization.generated.swift
review and commit the diff (generated coders are committed sources)
```

The tool finds the package through the manifest's `swift:` path and writes one extension per enum. Swift's synthesized `Codable` would write `.armSafetyNet(afterMillis: 3000)` as `{"armSafetyNet":{"afterMillis":3000}}`; the recordings need `{"case":"armSafetyNet","value":{"afterMillis":3000}}`, an object with a `case` and, when the case carries data, a `value`, which is also the shape the Kotlin core writes. This is the effect payload's encoder:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashSerialization.generated.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashSerialization.generated.swift
  public func encode(to encoder: Encoder) throws {
    var c = encoder.container(keyedBy: SumCodingKeys.self)
    switch self {
    case let .armSafetyNet(afterMillis):
      try c.encode("armSafetyNet", forKey: .caseName)
      var v = c.nestedContainer(keyedBy: ValueKey.self, forKey: .value)
      try v.encode(afterMillis, forKey: ValueKey("afterMillis"))
    case let .notifyListener(v0):
      try c.encode("notifyListener", forKey: .caseName)
      try c.encode(v0, forKey: .value)
    }
  }
```

A case with labeled data encodes its labels as an object under `value`; a case whose single payload is unlabeled, such as `.notifyListener(SplashDelegateEvent)`, encodes the payload bare. The file is a committed build product, and its header says how to regenerate it: `tools/duet record` regenerates the coders as part of each recording run, and `tools/duet canonical-sum` runs the regeneration alone. Build the package again; the last line starts with `Build complete!`.

### Write the reducer

The [reducer](https://docs.modaal.dev/articles/duet-glossary#reducer) is a pure function: it takes the state `inout` and an action, writes the new state, and returns the effects. It never sleeps, never reads a clock and never calls the host; it only decides. The three cases below are the whole behavior: `.appeared` arms the net once, and each completion path asks for a notification. Add it at the end of `SplashFeature.swift`:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashFeature.swift
public func splashReducer(
  state: inout SplashState,
  action: SplashAction
) -> [Effect<SplashEffectPayload>] {
  switch action {
  case .appeared:
    // The arming guard: a repeat `.appeared` writes nothing and emits nothing.
    guard !state.isArmed else { return [] }
    state.isArmed = true
    return [
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ]

  case .ceremonyFinished:
    return [.run(.notifyListener(.completed(path: .ceremony)))]

  case .safetyNetElapsed:
    return [.run(.notifyListener(.completed(path: .safetyNet)))]
  }
}
```

`.ceremonyFinished` neither cancels the net nor sets a "completed" flag. If the timer fires after the animation ended, the host is notified a second time and ignores the repeat. The rule lives in one place, the host, so the reducer stays three cases long, and the recordings in step 6 pin the second notification so that a later edit cannot add a latch without failing a check. Build the package again; the last line starts with `Build complete!`.

### Add the environment and the runtime

The reducer asked for two things it cannot do itself: wait, and notify the host. The environment protocol is the feature's only access to the platform, a clock to wait on and a sink for the delegate event; in Twins Tutorial 2, the splash's app-side package implements it for the iPhone app. It exposes a clock instead of calling `Task.sleep`, so a test can run the wait on virtual time. Create `SplashEnvironment.swift` beside `SplashFeature.swift`; like every source file in the package, it imports `Duet`:

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

The [effect handler](https://docs.modaal.dev/articles/duet-glossary#effect-handler) is the package's only impure code. It turns each payload into an `AsyncStream` of the actions the effect produces: `.armSafetyNet` sleeps on the environment's clock and then yields `.safetyNetElapsed`; `.notifyListener` calls the environment and yields nothing. When the store cancels the effect, the stream's termination cancels the task, the sleep throws, and the stream finishes without yielding. Create `SplashRuntime.swift` with the handler:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashRuntime.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashRuntime.swift
public func splashEffectHandler(
  environment: SplashEnvironment
) -> (SplashEffectPayload) -> AsyncStream<SplashAction> {
  { payload in
    switch payload {
    case let .armSafetyNet(afterMillis):
      let clock = environment.clock
      return AsyncStream { continuation in
        let task = Task {
          do {
            try await clock.sleep(nanoseconds: UInt64(afterMillis) * nanosPerMilli)
            continuation.yield(.safetyNetElapsed)
          } catch {
            // Cancelled: the store re-armed under the same id or tore down.
          }
          continuation.finish()
        }
        continuation.onTermination = { _ in task.cancel() }
      }

    case let .notifyListener(event):
      return AsyncStream { continuation in
        environment.notifyHost(event)
        continuation.finish()
      }
    }
  }
}
```

Below the handler, the file defines `nanosPerMilli` and `makeSplashStore(environment:)`, a `@MainActor` function that wires `splashReducer` and the handler into a `Store` with `SplashState()` as its initial state; it is the factory a shell calls to build its store. Build the package again with `(cd src-ios/Subtrees/Splash/SplashFeature && swift build)`; the last line starts with `Build complete!`.

### Write the feature spec

`parity/feature-specs/splash.md` is the one-page description of the feature in prose: identity and config, state, actions, transitions, effects, delegate events, what stays app-side, and the recordings. `tools/duet verify` checks that every recording listed in the manifest is named in that file in backticks: a spec that leaves out `splash.repeat-appear-inert` fails `verify`'s meta-checks with `manifest fixture 'splash.repeat-appear-inert' never mentioned`, and no lane runs. Copy the finished tree's file, or write your own from its headings; its last section is the table below.

**`parity/feature-specs/splash.md`**

```markdown parity/feature-specs/splash.md
Four leaves over one given (a fresh store, then `appeared`):

| Recording | Pins |
| --- | --- |
| `splash.ceremony-completes` | `appeared` arms the net; `ceremonyFinished` notifies `completed(ceremony)` |
| `splash.safety-net-fires` | `safetyNetElapsed` notifies `completed(safetyNet)`. Mutually exclusive with the row above, so it is a branch over the same given rather than a scenario of its own |
| `splash.both-paths-notify-twice` | the deliberate non-latch: `ceremonyFinished` then `safetyNetElapsed`, two notifications |
| `splash.repeat-appear-inert` | the arming guard: a second `appeared` writes nothing and emits nothing |
```

Run `tools/duet lint`. It prints the same five failures as in step 1: the scenario file and the four recordings are still missing, and step 6 writes all five.

### Describe the behavior as a scenario and record it

A [scenario](https://docs.modaal.dev/articles/duet-glossary#scenario) is a test written in the fixture-authoring language: a `Given` state, `When` steps, and `Then` and `ThenEffects` checks. Where two endings are mutually exclusive, they are `Branch`es over the same given, and each branch leaf becomes one recording. The splash has one given, the appeared splash, and four branches. Create `SplashScenarioTests.swift` in `Tests/SplashFeatureTests`. It imports `Duet`, `DuetTesting`, `SplashFeature` and `XCTest`, and declares the scenario:

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift
private let splashScenario = Scenario<SplashState, SplashAction, SplashEffectPayload>(
  feature: "splash",
  description:
    "The splash arms a safety net when it appears and completes by whichever "
    + "path gets there first: the animation ending, or the net firing. Both "
    + "paths notify the host every time; a repeat Appeared is inert."
) {
  Given(SplashState())

  When("the splash appears", .appeared)
  Then("the safety net is armed") { $0.isArmed }
  ThenEffects("exactly the keyed safety net, carrying its duration") {
    $0 == [
      .run(
        .armSafetyNet(afterMillis: SplashConfig.safetyNetMillis),
        id: SplashEffectIds.safetyNet)
    ]
  }

  Branch("ceremony completes") {
    When("the animation reaches its end", .ceremonyFinished)
    Then("the arming latch is untouched") { $0.isArmed }
    ThenEffects("the host is notified, by the ceremony path") { $0 == notified(.ceremony) }
  }

  Branch("safety net fires") {
    When("the armed delay elapses instead", .safetyNetElapsed)
    Then("the arming latch is untouched") { $0.isArmed }
    ThenEffects("the host is notified, by the safety-net path") { $0 == notified(.safetyNet) }
  }

  Branch("both paths notify twice") {
    When("the animation reaches its end", .ceremonyFinished)
    ThenEffects("the host is notified, by the ceremony path") { $0 == notified(.ceremony) }
    When("the armed delay elapses on top of it", .safetyNetElapsed)
    ThenEffects("the host is notified again, by the safety-net path") {
      $0 == notified(.safetyNet)
    }
  }

  Branch("repeat appear inert") {
    When("the splash reports appearing a second time", .appeared)
    Then("still armed, unchanged") { $0.isArmed }
    ThenEffects("nothing: the net is not re-armed") { $0.isEmpty }
  }
}
```

Below the scenario, the file defines `notified(_:)`, which returns the one `.notifyListener(.completed(path:))` effect the completion branches expect, and one XCTest method, `testSplashScenario`, whose body is `try ScenarioRunner.verifyOrRecord(splashScenario, reducer: splashReducer)`. Run as an ordinary test, `verifyOrRecord` checks the committed recordings; run by the `duet` tool with regeneration on, it writes them. Record it:

```sh
tools/duet record --feature splash
```

```text
duet record: rewrote 4 fixture(s):
  parity/fixtures/splash.both-paths-notify-twice.fixture.json
  parity/fixtures/splash.ceremony-completes.fixture.json
  parity/fixtures/splash.repeat-appear-inert.fixture.json
  parity/fixtures/splash.safety-net-fires.fixture.json
review the diff (git diff -- parity/fixtures), then run `duet verify` for both lanes
```

`git status parity/fixtures` lists the four new files. Open `splash.repeat-appear-inert.fixture.json`. Its `scenario.source` names `SplashScenarioTests.swift`, the file that wrote it, and each step holds the action, the expected state and the expected effects as canonical JSON, plus the label and the scenario line it came from. This is its second step, the repeat `appeared`:

**`parity/fixtures/splash.repeat-appear-inert.fixture.json`**

```json parity/fixtures/splash.repeat-appear-inert.fixture.json
    {
      "action": {
        "case": "appeared"
      },
      "expectedEffects": [],
      "expectedState": {
        "isArmed": true
      },
      "label": "repeat appear inert › the splash reports appearing a second time",
      "line": 55
    }
```

Fixtures are build products: review the diff like code, never edit them by hand.

### Check the recordings

Run the two checks on the recordings:

```sh
tools/duet verify
tools/duet record --check
```

`verify` runs the meta-checks, then the Swift lane, which is `swift test` in `src-ios/Subtrees/Splash/SplashFeature`; it reports the Kotlin lane as skipped and prints `duet verify: 4/4 fixture report(s) passed`, and its last line starts with `duet verify: PASS`. In the lane, the scenario test runs in verify mode: it drives each branch through the reducer and compares each step's bytes with the committed recording. `record --check` checks in the other direction, re-recording from the scenario and comparing the result with the files on disk, and prints `duet record --check: fixtures are up to date with their scenarios`. CI runs it to catch a scenario edited without re-recording.

Now compare your recordings with Tutorial 1's. The tree `tutorial1-complete` sits beside `tutorial1-swift-start` in your clone, and its four recordings were written by the Kotlin scenario. Stage your recordings so that git can show what the copy changes, then copy the Kotlin recordings over them:

```sh
git add parity/fixtures
cp ../tutorial1-complete/parity/fixtures/splash.* parity/fixtures/
git diff --stat parity/fixtures
```

`git diff --stat` reports `4 files changed, 13 insertions(+), 13 deletions(-)`. Read the changes with `git diff parity/fixtures`: the changed lines are `scenario.source`, which names the Kotlin scenario, and each step's `line`, which is that step's line in the Kotlin file. The actions, the expected states and the expected effects are unchanged. Run the regeneration check again:

```sh
tools/duet record --check
```

```text
duet record --check: metadata-only churn in 4 fixture(s) (admissible — behavioral fields unchanged):
  parity/fixtures/splash.both-paths-notify-twice.fixture.json
  parity/fixtures/splash.ceremony-completes.fixture.json
  parity/fixtures/splash.repeat-appear-inert.fixture.json
  parity/fixtures/splash.safety-net-fires.fixture.json
commit the refreshed metadata with the change that moved the scenario
```

It exits 0, because a [metadata-only](https://docs.modaal.dev/articles/duet-glossary#metadata-only) difference leaves the behavior unchanged. It also writes the Swift scenario's metadata back into the four files, so `git diff parity/fixtures` now prints nothing. The Swift reducer and scenario record the same behavior as Tutorial 1's Kotlin core.

### Generate the environment's test double

The store tests in step 9 need an environment that records what the feature sends the host. On this track every Swift test double is generated, never written by hand: a protocol marked `sourcery: CreateMock` gets a `<Name>Mock` class, which `tools/duet mocks` writes from a generator row in the manifest. Add the marker to `SplashEnvironment.swift`, as the last line of the doc comment above the protocol:

**`src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashEnvironment.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature/SplashEnvironment.swift
/// The feature's door to the platform: a clock to wait on and a sink for the
/// delegate event. The app implements it; tests use the generated
/// `SplashEnvironmentMock`.
/// sourcery: CreateMock
public protocol SplashEnvironment {
```

Then add the generator row at the end of the manifest. `bundle` pins the swift-sourcery-templates release that carries the generator engine and its templates; `sources` is the directory the generator reads; `output` is the generated file, in the test target, so no production target links the double; `args` are the modules the generated file imports.

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
mocks:
  bundle: 0.10.2
  generators:
    splash_mocks:
      output: src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/Generated/SplashFeatureMocks.swift
      template: Mocks.swifttemplate
      sources:
        - src-ios/Subtrees/Splash/SplashFeature/Sources/SplashFeature
      args:
        - import=Duet
        - import=SplashFeature
```

Generate the double:

```sh
tools/duet mocks
```

The first run downloads the swift-sourcery-templates 0.10.2 bundle into `.build/` at the tree's root, and the output ends with `SplashFeatureMocks.swift — 1 types, 57 lines`. Then run the check CI runs:

```sh
tools/duet mocks --check
```

The output ends with these two lines; the first names the file by its absolute path, shortened here:

```text
mock-templates: …/Tests/SplashFeatureTests/Generated/SplashFeatureMocks.swift is current — 4 inputs verified, body verified
Generated code is current ✓
```

The check reads the fingerprint block at the top of the generated file, which records a hash of each of the four Swift files under `sources` and of the generated body, and it does not run the generator. After a change to `SplashEnvironment` it reports `Generated code is stale.` and tells you to run `tools/duet mocks`. The generated `SplashEnvironmentMock` takes the clock in its initializer, `init(clock:)`. It counts and records each `notifyHost` call, and a test can supply a handler for it:

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/Generated/SplashFeatureMocks.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/Generated/SplashFeatureMocks.swift
    // MARK: - Methods
    func notifyHost(_ event: SplashDelegateEvent) {
        notifyHostCallCount += 1
        notifyHostArgs.append(event)
        if let __notifyHostHandler = self.notifyHostHandler {
            __notifyHostHandler(event)
        }
    }
    var notifyHostCallCount: Int = 0
    var notifyHostArgs: [SplashDelegateEvent] = []
    var notifyHostHandler: ((_ event: SplashDelegateEvent) -> ())? = nil
```

A comment at the top of the generated file states the naming rule: a method `load()` gives `loadCallCount`, `loadArgs` and `loadHandler`, and the read-only property `clock` gives `clockGetCount`, `clockGetHandler` and the stored `_clock`.

### Pin the safety net's timing on a test clock

The recordings pin what the reducer emits and nothing about time: the reducer never waits. The timing lives in the effect handler, so its test drives the real store through the handler on `TestClock`, the clock from `DuetTesting` that moves only when the test advances it. `SplashEnvironmentMock` is the environment: it hands the handler the test clock and records every delegate event in `notifyHostArgs`.

The suite has four tests: the net does not fire before its duration, fires after it, fires exactly once, and never fires after teardown. Every test starts from the same given, a [test store](https://docs.modaal.dev/articles/duet-glossary#test-store) that has received `.appeared`. Create `SplashTestStoreTests.swift` in `Tests/SplashFeatureTests`, with the same imports as the scenario file. The tests are methods of a `@MainActor final class SplashTestStoreTests: XCTestCase`, which builds that given in a private `armedStore(_:)` helper; a file-level `nanos(_:)` converts milliseconds to the clock's nanoseconds. The second and the fourth tests are below.

**`src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashTestStoreTests.swift`**

```swift src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashTestStoreTests.swift
  func testSafetyNetFiresAfterItsDuration() async {
    let clock = TestClock()
    let environment = SplashEnvironmentMock(clock: clock)
    let store = armedStore(environment)

    await clock.advance(byNanoseconds: nanos(SplashConfig.safetyNetMillis))

    // `receive`, not `send`: the handler emitted this action, so the store
    // already has it and the test states what arrived.
    await store.receive(.safetyNetElapsed)
    store.expectEffects([notifySafetyNet])
    await store.finish()
    XCTAssertEqual(environment.notifyHostArgs, [.completed(path: .safetyNet)])
  }
```

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

`TestStore` is exhaustive: `finish()` fails if an action arrived that the test never received, or if an effect is still running. The "exactly once" test advances the clock twice across the deadline; a second `.safetyNetElapsed` would be an unreceived action, and `finish()` would fail on it. Run the package's tests:

```sh
(cd src-ios/Subtrees/Splash/SplashFeature && swift test)
```

The last XCTest summary line starts with `Executed 5 tests, with 0 failures (0 unexpected)`: the scenario and the four store tests. Run `tools/duet verify` again; its last line starts with `duet verify: PASS`.

### Add the replay runner

The last package holds a small executable that serves the app's features over the replay protocol: the `duet` tool sends each recorded step to it and compares the bytes that come back. It checks the same recordings from outside the test process. `tools/duet verify` does not run it; its output names the protocol lane under `not covered by verify`. The package, `src-ios/Libraries/Replay`, has two targets. `ReplayAdapter` is a library that holds the registry, one entry per feature:

**`src-ios/Libraries/Replay/Sources/ReplayAdapter/ReplayAdapter.swift`**

```swift src-ios/Libraries/Replay/Sources/ReplayAdapter/ReplayAdapter.swift
// The replay registry: one typed entry per feature. `tools/duet protocol-run`
// sends each recorded step through this server and compares the bytes it
// returns with the recording.
public let appReplayRegistry = ReplayRegistry([
  ReplayFeature.entry("splash", SplashState.self, SplashAction.self) { state, action in
    splashReducer(state: &state, action: action)
  }
])
```

`replay-runner` is the executable that serves it:

**`src-ios/Libraries/Replay/Sources/replay-runner/main.swift`**

```swift src-ios/Libraries/Replay/Sources/replay-runner/main.swift
// The replay-protocol endpoint: the framework's stdio server over the
// registry. No app links this executable.
ReplayServer.serve(registry: appReplayRegistry)
```

Its `Package.swift` declares the two targets with the feature package's platforms and concurrency settings. It depends on the Duet package, for the `Duet` and `DuetReplay` products, and on the feature package by path:

**`src-ios/Libraries/Replay/Package.swift`**

```swift src-ios/Libraries/Replay/Package.swift
  dependencies: [
    // The Duet framework, pinned exactly, at the release every other manifest
    // in this tree names.
    .package(url: "https://github.com/modaal-agent/duet.git", exact: "0.7.0"),
    .package(path: "../../Subtrees/Splash/SplashFeature"),
  ],
```

Name the package to the `duet` tool with the `replayRunner:` key, above `features:` in the manifest:

**`parity/manifest.yaml`**

```yaml parity/manifest.yaml
# The Swift package that owns the Sources/replay-runner executable, the
# protocol lane's driver.
replayRunner: src-ios/Libraries/Replay
```

Drive it:

```sh
tools/duet protocol-run
```

`protocol-run` builds the `replay-runner` executable in the package `replayRunner:` names, starts it, and sends it every step of the four recordings. The output includes `4 leaf + 0 chain fixture(s), 9 step(s) byte-gated CLI-side`, and the last line starts with `protocol-run: PASS`. Without the `replayRunner:` key, it fails with `no manifest package root contains Sources/replay-runner`.

## What you now have

- One Swift package, `src-ios/Subtrees/Splash/SplashFeature`, with the behavior in four source files: the types and the reducer, the generated coders, the environment protocol and the runtime.
- Four recordings under `parity/fixtures/`, written by the Swift scenario, whose actions, states and effects match the recordings Tutorial 1's Kotlin core writes.
- A generated `SplashEnvironmentMock`, which `tools/duet mocks --check` reports current.
- Five passing tests in the package: the scenario and four test-clock checks.
- A green `tools/duet verify`, a green `tools/duet record --check`, and a replay runner that passes `tools/duet protocol-run`.
- A manifest row and a feature spec that the tool cross-checks against the recordings. On the finished tree, `tools/duet lint` prints `lockstep-lint: OK (1 feature(s), 4 fixture(s), ledger: 0 waiver(s) / 0 island(s), mocks: 1 generator(s))`.

[Duet Twins Tutorial 2](duet-twins-02-two-cores.md) starts from this tree: it adds a Kotlin twin that replays the same four recordings, and mounts the splash in a SwiftUI app and a Compose app.

## Exercise: break the arming guard

The recording `splash.repeat-appear-inert` pins that a second `.appeared` does nothing. To see it fail, remove the arming guard: in `SplashFeature.swift`, delete the line `guard !state.isArmed else { return [] }`, so that every `.appeared` arms a net. Run `tools/duet verify`. The Swift lane fails on one recording, and the report names the step, the expected effects, what the reducer emitted instead, and the scenario line whose check failed:

```text
duet verify: 3/4 fixture report(s) passed
✗ fixture 'splash.repeat-appear-inert' step 1 'repeat appear inert › nothing: the net is not re-armed' — Then failed   [swift]
  at expectedEffects[0]
  expected: ∅   actual: {"id":"splash.safetyNet","kind":"run","payload":{"case":"armSafetyNet","value":{"afterMillis":3000}}}
  scenario: src-ios/Subtrees/Splash/SplashFeature/Tests/SplashFeatureTests/SplashScenarioTests.swift:57
  ThenEffects returned false against the last emitted effects — the step's effects also diverged from the fixture
  next: verify materialize splash.repeat-appear-inert#1 --platform swift
```

The last line of the run starts with `duet verify: FAIL`. Run `tools/duet protocol-run` with the guard still deleted: the replay runner fails the same recording with `✗ [swift] splash.repeat-appear-inert#1: effects diverged`, and the last line starts with `protocol-run: FAIL`. Restore the guard and run `tools/duet verify` again; its last line starts with `duet verify: PASS`.

## Common questions

<details>
<summary><b>How does this tutorial differ from Tutorial 1?</b></summary>

The feature, its behavior and its four recordings are the same: step 7 shows that the two sets of files differ only in `scenario.source` and each step's `line`. The differences are in each language's tooling. This tutorial writes a Swift package where [Tutorial 1](duet-01-first-feature.md) writes a Gradle module; `tools/duet canonical-sum` generates the coders where Tutorial 1 registers serializers by hand; `tools/duet mocks` generates the environment double where Tutorial 1 writes `RecordingSplashEnvironment`; the scenario test replays the recordings, so there is no separate golden test; and `protocol-run` builds the Swift replay runner itself, where Tutorial 1 builds the Kotlin runner with Gradle and passes it with `--runner`.

</details>

<details>
<summary><b>Why is there no golden replay test?</b></summary>

The scenario test has both roles. `ScenarioRunner.verifyOrRecord` writes the recordings when `tools/duet record` runs it with regeneration on; in every other run it drives each branch through the reducer and compares each step's bytes with the committed recording, and those comparisons are the `4/4 fixture report(s)` that `verify` prints. A [golden test](https://docs.modaal.dev/articles/duet-glossary#golden-fixture) replays recordings on a platform whose scenario does not write them: in [Twins Tutorial 2](duet-twins-02-two-cores.md), the Kotlin twin replays these four files in a golden test.

</details>

<details>
<summary><b>Why does the feature package depend on the Duet framework only?</b></summary>

`tools/duet verify` checks, before any lane runs, that each Swift lane root resolves only Duet family packages (`duet` and `duet-services`). A feature package that adds another dependency fails that check with `host-lane check: FAIL`, which names the package and the allowlist, and no lane runs. App code needs app libraries, so it goes in a sibling package beside `SplashFeature`: Twins Tutorial 2 adds `SplashNode` for the splash's shell and view.

</details>

<details>
<summary><b>Why is the test double generated for a protocol with two members?</b></summary>

On this track the rule covers every Swift test double, whatever the protocol's size; the comment above the finished tree's `mocks:` row states it. The generator row keeps the double in step with the protocol: `tools/duet mocks --check` compares the hash of each input file with the one recorded in the generated file, so a change to `SplashEnvironment` without a regeneration fails the check with `Generated code is stale.` The Kotlin series writes its first Kotlin double by hand in Tutorial 1 and generates its Kotlin doubles from [Tutorial 3](duet-03-composing-features.md#generate-the-kotlin-test-doubles).

</details>

<details>
<summary><b>Where does the app come from?</b></summary>

From [Duet Twins Tutorial 2](duet-twins-02-two-cores.md). It starts from `tutorial1-swift-complete`, adds a Kotlin twin of the splash under `src-android/` that replays the same four recordings, and mounts the feature in a SwiftUI app under `src-ios/App` and a Compose app under `src-android/app`. The iPhone app links Swift packages only.

</details>

## Sources and further reading

- [The Duet framework repository](https://github.com/modaal-agent/duet) — the Swift `Store`, `Effect`, `KernelClock` and `CanonicalSumCodable` types in `Duet`; `Scenario`, `ScenarioRunner`, `TestStore` and `TestClock` in `DuetTesting`; the replay server in `DuetReplay`; and the contracts for the [store kernel](https://github.com/modaal-agent/duet/blob/main/contracts/store-kernel-contract.md), [serialization](https://github.com/modaal-agent/duet/blob/main/contracts/serialization.md) and the [replay protocol](https://github.com/modaal-agent/duet/blob/main/contracts/replay-protocol-v1.md).
- [The duet-tools repository](https://github.com/modaal-agent/duet-tools) — the `duet` command-line tool and the [manifest grammar](https://github.com/modaal-agent/duet-tools/blob/main/contracts/manifest.md).
- [The swift-sourcery-templates repository](https://github.com/modaal-agent/swift-sourcery-templates) — the bundle `tools/duet mocks` downloads: the generator engine, the `Mocks.swifttemplate` template and the fingerprint check.
- [Sourcery](https://github.com/krzysztofzablocki/Sourcery) — the Swift code generator the bundle runs.
- [The duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) — `tutorial1-swift-start` and `tutorial1-swift-complete`, and `scripts/run-tree.sh`, the per-tree checks CI runs on them.
- [SE-0295: Codable synthesis for enums with associated values](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0295-codable-synthesis-for-enums-with-associated-values.md) — the synthesized enum coding that the generated coders replace with the canonical `case` and `value` shape.
- [The Duet glossary](https://docs.modaal.dev/articles/duet-glossary) — [feature](https://docs.modaal.dev/articles/duet-glossary#feature), [reducer](https://docs.modaal.dev/articles/duet-glossary#reducer), [scenario](https://docs.modaal.dev/articles/duet-glossary#scenario), [behavior recording](https://docs.modaal.dev/articles/duet-glossary#fixture), [metadata-only](https://docs.modaal.dev/articles/duet-glossary#metadata-only) and [the checks](https://docs.modaal.dev/articles/duet-glossary#gate).

## Read next

- **[Twins Tutorial 2: One Behavior, Two Cores](duet-twins-02-two-cores.md)** — Add a Kotlin twin of the splash that replays the same four recordings, then mount the feature in a SwiftUI app and a Compose app.
- **[Tutorial 1: Your First Feature](duet-01-first-feature.md)** — The same feature and the same four recordings, written in Kotlin Multiplatform.
- **[The Duet tutorial series](README.md)** — The nine tutorials, the app they build, the prerequisites and the versions they are verified against.
- **[Duet Twins: a Swift core for iPhone, a Kotlin core for Android](https://docs.modaal.dev/articles/duet-twins)** — What a Duet Twins project is: each app runs its own feature logic, and one set of recordings checks both copies in CI.
