# Duet Tutorial 2: One Behavior, Two Apps

[Series overview](README.md) · [← Tutorial 1: Your First Feature](duet-01-first-feature.md) · [Tutorial 3: Composing Features →](duet-03-composing-features.md) · [On docs.modaal.dev](https://docs.modaal.dev/tutorials/duet-02-two-apps)

Trees: [`tutorial2-start/`](../tutorial2-start) is what you open, [`tutorial2-complete/`](../tutorial2-complete) is what you have at the end. Run either tree's checks with `scripts/run-tree.sh tutorial2-start`.

In this tutorial you put the splash feature from [Tutorial 1](duet-01-first-feature.md) on two screens. First you build the Kotlin core into an Apple framework and replay the four recordings across the Swift boundary, so the reducer that reaches the iOS app is provably the one the Kotlin lane checked. Then you write the two shells: a Swift shell and a SwiftUI view in the iOS app, and a Compose screen in the Android app. Each shell does three things and nothing else: it turns intents into actions, projects state into what the view renders, and brackets the store's lifetime. At the end both apps play the splash from the same store and land on the same placeholder screen; [Tutorial 3](duet-03-composing-features.md) replaces that placeholder with the feature tree. This is the second page of the [nine-tutorial series](README.md).

> [!NOTE]
> **The manual setup below is what Modaal automates.** Every tree this tutorial
> opens and every file it asks you to write is produced by the Duet templates in
> [Modaal](https://modaal.dev), and a coding agent working in Modaal writes the
> feature, records it and runs the checks for you. This series walks the same
> ground by hand so you know what the scaffold emits and why:
> [start a Duet project in Modaal](https://docs.modaal.dev/articles/new-project)
> when you would rather skip the setup.

## What will you build?

The Kotlin module you wrote in Tutorial 1 becomes one static framework, `FoyerKit`, built by Gradle and linked by two Swift packages: a test-only package that replays the recordings across the boundary, and the app's consumer package that holds the bridge and the shell. The iOS app is a scene delegate, a host object and two SwiftUI views; the Android app is an Activity, the same host in Kotlin and two composables. Neither app contains a line of feature logic. The splash reveals the app's name over 1.6 seconds, sends `CeremonyFinished` when the reveal ends, and the host swaps in a placeholder that names which path completed it.

This is the most setup-heavy tutorial in the series. The framework assembly is the setup the Modaal scaffold automates; here you write it by hand, in three files and one script. Expect about two hours, most of it in the first four steps.

You will have at the end:

- `src-kmp/apple-umbrella`: the framework module and the replay boundary it exports, assembled by `scripts/assemble_kit.sh`.
- Six passing tests in `src-kmp/apple-umbrella/swift-consumer`: the four recordings replayed across the boundary, plus two on the error channel.
- `src-ios/Libraries/FoyerKit`: the `FoyerBridge` target with the store mirror, the `SplashShell` target with the shell, the builder and the view, and three passing shell tests.
- `src-ios/App`: the iOS app, built from an XcodeGen spec.
- `src-kmp/app`: the Android app, with two passing host tests on the JVM.
- A green `parity/scripts/apple-boundary-lane.sh`, and `tools/duet doctor` reporting two target declarations.

## Where do you start?

Open `tutorial2-start` from the [duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials). It is Tutorial 1's finished tree plus one failing test, the closing exercise, and it resolves Duet 0.7.0, duet-tools 0.24.0 and duet-services 0.11.1. You need Xcode 26.6, a JDK 25, XcodeGen (`brew install xcodegen`) and the Android SDK with platform 36; the [series index](README.md#what-do-you-need-installed) lists them. Run the checks once before you edit anything:

```sh
tools/duet verify
```

The Kotlin lane reports 10 tests with one failure, `Tutorial2ExerciseMountBracketTest`; that is the exercise, and everything else is green. `TUTORIAL_SKIP_STUBS=1 tools/duet verify` leaves the stub out and ends with `duet verify: PASS`.

## The steps

### Declare the Apple framework module

The Apple side of a Duet app links one Kotlin/Native framework that aggregates the kernel and every feature module. One framework rather than one per feature, because a static Kotlin/Native framework embeds the Kotlin runtime and two of them would carry it twice. The module is `src-kmp/apple-umbrella`. It applies the multiplatform plugin and SKIE, the compiler plugin that projects sealed hierarchies as Swift enums and `StateFlow` as an async sequence. Add the SKIE version and plugin to the version catalog:

**`src-kmp/gradle/libs.versions.toml`**

```toml src-kmp/gradle/libs.versions.toml
# SKIE: the Swift-friendly projection of the Kotlin/Native framework — sealed
# hierarchies as enums, StateFlow as an async sequence.
skie = "0.10.14"
```

**`src-kmp/gradle/libs.versions.toml`**

```toml src-kmp/gradle/libs.versions.toml
skie = { id = "co.touchlab.skie", version.ref = "skie" }
```

Register it once in the root build file with `apply false`, and include the module in the settings file:

**`src-kmp/build.gradle.kts`**

```kotlin src-kmp/build.gradle.kts
  // The Apple framework's Swift projection, applied by :apple-umbrella.
  alias(libs.plugins.skie) apply false
```

**`src-kmp/settings.gradle.kts`**

```kotlin src-kmp/settings.gradle.kts
// The Apple boundary: one Kotlin/Native framework over the kernel and every
// feature module, consumed by the iOS app's shells package and by the
// boundary replay suite, both through scripts/assemble_kit.sh.
include(":apple-umbrella")
```

The module's build file declares three arm64 targets, names the framework, and exports the feature module and the kernel. `export` is what puts a dependency's declarations in the framework's headers; a module that is linked but not exported compiles, and Swift cannot name a single type from it.

**`src-kmp/apple-umbrella/build.gradle.kts`**

```kotlin src-kmp/apple-umbrella/build.gradle.kts
kotlin {
  jvmToolchain(25)

  val xcf = XCFramework("FoyerKit")

  listOf(
    macosArm64(),
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { target ->
    target.binaries.framework {
      baseName = "FoyerKit"
      isStatic = true
      binaryOption("bundleId", "dev.modaal.foyer.kit")
      // `export` puts a dependency's declarations in the framework's headers.
      // A module that is linked but not exported compiles, and Swift cannot
      // name a single type from it. One line per feature module.
      export(dependencies.project(":subtrees:splash:logic"))
      export(libs.duet.kernel)
      xcf.add(this)
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(project(":subtrees:splash:logic"))
      api(libs.duet.kernel)
    }
  }
}
```

Check it with `(cd src-kmp && ./gradlew projects -q)`; the hierarchy now lists `Project ':apple-umbrella'`.

### Export the replay boundary

A framework over an empty `commonMain` produces no XCFramework at all, so the module needs a source file, and there is one thing that is per app rather than per feature: the replay registry the Swift side drives. It names the same four declarations the Kotlin replay runner names, one entry per feature, and exposes two calls: canonicalize any fixture JSON through the core's own writer, and open a replay session over a registered feature.

**`src-kmp/apple-umbrella/src/commonMain/kotlin/dev/modaal/foyer/kit/FoyerBoundary.kt`**

```kotlin src-kmp/apple-umbrella/src/commonMain/kotlin/dev/modaal/foyer/kit/FoyerBoundary.kt
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

/**
 * The surface the Swift replay suite drives across the framework: canonicalize
 * fixture JSON through the core's own writer, and open a replay session over
 * a registered feature. Swift loads the files, threads the steps and compares
 * bytes; every canonical byte on both sides is produced here.
 */
object FoyerBoundary {
  // `@Throws` is the error channel. A Kotlin exception that crosses
  // Kotlin/Native without it terminates the process instead of surfacing as
  // a Swift error, so every throwing path exported to Swift carries it.
  @Throws(IllegalArgumentException::class)
  fun canonicalize(rawJson: String): String = BoundaryReplay.canonicalize(rawJson)

  @Throws(IllegalArgumentException::class)
  fun makeSession(feature: String, initialStateJson: String): ReplaySession =
    BoundaryReplay.makeSession(registry, feature, initialStateJson)
}
```

The `@Throws` annotations are a contract, not a style: a Kotlin exception that crosses Kotlin/Native without one terminates the process. Build the framework once with `(cd src-kmp && ./gradlew :apple-umbrella:assembleFoyerKitDebugXCFramework)`. The first build downloads the Kotlin/Native toolchain and compiles three targets; allow ten minutes. It ends with `BUILD SUCCESSFUL`, and `src-kmp/apple-umbrella/build/XCFrameworks/debug/FoyerKit.xcframework` exists.

### Assemble the framework with one script

SwiftPM links a prebuilt binary and has no build-graph link to Gradle. Every Swift consumer therefore points at one path that a single script writes, so `swift test` and the app build always link what was assembled last, and nothing links Gradle's per-flavor output directories directly.

**`scripts/assemble_kit.sh`**

```sh scripts/assemble_kit.sh
(cd "$ROOT/src-kmp" && ./gradlew ":apple-umbrella:$TASK" --console=plain -q)

SRC="$ROOT/src-kmp/apple-umbrella/build/XCFrameworks/$FLAVOR/FoyerKit.xcframework"
DST="$ROOT/src-kmp/apple-umbrella/build/XCFrameworks/app/FoyerKit.xcframework"
mkdir -p "$(dirname "$DST")"
rsync -a --delete "$SRC/" "$DST/"
echo "assemble_kit: $FLAVOR → ${DST#"$ROOT"/}"
```

The full script also resolves `JAVA_HOME` when it is unset, because Xcode runs script phases with a minimal environment. Run `scripts/assemble_kit.sh debug`; its last line is `assemble_kit: debug → src-kmp/apple-umbrella/build/XCFrameworks/app/FoyerKit.xcframework`.

### Replay the recordings across the boundary

The first consumer is a test-only Swift package next to the module. Its manifest declares the binary target at the consumed path and one test target that links it. Kotlin/Native frameworks need libc++, which Swift does not autolink.

**`src-kmp/apple-umbrella/swift-consumer/Package.swift`**

```swift src-kmp/apple-umbrella/swift-consumer/Package.swift
    .binaryTarget(
      name: "FoyerKit",
      path: "../build/XCFrameworks/app/FoyerKit.xcframework"
    ),
    .testTarget(
      name: "BoundaryTests",
      dependencies: ["FoyerKit"],
      swiftSettings: [.enableExperimentalFeature("StrictConcurrency")],
      linkerSettings: [
        // FoyerKit is a static Kotlin/Native framework; its runtime needs
        // libc++ symbols Swift does not autolink.
        .linkedLibrary("c++"),
        .linkedFramework("Foundation"),
      ]
    ),
```

The harness reads a recording from `parity/fixtures/`, opens a session, and sends each step's action through it. The expected state and effects go through the core's `canonicalize` before the comparison, so both sides of every assertion come from the same writer.

**`src-kmp/apple-umbrella/swift-consumer/Tests/BoundaryTests/BoundaryReplayHarness.swift`**

```swift src-kmp/apple-umbrella/swift-consumer/Tests/BoundaryTests/BoundaryReplayHarness.swift
    var replayed = 0
    for rawStep in document["steps"] as! [[String: Any]] {
      let label = rawStep["label"] as? String ?? "step \(replayed)"
      let result = session.step(actionJson: try compactString(rawStep["action"]!))
      let expectedState = try FoyerBoundary.shared.canonicalize(
        rawJson: try compactString(rawStep["expectedState"]!))
      let expectedEffects = try FoyerBoundary.shared.canonicalize(
        rawJson: try compactString(rawStep["expectedEffects"]!))
      XCTAssertEqual(
        result.stateCanonical, expectedState, "\(fixture)/\(label): state diverged",
        file: file, line: line)
      XCTAssertEqual(
        result.effectsCanonical, expectedEffects, "\(fixture)/\(label): effects diverged",
        file: file, line: line)
      replayed += 1
    }
    return replayed
```

One test method per recording, so a recording without a row here is a recording this suite does not check:

**`src-kmp/apple-umbrella/swift-consumer/Tests/BoundaryTests/SplashBoundaryReplayTests.swift`**

```swift src-kmp/apple-umbrella/swift-consumer/Tests/BoundaryTests/SplashBoundaryReplayTests.swift
  private func replay(_ fixture: String) throws {
    let steps = try BoundaryReplayHarness.replay(fixture: fixture) { initialState in
      try FoyerBoundary.shared.makeSession(feature: "splash", initialStateJson: initialState)
    }
    XCTAssertGreaterThan(steps, 0, "the recording must contain steps to replay")
  }

  func testCeremonyCompletes() throws {
    try replay("splash.ceremony-completes")
  }
```

A second file, `BoundaryErrorChannelTests.swift`, pins that an unregistered feature name arrives in Swift as a catchable error carrying the Kotlin message, and that `canonicalize` orders keys. Run `(cd src-kmp/apple-umbrella/swift-consumer && swift test)`; the summary line reads `Executed 6 tests, with 0 failures`.

### Write the consumer package and the store mirror

The app links the framework through a second package, `src-ios/Libraries/FoyerKit`. It has a target for the bridge, one shell target per feature, and one test target per shell, and it pins the Duet Swift package exactly at 0.7.0, the same release the Kotlin side resolves.

**`src-ios/Libraries/FoyerKit/Package.swift`**

```swift src-ios/Libraries/FoyerKit/Package.swift
    .target(
      name: "FoyerBridge",
      dependencies: [
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency,
      linkerSettings: [
        // FoyerKit is a static Kotlin/Native framework; its runtime needs
        // libc++ symbols Swift does not autolink.
        .linkedLibrary("c++"),
        .linkedFramework("Foundation"),
      ]
    ),
    .target(
      name: "SplashShell",
      dependencies: [
        "FoyerBridge",
        .target(name: "FoyerKit"),
        .product(name: "DuetShells", package: "duet"),
      ],
      swiftSettings: strictConcurrency
    ),
    .testTarget(
      name: "SplashShellTests",
      dependencies: ["SplashShell"],
      swiftSettings: strictConcurrency
    ),
```

The bridge target holds one class. The Kotlin `Store` is the runtime; `BridgedStore` is its Swift face: a `@Published` mirror of the bridged `StateFlow`, a synchronous `send`, and a `cancel()` that stops the Kotlin runtime. The class exists for one reason. A shell must observe a reduce before `send` returns, and a bridged `StateFlow` delivers asynchronously, so the mirror re-reads the Kotlin state in the same call.

**`src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedStore.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/FoyerBridge/BridgedStore.swift
  /// Reduces before returning: the Kotlin store reduces synchronously on
  /// this thread, and the mirror re-reads `state.value` in the same call.
  public func send(_ action: Action) {
    sendAction(action)
    let latest = stateFlow.value
    if latest != state {
      state = latest
    }
  }

  /// `StoreHost` teardown: stops the collector, then the Kotlin runtime.
  public func cancel() {
    collector?.cancel()
    collector = nil
    teardownRuntime()
  }
```

The class is feature-generic; every shell in the app uses it with its own state and action types.

### Write the Swift shell

The shell subclasses `ViewShell` from the Duet shells package. Its `bind()` runs at `activate()` and adopts two things into the shell's `StoreHost`: the store mirror, and a `StateTransitions` observation that projects each state into the view state. Adopting the mirror first means it unwinds last, after the projection, when `deactivate()` runs. The two intents are the two things the view reports.

**`src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashViewShell.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashViewShell.swift
public final class SplashViewShell: ViewShell {
  public let viewState = SplashViewState()
  public let store: SplashKitStore

  public init(store: SplashKitStore) {
    self.store = store
    super.init()
  }

  override public func bind() {
    // Registered first so it unwinds last: whatever this shell adopts on top
    // of the store stops before the store's `cancel()` ends its effects.
    // Adopting the mirror is also what makes `deactivate()` stop the Kotlin
    // runtime: its `cancel()` is the store's teardown plus its scope's.
    host.adopt(store)

    // State to view state. `StateTransitions` replays the current value on
    // subscription, so this adoption is also the initial projection.
    host.adopt(
      StateTransitions(state: store.$state) { [weak self] _, state in
        self?.apply(state)
      })
  }

  // MARK: - Intents (the view calls these)

  public func appeared() { store.send(SplashActionAppeared.shared) }
  public func ceremonyFinished() { store.send(SplashActionCeremonyFinished.shared) }

  // MARK: - State to view state

  private func apply(_ state: SplashState) {
    viewState.isArmed = state.isArmed
  }
}
```

The environment interface from Tutorial 1 arrives in Swift as an Objective-C protocol, so an `NSObject` subclass implements it: the clock is the Kotlin `LiveClock` object, and `notifyHost` forwards to a closure the host supplies. The builder wires a mount in four lines: a main-immediate scope, the Kotlin store from `makeSplashStore`, the mirror over `splashStateFlow`, and the shell.

**`src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashBuilder.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashBuilder.swift
final class LiveSplashEnvironment: NSObject, SplashEnvironment {
  private let onDelegate: (SplashDelegateEvent) -> Void

  init(onDelegate: @escaping (SplashDelegateEvent) -> Void) {
    self.onDelegate = onDelegate
  }

  /// The wall clock, the Kotlin object. Under `runTest` the Kotlin lane
  /// swaps in virtual time; the app runs on this.
  var clock: KernelClock { LiveClock.shared }

  func notifyHost(event: SplashDelegateEvent) {
    onDelegate(event)
  }
}
```

**`src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashBuilder.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashBuilder.swift
  @MainActor
  public func buildSplash(
    onDelegate: @escaping (SplashDelegateEvent) -> Void
  ) -> SplashChild {
    let scope = mainImmediateStoreScope()
    let store = makeSplashStore(
      environment: LiveSplashEnvironment(onDelegate: onDelegate),
      scope: scope)
    let bridged = SplashKitStore(
      state: splashStateFlow(store: store),
      send: { store.send(action: $0) },
      teardown: {
        store.teardown()
        cancelStoreScope(scope: scope)
      })
    return SplashChild(shell: SplashViewShell(store: bridged))
  }
```

The teardown closure is what makes `deactivate()` reach the Kotlin side: it cancels the store's effects and then the scope they ran in.

### Write the SwiftUI view

The view reads the shell's view state and calls the shell's intents. It owns the reveal animation, because the animation is presentation: the feature knows nothing about 1.6 seconds, only that a ceremony ends. `onAppear` reports `appeared()`, and the animation's completion reports `ceremonyFinished()`. The progress line appears when the state says the safety net is armed.

**`src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashView.swift`**

```swift src-ios/Libraries/FoyerKit/Sources/SplashShell/SplashView.swift
  public var body: some View {
    VStack(spacing: 12) {
      Text("Foyer")
        .font(.system(size: 45, weight: .medium, design: .default))
      Text("One core, two apps")
        .font(.body)
        .foregroundStyle(.secondary)
      if viewState.isArmed {
        ProgressView()
          .progressViewStyle(.linear)
          .frame(width: 120)
          .padding(.top, 24)
      }
    }
    .opacity(revealed ? 1 : 0)
    .offset(y: revealed ? 0 : 16)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onAppear {
      shell.appeared()
      withAnimation(.easeOut(duration: ceremonySeconds)) {
        revealed = true
      } completion: {
        shell.ceremonyFinished()
      }
    }
  }
```

Build the package with `(cd src-ios/Libraries/FoyerKit && swift build)`; it ends with `Build complete!`.

### Pin the crossing in the shells lane

The shell's tests check what only the bridge can break, and nothing the recordings already pin. The first row drives the builder's wiring and asserts that `appeared()` projects back before it returns. The second builds the runtime by hand with a spy environment, so the test can see the Kotlin effect loop call back into Swift with the ceremony path. The third deactivates the shell and shows that a further action still reduces but runs no effect, because the scope is gone.

**`src-ios/Libraries/FoyerKit/Tests/SplashShellTests/SplashViewShellSpec.swift`**

```swift src-ios/Libraries/FoyerKit/Tests/SplashShellTests/SplashViewShellSpec.swift
  func testTheBuilderComposesTheRuntime() {
    let child = SplashBuilder().buildSplash(onDelegate: { _ in })
    let shell = child.shell
    shell.activate()
    defer { shell.deactivate() }

    XCTAssertFalse(shell.viewState.isArmed)
    shell.appeared()
    XCTAssertTrue(shell.viewState.isArmed, "the projection reads back before send returns")
  }
```

**`src-ios/Libraries/FoyerKit/Tests/SplashShellTests/SplashViewShellSpec.swift`**

```swift src-ios/Libraries/FoyerKit/Tests/SplashShellTests/SplashViewShellSpec.swift
    shell.activate()
    shell.appeared()
    XCTAssertTrue(kotlinState.value.isArmed)

    shell.deactivate()

    // Driven directly: the store object is still alive, only its scope is
    // gone. A notification arriving here would mean teardown did not cross.
    store.send(action: SplashActionCeremonyFinished.shared)
    try? await Task.sleep(nanoseconds: 200_000_000)
    XCTAssertTrue(environment.notified.isEmpty, "an effect ran after deactivate")
```

These rows run on real time, deliberately: no test dispatcher crosses the boundary, and virtual time stays with the Kotlin lane. One script, the Apple boundary lane, runs the assembly and both packages in the order SwiftPM cannot express, and fails if either package executed zero tests. Its second half, the consumer package's own tests, is what this series calls the shells lane:

**`parity/scripts/apple-boundary-lane.sh`**

```sh parity/scripts/apple-boundary-lane.sh
echo "apple-boundary-lane: boundary replay (swift-consumer)"
run_swift_tests "boundary replay" "$ROOT/src-kmp/apple-umbrella/swift-consumer"

echo "apple-boundary-lane: Swift shells (FoyerKit)"
run_swift_tests "shells" "$ROOT/src-ios/Libraries/FoyerKit"
```

Run `parity/scripts/apple-boundary-lane.sh`; it prints `boundary replay: 6 test(s) executed`, `shells: 3 test(s) executed` and ends with `apple-boundary-lane: PASS`.

### Build the iOS app

The app target has no feature code. A host object mounts the splash through the builder, receives its delegate event, tears the splash down and switches the phase; a view switches on the phase. Both notification paths call the host, and the host acts on the first only.

**`src-ios/App/Foyer/AppHost.swift`**

```swift src-ios/App/Foyer/AppHost.swift
  private func splashCompleted(_ event: SplashDelegateEvent) {
    // Both completion paths notify every time; the first notification moves
    // the app on and the rest arrive after the splash is gone.
    guard case .splash = phase,
      case .completed(let completed) = onEnum(of: event)
    else { return }
    splash?.shell.deactivate()
    splash = nil
    phase = .placeholder(completedBy: completed.path)
  }
```

`onEnum(of:)` is SKIE's projection of the Kotlin sealed interface as a Swift enum. The screen renders the splash while the phase is `.splash` and the placeholder after:

**`src-ios/App/Foyer/AppScreen.swift`**

```swift src-ios/App/Foyer/AppScreen.swift
  var body: some View {
    switch app.phase {
    case .splash:
      if let splash = app.splash {
        SplashView(viewState: splash.shell.viewState, shell: splash.shell)
      }
    case .placeholder(let completedBy):
      PlaceholderView(completedBy: completedBy)
    }
  }
```

The scene delegate builds the host, shows the window, and activates the host after the window is visible, so the first frame renders from mounted state. `sceneDidDisconnect` calls `teardown()`, because dropping the reference alone cancels nothing on the Kotlin side. The project itself is generated from an XcodeGen spec that names the consumer package's two products and runs the assemble script as the scheme's build pre-action:

**`src-ios/App/xcodegen.yml`**

```yaml src-ios/App/xcodegen.yml
    dependencies:
    - package: FoyerKit
      product: FoyerBridge
    - package: FoyerKit
      product: SplashShell
```

**`src-ios/App/xcodegen.yml`**

```yaml src-ios/App/xcodegen.yml
      preActions:
      - name: Assemble FoyerKit (Kotlin core)
        script: |
          KIT="$PROJECT_DIR/../../scripts/assemble_kit.sh"
          [ -x "$KIT" ] || exit 0
          FLAVOR=debug
          if [ "$CONFIGURATION" != "Debug" ]; then FLAVOR=release; fi
          "$KIT" "$FLAVOR"
        settingsTarget: Foyer
```

Generate and build with `(cd src-ios/App && xcodegen generate --spec xcodegen.yml && xcodebuild build -project Foyer.xcodeproj -scheme Foyer -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO ARCHS=arm64 | tail -1)`; the last line is `** BUILD SUCCEEDED **`. `ARCHS=arm64` matters: the framework has no x86_64 slice, and a generic simulator build otherwise compiles for both.

### Build the Compose app

The Android app is a Gradle module, `src-kmp/app`, on the same plane as the feature modules. It depends on the splash module directly, on the kernel, and on the Duet `shells-compose` artifact for `StoreHost` and `RetainedRoot`.

**`src-kmp/app/build.gradle.kts`**

```kotlin src-kmp/app/build.gradle.kts
  implementation(project(":subtrees:splash:logic"))
  implementation(libs.duet.kernel)
  // StoreHost and RetainedRoot: the teardown registry and the retained
  // carrier the Activity keeps the app on across rotation.
  implementation(libs.duet.shells.compose)
  implementation(libs.essenty.instance.keeper)
```

The builder is the Swift builder's twin, shorter because there is no boundary to cross: the live environment is a Kotlin class, and the store is the module's own `makeSplashStore`.

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/SplashBuilder.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/SplashBuilder.kt
private class LiveSplashEnvironment(
  private val onDelegate: (SplashDelegateEvent) -> Unit,
) : SplashEnvironment {
  override val clock: KernelClock = LiveClock

  override fun notifyHost(event: SplashDelegateEvent) = onDelegate(event)
}

/**
 * Builds one splash mount: the live environment and the store, on the scope
 * the host owns. The store's lifetime belongs to the host, which is why the
 * scope is a build parameter.
 */
class SplashBuilder {
  fun buildSplash(onDelegate: (SplashDelegateEvent) -> Unit, scope: CoroutineScope): SplashStore =
    makeSplashStore(environment = LiveSplashEnvironment(onDelegate), scope = scope)
}
```

The host is the same object as on iOS, in Kotlin, and it is Android-free so a JVM test can drive it. It registers the store in a `StoreHost` and tears it down on the first completion:

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/AppHost.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/AppHost.kt
class AppHost(scope: CoroutineScope) {
  /** The teardown registry: what the host builds registers here and unwinds in reverse. */
  val host = StoreHost(scope)

  private val mutablePhase = MutableStateFlow<AppPhase>(AppPhase.Splash)
  val phase: StateFlow<AppPhase> = mutablePhase

  /** The splash store: mounted here, torn down when the splash completes. */
  var splash: SplashStore? =
    host.host(SplashBuilder().buildSplash(onDelegate = ::splashCompleted, scope = scope))
    private set

  private fun splashCompleted(event: SplashDelegateEvent) {
    // Both completion paths notify every time; the first notification moves
    // the app on and the rest arrive after the splash is gone.
    if (mutablePhase.value !is AppPhase.Splash) return
    val completed = event as SplashDelegateEvent.Completed
    splash?.teardown()
    splash = null
    mutablePhase.value = AppPhase.Placeholder(completedBy = completed.path)
  }

  /** Logical destruction only (finish, not rotation). */
  fun teardown() = host.teardownAll()
}
```

On Android the composable is the shell: it collects the store's state, and its launched effect sends `Appeared`, plays the reveal, then sends `CeremonyFinished`. After a rotation the effect runs again; the reducer's arming guard makes the second `Appeared` inert, and the host ignores a second completion. The rules you recorded in [Tutorial 1](duet-01-first-feature.md#describe-the-behavior-as-a-scenario-and-record-it) are what make rotation safe here.

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/SplashScreen.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/SplashScreen.kt
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

The Activity keeps the host on a retained scope, so rotation recreates the Activity and `getOrCreate` hands back the same host with its running store; finishing the Activity is the one teardown.

**`src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/MainActivity.kt`**

```kotlin src-kmp/app/src/main/kotlin/dev/modaal/foyer/app/MainActivity.kt
    retained =
      instanceKeeper().getOrCreate {
        RetainedRoot(Dispatchers.Main.immediate, AppHost::teardown) { scope -> AppHost(scope) }
      }

    setContent { AppRoot(retained.component) }
```

`AppRoot` collects the phase and shows `SplashScreen` or the placeholder. Include the module in the settings file (`include(":app")`), then build with `(cd src-kmp && ./gradlew :app:assembleDebug)`; it ends with `BUILD SUCCESSFUL`.

### Test the host on the JVM

Because the host is Android-free, its test runs on the JVM with the store on the test scope, so the safety net waits on virtual time. The first test drives the ceremony path and then checks that the torn-down store's net never fires; the second advances the clock past the duration instead.

**`src-kmp/app/src/test/kotlin/dev/modaal/foyer/app/AppHostTest.kt`**

```kotlin src-kmp/app/src/test/kotlin/dev/modaal/foyer/app/AppHostTest.kt
  @Test
  fun theCeremonyPathLandsOnThePlaceholderAndTearsTheSplashDown() = runTest {
    val app = AppHost(backgroundScope)
    val splash = assertNotNull(app.splash)
    assertEquals(AppPhase.Splash, app.phase.value)

    splash.send(SplashAction.Appeared)
    splash.send(SplashAction.CeremonyFinished)
    runCurrent()

    assertEquals(AppPhase.Placeholder(SplashCompletionPath.Ceremony), app.phase.value)
    assertNull(app.splash, "the completed splash is torn down")

    // The torn-down store's net never fires: past the deadline, nothing changes.
    advanceTimeBy(SplashConfig.SAFETY_NET_MILLIS)
    runCurrent()
    assertEquals(AppPhase.Placeholder(SplashCompletionPath.Ceremony), app.phase.value)
    app.teardown()
  }
```

Run `(cd src-kmp && ./gradlew :app:testDebugUnitTest)`; it ends with `BUILD SUCCESSFUL` and the report under `src-kmp/app/build/reports/tests/testDebugUnitTest/` lists two tests.

### Declare the targets and run both apps

`tools/duet doctor` reads the tree's declaration file and cross-checks it against what is on disk. Declare the two app targets, paired under one name:

**`.modaal/project.json`**

```json .modaal/project.json
  "targets": {
    "Foyer": {
      "platform": "iOS",
      "architecture": "duet",
      "template": "duet-kmp",
      "pair": "Foyer",
      "deviceTypes": ["iPhone"],
      "iphoneOrientations": ["UIInterfaceOrientationPortrait"]
    },
    "FoyerAndroid": {
      "platform": "Android",
      "architecture": "duet",
      "template": "duet-kmp",
      "pair": "Foyer",
      "androidOrientations": ["Portrait"]
    }
  },
```

Run `tools/duet doctor`; it ends with `duet doctor: PASS — 2 target declaration(s), 10 Swift source file(s) scanned`. Then run the apps. On iOS, open `src-ios/App/Foyer.xcodeproj` and run the `Foyer` scheme on an iPhone simulator; the pre-action assembles the core first. On Android, start an emulator and run `(cd src-kmp && ./gradlew :app:installDebug)`, then open Foyer from the launcher. Both apps reveal the name over 1.6 seconds and then show the placeholder with the line `Splash completed by the ceremony.`

![Two phone screens side by side, an iPhone simulator on the left and an Android emulator on the right. Each shows the word Foyer in large type, the line One core, two apps under it, and a thin progress line below, all partly faded in during the reveal animation.](images/duet-tutorial-2-splash-pair.png)

*The splash mid-reveal on both platforms: an iPhone 17 simulator on the left and a Pixel 8 emulator at API 36 on the right, from tutorial2-complete at Duet 0.7.0 and duet-tools 0.24.0.*

To see the other path, set a breakpoint in the reveal's completion on either platform and wait three seconds; the placeholder then reads `Splash completed by the safety net.`

## What you now have

- One framework, `FoyerKit`, assembled from the Kotlin core by `scripts/assemble_kit.sh` and linked by two Swift packages.
- The four recordings replayed across the Swift boundary, plus two error-channel tests, in `src-kmp/apple-umbrella/swift-consumer`.
- A Swift shell with its builder, view and three tests in `src-ios/Libraries/FoyerKit`, and a Compose screen with its builder, host and two tests in `src-kmp/app`.
- Two apps that play the splash from the same store and land on the same placeholder.
- A green `parity/scripts/apple-boundary-lane.sh`, a green `tools/duet verify`, and a doctor report with two targets.

The feature module did not change by one line between Tutorial 1 and here, and that is the point of the series.

## Exercise: finish the mount-bracket test

`tutorial2-start` carries `Tutorial2ExerciseMountBracketTest` in the splash module's `jvmTest`. Its given is written: one test store, armed, then torn down. Finish it so it pins that a second store over the same environment arms a fresh net, and that after advancing the clock past the duration the environment records exactly one `Completed(SafetyNet)`, the second store's. The first store's net was cancelled at teardown, and `first.finish()` fails on any action that arrived there. Run `tools/duet verify`; the Kotlin lane reports 10 tests with no failure and the run ends with `duet verify: PASS`. `tutorial2-complete` carries the finished test as `SplashMountBracketTest`.

## Common questions

<details>
<summary><b>Why a mirror class instead of collecting the StateFlow in the view?</b></summary>

Because a shell must observe a reduce before `send` returns, and every delivery of a bridged `StateFlow` crosses a continuation hop. `BridgedStore.send` re-reads the Kotlin state synchronously after forwarding the action, which is the ordering `StateTransitions` and the other shell helpers assume. Effect-fed actions, such as the safety net's tick, still arrive through the collector one hop later.

</details>

<details>
<summary><b>Why one framework for the whole app rather than one per feature?</b></summary>

A static Kotlin/Native framework embeds the Kotlin runtime. Two frameworks in one app carry it twice and can disagree about shared types at the boundary. The module boundaries between features stay enforced by Gradle; the Apple side sees one binary.

</details>

<details>
<summary><b>Why does the shell adopt the store before the projection?</b></summary>

`StoreHost` tears down in reverse registration order. Adopting the store first means the projection stops before the store's `cancel()` ends its effects, so no observation fires into a shell that is half torn down. On Android the same registry is `StoreHost` in `AppHost`, and `RetainedRoot` calls its `teardownAll` on logical destruction.

</details>

<details>
<summary><b>Why is the reveal duration in the view but the safety net in the reducer?</b></summary>

The reveal is presentation; each platform owns its animation and may change it without touching a recording. The safety net is behavior: it decides when the host is told, so it lives in the reducer, and its duration is in every recording. The Swift spec and the Kotlin test-clock suite both pin that the net still fires when no `CeremonyFinished` arrives.

</details>

<details>
<summary><b>Why do the Swift shell tests run on real time?</b></summary>

No test dispatcher crosses the boundary, so the Swift side cannot advance the Kotlin store's clock. The rows that need to wait poll in 20 ms slices against a ceiling, and the teardown row waits 200 ms for an effect that must not arrive. Timing behavior is pinned on the Kotlin lane, where virtual time is available.

</details>

<details>
<summary><b>Where does the Android host state live across rotation?</b></summary>

In the `AppHost` that `RetainedRoot` keeps in the Activity's `InstanceKeeper`. Rotation recreates the Activity and the composition, and `getOrCreate` hands the same host back with its store still running; the composable's effect sends `Appeared` again, which the arming guard makes inert. [Tutorial 5](duet-05-navigation-as-state.md#gather-the-route-spine-and-restore-from-it) adds process-death restore, which this mechanism does not cover.

</details>

## Sources and further reading

- [The Duet framework repository](https://github.com/modaal-agent/duet) — the `DuetShells` package this page's shell subclasses (`ViewShell`, `StoreHost`, `StateTransitions`), the Kotlin `shells-compose` artifact (`StoreHost`, `RetainedRoot`), and the kernel's `mainImmediateStoreScope` runtime seam.
- [The duet-tutorials repository](https://github.com/modaal-agent/duet-tutorials) — `tutorial2-start` and `tutorial2-complete`, and the checks CI runs on them.
- [SKIE](https://skie.touchlab.co/) — the Swift projection of the Kotlin/Native framework: sealed hierarchies as enums, `onEnum(of:)`, and `StateFlow` as an async sequence.
- [Kotlin Multiplatform: build final native binaries](https://kotlinlang.org/docs/multiplatform-build-native-binaries.html) — `binaries.framework`, `export`, and `XCFramework`.
- [Essenty](https://github.com/arkivanov/Essenty) — `InstanceKeeper`, the retained carrier under `RetainedRoot`.
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) — the project spec format `src-ios/App/xcodegen.yml` uses.
- [The Duet glossary](https://docs.modaal.dev/articles/duet-glossary) — [shell](https://docs.modaal.dev/articles/duet-glossary#shell), [host](https://docs.modaal.dev/articles/duet-glossary#host), [mount](https://docs.modaal.dev/articles/duet-glossary#mount), [Store](https://docs.modaal.dev/articles/duet-glossary#store) and [the Swift and Kotlin lanes](https://docs.modaal.dev/articles/duet-glossary#swift-and-kotlin-lanes).

## Read next

- **[Tutorial 1: Your First Feature](duet-01-first-feature.md)** — The splash feature both apps on this page mount: state, actions, effects and the recordings.
- **[Tutorial 3: Composing Features](duet-03-composing-features.md)** — The root, the sign-in gate and the profile tree: children mounted from state, delegate events received as parent actions, and chain recordings pinning each seam.
- **[The Duet tutorial series](README.md)** — The nine tutorials, the app they build, the prerequisites and the versions they are verified against.
