# Erlang review — opencode-night-issue-prs workflow-execution fix

## Summary

Workflow-execution fix on top of `c49c8b9c5b`. Five-file batch closes the
two runtime symptoms the human owner reported: (a) worktree was created
before `.opencode/` landed on `main` (commits pre-dating the skeleton
inherited an empty `.opencode/` directory, so the command template was
unresolved); (b) all 14 phases dispatched via `task` to sub-agents that
did not yet exist, silently failing and reporting "no work done". The
launcher now (i) syncs the worktree to `origin/<base>` via `git fetch` +
`git -C <wt> reset --hard <ref>` (no more `git branch -f` — that fails
when the branch is checked out by a worktree), (ii) emits the canonical
`opencode run --auto --command night-issue-prs '<json>'` shape so the
command frontmatter's `agent: night-worker` drives agent selection, and
(iii) sets `env["PWD"]` so subprocess bash resolves relative paths from
the worktree. The host agent (`.opencode/agent/night-worker.md`) gains a
"Specialist availability (HARD — 2026-09-18)" rule with the
**task → inline → record executor** fall-back. Two new sub-agents
(`triage.md`, `work.md`) cover the two phases the host agent dispatches
first. **One real bug** found (triage.md "read-only" claim contradicts
Step 6's instruction to file children); a handful of suggestions / nits
listed below. 21/21 pytest green; dry-run output matches the expected
command shape exactly. Recommendation: **request-changes** — reconcile
the read-only contradiction in `triage.md` and surface the new rule files
for explicit human approval (per AGENTS.md "Human review of agent rules
(HARD GATE)") before committing.

## Scope

- Base: commit `c49c8b9c5b` (prior runtime-fix commit on top of `main`).
- Head: working tree only (`git status --short` shows 2 modifications
  + 2 untracked new files).
- Files: 5 (`scripts/opencode-night-issue-prs.py`, `scripts/test_opencode_night_issue_prs.py`,
  `.opencode/agent/night-worker.md`, plus NEW `.opencode/agent/triage.md`,
  `.opencode/agent/work.md`).
- Prior report: `docs/ai-generated/code-reviews/opencode-night-issue-prs-runtime-fix-erlang.md`
  (recommendation: approve; this batch closes the two bugs the human
  owner reported on top of it).
- Memory patterns hit: cross-platform path/subprocess discipline
  (re-confirmed); rule-file approval gate (re-applied).

## Recommendation

**request-changes**

## Gate

- Blocking bugs: **1** (Issue 1 — triage.md internal contradiction)
- Blocking rule-gate items: **2 uncommitted rule files** must surface
  for explicit human approval before commit (Issue 8 — HARD GATE per
  root AGENTS.md).
- May commit/push: **no** — fix Issue 1; surface Issues 2-7 to author;
  wait for human approval of all three rule files before committing
  `night-worker.md`, `triage.md`, `work.md`.

## Specific checks performed (per the implementer's 12-item request)

| # | Check | Result | Where |
|---|-------|--------|-------|
| 1 | `build_opencode_command` correctness | **PASS** | `scripts/opencode-night-issue-prs.py:183-209`; dry-run output below. |
| 2 | `ensure_worktree` correctness (no `git branch -f`, atomic `-B`, `reset --hard`) | **PASS** | `scripts/opencode-night-issue-prs.py:110-165`; no `branch -f` anywhere in the diff. |
| 3 | `env["PWD"]` set in `build_env` | **PASS but not test-locked** | `scripts/opencode-night-issue-prs.py:222`; **not asserted** in `test_build_env_sets_all_night_vars` (Issue 6). |
| 4 | `--sync-from-local` flag (default off, dev-only) | **PASS but undocumented in script docstring / scripts/README.md; no test** | `scripts/opencode-night-issue-prs.py:259-265`; not in docstring Usage block; not in `scripts/README.md`; not exercised by any test (Issues 3, 4). |
| 5 | Cross-platform: `pathlib.Path` throughout | **PASS** | `scripts/opencode-night-issue-prs.py` uses `Path` for all path construction; `Path(raw).expanduser()` for user-supplied; no `os.path.join`; no hardcoded `/` or `\` in filesystem paths. Hardcoded `/` appears only inside `f"origin/{base_branch}"` (a git ref string, not a filesystem path) and within `Path("/tmp/...")` literals in **test fixtures** (`scripts/test_opencode_night_issue_prs.py:62,92,122`) — pytest creates `tmp_path` and these `/tmp/...` literals are usage-of-the-worktree for fast assertions (the test scaffolding is intentionally Unix-flavored for the GNU CI runner per the prior review); not a portability regression. |
| 6 | `subprocess` discipline | **PASS** | All `subprocess.run` calls use list args + `shell=False` + explicit `timeout=` + `check=True` or `check=False` (only the final opencode invocation uses `check=False` so the child's returncode propagates — correct). `subprocess.run(cmd, shell=False, check=True, timeout=120)` at line 136-142 (fetch); `…timeout=60` at 147-153 (reset); `…timeout=300` at 159-165 (worktree add); `…shell=False, check=False, timeout=None` at 318-325 (opencode invocation). `shlex.join` used only for log strings, never as the actual argv. |
| 7 | `triage.md` read-only + filter rules + 3-slice vertical | **PARTIAL** — read-only is contradicted by Step 6 (Issue 1); filter rules and 3-slice vertical correctly described. |
| 8 | `work.md` correctness + hard bans + labels | **PASS** | All five hard bans correctly codified; labels `operator:opencode`, `operator:night-issue-prs`, `model:${NIGHT_MODEL_ID}` match `night-worker.md:118-122`; build evidence contract (modules_built / build_evidence / downstream_checked / `BUILD SUCCESS` / `Tests run: N, Failures: 0`) matches night-gates C1/C2/C3 contract. |
| 9 | Host agent "Specialist availability" addition | **PASS but cross-reference points wrong direction** | `.opencode/agent/night-worker.md:55-57` says "see 'Specialist availability' above" but the section heading lives below at line 79 (Issue 2). |
| 10 | Tests updated with new stub fields; no stale assertions | **PASS** | `scripts/test_opencode_night_issue_prs.py` correctly extends `type("Args", …)` stubs with `command`, `max_issues`, `base_branch`, `max_prs`, `include_pr_followup` for all paths that exercise the default (`--command`) branch. The previous "stale `assert '--prompt' in cmd`" assertion flagged in the prior review is fully removed. |
| 11 | No copyrighted material / secrets / hardcoded model slugs | **PASS** | No secrets. No hardcoded provider/model slugs (`agent:` frontmatter in `night-issue-prs.md` and the two new sub-agents says `night-worker` / mode `subagent`, which are agent-type identifiers, not model slugs). `NIGHT_MODEL_ID` is documented to be detected from runtime, not hardcoded. |
| 12 | Reproducibility | **PASS** | `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v` reports `21 passed in 0.25s`. Dry-run output: `opencode run --auto --command night-issue-prs '{"max_issues": 1, "base_branch": "main", "max_prs": 6, "include_pr_followup": true}'`. Matches the expected shape verbatim. |

### pytest output (verbatim)

```
============================== 21 passed in 0.25s ==============================
```

### Dry-run output (verbatim)

```
$ python3 scripts/opencode-night-issue-prs.py --dry-run --max-issues 1 --log-level INFO
2026-09-18 18:24:03,001 INFO repo_root=… worktree=… base_branch=main report=…/scratch/night-report.md
2026-09-18 18:24:03,002 INFO dry-run: would invoke opencode run --auto --command night-issue-prs '{"max_issues": 1, "base_branch": "main", "max_prs": 6, "include_pr_followup": true}'
2026-09-18 18:24:03,002 INFO dry-run: cwd=…
2026-09-18 18:24:03,002 INFO dry-run: env NIGHT_WORKTREE_PATH=… NIGHT_BASE_BRANCH=main NIGHT_REPORT_PATH=…
```

## Gate confirmations

- **Human-approval gate for rule files (PENDING).** Three rule-class files in this diff — `.opencode/agent/night-worker.md` (modified; adds "Specialist availability (HARD — 2026-09-18)"), `.opencode/agent/triage.md` (NEW), `.opencode/agent/work.md` (NEW). Per root `AGENTS.md` → **Human review of agent rules (HARD GATE)**, the human owner must explicitly approve each before commit. The implementer's session note acknowledges this: "The new sub-agents are an extension of that scope to make the workflow runnable; surface them explicitly for approval before committing." **Status: acknowledged, awaiting human sign-off. Author must not push or merge until approval is recorded (e.g. inline reply on the PR with the human's "approve" + a checklist per file).**
- **Pre-PR Maven verification (N/A).** No Maven module sources touched. Test gate is `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v` (not `mvnw`). 21/21 green.
- **Cross-platform path/file I/O gate (MET).** `pathlib.Path` end-to-end; `subprocess.run(..., shell=False, check=…)` with `timeout=` for every external call; no `shell=True`, no `os.system`, no `/bin/sh`, no `bash -c`; `shlex.join` only used for log strings. `env["PWD"]` set to a `Path` rendered through `str()` — does not introduce OS-specific separators.
- **Unit-test gate (MET for what is in scope; small coverage gap for new `sync_from_local` behavior — Issue 4).**
- **Copyright header gate (N/A).** No new files outside `.opencode/`. New `.opencode/agent/triage.md` and `.opencode/agent/work.md` follow the established convention (no Intersoft header on agent markdown; no other agent file in `.opencode/agent/` carries one either).
- **No secrets / credentials / hardcoded model slugs in the diff.**

## Issues

### Issue 1 — Severity: bug (BLOCKING)

- File: `.opencode/agent/triage.md:2`, `:102-117` (Step 6), `:175-176` (Hard rules)
- Description: The agent's description frontmatter (line 2) and hard-rules block (lines 175-176) both declare it "Read-only — never edit issues" and "Never run `gh issue edit`, `gh issue close`, `gh pr create`, or any state-changing command." Step 6 ("Apply oversized-issue 3-slice rule") then instructs: *"if this is a p1-p6 PRODUCT epic, file 3 vertical children and queue one"*. Filing issues on GitHub requires `gh issue create`, which is a state-changing command and is forbidden by the agent's own hard rules. **The spec contradicts itself.** Two possible readings:
  1. Triage is actually allowed to file (in which case the frontmatter + Hard rules are wrong).
  2. Triage is truly read-only and Step 6's "file" is aspirational shorthand meaning "discover and propose" (in which case Step 6 is wrong; children belong to `work.md`).
  Either way, an LLM implementing the agent at runtime will see two contradictory instructions and the resulting behavior will be non-deterministic.
- Suggestion: Pick one. Option A (preferred): leave triage strictly read-only; rewrite Step 6 to *"propose the children; the Work sub-agent will file them when it claims the candidate. Add a `child_proposals: [{number, parent, title, disposition: skip}]` field to `scratch/triage.json` so the contract between triage and work is explicit"*. Option B: change Hard rule to *"Read-only on EXISTING issues; filing NEW child issues in Step 6 (oversized-issue 3-slice) is allowed and expected"*, and add an explicit `gh issue create` example inside Step 6 (with body template).
- Pattern-id: `agent.spec.internal-contradiction`

### Issue 2 — Severity: suggestion

- File: `.opencode/agent/night-worker.md:55-57`
- Description: The new fall-back paragraph says *"see 'Specialist availability' above"* — but the actual `### Specialist availability (HARD — 2026-09-18)` heading sits **below** the paragraph (line 79). The cross-reference is backwards and the anchor paragraph it points to has no heading. Confusing for a reader scrolling the file top-to-bottom.
- Suggestion: Either say *"see 'Specialist availability' below"* OR move the fall-back paragraph below the new `### Specialist availability` heading so the cross-reference can resolve to a real anchor.

### Issue 3 — Severity: suggestion

- File: `scripts/opencode-night-issue-prs.py:24-32` (Usage block), `:51-56` (Windows example), and `scripts/README.md` (no entry for any of the three new flags).
- Description: The script's argparse help text documents `--command`, `--prompt`, and `--sync-from-local` correctly. But the script's **top-of-file docstring** `## Usage` / `## Examples` / `## Cron wiring` blocks were not extended for the new flags — they're invisible to a `head -50 file.py` reader. Same for `scripts/README.md` — confirmed by `grep -n 'sync-from-local\|sync_from_local\|--command\|--prompt' scripts/README.md` which returns nothing. Not a regression (those flags work and argparse help covers them), but the Session A review's "engineering README entry" convention expects new flags to be discoverable via the existing one-line-per-flag pattern.
- Suggestion: Add a short subsection to `scripts/README.md` under `### opencode-night-issue-prs.py` — e.g. `#### Advanced flags` listing each new flag with one line; mirror in the script docstring's `## Usage` block: `python3 scripts/opencode-night-issue-prs.py [--max-issues N] [--base-branch BR] [--command NAME] [--prompt TEXT] [--sync-from-local] …`. One paragraph; not blocking.

### Issue 4 — Severity: suggestion

- File: `scripts/test_opencode_night_issue_prs.py` (no test for `ensure_worktree(sync_from_local=True)` or for the new `git reset --hard` flow on subsequent runs)
- Description: The `ensure_worktree()` body in this diff introduces two NEW behaviors that were not in the prior commit:
  1. `git fetch origin <base>` is now always called (when `sync_from_local=False`).
  2. `git -C <wt> reset --hard <sync_ref>` now runs on every subsequent run.
  3. `sync_ref` flips between `origin/<base>` and local `<base>` based on `--sync-from-local`.
  None of these are exercised by any test. The pre-existing `test_ensure_worktree_no_ops_when_worktree_exists` only asserts the worktree is still registered after the call — it does not assert (a) that `git fetch` was called, (b) that HEAD actually moved, or (c) that `--sync-from-local=True` skips the fetch. A future refactor that silently removes the reset would still pass this test.
- Suggestion: Add two tests using the same `git init --bare` + clone + push + worktree scaffolding as the existing `test_worktree_exists_true_for_real_worktree`:
  - `test_ensure_worktree_resets_head_on_existing_worktree` — push a 2nd commit, run `ensure_worktree`, assert `git -C <wt> log --oneline` shows only the 2nd commit's SHA.
  - `test_ensure_worktree_sync_from_local_skips_fetch_and_uses_local_ref` — point `sync_from_local=True` at a local branch, run `ensure_worktree`, assert no `git fetch origin main` was attempted (you can use `monkeypatch` to wrap `subprocess.run` and count `fetch` calls).

### Issue 5 — Severity: suggestion

- File: `scripts/opencode-night-issue-prs.py:222`
- Description: `env["PWD"] = str(worktree)` overrides the inherited PWD. On Linux this is correct and matches what `bash` would compute after `cd`. On Windows the bash tool often runs under MSYS / Git Bash which track PWD via their own state — setting `env["PWD"]` may not be honored by the bash tool's internal `pwd` (it may also accept a Windows-style path). The docstring comment on line 219-221 doesn't call out the cross-platform caveat. The behavior is *probably* benign on Windows (opencode's bash tool recomputes PWD from its CWD), but it's not testable from this repo without a Windows CI runner.
- Suggestion: Optional — extend the comment on lines 219-221 with one line: *"On Windows / under MSYS, the bash tool may re-derive PWD from its CWD after launch; setting it here is best-effort and only matters for tools that consult `$PWD` directly without a corresponding `cd`."*

### Issue 6 — Severity: nit

- File: `scripts/opencode-night-issue-prs.py:222` and `scripts/test_opencode_night_issue_prs.py:99-115` (`test_build_env_sets_all_night_vars`)
- Description: `build_env` sets `env["PWD"]` as part of the new behavior, but `test_build_env_sets_all_night_vars` asserts only the five NIGHT_* vars and **does not lock PWD**. The behavior is unlocked; a future refactor that drops the PWD line would still pass the test.
- Suggestion: One-line addition: `assert env["PWD"] == str(worktree)` in the existing test.

### Issue 7 — Severity: nit

- File: `.opencode/agent/triage.md:155-163` and `.opencode/agent/work.md`
- Description: `triage.md` writes `candidates: [...]` and `slotted_for_work: [...]` into `scratch/triage.json`. `work.md` "claim-check" reads `${NIGHT_WORKTREE_PATH}/scratch/triage.json` for its candidate. The implicit contract between the two agents — which fields `work.md` actually consumes, what the disposition values mean, how `parent_issue` flows — is not documented in either file. Reasonable today (a future agent author reading both will see the names), but freezes poorly; a rename in one without the other is silent breakage.
- Suggestion: Add a one-paragraph "Contract with `work` sub-agent" subsection at the bottom of `triage.md`'s "Hard rules" section: *"`work.md` consumes `candidates[*]` from `scratch/triage.json`; it skips any candidate with `disposition: skip`. The fields it reads are `number`, `title`, `parent_issue`, `modules_built_hint`, and any `child_proposals` queued for a parent epic. A schema bump must update both files in the same commit."*

### Issue 8 — Severity: gate (not a bug — a HARD GATE checklist item)

- File: All three of `.opencode/agent/night-worker.md`, `.opencode/agent/triage.md`, `.opencode/agent/work.md`.
- Description: Per root `AGENTS.md` → **Human review of agent rules (HARD GATE)**, every agent rule/instruction file change must surface for explicit human review before commit. The implementer's session note acknowledges this and asks to be reminded. **Status: pending.** The author may not commit these three files (or open a PR containing them) until the human owner has reviewed and approved each diff.
- Suggestion: When requesting human approval, present each file's intent in one paragraph so the reviewer can sign off without re-reading the file cold:
  - `night-worker.md` — adds a `### Specialist availability (HARD — 2026-09-18)` subsection that codifies the **task → inline fall-back → record-executor** policy. New behavior: when a sub-agent file is missing or `task` fails, the host executes the phase inline using its own tools and stamps the phase status table with `executor: inline`.
  - `triage.md` — NEW sub-agent implementing rhai Phase 6 Triage: maintainer discovery, candidate filtering, PRODUCT/DEBT ranking, oversized-issue 3-slice handling. Read-only (modulo Issue 1 contradiction).
  - `work.md` — NEW sub-agent implementing rhai Phase 8 Work: claim-check, branch, code, build evidence (C1/C2/C3), commit + push (`--force-with-lease` only), open PR with required labels + structured `## Build evidence` body, update parent body table, write `scratch/work-<N>.json`.

## Change-class completeness

| Change-class item | Status | Notes |
|---|---|---|
| Primary artifact (launcher — three behavior fixes) | Met | `scripts/opencode-night-issue-prs.py:110-165`, `:183-209`, `:212-223`. |
| Companion test file | Met | `scripts/test_opencode_night_issue_prs.py` — 21/21 green; new tests cover JSON serialization + prompt override; coverage gap on `sync_from_local` and reset behavior flagged in Issue 4. |
| Rule files (`night-worker.md` modified; `triage.md` + `work.md` new) | Pending human approval | See Issue 8. |
| `scripts/README.md` (advanced flags) | Not updated | See Issue 3 (suggestion). |
| `.opencode/command/night-issue-prs.md` (`agent:` frontmatter check) | Met | Verified: frontmatter `agent: night-worker` matches the launcher's `AGENT_NAME` constant. The launcher's `--command night-issue-prs` (default) therefore correctly selects the night-worker agent via command-template frontmatter per opencode's documented `--command` semantics. |
| `.opencode/agent/<other sub-agents>` | **Deferred to Session B (out of scope)** | Eight remaining sub-agents (`preflight`, `reconcile`, `pr-follow-up`, `peer-pr-review`, `pr-cluster`, `security-audit`, `cycle-verify`, `human-qa`) are explicitly deferred; the new "Specialist availability (HARD)" rule ensures the host falls back to inline for them today. Not a Session B blocker for this batch. |
| Maven pre-PR build (per AGENTS.md) | N/A | No Maven module sources touched. |
| Product-docs update | N/A | Internal engineering tooling; not an operator- or user-facing product behavior change. |
| Night-runner plugin `.opencode/plugin/night.ts` | Out of scope | Plugin reads the `NIGHT_*` env vars the launcher sets; unchanged. |

## Voice

This is a solid follow-up that lands the two behaviors the human asked for
and adds the bare minimum so the workflow can actually run today. The
launcher is correct end-to-end; pytest is green; the dry-run shows exactly
the command shape we'd want. The one real defect is `triage.md`
contradicting itself on read-only vs filing children — easy to fix but
worth fixing before this becomes an LLMs-arguing-with-themselves
runtime surprise. The five remaining points are suggestions / nits /
coverage gaps that the author should land in the same batch or a quick
follow-up, not blockers.

## Durable artifact / cross-reference

- This report is the canonical review for the workflow-execution fix on
  top of `c49c8b9c5b`.
- Prior runtime-fix review: `docs/ai-generated/code-reviews/opencode-night-issue-prs-runtime-fix-erlang.md`.
- Prior Session A batch review: `docs/ai-generated/code-reviews/session-a-opencode-night-batch-erlang.md`.
- Prior skeleton review: `docs/ai-generated/code-reviews/opencode-night-issue-prs-skeleton-erlang.md`.

## Pattern memory

No new patterns to promote. The triage read-only / state-change
contradiction (Issue 1) is too agent-spec-specific to generalize into
`erlang-review/patterns.md`. If a similar "this agent is read-only except
for step X" contradiction recurs across the opencode / grok sub-agent
specs, promote then.
