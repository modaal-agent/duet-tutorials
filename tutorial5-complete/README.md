# Foyer — tutorial 5, finished

The tree Duet Tutorial 5 ends with: Tutorial 4's app plus the onboarding
gate with its three steps and progress row, the upgrade flow mounted in the
main level's sheet slot, two deep links, the route spine each app saves and
rebuilds the tree from after process death, and the Android back policy.
Every screen is reached from state; Back is an action. The tutorial text
lives at <https://docs.modaal.dev/tutorials/duet-05-navigation-as-state>;
this README is a pointer, not a copy.

```sh
tools/duet verify
tools/duet mocks --check
(cd src-kmp && ./gradlew :backend-local:jvmTest :app:testDebugUnitTest)
parity/scripts/apple-boundary-lane.sh
```

All four are green in this tree.
