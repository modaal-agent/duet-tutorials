# Foyer — tutorial 8, finished

The tree Duet Tutorial 8 ends on: Tutorial 6's finished tree with every
string the two apps show moved out of the views — into one string catalog
per shell on iOS, with the accessors the xcstrings-tool plugin generates,
and into string resources on Android — and German as the second language on
both. The recordings are byte for byte Tutorial 6's. The tutorial text lives
at <https://docs.modaal.dev/tutorials/duet-08-localization>; this README is
a pointer, not a copy.

```sh
tools/duet verify
tools/duet record --check
(cd src-kmp && ./gradlew :app:testDebugUnitTest)
```

The first is green; the second reports every fixture up to date; the third
runs the translation-completeness test beside the app's other unit tests.
