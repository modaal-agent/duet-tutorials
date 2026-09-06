# Foyer — tutorial 3, complete

The tree Duet Tutorial 3 ends with: the feature tree over Tutorial 2's
splash — `root`, `signin`, `main`, `home`, `profile`, `account`, `editname` —
the `ports` module with the four port interfaces and the four mock services
per platform, the Builder/Component/Dependency triple at every level on both
sides, four chain recordings, and the mocks pipeline (Swift Components and
test doubles under `Generated/`, Kotlin doubles from the KSP processor). Both
apps play the same tree from the same core. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-03-composing-features>; this README
is a pointer, not a copy.

```sh
tools/duet verify
tools/duet mocks --check
parity/scripts/apple-boundary-lane.sh
(cd src-kmp && ./gradlew :app:testDebugUnitTest :app:assembleDebug)
```

The commands above are green as committed. The iOS app builds from
`src-ios/App/xcodegen.yml` after `xcodegen generate`; the scheme's pre-action
assembles the Kotlin core.
