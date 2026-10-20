# Foyer — the Duet Twins tutorial 2, starting point

The tree Duet Twins Tutorial 2 opens: Swift Tutorial 1's finished tree — the
`splash` feature in Swift, its recordings, its test-clock suite and the Swift
replay runner — plus one deliberately failing test, the tutorial's closing
exercise. No Kotlin and no UI yet. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-twins-02-two-cores>; this README is a
pointer, not a copy.

```sh
tools/duet verify
```

The command above is green before the first edit, apart from the one
deliberately failing test the tutorial's closing exercise asks you to make
pass (`Tutorial2ExerciseMountBracketTests`). `TUTORIAL_SKIP_STUBS=1 tools/duet
verify` skips it.
