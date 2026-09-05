# Security

Report a vulnerability through GitHub's private reporting on this repository
(**Security → Report a vulnerability**). Do not open a public issue for it.

Supported: `main` and the latest dated tag. Earlier tags are snapshots of the
family versions they were verified against and receive no fixes; the fix
lands on `main` and the next tag carries it.

The trees run only published, pinned dependencies (see `pins.env`); a
vulnerability in one of those is reported to that project, and the pin here
moves in the re-pin commit that follows its fix.
