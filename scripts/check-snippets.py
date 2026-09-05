#!/usr/bin/env python3
"""Every fenced code block on a tutorial page is a verbatim excerpt of that
tutorial's -complete tree.

    scripts/check-snippets.py <docs-checkout> [N ...]

<docs-checkout> is a local checkout of the documentation site; the pages are
`tutorials/duet-0N-*.mdx`, and tutorial N's blocks are checked against
`tutorialN-complete/` here. With no N given, every page that has a tree is
checked.

A block names its file in the fence's info string, either as a bare path
after the language (```kotlin src-kmp/.../Feature.kt) or as title="path".
Blocks with no path — commands and their output — are not checked. The block
body must appear in the named file as a contiguous run of lines, compared
with trailing whitespace stripped.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FENCE = re.compile(r"^```([^\n]*)\n(.*?)^```[ \t]*$", re.MULTILINE | re.DOTALL)
TITLE = re.compile(r'title="([^"]+)"')


def blocks(page: Path):
    for match in FENCE.finditer(page.read_text()):
        info, body = match.group(1).strip(), match.group(2)
        path = None
        title = TITLE.search(info)
        if title:
            path = title.group(1)
        else:
            for token in info.split()[1:]:
                if "/" in token or "." in token:
                    path = token
                    break
        if path:
            line = page.read_text()[: match.start()].count("\n") + 1
            yield line, path, body


def normalize(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.rstrip("\n").split("\n"))


def check(docs: Path, n: int) -> int:
    pages = sorted((docs / "tutorials").glob(f"duet-{n:02d}-*.mdx"))
    tree = ROOT / f"tutorial{n}-complete"
    if not pages:
        print(f"check-snippets: no page for tutorial {n} under {docs}/tutorials")
        return 1
    if not tree.is_dir():
        print(f"check-snippets: {tree.name} is not on the tree")
        return 1
    failures = 0
    for page in pages:
        for line, path, body in blocks(page):
            target = tree / path
            if not target.is_file():
                print(f"{page}:{line}: names {tree.name}/{path}, which does not exist")
                failures += 1
                continue
            if normalize(body) not in normalize(target.read_text()):
                print(f"{page}:{line}: block is not a verbatim excerpt of {tree.name}/{path}")
                failures += 1
        print(f"check-snippets: {page.name} against {tree.name}: "
              f"{'ok' if failures == 0 else f'{failures} failing block(s)'}")
    return failures


def main(argv: list[str]) -> int:
    if len(argv) < 2 or argv[1] in ("-h", "--help"):
        print(__doc__.strip())
        return 0
    docs = Path(argv[1]).expanduser().resolve()
    numbers = [int(a) for a in argv[2:]] or [
        n for n in range(1, 10) if (ROOT / f"tutorial{n}-complete").is_dir()
    ]
    total = sum(check(docs, n) for n in numbers)
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
