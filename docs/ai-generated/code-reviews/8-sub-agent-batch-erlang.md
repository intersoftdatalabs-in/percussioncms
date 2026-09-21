# Erlang review — 8 night-issue-prs sub-agent prompts (batch)

## Summary

Eight NEW `.opencode/agent/<name>.md` files implement the deferred specialists
for the opencode night-issue-prs workflow. The batch is largely faithful to
`.grok/workflows/README.md` v2.0.6: every file uses `mode: subagent`, writes
`scratch/<phase>.json` with the `executor: sub-agent:<name>` field the host
needs, and encodes the right hard gates for its phase.

**Two blocker bugs** invalidate downstream contracts and must be fixed before
commit:

1. **`human-qa.md` Q8** reads top-level `build_failures` / `playwright_failures`
   from `scratch/cycle-verify.json`, but **`cycle-verify.md` writes neither** —
   `build_evidence[]` (per-module objects with `result`) and
   `playwright.failed_specs` (nested). Q8 will silently pass for every PR even
   when the integration-tip build failed. This is a hard gate leak — the
   human-QA handoff will assign broken CI/PRs.
2. **`human-qa.md` Q2** GraphQL jq filter references
   `.parent.pullRequest.author.login` in a pipeline that has no `.parent`.
   The filter will return `length > 0 == false` for every PR, so Q2 will
   silently fail even when an independent APPROVE exists — but in the
   *opposite* direction of bug #1, so they will fight each other on real runs
   (Q8 always green, Q2 always red).

Smaller issues: missing skip-rule sections in 4 of 8 agents, a broken jq
example in `preflight.md` Phase 2A, a `model_id` arg that `reconcile.md`
uses without declaring, and a too-strict Q4 grep anchor in `human-qa.md`.

Recommendation: **REQUEST_CHANGES** on these rule files. Per root
`AGENTS.md` "Human review of agent rules (HARD GATE)", rule files
must surface for explicit human approval before commit anyway — Erlang's
role is to flag defects, not authorize the commit.

## Scope

- Base: `8092eb42b2` (current `main`, worktree clean of tracked changes)
- Head: 8 NEW uncommitted files under `.opencode/agent/`
- Files reviewed (count: 8):
  - `.opencode/agent/preflight.md` (279 lines)
  - `.opencode/agent/reconcile.md` (198 lines)
  - `.opencode/agent/pr-follow-up.md` (227 lines)
  - `.opencode/agent/peer-pr-review.md` (199 lines)
  - `.opencode/agent/pr-cluster.md` (286 lines)
  - `.opencode/agent/security-audit.md` (265 lines)
  - `.opencode/agent/cycle-verify.md` (291 lines)
  - `.opencode/agent/human-qa.md` (318 lines)
- Reference docs loaded:
  - `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md` (Erlang profile)
  - `modules/ai-shared-develop/src/main/resources/skills/codeql-pr/SKILL.md`
  - `.grok/workflows/README.md` (rhai workflow contract)
  - `.opencode/command/night-issue-prs.md` (host command / arg source-of-truth)
  - `.opencode/agent/night-worker.md`, `triage.md`, `work.md` (sibling style baseline)
- Memory patterns hit:
  - **rules.human-review-first** (root AGENTS.md "Human review of agent rules")
  - **gates.maven-clean-install-no-skip** (root AGENTS.md "Pre-PR Maven verification")
  - **gates.hard-ban-force-with-lease-to-base** (root AGENTS.md "Hard bans")
  - **contracts.cross-agent-scratch-json** (host's specialist-availability fallback)
- Prior topic reports: none (no prior review of these agents)

## Recommendation

**request-changes**

## Gate

- Blocking bugs: 2 (Q8 contract mismatch, Q2 jq filter)
- Suggestions: 4 (skip-rule sections missing in 4 agents; preflight 2A jq; Q4 grep; reconcile Args)
- Nits: 3 (Q7 bash-only param expansion; `playwright.json` JSON schema inconsistency; cycles' `pr_number` shape)
- May commit/push: **no** — fixes required; rule files also require human review under root AGENTS.md "Human review of agent rules (HARD GATE)"

## Issues

### Issue 1 — Severity: bug (BLOCK)

- File: `.opencode/agent/human-qa.md:177-183` × `.opencode/agent/cycle-verify.md:216-270`
- Description: Q8 contract mismatch. `human-qa.md` Q8 reads
  `'.build_failures // [], .playwright_failures // []'` from
  `scratch/cycle-verify.json`, but `cycle-verify.md` writes:
  - `build_evidence: [{ module, command, result, tests_run, failures, tail_log }]`
    (no top-level `build_failures` array)
  - `playwright: { ..., failed_specs: [...] }` (nested under `playwright`, not top-level)
  The jq will always return `[]` for both, so Q8 will silently pass for every
  PR — even when the integration-tip Maven / Playwright step reported a
  failure. This is the **exact** "fake-green" failure mode the rhai README
  warns against and that root AGENTS.md "Change-class completeness (HARD
  GATE)" exists to catch.
- Suggestion: Two coordinated fixes:
  1. In `cycle-verify.md`, emit top-level `build_failures: [{ pr_number, module, summary, tail_log }]` and `playwright_failures: [{ pr_number, spec, summary }]` arrays **populated with the affected PR numbers** (resolved from the integration-tip's PR set — easiest path: each `build_evidence` entry that maps to a PR whose diff touched that module).
  2. In `human-qa.md`, keep Q8 as written (the path then works) OR change to
     `'[.build_evidence[]? | select(.result == "BUILD FAILURE") | ...] as $b | [.playwright.failed_specs[]?] ...'`.
  Note that cycle-verify currently has no per-PR linkage for `build_evidence`
  entries — only the integration tip's module list. Adding `pr_number`
  requires the agent to know which PR(s) touched each module. The simplest
  source-of-truth: read the union of `headRefName` from each open PR whose
  files overlap `build_evidence[].module` (or all open PRs for the
  integration tip), then list those in `pr_number`.
- Pattern-id: contracts.cross-agent-scratch-json
- Status: open

### Issue 2 — Severity: bug (BLOCK)

- File: `.opencode/agent/human-qa.md:89-107`
- Description: Q2 GraphQL jq filter references
  `.parent.pullRequest.author.login` in a pipeline that has no `.parent`.
  The jq context for `map(...)` is `.data.repository.pullRequest.reviews.nodes`
  (an array of reviews); each element has `.author.login` and `.state`, but
  there is no `.parent` field. `.parent` is undefined → jq silently emits
  `null` for the LHS of `!=`, and `null != null` is `false`, so the
  `select` matches nothing → `length > 0` is always `false`.
  Net effect: Q2 will report "no independent APPROVE" even when one exists,
  which combined with Issue #1 (Q8 always green) means every PR will be
  classified `qa_deferred_quality` on real runs.
- Suggestion: pass the PR author as an arg and compare against it. Use a
  single GraphQL query that pulls author + reviews, then:
  ```bash
  gh api graphql -F query='
    query($owner:String!, $repo:String!, $pr:Int!) {
      repository(owner:$owner,name:$repo) {
        pullRequest(number:$pr) {
          author { login }
          reviews(last:50) { nodes { author { login } state } }
        }
      }
    }
  ' -f owner=intersoftdatalabs-in -f repo=percussioncms -F pr=<N> \
    | jq --arg author "$(gh pr view <N> --repo <repo> --json author --jq .author.login)" \
      '[.data.repository.pullRequest.reviews.nodes[]
        | select(.state == "APPROVED" and .author.login != $author)] | length > 0'
  ```
  Or capture author in the same query and pass via `--arg`:
  ```bash
  ... | jq '[.data.repository.pullRequest | .author.login as $a
    | .reviews.nodes[] | select(.state == "APPROVED" and .author.login != $a)] | length > 0'
  ```
- Pattern-id: contracts.cross-agent-scratch-json
- Status: open

### Issue 3 — Severity: suggestion

- File: `.opencode/agent/reconcile.md:22-26` (Args table)
- Description: `reconcile.md` is the only agent whose Args table does not
  declare `model_id: from env`, yet step 4's `gh issue create` example uses
  `--label "model:${NIGHT_MODEL_ID}"` (line 100). All seven other agents in
  this batch declare it. Inconsistency makes the host's `args_echo` field
  asymmetric across the JSON outputs.
- Suggestion: add `model_id` row to the Args table (default `from env`) to
  match the other agents.
- Status: open

### Issue 4 — Severity: suggestion

- File: `.opencode/agent/preflight.md:41-47` (Phase 2A jq snippet)
- Description: Example jq
  `'.updatedAt as $u | .labels[].name | select(. == "In Progress" or . == "in progress") | {number: input, updatedAt: $u}'`
  has three problems:
  1. `select` returns strings (`.labels[].name`); the subsequent object
     construction operates on a string and produces nothing useful.
  2. `input` is the jq function for feeding stdin; this pipeline has no
     stdin input (the file's data comes from `--jq` with no `--slurp` or
     `--null-input`), so `input` returns nothing.
  3. The object construction will yield `{number: null, updatedAt: $u}` for
     each match — `number` is hard-null because the pipeline lost the
     issue number.
  An agent following this verbatim will produce garbage.
- Suggestion: rewrite the example to a working form, e.g.
  `gh issue view <N> --repo <repo> --json updatedAt,labels \
   | jq 'select(.labels | map(.name) | any(. == "In Progress" or . == "in progress")) | {number: input, updatedAt}'`
  with `--null-input` and stdin injection of `<N>`, OR (cleaner) use
  `gh issue list --label "In Progress" --json number,updatedAt` as the
  discovery step and a separate `gh issue edit` per issue.
- Status: open

### Issue 5 — Severity: suggestion

- File: `.opencode/agent/human-qa.md:127-128` (Q4 grep)
- Description: `grep -E '^BUILD SUCCESS|Tests run:|modules_built:'` requires
  `BUILD SUCCESS` at line start. The `work.md` PR body template at line 134
  puts it after a backtick-quoted command and `→` arrow:
  `` `cd rest && ../mvnw clean install` → BUILD SUCCESS, Tests run: 142, Failures: 0 ``
  The `^BUILD SUCCESS` anchor will never match this line. Q4 will fail even
  on PRs that have valid build evidence.
- Suggestion: drop the `^` anchor (use `\bBUILD SUCCESS\b`), or match the
  full template with `'BUILD SUCCESS.*Tests run:'`. The bare `BUILD SUCCESS`
  literal is enough on its own — `Tests run:` and `modules_built:` lines
  are nice-to-have.
- Status: open

### Issue 6 — Severity: suggestion

- File: `.opencode/agent/reconcile.md` (no Skip-rule section), `.opencode/agent/pr-follow-up.md` (no Skip-rule section), `.opencode/agent/pr-cluster.md` (no Skip-rule section), `.opencode/agent/peer-pr-review.md` (no Skip-rule section), `.opencode/agent/human-qa.md` (no Skip-rule section)
- Description: Six of the eight new agents lack the explicit `## Skip rule`
  section that the host command file (`.opencode/command/night-issue-prs.md`
  phase table) treats as authoritative. The other two (`cycle-verify.md`,
  `security-audit.md`) do have it. Each agent's Args table also fails to
  document the corresponding `include_*` flag from
  `.opencode/command/night-issue-prs.md` (`include_reconcile`,
  `include_pr_followup`, `include_pr_cluster`, `include_peer_pr_review`,
  `include_human_qa`).
- Suggestion: add a `## Skip rule` section to each of the six agents that
  documents (a) the `include_*` flag it accepts and (b) the early-exit
  echo when the flag is false / no eligible work. Example shape from
  `cycle-verify.md:34-41`:
  ```bash
  if [ "$include_pr_followup" != "true" ]; then
    echo '{"status": "skipped", "reason": "include_pr_followup=false"}'
    exit 0
  fi
  ```
  This makes the agent safe to dispatch unconditionally — the host doesn't
  have to pre-filter.
- Status: open

### Issue 7 — Severity: nit

- File: `.opencode/agent/human-qa.md:166-169`
- Description: Q7 search uses bash-specific parameter expansion `${PR%%#*}`
  and `${PARENT_PR}`. POSIX-sh and PowerShell won't interpret these. In
  practice the workflow runs in bash so it's fine, but the search string
  is itself a GitHub search query (passed to `gh issue list --search`),
  and a hardcoded `#<N>` literal is simpler.
- Suggestion: hardcode the PR number rather than parameter-expanding —
  `in:title "QA (#${N})"`.
- Status: open

### Issue 8 — Severity: nit

- File: `.opencode/agent/cycle-verify.md:241-252`
- Description: The `build_evidence[]` schema entries lack a `pr_number` /
  `affected_prs` linkage. The rhai README's Q8 contract is per-PR ("do not
  assign PRs in `build_failures`"), but cycle-verify records per-module
  failures on the integration tip without telling the host which PRs to
  flag. Even after fixing Issue #1, the agent has to derive the
  PR-to-module mapping; the schema should declare it so the host and the
  next human-qa iteration don't have to.
- Suggestion: add `pr_numbers: [...]` (or `affected_prs`) to each
  `build_evidence` and Playwright failure entry, populated by intersecting
  the integration-tip's touched modules with each open PR's `files` list.
  The cycle-verify phase already has the integration-tip PR set; this is a
  one-line addition per entry.
- Status: open

### Issue 9 — Severity: nit

- File: `.opencode/agent/preflight.md:225-257` (`signals.json`)
- Description: The companion `signals.json` document is documented but
  the agent's `## Steps` (Phase 2G) does not say it writes `signals.json`.
  The downstream `skip_matrix` is the host's read-source for skip
  decisions; the file should be written in an explicit step (Phase 2H)
  and the Phase 2G step should end before Phase 2H starts.
- Suggestion: split Phase 2G ("write `scratch/preflight.json`") from
  Phase 2H ("write `scratch/signals.json`") as two numbered steps, each
  with its own payload.
- Status: open

## Confirmations (gates met)

- **Phase fidelity vs rhai README**: Spot-checks passed.
  - Preflight Phase 2A stale-In-Progress cleanup (cap 40): ✓ lines 33-58 of preflight.md.
  - Cycle verify integration-tip order (cluster > newest PR > origin/base): ✓ lines 45-59 of cycle-verify.md.
  - Human QA Q1-Q8 (esp. Q2 independence, Q8 cycle-verify fail): ✓ text matches, ✗ code in Q2 and Q8 buggy (Issues 1, 2).
- **JSON output schema**: Every agent writes `scratch/<phase>.json` with
  `executor: sub-agent:<name>` (preflight, reconcile, pr-follow-up,
  peer-pr-review, pr-cluster, security-audit, cycle-verify, human-qa). ✓
  Matches host's `Specialist-availability` record.
- **Cross-agent contracts**:
  - preflight → triage: ✓ `maintainer_logins`, `issues_compact`, `peer_pr_eligible` exposed.
  - triage → work: ✓ `slotted_for_work`, `child_proposals` (existing).
  - work → pr-follow-up POST: ✓ same agent, phase arg distinguishes.
  - work → cycle-verify: `modules_built` and `build_evidence` exist on `scratch/work-<N>.json`; cycle-verify reads PR body for `## Build evidence` (step 2 capture).
  - cycle-verify → human-qa Q8: ✗ **broken** (Issue 1).
  - peer-pr-review → human-qa Q2: human-qa does its own GraphQL Q2 query rather than reading `peer-pr-review.json`. Defensible — the source of truth is GitHub, not the local snapshot.
- **Hard bans**: each agent encodes the relevant rhai hard bans:
  - All Work-class agents: `--skipTests`, `-Dmaven.test.skip`, `--force` to base branches — encoded in security-audit, pr-follow-up, pr-cluster, cycle-verify, work.md. ✓
  - PR-touching: `--force-with-lease` only, no bare-resolve — pr-follow-up ✓, security-audit ✓, cycle-verify hard rules ✓.
  - Security audit: no dismiss-only PRs, sink-line only, no long `justification:` on Java line — encoded. ✓
  - Cycle verify: no fix-in-cycle-verify; only file next-cycle leads — encoded. ✓
  - Human QA: no assign before Q1-Q8, no `qa task` label without independent APPROVE — encoded in Hard rules. ✓
- **Read-only vs mutating** matches phase intent:
  - preflight: clears stale In Progress + collects inventory; no issue closure/PR mutation. ✓
  - reconcile: closes 100%-done issues + files residuals. ✓
  - pr-follow-up: rebase + push + reply/resolve threads. ✓
  - peer-pr-review: review + optional squash-merge. ✓
  - pr-cluster: cluster branch + cluster PR + close absorbed. ✓
  - security-audit: tracking issue + mitigation PRs. ✓
  - cycle-verify: next-cycle lead issues. ✓
  - human-qa: QA issues + assign. ✓
- **Operator + model labels**: every agent that opens a PR / files an
  issue applies `operator:opencode` + `operator:night-issue-prs` +
  `model:<id>`. Verified across reconcile (residuals), pr-cluster,
  security-audit, cycle-verify, human-qa. Preflight / pr-follow-up /
  peer-pr-review don't open — N/A.
- **Skip matrix**: preflight (always), reconcile (`include_reconcile: false`,
  not yet encoded — Issue 6), pr-follow-up PRE (no merge blockers — implicit
  via empty queue), peer-pr-review (no eligible PRs — encoded), pr-cluster
  (below `cluster_min_prs` or no thrash group — encoded), security-audit
  (no alerts — encoded in step 1), cycle-verify (no PR/cluster — encoded),
  human-qa (no independent APPROVE — partial via Q2 evaluation; missing
  `include_human_qa: false` short-circuit — Issue 6).
- **No copyrighted material, secrets, or hardcoded model slugs**: clean.
  All `model:*` labels and `model_id` env-var references; no hardcoded
  `grok-4.5` / `grok-4.6` strings anywhere in the new files. ✓
- **Cross-platform paths**: prompts only; `gh` invocations consistently use
  `--repo <repo>`. `mvnw` invocations use `../mvnw` / `../../mvnw` shell
  patterns that are portable on POSIX shells; the `night-gates` skill is
  the documented source for `mvnw.cmd` Windows handling. ✓
- **Human-approval gate (root AGENTS.md)**: rule files surface for explicit
  human approval before commit. Do not commit these 8 files in the same
  change set as any non-rule work — the human reviewer should see the
  diff in isolation.

## Change-class completeness

- No code artifacts changed; only rule files (`.opencode/agent/*.md`).
- Per root AGENTS.md "Human review of agent rules (HARD GATE)", these
  must surface for explicit human approval before commit. Erlang's role
  here is to flag defects, not to authorize the commit.
- No `product-docs/` update required: these are agent-facing
  configuration, not operator-visible product behavior.
- Pre-PR Maven verification (HARD GATE) does not apply: no Maven module
  sources, tests, resources, or `pom.xml` changed.

## Handoff

1. Issues 1 and 2 are blockers. Fix before commit.
2. Issues 3-6 are suggestions worth folding into the same change.
3. Issues 7-9 are nits; not required.
4. After fixes, re-run Erlang on the diff before requesting human review.
5. Human review of these 8 rule files is still required even after Erlang
   approves (root AGENTS.md "Human review of agent rules").
6. Durable artifact written: `docs/ai-generated/code-reviews/8-sub-agent-batch-erlang.md`.