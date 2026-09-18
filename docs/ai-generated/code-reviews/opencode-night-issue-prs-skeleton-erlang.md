# Erlang review — opencode night-issue-prs skeleton

## Summary

Three untracked prompt/plugin files (`/night-issue-prs` command, `night-worker` host agent, `night` plugin) that mirror `.grok/workflows/night-issue-prs.rhai` v2.0.6 for the opencode runtime. Schemas, bash policy merge, hard bans, operator-label shape, cross-platform path handling, copyright header, and deferral to the canonical rhai README all check out. No bugs found. A few low-impact suggestions and one cross-platform Windows-bash edge case.

## Scope

- Base: `main`
- Head: uncommitted (working tree only)
- Files: 3 added under `.opencode/`
  - `.opencode/command/night-issue-prs.md` (168 lines)
  - `.opencode/agent/night-worker.md` (175 lines)
  - `.opencode/plugin/night.ts` (150 lines)
- Prior report: none
- Memory patterns hit: `patterns.md` cross-platform checklist, `cfg.environment` schema footgun, agent-rule hard gate, deny-pattern overmatch

## Recommendation

**approve**

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Gate confirmations

- **Human-approval gate for rule files (MET):** Author's stated context — human owner reviewed the three drafts and explicitly approved committing as a separate commit. Per root AGENTS.md → **Human review of agent rules (HARD GATE)**, this gate is met. No further action required.
- **Pre-PR Maven verification (N/A):** Only `AGENTS.md` / `.opencode/` / non-Maven files touched. Per AGENTS.md → **Pre-PR Maven verification** exemption for docs/AGENTS/non-Maven files, no `clean install` required.
- **Cross-platform path/file I/O gate (MET, with one suggestion):** `night.ts` uses `node:os.homedir()` + `node:path.join` for the worktree default (portable); no hardcoded `/` or `\` literals in path construction; bash snippets in command file are POSIX-portable (test+brace-group, `mkdir -p "$(dirname …)"`). One minor Windows-bash edge case on the *relative* report path (see Issue 1).
- **Copyright header gate (MET):** `night.ts` has the Apache 2.0 / `Copyright (c) 2026 Intersoft Data Labs, Inc.` block per AGENTS.md → **Copyright / Apache license headers (HARD GATE)**. MD files do not have a header, matching the existing convention under `.kilo/workflows/*.md` and `.grok/workflows/*.rhai`.
- **`cfg.environment` removal (CONFIRMED):** The plugin's `config` hook only mutates `cfg.permission.bash`. It does NOT touch `cfg.environment` (which `Config.additionalProperties: false` rejects anyway). Confirmed by reading `.opencode/plugin/night.ts:135-143`.
- **`mergeBashPolicy` shape handling (CONFIRMED):** Handles all three valid shapes per opencode schema `PermissionRuleConfig = PermissionActionConfig | PermissionObjectConfig`:
  - `object` (non-null, non-array) → merge with `existing` overriding on key collisions (`.opencode/plugin/night.ts:114-122`)
  - string `"allow"|"ask"|"deny"` → apply to `"*"` key while keeping night-specific allows/denies (`.opencode/plugin/night.ts:123-127`)
  - undefined / other → use night policy verbatim (`.opencode/plugin/night.ts:128`)
- **Operator + model labels (MATCHES SHAPE):** New `operator:opencode` follows the same `operator:<tool>` / `operator:<workflow>` / `model:<id>` shape as the rhai README label table (`.grok/workflows/README.md:517-528`). The new `operator:opencode` is an addition (rhai lists `operator:grok|kilo|minimax|nate`), not a replacement, so existing status-label consumers still match by `operator:night-issue-prs` and `model:*`.
- **Arg-table drift vs rhai v2.0.6 (NONE):** Cross-checked all 36 args in `.opencode/command/night-issue-prs.md:16-54` against `.grok/workflows/README.md:238-277`. Every arg, type, and default matches. (`agent_budget` is rhai-engine-specific and intentionally omitted; opencode has no equivalent.)
- **Phase order (MATCHES rhai v2.0.6):** Identity → Stale → Preflight → Reconcile → PR-follow-up PRE → Triage → Peer PR review → Work → PR-follow-up POST → PR cluster → Security → Cycle verify → Human QA → Report. Same order in both command file (`.opencode/command/night-issue-prs.md:102-117`) and worker file (`.opencode/agent/night-worker.md:31-46`). The decomposition of Stale-In-Progress cleanup into its own phase #2 (vs embedded inside Preflight in the rhai README) is a structural choice, not a drift — the canonical gates (`.grok/workflows/README.md:163-176`) say the behavior is "Part A of Preflight"; running it as a separate inline phase before Preflight is functionally equivalent. No bug.
- **Hard bans (PRESENT):** Both command file (`.opencode/command/night-issue-prs.md:148-155`) and worker file (`.opencode/agent/night-worker.md:138-145`) list the AGENTS.md hard gates including `--skipTests`, `-Dmaven.test.skip`, `git push --force`, `--force-with-lease` to `main` or `night-issue-prs-main`, direct push to `main`, bare-resolving review threads, Human QA assignment without independent APPROVE, and same-night merge of Work PRs. The wider C1–C5 / B1–B3 / Q1–Q8 / cluster / cycle-verify / peer-review bans are explicitly deferred to `.grok/workflows/README.md` as source of truth (`.opencode/command/night-issue-prs.md:60-62` and `.opencode/agent/night-worker.md:15-16`). Correct mirror pattern.
- **Plugin deny patterns (PRESENT):** `*--skipTests*`, `*-Dmaven.test.skip*`, `*-Dmaven.test.skip.exec*`, `*push*--force*main*`, `*push*--force*night-issue-prs-main*` (`.opencode/plugin/night.ts:101-105`). All four classes called out by the user are covered.

## Issues

### Issue 1 -- Severity: suggestion
- File: `.opencode/plugin/night.ts:29-31`
- Description: `defaultReportPath()` uses `path.join("scratch", "night-report.md")` for a **relative** path that is then consumed by a bash command (`mkdir -p "$(dirname "$NIGHT_REPORT_PATH")"` in `.opencode/command/night-issue-prs.md:81`). On Windows, `path.join` produces `scratch\night-report.md`. In a POSIX bash context, `dirname "scratch\night-report.md"` returns `scratch\night-report` (backslash is literal in POSIX), so `mkdir -p scratch\night-report` creates a directory whose name contains a literal `\n` — and the report write at the end of the run goes to a directory the agent never created (or creates with the wrong name). This is a Windows-bash edge case, not a Unix/macOS one.
- Suggestion: Use a literal POSIX-style string for the report default — `function defaultReportPath(): string { return "scratch/night-report.md" }` — or use `path.posix.join`. The *worktree* default correctly uses native `path.join` because `git worktree add` is platform-aware.
- Pattern-id: paths.os-native-join-consumed-by-bash

### Issue 2 -- Severity: suggestion
- File: `.opencode/plugin/night.ts:41-47`
- Description: `applyNightDefaults` provides defaults for `NIGHT_WORKTREE_PATH`, `NIGHT_BASE_BRANCH`, `NIGHT_REPORT_PATH`, `NIGHT_OPERATOR`, `NIGHT_CODING_TOOL`, but **not** `NIGHT_MODEL_ID` or `NIGHT_CODING_TOOL_VERSION`. Both are interpolated into the agent footer templates (`.opencode/agent/night-worker.md:100`, `:172-173`) and the operator+model label table (`.opencode/command/night-issue-prs.md:142`). If the opencode runtime does not set them, the resulting `model:` label will be empty and the footer will have a literal `${NIGHT_CODING_TOOL_VERSION}` substring in the rendered PR body.
- Suggestion: Either (a) document the expectation that the runtime must set these (cross-reference opencode's identity hook), or (b) provide conservative fallbacks (e.g. `env.NIGHT_MODEL_ID ?? "opencode"`, `env.NIGHT_CODING_TOOL_VERSION ?? "0.0.0"`). Option (a) is preferred since hardcoding defeats the rhai lesson "Never hardcode `grok-4.5`."
- Pattern-id: runtime.env-fallback-required

### Issue 3 -- Severity: nit
- File: `.opencode/plugin/night.ts:104-105`
- Description: The deny patterns `*push*--force*main*` and `*push*--force*night-issue-prs-main*` are substring globs without word boundaries. A push like `git push --force-with-lease origin my-main-feature-branch` will be denied (false positive — the destination branch has `main` as a substring but is not `main`). This is defensive and errs on the safe side; the operator can approve the push on retry. Worth tightening if false positives become friction.
- Suggestion: When opencode policy matching supports it, anchor with a literal space (`*push*--force* main*`) so the literal branch name `main` is required, not a substring.

## Change-class completeness (intentional follow-ups for the author)

The skeleton intentionally defers the following to follow-up PRs. None are bugs in *this* diff, but they are owed before the first real `/night-issue-prs` run via opencode.

| Skeleton piece | Status | Follow-up |
|---|---|---|
| 9 referenced sub-agents (`preflight`, `triage`, `reconcile`, `pr-follow-up`, `peer-pr-review`, `work`, `pr-cluster`, `security-audit`, `cycle-verify`, `human-qa`) | Not present — only named in `.opencode/agent/night-worker.md:48-66` | Create `.opencode/agent/<name>.md` for each, or explicitly route them to the existing rhai agents (cross-engine delegation) |
| `night-gates` skill | Referenced at `.opencode/command/night-issue-prs.md:63-66` but `modules/ai-shared-develop/src/main/resources/skills/night-gates/` does not exist (only `add-locale-support`, `codeql-pr`, `erlang-review`, `javadoc`, `java-unit-testing`, `maven-integrity-validator`, `patch`, `percussioncms-dev` are present) | Either create the skill (extract C1–C5/B1–B3/Q1–Q8 + hard bans from `.grok/workflows/README.md` into a portable skill) or remove the reference and let the host re-derive from the rhai README as the fallback already says |
| Identity-phase detection of `NIGHT_MODEL_ID` / `NIGHT_CODING_TOOL_VERSION` | Plugin does not default these; agent prompt relies on them being set | Wire Identity phase (or opencode runtime) to set both, per Issue 2 |
| Resumption / signals.json consumers | `.opencode/agent/night-worker.md:68-72` requires `preflight` / `triage` / `reconcile` to write `${NIGHT_WORKTREE_PATH}/scratch/<phase>.json`; no consumer schema defined | Add a typed schema for `signals.json` once those sub-agents exist |
| Worktree hygiene on `night-issue-prs-main` branch lifecycle | Drafts don't address when the dedicated worktree is removed after a merged/closed PR | Mirror the `.kilo/rules/worktree-hygiene.md` discipline (and `.grok/` analogue) into a `.opencode/` rule once the workflow goes live |
| Product-docs companion for operator labels | The `operator:opencode` label is a new operator-type the rhai README does not list | Decide whether to extend `.grok/workflows/README.md` to register `operator:opencode` in the operator-cell table (lines 545-555) so daily-status consumers recognize the new label, OR document the fork in `.opencode/README.md` (no file exists yet) |

## Pattern memory

No new generalized patterns promoted this review — existing `patterns.md` already covers `paths.os-native-join-consumed-by-bash` and `runtime.env-fallback-required`. Issue 1 and Issue 2 are good one-line additions if the same shape recurs in future opencode plugin work.

## Voice

- The skeleton is sound. Phase order, args table, hard bans, and operator-label shape all mirror rhai v2.0.6 faithfully. Recommend committing.
- Two small follow-ups before the first live `/night-issue-prs` run: (1) tighten `defaultReportPath` to POSIX form, (2) make sure Identity sets `NIGHT_MODEL_ID` / `NIGHT_CODING_TOOL_VERSION` (the plugin deliberately doesn't hardcode them).
- Six sub-agent files and the `night-gates` skill are intentionally out of scope for this commit but must land before the workflow is exercised against real issues.