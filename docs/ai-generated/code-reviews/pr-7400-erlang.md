# Erlang review — PR #7400 (pre-commit pass, 2026-11-28)

> Independent pre-commit review of the uncommitted diff on branch
> `fix/night-launcher-orphaned-worktree` (reviewer is not the implementer).
> Follow-up to merged #7386: the next stall had a different cause (orphaned
> husk, lost `.git`, live nested checkout for open PR #7396 inside).

## Verdict

LGTM (approve), no BLOCK findings. Two P2 suggestions folded in before commit.

## Static gate (sanitized `mkd-code-review --machine-only`, re-run on final diff)

## Summary

Machine analysis found **0** blocking bug(s). LLM skipped (machine_only_mode)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

(The single suppressed row is the pre-existing adjudicated FP
`paths.hardcoded_sep` doc example, issues #6924/#6779/#6781 — untouched.)

## Independent verification (reviewer-performed, not author claims)

- **Diff read in full:** 2 files — reclaim helpers + call site + docstrings,
  3 regression tests. No production logic touched.
- **Behavioral tests:** `python3 -m pytest scripts/test_opencode_night_issue_prs.py`
  → 71 passed (68 existing + 3 new: reclaim success with husk preserved
  aside, nested-refusal with husk + nested checkout untouched, stale
  admin-registration recovery without manual prune).
- **Cross-platform checklist:** `Path` ops (`parent / f"…"` joins,
  `.resolve()`, `.parents` containment), `shutil.move`, `subprocess.run`
  with `shell=False` + timeouts, fail-open warn paths preserved, no
  hardcoded separators, no Unix-only roots.
- **No secrets** in code, logs, or report.

## Findings (both P2, folded in pre-commit)

- **P2 (folded in):** lost `.git` with stale admin registration still present
  — launcher now runs defensive `git worktree prune` after reclaim, before
  `add`. Covered by `test_ensure_worktree_recovers_with_stale_registration`.
- **P2 (folded in):** `shutil.move` can raise `shutil.Error` (not `OSError`) —
  now caught alongside `OSError` and mapped to `LauncherError`.

## Codex review

Ran clean of P0/P1 (the two P2s above); no blocking findings.
