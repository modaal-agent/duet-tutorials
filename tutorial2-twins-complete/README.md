# Foyer — the Duet Twins tutorial 2, complete

The tree Duet Twins Tutorial 2 ends with: the `splash` feature twice, as a
Swift twin the iPhone app runs (`src-ios/Subtrees/Splash`) and a Kotlin twin
the Android app runs (`src-android/subtrees/splash/logic`). The Swift
scenario writes the four recordings under `parity/fixtures/`, and both twins
replay them. The SwiftUI app is in `src-ios/App` and the Compose app in
`src-android/app`. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-twins-02-two-cores>; this README is a
pointer, not a copy.

```sh
tools/duet verify
tools/duet protocol-run --platform both
(cd src-ios/Subtrees/Splash/SplashNode && swift test)
./src-android/gradlew -p src-android :app:testDebugUnitTest :app:assembleDebug
```

The commands above are green as committed. The iOS app builds from
`src-ios/App/xcodegen.yml` after `xcodegen generate`, with Xcode alone: the
iPhone app links Swift packages only.
