#!/usr/bin/env bash
# Assembles the Kotlin core's XCFramework and copies it to the one path every
# Swift consumer links:
#
#   src-kmp/apple-umbrella/build/XCFrameworks/app/FoyerKit.xcframework
#
# SwiftPM links a prebuilt artifact and has no build-graph link to Gradle, so
# a bare `swift test` replays whatever was assembled last. Every consumer (the
# boundary lane, the app's build pre-action, a local loop) goes through this
# script, which is what keeps the two-step ordering one step.
#
#   scripts/assemble_kit.sh [debug|release]     (default: debug)
#
# The flavor selects Gradle's build type; the consumed path is flavor-less on
# purpose, so Debug and Release app builds link one path with the content of
# the last assembly.

set -euo pipefail

FLAVOR="${1:-debug}"
case "$FLAVOR" in
  debug) TASK=assembleFoyerKitDebugXCFramework ;;
  release) TASK=assembleFoyerKitReleaseXCFramework ;;
  *)
    echo "assemble_kit: unknown flavor '$FLAVOR' (want debug or release)" >&2
    exit 2
    ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Xcode script phases run with a minimal environment and no JAVA_HOME. Resolve
# the JDK the modules declare, and only that major: `java_home -v 25` still
# answers with another JDK when none at 25 is installed.
if [[ -z "${JAVA_HOME:-}" ]]; then
  ranked="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
  if [[ -n "$ranked" && -x "$ranked/bin/java" ]] &&
     grep -q '^JAVA_VERSION="25[."]' "$ranked/release" 2>/dev/null; then
    export JAVA_HOME="$ranked"
  fi
fi

(cd "$ROOT/src-kmp" && ./gradlew ":apple-umbrella:$TASK" --console=plain -q)

SRC="$ROOT/src-kmp/apple-umbrella/build/XCFrameworks/$FLAVOR/FoyerKit.xcframework"
DST="$ROOT/src-kmp/apple-umbrella/build/XCFrameworks/app/FoyerKit.xcframework"
mkdir -p "$(dirname "$DST")"
rsync -a --delete "$SRC/" "$DST/"
echo "assemble_kit: $FLAVOR → ${DST#"$ROOT"/}"
