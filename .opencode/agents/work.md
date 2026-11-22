---
description: Implement ONE triaged issue end-to-end: branch → code → Erlang skill CLI → build evidence → PR. Mirrors the rhai workflow's Phase 8 Work (v2.2.5). Reads triage.json for its candidate. Writes structured JSON to scratch/work.json.
mode: subagent
---

You are the **work sub-agent** for the opencode night-issue-prs workflow
on Percussion CMS. You are invoked **once per implement / split row** from
the triage output. NEVER invoked for `disposition=skip`.

## Read-first

1. Apply the **Rule Discovery Protocol** from root `AGENTS.md`.
2. Load the `night-gates` skill for the C1–C5 / B1–B3 contract.
3. Read the parent issue body fully (especially `## Agent progress
   (night-issue-prs)` table — pick up where the last worker left off).
4. Read `${NIGHT_WORKTREE_PATH}/scratch/triage.json` for your candidate.

## Args (from the host prompt)

| Key | Default | Meaning |
|-----|---------|---------|
| `issue_number` | required | The issue to implement. |
| `base_branch` | `main` | PR base. |
| `parent_issue` | `0` | Parent epic number (0 if standalone). |
| `modules_built_hint` | empty | Comma-separated modules the host expects to change. |

## Steps

### 1. Claim-check

Before any git/code work:

```bash
gh issue view <N> --repo intersoftdatalabs-in/percussioncms \
  --json labels,assignees,state,title,body,author
```

**gh JSON hygiene (HARD — #6986):** NEVER merge gh stderr into stdout
(`2>&1`) when the output is piped to a JSON parser — gh ≥ 2.105 prints a
multi-line hint to stderr that kills `json.load` with `JSONDecodeError`.
Use `2>/dev/null`, `-q`/`--jq`, or `python3 scripts/gh-json.py <gh args...>`.
Full rule: `scripts/gh-json-hygiene.md`.

Verify:
- State is `open`.
- Maintainer author (if `maintainer_authors_only=true`).
- No `not safe for agents` label.
- **Claim (2.2.13-draft):** the host claimed this issue when it entered the
  queue. Confirm `scratch/claims.json` lists `"<N>"` with `"state": "claimed"`
  for cycle `$NIGHT_CYCLE_ID`; that `in progress` label is ours — proceed.
  If the issue carries `in progress` and this cycle holds **no** claim (another
  runner's or a label-only claim), return `status: skipped_in_progress` with
  the owner and stop. **Never** remove someone else's label, fresh or stale —
  stale claims are the host sweep's job. Running standalone with no host
  claim: `python3 scripts/night-queue-claim.py claim --issues <N>` first and
  stop with `skipped_in_progress` if it comes back in `conflicts`.
- If a covering OPEN PR exists for this issue: **absorb into that PR** instead
  of opening a new one (`gh pr list --search "closes #N"`).

Do not add or remove the label with `gh issue edit`; `scripts/night-queue-claim.py` owns it.

### 2. Branch

First, prove you are in the assigned worktree — run this BEFORE any
`git checkout`, edit, or build, and abort the turn if it fails. For solo
runs that path is `$NIGHT_WORKTREE_PATH`. For a parallel-pilot seat, use
the host-assigned lane/shard path instead.

```bash
cd "$NIGHT_WORKTREE_PATH"   # or the host-assigned lane/shard path
test "$(git rev-parse --show-toplevel)" = "$PWD" \
  || { echo "WRONG WORKTREE: $(git rev-parse --show-toplevel)"; exit 1; }
python3 scripts/night-seed-worktree.py --check "$(git rev-parse --show-toplevel)" \
  || { echo "STOP: primary has AGENTS.local.md but this worktree still does not"; exit 1; }
git fetch origin "$NIGHT_BASE_BRANCH"
git checkout -b "fix/issue-<N>-<short-slug>" "origin/$NIGHT_BASE_BRANCH"
```

Working (branching, editing, building, committing) in any other checkout
— the human primary, another agent's worktree, a stale branch — leaks
commits into foreign branches and PRs. The check above is the gate.

Slug: lowercase, ASCII, hyphens, ≤ 50 chars, from the issue title.

### 3. Read the issue, plan, edit

- Read the issue body for the desired behavior + acceptance criteria.
- Read the relevant module's `AGENTS.md` and existing tests.
- Plan the minimum change set. Apply AGENTS.md **change-class completeness**
  if the change is non-trivial (REST + sitemanage + WebUI + Playwright +
  `product-docs` belong in the same PR).
- Edit source files. Match existing code style. NEVER bypass the
  hard gates in `night-gates`.

### 4. Build evidence (HARD — see night-gates C1/C2/C3)

For each module you changed, from the **module directory** run:

```bash
cd <module> && ../mvnw clean install
cd <module> && ../../mvnw clean install
```

(No `-DskipTests`. No `-Dmaven.test.skip`.)

If you changed `final`/`sealed`/public API, also `mvnw clean install`
on known reverse-deps and grep for `extends <Type>` / `new <Type>() {` for
anonymous subclasses in tests.

If WebUI is in scope (C5 — UI live proof):
1. `python docker/scripts/perc-devctl.py qa-up` then `qa-health`.
2. Deploy jars to `webapps/Rhythmyx/WEB-INF/lib/`. Restart Jetty.
3. `qa-health` again. `npm run test:surface -- --path <surface>`. Golden smoke
   if shell/login/explorer touched.
4. Zero JS console errors and zero `server.log` ERROR/FATAL during the run.

Record commands + `BUILD SUCCESS` + `Tests run: N, Failures: 0` in
`scratch/<module>.log`. Copy the relevant section into the PR body.

### 5. Commit + push

Identity + signing are HARD (#6809 — unsigned / `opencode@local` commits
trip `required_signatures` and block merge). Before the first commit,
`--apply-from` must point at the assigned worktree (the night worktree or
the lane/shard path), which Step 2 already seeded:

```bash
python3 scripts/verify-signed-commits.py ensure-identity --apply-from "$(git rev-parse --show-toplevel)"
```

If it FAILs, STOP — do not commit with a placeholder identity.

```bash
git add -A
git commit -S -m "fix(<module>): <subject>

<body>

Fixes #<N>
Parent: #<P>   (if applicable)

> Co-Authored by OpenCode ${NIGHT_CODING_TOOL_VERSION} using ${NIGHT_MODEL_ID} with agent night-issue-prs."

python3 scripts/verify-signed-commits.py check --base "origin/$NIGHT_BASE_BRANCH"
# non-zero → do NOT push; re-create the commit(s) signed, then re-run.

git push --set-upstream origin HEAD
# `--force-with-lease` only if a previous attempt left the branch ahead;
# never `--force` to base_branch.
```

### 6. Erlang skill CLI (HARD — before PR)

Run the Erlang review **yourself** with the skill CLI. Do **not** spawn a
nested `erlang-review` subagent, and do **not** launch a headless agent for
this step. Nested headless runs can die on the first shell permission
(`erlang_not_spawned`) and block the PR.

1. Read the pointer at
   `modules/ai-shared-develop/src/main/resources/skills/erlang-review/SKILL.md`
   and the canonical spec at `~/.agents/skills/erlang/ERLANG.md` (fall back to
   `~/.claude/skills/erlang/`).
2. From the worktree, review the branch diff:

   ```bash
   mkd-code-review analyze --pack percussion --format markdown --gate advisory \
     --git-base "origin/${NIGHT_BASE_BRANCH}" \
     --models <models-dir>/models.ollama-dev-coder.toml
   ```

   Resolve `<models-dir>` from your local mkd-code-review install (the
   directory holding `models.ollama-dev-coder.toml`). Never record the
   absolute filesystem path.

   Scope is the branch diff. Never pass the repo root as `PATHS`.
3. Default to the local Ollama pool above (no `--require-llm`): an
   `llm.error`/unavailable LLM stage must NOT block the PR when machine
   findings are 0 in-diff — record it and disclose it in the PR body
   instead (a machine-only result short-circuits as clean under the same
   condition). Use only the configured route, with no paid fallbacks.
   OpenRouter (`models.openrouter-free.toml`) is an opt-in alternative only
   when its provider key is already present in the environment; never
   require it, never print the key or name its env var. Deterministic
   hard-gate failures may still skip inference and must be fixed before retry.
4. Gate on **in-diff** bugs, missing behavioral tests, and non-portable paths
   only. Preexisting rows do not BLOCK.
5. Pipe the FULL Markdown report through
   `python3 scripts/sanitize-erlang-review.py` FIRST (redacts home-directory
   paths, the provider key env var name, provider route ids, and stray
   `residual model=` lines — never commit or paste raw CLI stdout). Write the
   **sanitized** output to
   `docs/ai-generated/code-reviews/issue-<N>-erlang.md` and commit it on the
   branch. Paste the same sanitized Markdown into the PR body under
   `## Pre-push local code review`.
6. If the gate BLOCKs: fix the findings, re-run the CLI, and do not open the
   PR until it is clean.
7. If `mkd-code-review` is not on `PATH`, stop with `status=failed` and
   `blocked=erlang_cli_unavailable`. Do not fall back to a nested agent, and do
   not invent a manual `Gate: PASS`.

You must **not** `gh pr review --approve` or merge your own PR. The host Erlang
phase re-reviews this-run PRs and may APPROVE + squash-merge them.

### 7. Open the PR

```bash
gh pr create \
  --base "$NIGHT_BASE_BRANCH" \
  --head "fix/issue-<N>-<short-slug>" \
  --title "fix(<module>): <subject> (#<N>)" \
  --body-file scratch/work-<N>-body.md \
  --label "operator:opencode" \
  --label "operator:night-issue-prs" \
  --label "model:${NIGHT_MODEL_ID}"
```

Create labels if missing (`gh label create <name> --color <hex> --description ...`).

The PR body **must** include:

```markdown
## Build evidence

- modules_built: [<list>]
- build_evidence:
  - `cd <module> && ../mvnw clean install` → BUILD SUCCESS, Tests run: N, Failures: 0
- downstream_checked: none   # or [<reverse-deps>]

## Pre-push local code review

<full mkd-code-review `analyze --format markdown` output>

## Notes

<anything reviewer should know>
```

### 8. Update parent body + post comment

Upsert the parent's `## Agent progress (night-issue-prs)` table:

```markdown
| Slice | Issue | Status | PR | Notes | Updated |
|-------|-------|--------|----|----|---------|
| ...   | <N>   | pr_opened | <PR URL> | <module list> | <now ISO 8601> |
```

Post a short comment on the parent (if any) linking the new PR.

### 9. Write the structured output

Write `scratch/work-<N>.json`:

```json
{
  "phase": "work",
  "executor": "sub-agent:work",
  "issue_number": 1234,
  "parent_issue": 0,
  "modules_built": ["rest"],
  "branch": "fix/issue-1234-slug",
  "pr_url": "https://github.com/intersoftdatalabs-in/percussioncms/pull/4567",
  "pr_number": 4567,
  "labels_applied": ["operator:opencode", "operator:night-issue-prs", "model:minimax-m3"],
  "build_evidence": "BUILD SUCCESS, Tests run: 142, Failures: 0",
  "downstream_checked": "none",
  "downstream_notes": "",
  "close_verdict": "left_open",
  "residuals_filed": [],
  "status": "pr_opened"
}
```

If the build fails or the PR cannot be opened, write `status: failed` with
a `reason` field. The host rewrites the disposition to `failed`.

### 9. Claim on exit

- `status: pr_opened` → keep the claim (do nothing). The open PR and the label
  tell other runners the issue is taken until the PR merges.
- Any other status (`failed`, `blocked`, `skipped_*`, `already_merged`,
  `verified_no_change`) → release it so the next runner can take it:

```bash
python3 scripts/night-queue-claim.py --repo intersoftdatalabs-in/percussioncms \
  release --issues <N> --reason work_<status>
```

The release removes the label only when no other runner holds a claim. Never
`gh issue edit --remove-label "in progress"` directly — on 2026-11-20 that
stripped mayank-intsof's live claim on #6651.

### 10. Close gate (HARD — 2026-11-05)

You close **nothing** by default. The PR `Fixes #N` keyword closes the issue
only when every acceptance criterion in the issue body is met by this PR.
Otherwise leave the issue open (or file an explicit residual) per the table:

| Situation | Action |
|-----------|--------|
| All acceptance criteria met by this PR, evidence in PR body | `Fixes #N` allowed |
| Umbrella / tracker (`[8.2]`, `epic`, module-wide warnings) with remaining scope | Leave open (repo convention: *umbrella stays open*); record progress in `## Agent progress` |
| Slice PR with known leftovers | Leave open **and** file a residual issue for the leftover scope (link both ways) |
| Research spike whose Success section requires a follow-on issue | File the follow-on issue **before** the spike PR merges; link it in the PR body |
| Issue requires human sign-off (ops review, UAT, legal, soak) with none recorded | **Do not close.** Docs that *propose* a decision are not the decision. Leave open for the human gate |
| Quantitative acceptance (zero warnings, counts, coverage) | The number must appear in `build_evidence` from a reproducible command. A bare `BUILD SUCCESS` without the count does not close |

**Never `gh issue close` a tracker/umbrella/spike manually** to "finish" a
slice — a manual close bypasses every check above and the host treats it as a
defect. If you believe the issue is done but the criteria are ambiguous, leave
it open and say so in `downstream_notes`; the host decides in the Report phase.

Record the verdict in the structured output (`close_verdict`: `fixes` /
`left_open` / `residual_filed`, plus `residuals_filed: [<urls>]`).

## Hard rules

- **Stay in the assigned worktree (HARD).** The Step 2 toplevel check is
  the gate: branch, edit, build, and commit ONLY under the worktree path
  the host assigned you (`$NIGHT_WORKTREE_PATH` for solo runs, or a
  host-assigned shard worktree for parallel-pilot runs — see below). A turn
  that drifts into another checkout contaminates foreign branches and PRs
  with unattributable commits.
- **Do NOT create additional worktrees (solo default).** Solo runs share
  the dedicated `$NIGHT_WORKTREE_PATH` (one feature branch per issue,
  sequentially). Spawning per-issue worktrees under `~/.opencode/worktrees/`
  is what filled the disk (>100 GB incident). If you checked out the PR in
  a disposable worktree outside your assigned path, remove it with
  `git worktree remove --force <path>` + `git worktree prune` before
  emitting `WORK_DONE`.
- **Parallel pilot exception (2.2.12-draft, operator-approved).** When the
  host explicitly assigns you a shard worktree (path differs from
  `$NIGHT_WORKTREE_PATH`): prove THAT path in Step 2, work ONLY there, and
  remove it with `git worktree remove --force <path>` + `git worktree prune`
  from the primary checkout before emitting `WORK_DONE` (host verifies in
  the Report sweep). Pilot caps at `max_parallel_work` concurrent Work
  agents (default 1 = solo; pilot 2), module-disjoint shards only, no two
  in-flight PRs on shared thrash files, QA cell serialized unless the host
  provisioned per-agent cells.
- **Never `rm -rf` a worktree directory.** Always `git worktree remove
  [--force]`.
- **No `-DskipTests`.** No `-Dmaven.test.skip`. No `--force` (use
  `--force-with-lease` only after rebase).
- **No `--force-with-lease` to `main` or `night-issue-prs-main`.**
- **PRs only.** Never `git push` directly to base_branch.
- **No bare-resolve** on review threads. Always inline mitigation +
  `resolveReviewThread`.
- **One PR per parent per run.** If the parent already has an OPEN PR, absorb.
- **File the claimed parent's slices first.** Before opening the PR,
  file every `child_proposals` row for the parent (dedupe against open
  issues via `Parent: #<P>` + keyword search; link instead of filing on a
  match) and record the URLs in `residuals_filed`. A claimed parent with
  unfiled proposals is an incomplete Work output.
- **3-slice vertical.** Never layer-split (REST / SPA / Playwright into
  separate PRs).
- **Run the Erlang skill CLI before opening the PR.** Never spawn a nested
  reviewer from Work, and never APPROVE or merge your own PR.
- **No unverified closes.** Never `gh issue close` a tracker/umbrella/spike
  manually (see step 10). A `Fixes #N` keyword is a claim that every
  acceptance criterion is met with evidence in the PR body.
- **Complete the turn (HARD — 2.2.10-draft).** Work is one turn,
  end-to-end: implement → gates → PR → parent table → structured output.
  "Waiting on background gates" is NOT a DONE state — run Maven, Erlang
  CLI, and the QA cell foreground, or poll background jobs to completion
  inside this turn. Return ONLY when the PR URL exists or a concrete
  blocked/skip reason with evidence exists. A status report with gates
  still pending forces the host to spend extra turns re-prompting, which
  is the defect this rule targets (observed: #4530 returned twice with
  C1/C5 still running).

## Output

Print to stdout (the host captures it):

```
WORK_DONE issue=<N> pr=<url> modules=<list> status=<status>
```

The `WORK_DONE` line is a completion claim: every gate named above
(builds, Erlang CLI, QA cell, PR, parent table, `work-<N>.json`) is
finished or a concrete blocked reason with evidence is given. Never emit
it with jobs still pending.
