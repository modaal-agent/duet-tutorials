# Contributing

The rules every family repository holds, and this repository's own two.

- **This repository is public.** Write every file, commit message and
  generated artifact for a reader outside the project: no references to
  private repositories, internal planning documents, or the toolchains that
  consume the framework — "a consuming app" or "a consuming toolchain",
  never which one.
- **Docs state the present rule, not the transition.** README text and code
  comments are forward-looking: state what the tree does and the action the
  reader takes. Do not frame behavior as a replacement of past practice
  ("X replaces Y", "previously", "no longer"). Historical contrast belongs in
  commit messages.
- **The tutorial voice.** The page is the documentation and the tree is what
  the reader types: comments in a tree say what a line does, not why the
  tutorial exists. Prose before code; a milestone is stated as what now
  passes, not celebrated.
- **Test doubles never live in product sources, `#if DEBUG` included.** A
  generated `*Mock` class lives in the test source set that uses it. The
  `Local*` classes under `src-kmp/backend-local` are not doubles: they are
  the sample app's backend behind the same ports a real backend implements,
  and they ship in product sources by design.
- **A tree passes its own gate.** Every `tutorialN-start` and
  `tutorialN-complete` directory is a complete repository at that step:
  `scripts/run-tree.sh <dir>` is green locally and in CI before it merges.
  A `-start` tree is its predecessor's `-complete` tree plus exactly one
  failing test, the closing exercise, in a class named `Tutorial<N>Exercise…`;
  `run-tree.sh` filters that class unless `--with-stubs` is given.
- **Page and trees change together.** A tutorial edit that touches what the
  reader types lands as one change set: the trees here and the page in the
  documentation repository. Every fenced block on a page is a verbatim
  excerpt of the `-complete` tree, checked by `scripts/check-snippets.py`.
- **One pins file.** Family and toolchain versions are written in `pins.env`
  and nowhere else by hand; `scripts/check-pins.sh` fails on the first tree
  that disagrees. A family release is one commit: edit `pins.env`, re-pin the
  trees, re-record where the release notes say bytes moved, open one pull
  request, and let the full matrix decide.
- **Copyright header** in every source file: `// Copyright (c) 2026
  Modaal.dev` and the MIT reference, as the family's files carry.
- **Licensing**: MIT, inbound = outbound; submitting a pull request means
  your contribution is licensed under the [MIT License](LICENSE).
