#!/usr/bin/env bash
# Every tree agrees with pins.env, or this exits 1 naming the first line that
# does not. Run from anywhere; CI runs it before the tree matrix.
#
#   scripts/check-pins.sh            # every tutorial*-* directory
#   scripts/check-pins.sh tutorial3-complete
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "$ROOT/pins.env"

failures=0
fail() { echo "check-pins: $1" >&2; failures=$((failures + 1)); }

# The value of `key = "value"` in a TOML/properties-shaped file, first match.
toml_value() { grep -E "^$2[[:space:]]*=" "$1" | head -1 | sed -E 's/^[^=]*=[[:space:]]*"?([^"]*)"?.*$/\1/'; }

check_tree() {
  local tree="$1" dir="$ROOT/$1"
  local ref="$dir/parity/duet-tools.ref"
  if [[ -f "$ref" ]]; then
    local pinned
    pinned="$(grep -vE '^[[:space:]]*(#|$)' "$ref" | head -1 | tr -d '[:space:]')"
    [[ "$pinned" == "$DUET_TOOLS" ]] || fail "$tree/parity/duet-tools.ref pins $pinned, pins.env says DUET_TOOLS=$DUET_TOOLS"
  else
    fail "$tree has no parity/duet-tools.ref"
  fi

  local catalog="$dir/src-kmp/gradle/libs.versions.toml"
  if [[ -f "$catalog" ]]; then
    local v
    v="$(toml_value "$catalog" duet)";           [[ "$v" == "$DUET" ]]          || fail "$tree: libs.versions.toml duet = $v, pins.env DUET=$DUET"
    v="$(toml_value "$catalog" duetServices)";   [[ "$v" == "$DUET_SERVICES" ]] || fail "$tree: libs.versions.toml duetServices = $v, pins.env DUET_SERVICES=$DUET_SERVICES"
    v="$(toml_value "$catalog" mocksProcessor)"; [[ "$v" == "$KSP_MOCKS" ]]     || fail "$tree: libs.versions.toml mocksProcessor = $v, pins.env KSP_MOCKS=$KSP_MOCKS"
    v="$(toml_value "$catalog" kotlin)";         [[ "$v" == "$KOTLIN" ]]        || fail "$tree: libs.versions.toml kotlin = $v, pins.env KOTLIN=$KOTLIN"
  fi

  local wrapper="$dir/src-kmp/gradle/wrapper/gradle-wrapper.properties"
  if [[ -f "$wrapper" ]]; then
    grep -q "gradle-$GRADLE-bin.zip" "$wrapper" || fail "$tree: gradle-wrapper.properties does not name gradle-$GRADLE-bin.zip"
    grep -q "distributionSha256Sum=$GRADLE_SHA256" "$wrapper" || fail "$tree: gradle-wrapper.properties sha256 differs from pins.env GRADLE_SHA256"
  fi

  local daemon="$dir/src-kmp/gradle/gradle-daemon-jvm.properties"
  if [[ -f "$daemon" ]]; then
    grep -q "^toolchainVersion=$JDK$" "$daemon" || fail "$tree: gradle-daemon-jvm.properties toolchainVersion is not $JDK"
  fi

  local manifest="$dir/parity/manifest.yaml"
  if [[ -f "$manifest" ]] && grep -qE '^[[:space:]]+bundle:' "$manifest"; then
    local bundle
    bundle="$(grep -E '^[[:space:]]+bundle:' "$manifest" | head -1 | sed -E 's/.*bundle:[[:space:]]*//; s/[[:space:]]*(#.*)?$//')"
    [[ "$bundle" == "$MOCKS_BUNDLE" ]] || fail "$tree: manifest mocks bundle is $bundle, pins.env MOCKS_BUNDLE=$MOCKS_BUNDLE"
  fi

  # Every Swift manifest that names a family package pins it `exact:` at the
  # family version. One `.package(...)` per line is the shape the trees use.
  while IFS= read -r -d '' pkg; do
    local rel="${pkg#"$ROOT"/}"
    while IFS= read -r line; do
      local want
      case "$line" in
        *modaal-agent/duet-services*) want="$DUET_SERVICES" ;;
        *modaal-agent/duet-migration*) continue ;;
        *modaal-agent/duet[\"./]*|*modaal-agent/duet\)*) want="$DUET" ;;
        *) continue ;;
      esac
      local got
      got="$(echo "$line" | sed -nE 's/.*exact:[[:space:]]*"([^"]+)".*/\1/p')"
      [[ "$got" == "$want" ]] || fail "$rel pins '$got' on: $line (pins.env says $want)"
    done < <(grep -E '\.package\(' "$pkg" | grep -E 'modaal-agent/duet')
  done < <(find "$dir" -name Package.swift -not -path '*/.build/*' -print0)
}

# The workflows select the same floors the trees declare.
for wf in "$ROOT"/.github/workflows/*.yml; do
  rel="${wf#"$ROOT"/}"
  while IFS= read -r line; do
    v="$(echo "$line" | sed -nE 's/.*xcode-version:[[:space:]]*"?([0-9.]+)"?.*/\1/p')"
    [[ -z "$v" || "$v" == "$XCODE" ]] || fail "$rel selects Xcode $v, pins.env XCODE=$XCODE"
  done < <(grep -E 'xcode-version:' "$wf" || true)
  while IFS= read -r line; do
    v="$(echo "$line" | sed -nE 's/.*java-version:[[:space:]]*"?([0-9]+)"?.*/\1/p')"
    [[ -z "$v" || "$v" == "$JDK" ]] || fail "$rel selects JDK $v, pins.env JDK=$JDK"
  done < <(grep -E 'java-version:' "$wf" || true)
done

if [[ $# -gt 0 ]]; then
  for t in "$@"; do check_tree "${t%/}"; done
else
  for d in "$ROOT"/tutorial*-*/; do check_tree "$(basename "$d")"; done
fi

if [[ $failures -gt 0 ]]; then
  echo "check-pins: $failures disagreement(s) with pins.env" >&2
  exit 1
fi
echo "check-pins: every tree agrees with pins.env (DUET=$DUET DUET_TOOLS=$DUET_TOOLS DUET_SERVICES=$DUET_SERVICES)"
