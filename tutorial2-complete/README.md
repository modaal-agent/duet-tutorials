# Foyer — tutorial 2, complete

The tree Duet Tutorial 2 ends with: Tutorial 1's `splash` feature, the Apple
boundary (the `apple-umbrella` framework, `scripts/assemble_kit.sh` and the
boundary replay suite), the Swift shell in `src-ios/Libraries/FoyerKit`, the
SwiftUI app in `src-ios/App`, and the Compose app in `src-kmp/app`. Both apps
play the splash from the same core and land on the same placeholder screen.
The tutorial text lives at <https://docs.modaal.dev/tutorials/duet-02-two-apps>;
this README is a pointer, not a copy.

```sh
tools/duet verify
parity/scripts/apple-boundary-lane.sh
(cd src-kmp && ./gradlew :app:testDebugUnitTest :app:assembleDebug)
```

The commands above are green as committed. The iOS app builds from
`src-ios/App/xcodegen.yml` after `xcodegen generate`; the scheme's pre-action
assembles the Kotlin core.
