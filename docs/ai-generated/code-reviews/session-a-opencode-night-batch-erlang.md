# Erlang review — Session A batch (opencode night-issue-prs follow-ups)

## Summary

Six-file Session A batch on top of the already-committed skeleton
(`aba72a7993` + `23e7bf8987`): `.gitignore` opencode block, two `AGENTS.md`
table/list additions, new `.opencode/rules/worktree-hygiene.md`, new
`scripts/opencode-night-issue-prs.py` launcher, a `scripts/README.md` section
documenting it, and the new `modules/ai-shared-develop/src/main/resources/skills/night-gates/SKILL.md`
that closes the prior Erlang skeleton's deferred follow-up. Two real bugs
found — both blocking. The new launcher has a **broken worktree-existence
check** (uses `.is_dir()` on the inner `.git`, which is a **file** in any git
worktree, so it always thinks the worktree is missing and re-runs `git worktree
add`, which crashes on the second consecutive run). And the Session A batch
ships **no test file** for the new launcher, violating change-class
completeness. A soft concern: the `.gitignore` re-include rules for the
**singular** dirs (`.opencode/command/`, `.opencode/agent/`,
`.opencode/plugin/` — the paths the existing skeleton actually uses) lack the
content-filter pattern that the **plural** rules have, so any file dropped
into a singular dir is tracked). Several suggestions (defensive) below.

## Scope

- Base: `main`
- Head: uncommitted (working tree only)
- Files: 6 modifications/additions
  - `.gitignore` — added `.opencode` ignore block (last-match-wins mirroring `.grok`).
  - `AGENTS.md` — line 26 `.opencode/**` in rule-class table; lines 79-80 `.opencode/worktrees/` in worktree list.
  - `.opencode/rules/worktree-hygiene.md` — NEW (67 lines).
  - `scripts/opencode-night-issue-prs.py` — NEW (239 lines).
  - `scripts/README.md` — new `### opencode-night-issue-prs.py` section (lines 15-57).
  - `modules/ai-shared-develop/src/main/resources/skills/night-gates/SKILL.md` — NEW (197 lines).
- Prior report: `docs/ai-generated/code-reviews/opencode-night-issue-prs-skeleton-erlang.md` (skeleton review; identified `night-gates` skill as a deferred follow-up that this batch now closes).
- Memory patterns hit: cross-platform `.is_dir()`-vs-`.exists()` worktree detection (new — see Issue 1); change-class companion test gap; `gitignore.last-match-wins.singular-vs-plural` (new).

## Recommendation

**request-changes**

## Gate

- Blocking bugs: **2**
- May commit/push: **no** — fix Issues 1 and 2 (plus optionally 3) before committing.

## Gate confirmations

- **Human-approval gate for rule files (MET):** Author's stated context — human owner has explicitly approved committing each rule file as a separate commit. Per root `AGENTS.md` → **Human review of agent rules (HARD GATE)**, the gate is satisfied in principle. Drafts that need fixes (Issues 1, 2 below) should be re-submitted for human approval after the fixes.
- **Pre-PR Maven verification (N/A):** Only `AGENTS.md` / `.gitignore` / `.opencode/` / `scripts/README.md` / `scripts/opencode-night-issue-prs.py` / `modules/ai-shared-develop/src/main/resources/skills/night-gates/` touched. Per `AGENTS.md` → **Pre-PR Maven verification** exemption for docs / AGENTS / non-Maven files, no `mvnw clean install` required.
- **Cross-platform path/file I/O gate (MET with one bug, see Issue 1):** The launcher uses `pathlib.Path` end-to-end (no `os.path.join` with hardcoded `/` or `\`), `Path.home()` for the worktree root, `Path(__file__).resolve().parent.parent` for the repo root, all `subprocess.run(..., shell=False)` with appropriate `check=True/False` and `timeout=` arguments. The docstring's `## Behavioral Notes` (FR-009b) and `logging.getLogger(__name__)` with format `%(asctime)s %(levelname)s %(message)s` (FR-009c-ish) are present. Stdlib-only at runtime (no `requests`, no third-party). The `--dry-run` path is **truly** non-mutating (verified: returns 0 before `ensure_worktree`, before `report.parent.mkdir`, and before any `subprocess.run`). The Windows Task Scheduler example string on line 47 (`cmd /c cd /d C:\...`) is a documentation string, not an executed command — acceptable. **One bug**: the worktree-existence check on lines 103, 213, 218 uses `(worktree / ".git").is_dir()`, which returns `False` for any git **worktree** (where `.git` is a **file** pointing to the worktree's gitdir), not a directory. The sibling `nightly_i18n_refresh.py` correctly uses `worktree.exists()` (line 139) and even cross-checks with `git worktree list --porcelain` (lines 141, 174).
- **Copyright header gate (MET by convention):** Per `AGENTS.md` → **Copyright / Apache license headers (HARD GATE)**, new source files should carry `Copyright (c) 2026 Intersoft Data Labs, Inc.` + Apache 2.0 block. The new Python file has **no** header. However, every existing 2026 Python file in `scripts/` (e.g. `nightly_i18n_refresh.py`, `prune-stale-worktrees.py`, `install-cms-dev.py`, `erlang-harvest-review-patterns.py`, `stage-triage-cluster.py`, `hot-deploy-local.py`) also lacks a header, so the diff **follows the established convention**. The Markdown files (`worktree-hygiene.md`, `night-gates/SKILL.md`) match the convention of other skill/rule markdown (no header). No rebrand or copyleft content present.
- **Operative deployment note (N/A):** The launcher script is operational tooling, not part of spec 994; `scripts/README.md` documents it as such (lines 55-57).
- **`night-gates` skill body (MATCHES rhai README):** C1, C2, C3, C5 are present (matching `.grok/workflows/README.md:55-58` main Work gates table — the README itself skips C4, mirroring that omission); B1, B2, B3 match the cluster gates table (`.grok/workflows/README.md:478-481`); Q1–Q8 match the human-QA handoff gates table (`.grok/workflows/README.md:289-297`). Frontmatter has `name: night-gates` and a multi-line `description: >-`. References to `codeql-pr`, `erlang-review`, `percussioncms-dev`, `java-unit-testing`, `maven-integrity-validator`, `javadoc` all resolve to existing skills (verified by `ls`). One fidelity gap (see Issue 3): C4 (product documentation gate, defined in `.grok/workflows/night-issue-prs.rhai:1972` and `.grok/workflows/README.md:84` indirectly via the "HARD BAN" mentioning `product-docs/`) is **not** present in either the rhai README's main gates table or this skill — both mirror each other. Acceptable.

## Issues

### Issue 1 -- Severity: bug (BLOCKING)

- File: `scripts/opencode-night-issue-prs.py:103`, `:213`, `:218`
- Description: `(worktree / ".git").is_dir()` is the wrong existence check for a git worktree. When a worktree is created via `git worktree add`, the inner `.git` is a **file** (`gitdir: <main>/.git/worktrees/<name>`), not a directory. Confirmed empirically: created a worktree at `/tmp/wt-test6/wt-dir`, ran `ls -la`, got `-rw-r--r-- ... .git` (regular file). `test -d /tmp/wt-test6/wt-dir/.git` returns false (not a directory); `test -e ...` returns true. Consequence: on every run after the first, `(worktree / ".git").is_dir()` returns False, so `ensure_worktree` proceeds to `git fetch`, `git branch -f night-issue-prs origin/main`, then `git worktree add <path> night-issue-prs`. The `git worktree add` step **fails** with `'path' already exists` (exit 128) because the worktree is already registered — the dry-run mode reproduces the same diagnostic ("worktree does not exist yet; would create from origin/main") even when the worktree exists. The launcher will crash the overnight worker on the second consecutive run, which is exactly the case that matters for an unattended cron-driven workflow. The sibling `scripts/nightly_i18n_refresh.py:139, 176` correctly uses `worktree.exists()` and even cross-checks with `git worktree list --porcelain`. `scripts/erlang-harvest-review-patterns.py:233` explicitly distinguishes worktree (`.git` is a file) from main checkout (`.git` is a directory).
- Suggestion: Change `(worktree / ".git").is_dir()` to `(worktree / ".git").exists()` (or simpler, `worktree.exists()`) on lines 103, 213, 218. Optionally, add a `git worktree list --porcelain` cross-check matching `nightly_i18n_refresh.py`'s `worktree_registered()` helper to also catch the "path exists but is not a registered worktree" case (e.g. a stale dir from a previous failed run).
- Pattern-id: `paths.worktree-git-is-file-not-dir`

### Issue 2 -- Severity: bug (BLOCKING)

- File: `scripts/` (missing file)
- Description: The new Python launcher has **no companion test file** (`scripts/test_opencode_night_issue_prs.py` does not exist). Per root `AGENTS.md` → **Project Rules**: "you must ALWAYS update or create unit tests for any code change that you make, new or edited. And the tests must pass. No exceptions." Per spec 994 FR-009, every script under `scripts/` ships with a colocated `scripts/test_<name>.py` module — every existing sibling script (`nightly_i18n_refresh.py` → `test_nightly_i18n_refresh.py`, `prune-stale-worktrees.py` → `test_prune_stale_worktrees.py`, `install-cms-dev.py` → `test_install_cms_dev.py`, `erlang-harvest-review-patterns.py` → `test_erlang_harvest_review_patterns.py`, `stage-triage-cluster.py` → `test_stage_triage_cluster.py`, `hot-deploy-local.py` → `test_hot_deploy_local.py`, etc.) follows the pattern. The new launcher's behavior is non-trivial (default worktree derivation, `--dry-run` non-mutation, `--no-worktree-create` short-circuit, env-var pass-through, `NIGHT_*` env construction, `ensure_worktree` idempotence — which is also where Issue 1 lives). Without tests, Issue 1 would not have been caught.
- Suggestion: Add `scripts/test_opencode_night_issue_prs.py` that covers at minimum: (a) `default_worktree_path()` uses `Path.home()` not `~`; (b) `resolve_worktree` honors `--worktree`, then `NIGHT_WORKTREE_PATH`, then default; (c) `resolve_report_path` honors `--report-path`, then `NIGHT_REPORT_PATH`, then `<worktree>/scratch/night-report.md`; (d) `ensure_worktree` no-ops when worktree already exists (would have caught Issue 1); (e) `--dry-run` does not create directories (assert worktree parent absent after run); (f) `build_opencode_command` returns `["opencode","run","--agent","night-worker","--auto","--prompt",prompt]` and appends `--model` only when set; (g) `build_env` sets all five `NIGHT_*` env vars including `NIGHT_OPERATOR=opencode` and `setdefault("NIGHT_CODING_TOOL", "OpenCode")`. Use the existing pattern: `subprocess.run([sys.executable, str(script_path), ...])` per spec 994 R4. Per AGENTS.md "Pre-PR Maven verification" — `scripts/` is not under any Maven module; tests run via `python3 -m pytest scripts/test_opencode_night_issue_prs.py -v` (no `mvnw` needed).
- Pattern-id: `tests.missing-for-new-script`

### Issue 3 -- Severity: suggestion (NOT BLOCKING)

- File: `scripts/opencode-night-issue-prs.py:140-151`
- Description: `build_opencode_command` always appends `["--prompt", args.prompt]`, even when the user passes `--prompt ""`. The `--model` branch (line 148) is conditional on `args.model`, but `--prompt` is not. An empty-string prompt is likely to break `opencode run` (unknown CLI flag value). Edge case, but trivially fixable.
- Suggestion: Mirror the `--model` pattern: `if args.prompt: cmd.extend(["--prompt", args.prompt])`. Document the default-prompt behavior in the `--prompt` arg help text.

### Issue 4 -- Severity: suggestion (NOT BLOCKING)

- File: `.gitignore:83-122`
- Description: The `.opencode` ignore block has asymmetric coverage between **singular** and **plural** dir names. The plural forms (`.opencode/commands/`, `.opencode/agents/`, `.opencode/plugins/`, `.opencode/rules/`, `.opencode/skills/`) get the full "ignore everything under + re-include specific file types" pattern (lines 93-112). The **singular** forms (`.opencode/command/`, `.opencode/agent/`, `.opencode/plugin/`) only get a top-level re-include of the **directory** itself (lines 85, 87, 89) with no content-filter pattern. Verified empirically: `git check-ignore` on a hypothetical `.opencode/command/test.md`, `.opencode/agent/test.md`, or `.opencode/plugin/test.ts` returns "not ignored" — they would be tracked. The existing skeleton happens to use the singular paths (`.opencode/command/night-issue-prs.md`, `.opencode/agent/night-worker.md`, `.opencode/plugin/night.ts`), and the `**/auth.json`, `**.lock`, `**.local.*` belt-and-suspenders lines (120-122) catch the most common risk files via the `**` glob, so this is a **soft leak** risk, not a hard bug. Note also: the comment on line 80 says "track shareable config (commands/agents/plugin/rules)" — the comment uses **plural** form while the actual skeleton's content lives under **singular** dirs. Either the comment or the skeleton's path choices is misleading.
- Suggestion: Two options — **(a)** mirror the plural content-filter pattern for the singular dirs:
  ```
  .opencode/command/**
  .opencode/agent/**
  .opencode/plugin/**
  !.opencode/command/**/
  !.opencode/agent/**/
  !.opencode/plugin/**/
  !.opencode/command/**/*.md
  !.opencode/agent/**/*.md
  !.opencode/plugin/**/*.ts
  !.opencode/plugin/**/*.js
  !.opencode/command/**/README.md
  !.opencode/agent/**/README.md
  !.opencode/plugin/**/README.md
  ```
  or **(b)** rename the skeleton's `.opencode/command/`, `.opencode/agent/`, `.opencode/plugin/` to the canonical plural paths (`.opencode/commands/`, `.opencode/agents/`, `.opencode/plugins/`) which match the official opencode docs (https://opencode.ai/docs/agents/, https://opencode.ai/docs/commands/, https://opencode.ai/docs/plugins/ — all use plural) and drop the singular aliases from `.gitignore`. Option (a) is the safer fix (no source relocation). Also, tighten the comment on line 80 to note that both forms are accepted defensively, or just say "track shareable config under `.opencode/{commands,command,agents,agent,plugins,plugin,rules,skills}`".
- Pattern-id: `gitignore.last-match-wins.singular-vs-plural`

### Issue 5 -- Severity: nit (NOT BLOCKING)

- File: `scripts/opencode-night-issue-prs.py:175`
- Description: `--include-pr-followup` accepts the literal string `"true"` or `"false"` (via `choices=["true", "false"]`) and is never converted to a Python `bool`. The default prompt text on lines 178-183 says `Parse --include-pr-followup from the surrounding shell`, so the agent will see a string `"true"`/`"false"` in `$ARGUMENTS`. Consistent with the rhai README's boolean-style args, but slightly fragile (a user who types `--include-pr-followup yes` will get an argparse error). Not a bug — could be `type=str` with no `choices`, or accept `yes/no/1/0`. Optional polish.

### Issue 6 -- Severity: nit (NOT BLOCKING)

- File: `.opencode/rules/worktree-hygiene.md:5`
- Description: "for the `night-issue-prs` workflow (and any future opencode-hosted overnight worker)" — fine, but the section "Cross-tool coordination" table (lines 49-55) omits the case where multiple opencode agents might share the same `<home>/.opencode/worktrees/` root with non-overlapping subdirs; not a real issue today (only `night-issue-prs` exists), just noting it.

### Issue 7 -- Severity: nit (NOT BLOCKING)

- File: `modules/ai-shared-develop/src/main/resources/skills/night-gates/SKILL.md:69-80`
- Description: C2 description omits the upstream-downstream `(...)`-style grep details from `.grok/workflows/README.md:56` (the README mentions `extends <Type>` and `new <Type>() {` only; this SKILL matches). Acceptable faithfulness. However, the SKILL's "Hard rules" section (lines 23-42) is **not** derived directly from the rhai README's main hard-bans list (`.grok/workflows/README.md:60-69`) but rather from the broader `.grok/workflows/README.md` body — the SKILL hard rules are a curated subset focused on agent-visible gates, not a full mirror. Acceptable for a distilled opencode skill. Optional: add a note that the canonical exhaustive list lives in `.grok/workflows/README.md`.

## Change-class completeness (deferred items vs Session A scope)

| Change-class item | Status | Follow-up |
|---|---|---|
| `scripts/test_opencode_night_issue_prs.py` | **MISSING** (Issue 2) | Required before commit per AGENTS.md "ALWAYS update or create unit tests" + spec 994 FR-009. |
| Test for `night-gates/SKILL.md` (if any) | N/A | SKILL.md files in this repo do not have colocated tests; not expected per the existing convention (`codeql-pr`, `erlang-review`, `percussioncms-dev`, `java-unit-testing`, `maven-integrity-validator`, `javadoc` all have SKILL.md only, no test). Convention followed. |
| `.opencode/` companion files referenced by the launcher / agent | Not yet present. The skeleton (commit `aba72a7993`) and the prior Erlang skeleton's deferred follow-ups table already flagged this gap. The Session A batch closes `night-gates` (this skill) and adds the worktree-hygiene rule. Still missing (out of Session A scope but still owed): the 9 sub-agents (`preflight`, `triage`, `reconcile`, `pr-follow-up`, `peer-pr-review`, `work`, `pr-cluster`, `security-audit`, `cycle-verify`, `human-qa`) and a signals.json schema. These were the skeleton's deferred follow-ups and are explicitly out of Session A scope. | Will be created in a separate batch / PR; Session A's `.opencode/rules/worktree-hygiene.md` and `.opencode/command/night-issue-prs.md` → `.opencode/agent/night-worker.md` references remain correct (the agent file references the 9 sub-agents by name; the plugin reads `NIGHT_*` env vars that the launcher sets). No new gap introduced by this Session A batch. |
| `product-docs/` update for the new operational script | Per AGENTS.md → **Product documentation (HARD GATE)**, the script is **operator-facing** (cron / Task Scheduler). It is also a **dev/QA operational tool** (not user-facing product behavior), so the gate exemption "Internal engineering fixes with no operator-, admin-, integrator-, or end-user-visible surface" arguably applies. However, the script is described in `scripts/README.md` (the engineering-facing tree), not in `product-docs/8.2/` (the customer/operator-facing tree). Decision is the author's; if the script is operator-only (cron-installed nightly worker), the engineering README entry is sufficient. If intended to be exposed to customer operators, a `product-docs/8.2/admin/night-issue-prs.md` page is owed. |
| `.opencode/README.md` (mentioned in `.gitignore:112` re-include) | File does not exist in the tree yet, but the `.gitignore` re-include allows it. Not a Session A blocker; once the opencode layout stabilizes, add an index README. |
| OpenAPI / REST / install / config changes | N/A — this batch is agent-tooling only. |
| Maven build verification (per AGENTS.md Pre-PR Maven verification) | N/A — no Maven module sources touched. |

## Durable-artifact / cross-reference

- This report is the canonical review for the Session A batch.
- Prior report for the skeleton: `docs/ai-generated/code-reviews/opencode-night-issue-prs-skeleton-erlang.md` (recommendation: approve; this Session A batch closes the skeleton's `night-gates` deferred follow-up).

## Pattern memory

Promote two new generalized principles into `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`:

- **`paths.worktree-git-is-file-not-dir`** — A git worktree's inner `.git` is a **file** (`gitdir: <main>/.git/worktrees/<name>`), not a directory. Code that checks `worktree/.git.is_dir()` to detect "is this a worktree?" returns `False` for any real worktree and will always proceed to re-create it, which then fails because the path already contains a git worktree. Correct check: `(worktree / ".git").exists()` or `worktree.exists()`. Even better: cross-check with `git worktree list --porcelain` to distinguish "path absent", "path exists but not a worktree" (stale dir), and "path is a registered worktree". Reference: `scripts/opencode-night-issue-prs.py:103,213,218`. Counter-example: `scripts/nightly_i18n_refresh.py:139,176` and `scripts/erlang-harvest-review-patterns.py:233` do it right.
- **`gitignore.last-match-wins.singular-vs-plural`** — When gitignoring shareable directories in a layered-tool layout (e.g. opencode uses `.opencode/commands/` per docs but a skeleton in the same repo uses `.opencode/command/`), a defensive `.gitignore` will re-include both forms. But the re-include pattern must mirror the full pattern set: top-level dir re-include alone is insufficient — files dropped into a singular-form dir will not be matched by the **plural** content-filter rules and will leak through. Either (a) duplicate the full plural pattern set for the singular dir, or (b) pick one canonical form and rename the skeleton to match. Reference: `.gitignore:83-122`.

## Voice

- The launcher works on the first run; it crashes on the second. That's a bad place to be for an unattended cron job. Fix Issue 1 before committing.
- Tests are missing for a non-trivial Python script that runs `git fetch`, `git branch -f`, and `git worktree add`. Add the test (Issue 2) — this would have caught Issue 1 in CI.
- `.gitignore` is functionally fine for the skeleton's actual layout (singular paths, already tracked) but leaks any new file into those singular dirs. Defensive hardening (Issue 4) is suggested but not blocking.
- The `night-gates` skill is a faithful, well-structured mirror of the rhai README gates — closes the prior skeleton's most important deferred follow-up. Good work.