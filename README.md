# Duet tutorials

The Duet tutorial series and the step trees it builds. The nine pages are in
[`docs/`](docs/) as Markdown, and at <https://docs.modaal.dev/tutorials/duet>
with the site's navigation and search. They build one app, Foyer — a splash,
a sign-in gate, an onboarding gate, a home with a paid feature behind an
entitlement check, an upgrade flow and a profile tree — with the feature
logic written once in Kotlin and consumed by a SwiftUI app and a Compose
app. Each tutorial has a `-start` tree (what you open) and a `-complete`
tree (what you have at the end); every tree is a complete repository that
resolves the Duet family at the versions in `pins.env` and passes its own
checks.

| tree | page |
| --- | --- |
| `tutorial1-start` | [Tutorial 1: Your First Feature](docs/duet-01-first-feature.md) |
| `tutorial1-complete` | [Tutorial 1: Your First Feature](docs/duet-01-first-feature.md) |
| `tutorial2-start` | [Tutorial 2: One Behavior, Two Apps](docs/duet-02-two-apps.md) |
| `tutorial2-complete` | [Tutorial 2: One Behavior, Two Apps](docs/duet-02-two-apps.md) |
| `tutorial3-start` | [Tutorial 3: Composing Features](docs/duet-03-composing-features.md) |
| `tutorial3-complete` | [Tutorial 3: Composing Features](docs/duet-03-composing-features.md) |
| `tutorial4-start` | [Tutorial 4: Workers](docs/duet-04-workers.md) |
| `tutorial4-complete` | [Tutorial 4: Workers](docs/duet-04-workers.md) |
| `tutorial5-start` | [Tutorial 5: Navigation as State](docs/duet-05-navigation-as-state.md) |
| `tutorial5-complete` | [Tutorial 5: Navigation as State](docs/duet-05-navigation-as-state.md) |
| `tutorial6-start` | [Tutorial 6: The Checks in CI](docs/duet-06-checks-in-ci.md) |
| `tutorial6-complete` | [Tutorial 6: The Checks in CI](docs/duet-06-checks-in-ci.md) |
| `tutorial7-start` | [Tutorial 7: Theming with Design Tokens](docs/duet-07-theming.md) |
| `tutorial7-complete` | [Tutorial 7: Theming with Design Tokens](docs/duet-07-theming.md) |
| `tutorial8-start` | [Tutorial 8: Localizing the App](docs/duet-08-localization.md) |
| `tutorial8-complete` | [Tutorial 8: Localizing the App](docs/duet-08-localization.md) |
| `tutorial9-start` | [Tutorial 9: Adding Analytics](docs/duet-09-analytics.md) |
| `tutorial9-complete` | [Tutorial 9: Adding Analytics](docs/duet-09-analytics.md) |

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
| `docs/` | the nine tutorial pages as Markdown, and their images |
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
