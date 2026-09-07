# Foyer — tutorial 4, finished

The tree Duet Tutorial 4 ends with: Tutorial 3's feature tree over the
on-device backend, the session and entitlement streams observed by two
workers adopted at the root mount, the entitlement projected into the home
and profile tabs as a slice, and the Insights card locked until a purchase
flips the stream. No mock service remains. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-04-workers>; this README is a
pointer, not a copy.

```sh
tools/duet verify
tools/duet mocks --check
(cd src-kmp && ./gradlew :backend-local:jvmTest :app:testDebugUnitTest)
parity/scripts/apple-boundary-lane.sh
```

All four are green in this tree.
