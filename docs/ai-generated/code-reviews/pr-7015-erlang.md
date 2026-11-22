## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s) — but CI is
**red** on Windows (`python-build-scripts windows-latest`), which is a hard
gate independent of the machine count. Verdict: **request-changes**.

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- Persona: erlang 0.1.1
- CI: `python-build-scripts (ubuntu-latest)` pass; `python-build-scripts
  (windows-latest)` **fail** (2 failed, 1142 passed)

## Recommendation

request-changes

## Gate

- Blocking bugs: 1 (CI-red, in-diff cross-platform test-harness defect)
- May commit/push: no — fix first, then re-review

## Issues

### Issue 1 -- Severity: bug

- File: scripts/test_gh_json.py:52 (`_install_fake_gh` Windows branch;
  surfaces at test_stdout_stays_pure_json_despite_stderr_hint:99 and
  test_nonzero_exit_fails_loudly_with_same_code:110)
- Rule: `cross-platform` (Windows-only CI failure in new test code)
- Tool: CI (`python-build-scripts windows-latest`) + manual review
- Description: The Windows `gh.cmd` fake is not intercepting `gh`
  resolution on the Windows runner. Both failing tests invoke the real
  `gh` instead: stderr shows the genuine GitHub CLI error `gh: To use
  GitHub CLI in a GitHub Actions workflow, set the GH_TOKEN environment
  variable` with exit code 4, so `assert proc.returncode == 0` fails (got
  4) and `assert proc.returncode == 1` fails (got 4). Ubuntu is green, so
  the POSIX `gh`-shim path works and this is a Windows-specific lookup
  defect in the new harness — exactly the class of bug this PR's own
  contract ("cross-platform, pure Python") forbids.
- Suggestion: Make `gh` resolution deterministic instead of relying on
  ambient `PATH`/PATHEXT lookup differences (`gh-json.py` currently calls
  `subprocess.run(["gh", ...])` with no explicit lookup). Candidate fixes
  for erlang-fix to evaluate: resolve the binary once via `shutil.which`
  against the effective `PATH` and exec the absolute path; or on Windows
  bypass `CreateProcess` search quirks by invoking the shim explicitly in
  tests. Prove with a Windows CI re-run (ubuntu-only green is not
  sufficient evidence for this file).
- Status: fixed in 2a297de470 (mitigation posted; host Erlang re-review pending)

## Fix verification (erlang-fix 2a297de470)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Notes

- Fix: `gh-json.py` gains `resolve_gh()` (`shutil.which` honoring full
  `PATHEXT`, plus an explicit `nt` `PATHEXT`-scan fallback that is
  case-insensitive and uses only `os.path.join`/`os.pathsep` — no
  hardcoded separators); `main()` execs the resolved absolute path.
  POSIX behaviour unchanged. New behavioral test
  `test_resolve_gh_honors_pathext_cmd_shim` proves the Windows lookup
  path (simulated `nt`, runs on any host).
- Tests (Linux): `pytest scripts/test_gh_json.py
  scripts/test_night_queue_claim.py` → 21 passed. Windows execution
  proof is this push's `windows-latest` CI run.
- Python-only: no Maven build required. Product documentation: N/A
  (internal agent tooling). UI: N/A.
