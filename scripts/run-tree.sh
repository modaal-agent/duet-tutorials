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
#   src-kmp/replay-runner (+ a feature)  the protocol lane
#   src-kmp/telemetry                    ./gradlew :telemetry:jvmTest
#   parity/scripts/apple-boundary-lane.sh   the Apple boundary + shells lane   [macos]
#   src-ios/App/xcodegen.yml             xcodegen + xcodebuild build           [macos]
#   src-kmp/app/build.gradle.kts         :app:testDebugUnitTest :app:assembleDebug [android]
#   a `-start` tree                      the ladder check against its predecessor
#
# Exercise stubs: every `-start` tree carries exactly one failing test, the
# closing exercise, in a test class named `Tutorial<N>Exercise…`. The trees'
# Gradle build excludes those classes when TUTORIAL_SKIP_STUBS=1 is set; this
# script sets it unless --with-stubs is given, so CI is green and a reader's
# bare `tools/duet verify` shows the deliberate red.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tree=""; lane="all"; with_stubs=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --lane) lane="$2"; shift 2 ;;
    --with-stubs) with_stubs=1; shift ;;
    -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
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
if [[ "$tree" =~ ^tutorial([0-9]+)-start$ ]]; then
  n="${BASH_REMATCH[1]}"
  if (( n >= 2 )); then
    if (( n >= 7 )); then prev="tutorial6-complete"; else prev="tutorial$((n - 1))-complete"; fi
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
fi

cd "$dir"

# ── the toolchain gates (every lane)
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
if [[ -n "$features" && -d src-kmp/replay-runner ]]; then
  step "protocol lane"
  (cd src-kmp && ./gradlew :replay-runner:installDist --console=plain -q)
  tools/duet protocol-run --runner src-kmp/replay-runner/build/install/replay-runner/bin/replay-runner
fi
if [[ -d src-kmp/telemetry ]]; then
  step "telemetry grammar tests"; (cd src-kmp && ./gradlew :telemetry:jvmTest --console=plain -q)
fi

# ── the macOS lane: the Apple boundary and the iOS app
if [[ "$lane" == "all" || "$lane" == "macos" ]]; then
  if [[ -f parity/scripts/apple-boundary-lane.sh ]]; then
    step "apple boundary + shells lane"; parity/scripts/apple-boundary-lane.sh
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
    xcodebuild build -project "$project" -scheme "$scheme" \
      -destination 'generic/platform=iOS Simulator' -quiet CODE_SIGNING_ALLOWED=NO ARCHS=arm64
  fi
fi

# ── the Android lane: the Compose app
if [[ "$lane" == "all" || "$lane" == "android" ]]; then
  if [[ -f src-kmp/app/build.gradle.kts ]]; then
    step "Android app: unit tests + assembleDebug"
    # The Gradle plugin finds the SDK through ANDROID_HOME; CI's image sets
    # it, a Mac with Android Studio has it at the default location.
    if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
      export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
    (cd src-kmp && ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain -q)
  elif [[ "$lane" == "android" ]]; then
    echo "run-tree: $tree has no Android app yet — nothing for the android lane"
  fi
fi

echo; echo "run-tree: $tree green (lane: $lane, stubs: $([[ $with_stubs -eq 1 ]] && echo included || echo filtered))"
