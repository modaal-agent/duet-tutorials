# Foyer — tutorial 6, finished

The tree Duet Tutorial 6 ends with: Tutorial 5's app plus the checks as one
workflow, `.github/workflows/parity.yml`, and the mutation drill's table,
`parity/mutations.json`, whose three rows the drill catches. The tutorial
text lives at <https://docs.modaal.dev/tutorials/duet-06-checks-in-ci>; this
README is a pointer, not a copy.

```sh
tools/duet verify
tools/duet mutate
```

Both are green in this tree. The workflow file is what the tutorial has you
copy into a repository of your own; it does not run from inside this
directory, because GitHub reads workflows at a repository's root only. The
tutorials repository's own workflows run the same commands on this tree.
