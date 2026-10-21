#!/usr/bin/env bash
# The per-tree gate — the same command CI runs, one tree per invocation.
#
#   scripts/run-tree.sh tutorial1-start
#   scripts/run-tree.sh tutorial3-complete --lane macos    # skip the Android app lane
#   scripts/run-tree.sh tutorial2-start --with-stubs       # let the exercise stub fail
#
# What runs is keyed on what the tree carries, so one script serves every
# rung of the ladder:
#   always            tools/duet version, lint, doctor, verify, record --check
#   parity/design-tokens.yaml            tools/duet design-tokens --check
#   a `mocks:` section in the manifest   tools/duet mocks --check
#   src-kmp/replay-runner (+ a feature)  the protocol lane, the Kotlin runner
#   `replayRunner:` (+ a feature)        the protocol lane: the Swift runner, and
#                                        the Kotlin runner too (--platform both)
#                                        when src-android/replay-runner exists
#   src-kmp/backend-local                ./gradlew :backend-local:jvmTest
#   src-kmp/telemetry                    ./gradlew :telemetry:jvmTest
#   src-kmp/theming                      ./gradlew :theming:jvmTest
#   parity/scripts/apple-boundary-lane.sh   the Apple boundary + shells lane   [macos]
#   src-ios/Subtrees/*/*Node/Package.swift  swift test per shell package      [macos]
#                                        (in a tree with no Apple boundary lane)
#   src-ios/App/xcodegen.yml             xcodegen + xcodebuild build           [macos]
#   <gradle root>/app/build.gradle.kts   :app:testDebugUnitTest :app:assembleDebug [android]
#                                        (the gradle root is src-kmp or src-android;
#                                        the android lane runs this step ONLY; the
#                                        toolchain gates run in the macOS job)
#   a `-start` tree                      the ladder check against its predecessor
#                                        (the Swift-first track: tutorial2-twins-start
#                                        follows tutorial1-swift-complete)
#
# Exercise stubs: every `-start` tree carries exactly one failing test, the
# closing exercise, in a test class named `Tutorial<N>Exercise…`. The trees'
# Gradle build excludes those classes when TUTORIAL_SKIP_STUBS=1 is set, and
# a Swift stub skips itself with XCTSkipIf when it is set; this script sets
# it unless --with-stubs is given, so CI is green and a reader's bare
# `tools/duet verify` shows the deliberate red.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tree=""; lane="all"; with_stubs=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --lane) lane="$2"; shift 2 ;;
    --with-stubs) with_stubs=1; shift ;;
    -h|--help) sed -n '2,37p' "$0"; exit 0 ;;
    *) tree="${1%/}"; shift ;;
  esac
done
[[ -n "$tree" ]] || { echo "run-tree: name a tree directory" >&2; exit 2; }
dir="$ROOT/$tree"
[[ -d "$dir" ]] || { echo "run-tree: no such tree: $tree" >&2; exit 2; }
case "$lane" in all|macos|android) ;; *) echo "run-tree: --lane is macos, android or all" >&2; exit 2 ;; esac

if [[ $with_stubs -eq 1 ]]; then unset TUTORIAL_SKIP_STUBS; else export TUTORIAL_SKIP_STUBS=1; fi

step() { echo; echo "── $tree · $*"; }

# ── the ladder check: a -start tree is its predecessor's -complete tree plus
#    the one stub file; README.md differs by design (each links its own page).
#    The Swift-first track runs tutorial1-swift-* → tutorial2-twins-* →
#    tutorial3-twins-* and on.
prev=""
if [[ "$tree" =~ ^tutorial([0-9]+)-start$ ]]; then
  n="${BASH_REMATCH[1]}"
  if (( n >= 7 )); then prev="tutorial6-complete"; elif (( n >= 2 )); then prev="tutorial$((n - 1))-complete"; fi
elif [[ "$tree" =~ ^tutorial([0-9]+)-twins-start$ ]]; then
  n="${BASH_REMATCH[1]}"
  if (( n == 2 )); then prev="tutorial1-swift-complete"; elif (( n >= 3 )); then prev="tutorial$((n - 1))-twins-complete"; fi
fi
if [[ -n "$prev" ]]; then
  step "ladder check against $prev"
  if [[ -d "$ROOT/$prev" ]]; then
    diffs="$(diff -rq \
      --exclude=README.md --exclude=.gradle --exclude=build --exclude=.build \
      --exclude=.duet-family --exclude=.runs --exclude=.DS_Store --exclude=.kotlin \
      --exclude='*.xcodeproj' --exclude=Info.plist --exclude=.swiftpm \
      "$ROOT/$prev" "$dir" || true)"
    unexpected="$(echo "$diffs" | grep -v '^$' | grep -vE "^Only in $dir(/[^:]*)?: .*Exercise.*" || true)"
    if [[ -n "$unexpected" ]]; then
      echo "$unexpected" >&2
      echo "run-tree: $tree differs from $prev beyond the exercise stub" >&2
      exit 1
    fi
    echo "ok: $tree = $prev + the exercise stub"
  else
    echo "predecessor $prev is not on the tree yet — skipped"
  fi
fi

cd "$dir"

# The Gradle root that carries the Android app: src-kmp in a Kotlin
# Multiplatform tree, src-android in a twins tree. Empty when there is none.
app_root=""
for plane in src-kmp src-android; do
  if [[ -f "$plane/app/build.gradle.kts" ]]; then app_root="$plane"; fi
done

# ── the Android lane alone: the Compose app and nothing else. The toolchain
#    gates below run in the macOS job for the same tree, and the pinned duet
#    CLI has no Linux release asset, so an ubuntu runner would build it from
#    source and fail on the Swift kernel's Apple frameworks.
if [[ "$lane" == "android" ]]; then
  if [[ -n "$app_root" ]]; then
    step "Android app: unit tests + assembleDebug"
    if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
      export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
    (cd "$app_root" && ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain -q)
  else
    echo "run-tree: $tree has no Android app yet — nothing for the android lane"
  fi
  echo; echo "run-tree: $tree green (lane: android, stubs: $([[ $with_stubs -eq 1 ]] && echo included || echo filtered))"
  exit 0
fi

# ── the toolchain gates (the macOS lane and the full run)
step "tools/duet version"; tools/duet version
step "tools/duet lint";    tools/duet lint
step "tools/duet doctor";  tools/duet doctor
step "tools/duet verify";  tools/duet verify

plan="$(tools/duet lint --json)"
features="$(printf '%s' "$plan" | python3 -c 'import json,sys; print("\n".join(sorted(json.load(sys.stdin)["features"].keys())))')"
chains="$(printf '%s' "$plan" | python3 -c 'import json,sys; print("\n".join(json.load(sys.stdin).get("chains", [])))')"
step "tools/duet record --check"
if [[ -z "$features" ]]; then
  tools/duet record --check
else
  for f in $features; do tools/duet record --feature "$f" --check; done
  for c in $chains; do tools/duet record --chain "$c" --check; done
fi

if [[ -f parity/design-tokens.yaml ]]; then
  step "tools/duet design-tokens --check"; tools/duet design-tokens --check
fi
if grep -qE '^mocks:' parity/manifest.yaml; then
  step "tools/duet mocks --check"; tools/duet mocks --check
fi
replay_runner="$(printf '%s' "$plan" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("replayRunner", ""))')"
if [[ -n "$features" && -n "$replay_runner" ]]; then
  # A Swift runner declared in the manifest: the CLI builds it, and with
  # --platform both it builds and drives the Kotlin runner as well.
  if [[ -d src-android/replay-runner ]]; then
    step "protocol lane (Swift and Kotlin runners)"; tools/duet protocol-run --platform both
  else
    step "protocol lane (Swift runner)"; tools/duet protocol-run
  fi
elif [[ -n "$features" && -d src-kmp/replay-runner ]]; then
  step "protocol lane"
  (cd src-kmp && ./gradlew :replay-runner:installDist --console=plain -q)
  tools/duet protocol-run --runner src-kmp/replay-runner/build/install/replay-runner/bin/replay-runner
fi
if [[ -d src-kmp/backend-local ]]; then
  # The on-device backend is no feature module, so `verify` never reaches
  # its tests; the lane runs them here.
  step "backend-local tests"; (cd src-kmp && ./gradlew :backend-local:jvmTest --console=plain -q)
fi
if [[ -d src-kmp/telemetry ]]; then
  step "telemetry grammar tests"; (cd src-kmp && ./gradlew :telemetry:jvmTest --console=plain -q)
fi
if [[ -d src-kmp/theming ]]; then
  # The theme tests: the token module is no feature module, so `verify`
  # never reaches them.
  step "theming tests"; (cd src-kmp && ./gradlew :theming:jvmTest --console=plain -q)
fi

# ── the macOS lane: the Apple boundary and the iOS app
if [[ "$lane" == "all" || "$lane" == "macos" ]]; then
  if [[ -f parity/scripts/apple-boundary-lane.sh ]]; then
    step "apple boundary + shells lane"; parity/scripts/apple-boundary-lane.sh
  else
    # A Swift core's shells live in one package per subtree, beside the
    # feature package `verify` runs; no other lane reaches their tests.
    for pkg in src-ios/Subtrees/*/*Node/Package.swift; do
      [[ -f "$pkg" ]] || continue
      step "shell package tests: $(dirname "$pkg")"; (cd "$(dirname "$pkg")" && swift test)
    done
  fi
  if [[ -f src-ios/App/xcodegen.yml ]]; then
    step "iOS app build"
    command -v xcodegen >/dev/null || { echo "run-tree: xcodegen is not on PATH (brew install xcodegen)" >&2; exit 1; }
    (cd src-ios/App && xcodegen generate --spec xcodegen.yml -q)
    project="$(ls -d src-ios/App/*.xcodeproj | head -1)"
    scheme="$(basename "$project" .xcodeproj)"
    # ARCHS=arm64: the Kotlin core ships arm64 slices only, and a generic
    # simulator destination otherwise also compiles x86_64, where the
    # framework has no slice.
    # A tree that links a build-tool plugin (Tutorial 8's string-catalog
    # generator) fails on a fresh machine without the validation skip: a
    # plugin is trusted interactively in Xcode, and a CI runner has no one to
    # click. The flag is inert for a tree with no plugin.
    xcodebuild build -project "$project" -scheme "$scheme" \
      -destination 'generic/platform=iOS Simulator' -quiet CODE_SIGNING_ALLOWED=NO ARCHS=arm64 \
      -skipPackagePluginValidation -skipMacroValidation
  fi
fi

# ── the Android app, in the full run
if [[ "$lane" == "all" && -n "$app_root" ]]; then
  step "Android app: unit tests + assembleDebug"
  # The Gradle plugin finds the SDK through ANDROID_HOME; CI's image sets
  # it, a Mac with Android Studio has it at the default location.
  if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
  fi
  (cd "$app_root" && ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain -q)
fi

echo; echo "run-tree: $tree green (lane: $lane, stubs: $([[ $with_stubs -eq 1 ]] && echo included || echo filtered))"
