#!/usr/bin/env bash
# Which trees the matrix runs, as JSON on stdout — the workflow's plan step.
#
#   scripts/plan-trees.sh all
#   scripts/plan-trees.sh changed <base-ref>    # trees touched since base-ref
#
# `changed` maps every changed path to the tree it sits in; a change to
# pins.env, scripts/ or the workflows selects every tree. Prints
# {"trees": [...], "android": [...]} — the second list is the subset that
# carries an Android app and so needs the ubuntu lane.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode="${1:-all}"; base="${2:-}"

all=()
for d in "$ROOT"/tutorial*-*/; do all+=("$(basename "$d")"); done

selected=()
if [[ "$mode" == "all" ]]; then
  selected=("${all[@]}")
else
  [[ -n "$base" ]] || { echo "plan-trees: changed needs a base ref" >&2; exit 2; }
  changed="$(git -C "$ROOT" diff --name-only "$base"...HEAD)"
  if echo "$changed" | grep -qE '^(pins\.env|scripts/|\.github/)'; then
    selected=("${all[@]}")
  else
    for t in "${all[@]}"; do
      if echo "$changed" | grep -q "^$t/"; then selected+=("$t"); fi
    done
  fi
fi

android=()
for t in "${selected[@]+"${selected[@]}"}"; do
  [[ -f "$ROOT/$t/src-kmp/app/build.gradle.kts" ]] && android+=("$t")
done

python3 - "${#selected[@]}" "${selected[@]+"${selected[@]}"}" -- "${android[@]+"${android[@]}"}" <<'PY'
import json, sys
n = int(sys.argv[1]); rest = sys.argv[2:]
trees = rest[:n]; android = [a for a in rest[n + 1:]]
print(json.dumps({"trees": trees, "android": android}))
PY
