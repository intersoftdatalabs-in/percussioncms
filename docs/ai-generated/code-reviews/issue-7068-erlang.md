# Code review — issue #7068 (installer dead-zip removal)

## Static gate (mkd-code-review, machine-only)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Codex review (`codex review --base origin/main`)

> The patch removes obsolete archive installation steps and their otherwise-unused
> property and target without leaving dangling references. No introduced defects were
> found; the XML parsing error in install.xml also exists in the base revision.

No `[P0]`/`[P1]` findings. No `[P2]`/`[P3]` notes. Gate: PASS.

## Reviewer notes

- `system/installResources/install.xml` fails strict `minidom` parsing at line ~1270,
  but the pristine `origin/main` copy fails identically (line ~1273; the 3-line shift
  is this change's own property removal). Pre-existing, out of scope.
- Edited `installServer.xml` parses as well-formed XML; no XML comment in the diff
  contains `--` (#2290 guard).
- Dual-ship check: `modules/perc-distribution-tree/.../Installer/installServer.xml`
  is a structurally different file and contains none of the removed refs — no mirror
  update needed. Same for its `install.xml`.
