#!/usr/bin/env bash
# The Apple boundary and Swift shells lane. One script owns the ordering
# SwiftPM cannot express: assemble the Kotlin core first, then run every Swift
# package that links it.
#
#   parity/scripts/apple-boundary-lane.sh
#
# Runs:
#   1. scripts/assemble_kit.sh debug   Gradle → XCFramework → the consumed path
#   2. swift test  src-kmp/apple-umbrella/swift-consumer
#                                      the boundary replay: the committed
#                                      recordings across the framework
#   3. swift test  src-ios/Libraries/FoyerKit
#                                      the Swift shells: one spec per feature

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
START_TIME=$SECONDS

echo "apple-boundary-lane: assembling the Kotlin core (debug)"
if ! "$ROOT/scripts/assemble_kit.sh" debug; then
  echo "apple-boundary-lane: FAIL (assemble_kit)"
  exit 1
fi

# `swift test` exits 0 when it discovers no tests, and a lane that passed
# having replayed nothing is a failure. Both runners print a summary
# ("Executed N tests" for XCTest, "Test run with N tests" for swift-testing);
# the check is on the larger of the two.
run_swift_tests() {  # <label> <package dir>
  local label="$1" dir="$2" log
  log="$(mktemp -t "duet-boundary-${label// /-}")"
  if ! (cd "$dir" && swift test 2>&1) | tee "$log"; then
    echo "apple-boundary-lane: FAIL ($label)"
    exit 1
  fi
  local executed
  executed=$(awk '
    match($0, /Executed [0-9]+ test/)      { n = substr($0, RSTART + 9); }
    match($0, /Test run with [0-9]+ test/) { n = substr($0, RSTART + 14); }
    { if (n + 0 > max) max = n + 0; n = 0 }
    END { print max + 0 }' "$log")
  if [ "$executed" -eq 0 ]; then
    echo "apple-boundary-lane: FAIL ($label): zero tests executed by either runner"
    exit 1
  fi
  echo "apple-boundary-lane: $label: $executed test(s) executed"
  rm -f "$log"
}

echo "apple-boundary-lane: boundary replay (swift-consumer)"
run_swift_tests "boundary replay" "$ROOT/src-kmp/apple-umbrella/swift-consumer"

echo "apple-boundary-lane: Swift shells (FoyerKit)"
run_swift_tests "shells" "$ROOT/src-ios/Libraries/FoyerKit"

echo "apple-boundary-lane: PASS in $((SECONDS - START_TIME))s"
