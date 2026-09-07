# Foyer — tutorial 7, finished

The tree Duet Tutorial 7 ends with: Tutorial 6's app with its cards on a
token vocabulary, `parity/design-tokens.yaml`, the value tables generated
from it on both platforms, and a second theme, high contrast, that the
system's contrast setting selects. The tutorial text lives at
<https://docs.modaal.dev/tutorials/duet-07-theming>; this README is a pointer,
not a copy.

```sh
tools/duet verify
tools/duet design-tokens --check
(cd src-kmp && ./gradlew :theming:jvmTest)
```

All three are green in this tree. The second theme changed no file under
`src-kmp/subtrees` and no view: it is one mapping per platform beside the
generated tables, and the recordings are Tutorial 6's, byte for byte.
