# Erlang review — PR #7386 (pre-commit pass, 2026-11-28)

> Independent pre-commit review of the uncommitted diff on branch
> `fix/night-launcher-untracked-conflicts` (reviewer is not the implementer).
> Static gate on the branch diff (`origin/main...HEAD`); tests executed in
> the primary checkout. Does not rubber-stamp: one BLOCK finding (F1) was
> raised and resolved before commit, plus two P2 hardenings folded in.

## Verdict

LGTM after F1/F2/F3 resolution (this file records the as-merged state).

## Static gate (sanitized `mkd-code-review --machine-only`)

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

## Independent verification (reviewer-performed, not author claims)

- **Diff read in full:** 3 files — launcher helper + call site + docstrings,
  4 regression tests, 1 README bullet. No production logic touched.
- **Behavioral tests:** `python3 -m pytest scripts/test_opencode_night_issue_prs.py`
  → 68 passed (64 existing + 4 new), including the stall-recovery regression
  and the symlink-external safety test.
- **Cross-platform checklist:** no `shell=True`; `pathlib` joins throughout
  (`worktree / rel` handles git's `/` on Windows); `str()` only at the
  `shutil.move` boundary; timeouts on all git subprocesses; fail-open
  warn-and-continue preserves prior behavior on git failure; backup under
  gitignored `tmp/` (test asserts `night-sync-backup` absent from
  `status --porcelain`); `write_text`/`read_text` round-trip is line-ending
  safe; NUL-split output avoids locale separator issues.
- **No secrets** in code, logs, or report.

## Findings (all resolved pre-commit)

- **F1 — BLOCK (change-class closure):** `scripts/README.md` carried a second,
  pre-existing hunk ("Unattended loop") referencing untracked
  `night-loop-*.sh` scripts. Left unstaged/out of this commit; this PR stages
  only the new stall-proof bullet.
- **F2 — P2 (folded in):** `ls-tree` results filtered to the untracked
  candidate set so a symlinked dir can't pull external files into the backup.
  Covered by `test_move_aside_never_moves_files_outside_worktree`.
- **F3 — P2 (folded in):** `ValueError` added to both scan exception handlers
  so an undecodable untracked filename warns instead of aborting the sync.

## Codex review

Exit 0. Two `[P2]`s (symlink traversal, non-UTF-8 filename crash) — both
resolved as F2/F3 above; no blocking findings.
