# Duet tutorials

The step trees for the Duet tutorial series at
<https://docs.modaal.dev/tutorials/duet>. Nine tutorials build one app,
Foyer — a splash, a sign-in gate, an onboarding gate, a home with a paid
feature behind an entitlement check, an upgrade flow and a profile tree —
with the feature logic written once in Kotlin and consumed by a SwiftUI app
and a Compose app. Each tutorial has a `-start` tree (what you open) and a
`-complete` tree (what you have at the end); every tree is a complete
repository that resolves the Duet family at the versions in `pins.env` and
passes its own checks.

| tree | page |
| --- | --- |
| `tutorial1-start` | [Tutorial 1: Your First Feature](https://docs.modaal.dev/tutorials/duet-01-first-feature) |
| `tutorial1-complete` | [Tutorial 1: Your First Feature](https://docs.modaal.dev/tutorials/duet-01-first-feature) |
| `tutorial2-start` | [Tutorial 2: One Behavior, Two Apps](https://docs.modaal.dev/tutorials/duet-02-two-apps) |
| `tutorial2-complete` | [Tutorial 2: One Behavior, Two Apps](https://docs.modaal.dev/tutorials/duet-02-two-apps) |
| `tutorial3-start` | Tutorial 3: Composing Features (page not yet published) |

## Running a tree's checks

```sh
scripts/run-tree.sh tutorial1-start
```

The same command CI runs. Prerequisites: macOS with Xcode 26.6 and a JDK 25;
an Android SDK for the trees that carry an Android app (`--lane macos` skips
that lane). Everything else is fetched at a pinned version on first use — the
`duet` CLI through each tree's `tools/duet`, Gradle through the committed
wrapper.

## Layout

| path | what |
| --- | --- |
| `tutorialN-start/`, `tutorialN-complete/` | the step trees |
| `pins.env` | the family and toolchain versions every tree is built against |
| `scripts/check-pins.sh` | every tree agrees with `pins.env` |
| `scripts/run-tree.sh` | the per-tree gate |
| `scripts/check-snippets.py` | every fenced block on a page is a verbatim excerpt of its `-complete` tree |
| `scripts/plan-trees.sh` | which trees a workflow run selects |
| `scripts/compose-pair.py` | composites an iPhone capture and an Android capture into one image for a tutorial page |
| `.github/workflows/trees.yml` | the tree matrix: changed trees on a pull request, every tree on `main` |
| `.github/workflows/nightly.yml` | every tree, then the mutation drill |

## License

MIT. Copyright (c) 2026 Modaal.dev.
