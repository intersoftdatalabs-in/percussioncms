# Erlang review — opencode-night-issue-prs runtime fix (follow-up)

## Summary

Targeted runtime fix to `scripts/opencode-night-issue-prs.py` (and its test
file) on top of Session A's already-approved commit (`a29fc44746`). Two
user-reported bugs closed: (a) `opencode run` received no message and printed
help because `--prompt` was a top-level opencode option not consumed by the
`run` subcommand — now passed as a positional argument; (b) `-v` was being
consumed as the value of `--log-level` (then rejected by `choices=[...]`) —
now registered as a separate `store_const` flag that sets `log_level="DEBUG"`.
Both fixes are minimal, behavior-preserving, and backed by 3 new/changed
unit tests. All 18 tests green; no regressions, no new warnings, no
cross-platform path concerns. Recommendation: **approve**.

## Scope

- Base: commit `0433d0f2da` (Session A batch on top of `a29fc44746`).
- Head: working tree only (uncommitted modifications to 2 files).
- Files: 2 modifications
  - `scripts/opencode-night-issue-prs.py:152-171, 184-225` — prompt is positional; `-v` is a separate store_const flag.
  - `scripts/test_opencode_night_issue_prs.py:132-159` — updated `test_build_opencode_command_with_model_and_prompt` (was `--prompt in cmd`, now `cmd[-1] == "do the thing" and "--prompt" not in cmd`); added `test_build_opencode_command_prompt_is_positional`, `test_minus_v_shortcut_sets_debug_log_level`, `test_minus_v_overrides_explicit_log_level`.
- Prior report: `docs/ai-generated/code-reviews/session-a-opencode-night-batch-erlang.md` (recommendation: request-changes with 2 blocking bugs — worktree `.is_dir()` check, missing test file; both since fixed). This review closes 2 follow-up bugs reported by the user on top of that batch.
- Memory patterns hit: none new. (Already-promoted `tests.missing-for-new-script` is no longer relevant — the test file exists.)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Gate confirmations

- **Human-approval gate for agent rule files (N/A):** Diff is `scripts/*.py` only — no `AGENTS.md`, `.kilocode/`, `.opencode/rules/`, skill, prompt, or workflow files. HARD GATE does not apply to this commit.
- **Pre-PR Maven verification (N/A):** `scripts/` is not under any Maven module; tests run via `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v` (no `mvnw` needed). The standalone pytest run is the equivalent pre-PR gate for this change class — see Test results below.
- **Cross-platform path/file I/O gate (MET):** No filesystem path code in the diff. `subprocess.run(cmd, ..., shell=False)` is used in `main()` (line 264), so argv is passed as a list — no shell interpolation of `$ARGUMENTS` / `$NIGHT_REPORT_PATH` in the default prompt text. `shlex.join` is used only for **logging** the resolved command (lines 122, 132, 142, 247, 263), never as the actual command to execute. Confirmed by reading `subprocess.run(cmd, shell=False, ...)` at `scripts/opencode-night-issue-prs.py:264`.
- **Unit-test gate (MET):** 18/18 tests pass under `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v`. The two regression-targeted tests (`test_minus_v_shortcut_sets_debug_log_level`, `test_minus_v_overrides_explicit_log_level`) and the new positional-prompt test (`test_build_opencode_command_prompt_is_positional`) plus the updated `test_build_opencode_command_with_model_and_prompt` cover the behavioral surface. The previously-passing test that asserted `assert "--prompt" in cmd` (which masked the bug) has been **correctly rewritten** — no stale assertion left.
- **Copyright header gate (N/A):** No new files.
- **No secrets / credentials** in the diff.
- **No silent failures** introduced. The `-v` last-wins interaction with `--log-level` is intentional and documented in the new test (`test_minus_v_overrides_explicit_log_level`).

## Specific checks performed (per the implementer's request)

1. **Positional prompt correctness — PASS.**
     `scripts/opencode-night-issue-prs.py:169-170` reads `if args.prompt: cmd.append(args.prompt)` (no `--prompt` prefix). Verified by dry-run:
     ```
     opencode run --agent night-worker --auto 'Run the night-issue-prs workflow with args from $ARGUMENTS. Parse ...'
     ```
     With `--model`: `opencode run --agent night-worker --auto --model anthropic/claude-sonnet-4-6 '<prompt>'`. Matches the expected shape exactly.
2. **`-v` / `--log-level` interaction — PASS.** All 5 cases verified via direct `parse_args` invocation:
     | argv | result |
     |---|---|
     | `["-v"]` | `log_level="DEBUG"` |
     | `["--log-level", "INFO"]` | `log_level="INFO"` |
     | `["-v", "--log-level", "WARNING"]` | `log_level="WARNING"` (last-wins) |
     | `["--log-level", "WARNING", "-v"]` | `log_level="DEBUG"` (last-wins; covered by new test) |
     | `["--log-level", "DEBUG", "-v"]` | `log_level="DEBUG"` (no-op for `-v`) |
     The two new tests cover the most user-likely cases; `-v --log-level X` (WARNING wins) is an edge case the implementer chose not to lock in — acceptable.
3. **Shell quoting — PASS.** `main()` at line 264 invokes `subprocess.run(cmd, shell=False, ...)`. `cmd` is a `list[str]` built by `build_opencode_command()`; no `shell=True`, no `shlex.join` in the actual invocation path. Default prompt text containing `$ARGUMENTS` and `$NIGHT_REPORT_PATH` is passed verbatim as one argv element — no shell interpolation. Confirmed.
4. **No regressions — PASS.** `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v` reports `18 passed in 0.23s`. The test that previously asserted `--prompt in cmd` is **gone**, replaced with a positional check + flag-absence check (`assert cmd[-1] == "do the thing"; assert "--prompt" not in cmd`). No leftover stale assertion that would mask a regression.
5. **No new warnings — PASS.** Only stdlib (`argparse`) used; no new imports; no new third-party deps.
6. **Prompt default quoting — PASS.** Default `--prompt` text at lines 203-208 contains `$ARGUMENTS` and `$NIGHT_REPORT_PATH`. Under `subprocess.run(cmd, shell=False)`, these are passed verbatim to argv — no shell interpolation. The placeholders are intentional (the night-worker agent is expected to substitute them with shell-supplied values). Pre-existing in the Session A commit; not introduced by this fix.

## Test results

```
18 passed in 0.23s
```

New / updated tests:
- `test_build_opencode_command_with_model_and_prompt` (updated) — `cmd[-1] == "do the thing"`; `"--prompt" not in cmd`.
- `test_build_opencode_command_prompt_is_positional` (new) — `cmd[-1] == "hello world"`.
- `test_minus_v_shortcut_sets_debug_log_level` (new) — `parse_args(["-v"])` → `log_level == "DEBUG"`.
- `test_minus_v_overrides_explicit_log_level` (new) — `parse_args(["--log-level", "WARNING", "-v"])` → `log_level == "DEBUG"`.

## Issues

None.

## Change-class completeness

| Change-class item | Status | Notes |
|---|---|---|
| Primary artifact (launcher fix) | Met | `scripts/opencode-night-issue-prs.py` — prompt positional, `-v` shortcut. |
| Companion test file | Met | `scripts/test_opencode_night_issue_prs.py` — 4 relevant tests updated/added. |
| Product docs (`product-docs/8.2/`) | N/A | Internal engineering fix; not operator- or user-facing surface change. Script already documented in `scripts/README.md` (Session A). |
| Agent rule files | N/A | No rule / instruction files in the diff. HARD GATE does not apply. |
| Maven pre-PR build | N/A | `scripts/` is non-Maven; pytest is the equivalent gate. |
| `scripts/README.md` | N/A | No new script; no behavior change visible at the script-discovery surface. |

## Voice

This is exactly the kind of small, targeted runtime fix Erlang likes to see:
two specific user-reported bugs, two corresponding code changes, four
behavior-locking tests, no scope creep, no opportunistic refactors. The
docstring on `build_opencode_command` documents **why** `--prompt` does not
work (top-level vs subcommand), so the next person to touch this won't reintroduce
the same bug. Author may commit.

## Durable artifact / cross-reference

- This report is the canonical review for the runtime fix follow-up to Session A.
- Prior Session A review: `docs/ai-generated/code-reviews/session-a-opencode-night-batch-erlang.md`.
- Prior skeleton review: `docs/ai-generated/code-reviews/opencode-night-issue-prs-skeleton-erlang.md`.

## Pattern memory

No new patterns to promote. The `--prompt`-flag-vs-positional trap is too
CLI-specific to generalize into `erlang-review/patterns.md`. If a similar
subcommand-vs-parent-flag issue recurs across the opencode/grok tooling, the
recurrence is the right time to add a one-liner.