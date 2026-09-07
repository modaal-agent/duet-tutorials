# Foyer — tutorial 9, finished

The tree Duet Tutorial 9 ends on: Tutorial 6's finished tree with analytics
added the way the recordings can check it. Seven events, one verb of the
app's own, each emitted by the reducer that owns the transition as a `Track`
effect and pinned in the recordings with its parameters; a console sink
worker on both platforms behind the family's fan-out; and two tests that
read the event envelopes back out of the recordings, one in Kotlin and one
in Swift. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-09-analytics>; this README is a
pointer, not a copy.

```sh
tools/duet verify
tools/duet record --check
(cd src-kmp && ./gradlew :telemetry:jvmTest :app:testDebugUnitTest)
```

The first is green; the second reports every fixture up to date; the third
runs the grammar module's two suites and the console sink's beside the app's
other unit tests.
