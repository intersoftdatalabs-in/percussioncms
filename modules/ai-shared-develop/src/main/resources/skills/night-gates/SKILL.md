---
name: night-gates
description: >-
  Build / merge / handoff hard gates for the opencode night-issue-prs workflow
  (mirrors .grok/workflows/night-issue-prs.rhai v2.2.4). Use when a sub-agent
  opens a PR, clusters PRs, or proposes human-QA handoff, and when the host
  agent enforces C1–C5 / B1–B3 / Q1–Q8. Skip if you are not running
  night-issue-prs.
---

# night-gates — opencode workflow build / merge / handoff gates

## Read first

- `.grok/workflows/README.md` — canonical phase list, skip matrix, hard bans.
  When this skill and the rhai README disagree, the rhai README wins.
- `.opencode/command/night-issue-prs.md` — args + phase table.
- `.opencode/agent/night-worker.md` — host dispatch contract.
- Root `AGENTS.md` — **Pre-PR Maven verification**, **Cross-Platform File
  I/O & Paths**, **Change-class completeness**, **Pre-commit code review
  (Erlang)**, **Git worktree hygiene**.

## Hard rules

These apply to **every** Work / Cluster / Human-QA step in the workflow. The
host agent enforces them; sub-agents produce the evidence. This list is the
distilled opencode subset of the exhaustive hard-bans catalogue in
`.grok/workflows/README.md` → "Hard bans" (C1–C5 / B1–B3 / hot-path / closing /
worktree); when in doubt, defer to that source of truth.

1. **Maven gates are non-negotiable.** No `-DskipTests`. No
   `-Dmaven.test.skip`. No "compile only." If the gate cannot run, the PR
   is **not** opened.
2. **Cross-platform paths only.** Java side uses NIO `Path` / `Path.of`;
   shell side uses `pathlib.Path` in Python and `Path` / `os.homedir()` in
   TS. Never hardcode `/` or `\` in filesystem joins.
3. **Evidence in the PR body.** Every gate references a section of the
   structured PR body the Work sub-agent must populate. A PR without
   `build_evidence` is **failed** by the host, not human-QA-eligible.
4. **Erlang is always a sub-agent** and **MAY APPROVE + squash-merge**
   when LGTM + checks green. Work must spawn Erlang before `gh pr create`
   (`allow_merge=false`). Host Erlang leftover (before Work) and this-run
   (after Work) merge clean PRs. Same-login APPROVE fail → COMMENT +
   `--admin` merge.
5. **Work never self-APPROVEs or self-merges.** Findings: **erlang-fix**
   on the same PR, then Erlang re-review (one retry). Out-of-scope
   leftovers become residual GitHub issues; they do not block merge of
   in-scope work. Goal: merged bug-free PRs with residuals logged.

## Work build gates (C1–C5)

Apply these to **every** Work sub-agent's PR. The host rejects a PR whose
body omits the matching `build_evidence` section.

### C1 — changed modules (HARD)

For **each** changed Maven module, run a standalone `mvnw clean install`
from the **module directory** (relative path to repo-root `mvnw` /
`mvnw.cmd`):

```bash
cd rest && ../mvnw clean install        # 1-level submodule
cd projects/sitemanage && ../../mvnw clean install   # 2-level
```

Do **not** default to root `./mvnw -pl … -am clean install` (rebuilds
upstream modules unnecessarily). Use root reactor only when standalone is
insufficient and justify in the PR body.

**No** `-DskipTests`. **No** `-Dmaven.test.skip`. **No** `-Dmaven.test.skip.exec`.

PR body must record `modules_built` (list of modules) and `build_evidence`
(exact commands + `BUILD SUCCESS` + `Tests run: N, Failures: 0`).

### C2 — API shape / reverse-deps (HARD when applicable)

Triggers: a type becomes `final` / `sealed`, or any public / protected /
package-visible cross-module signature changes.

1. Monorepo grep: `extends <Type>` and `new <Type>() {` — find anonymous
   subclasses in tests.
2. Standalone clean install on **known reverse-deps**:
   - `system/objectstore` → `modules/perc-toolkit`
   - `rest` → `projects/sitemanage`

PR body must record `downstream_checked: none | <module-list>`.

### C3 — evidence in PR body (HARD)

Every Work PR body includes the structured sections:

```markdown
## Build evidence

- modules_built: [rest, projects/sitemanage]
- build_evidence:
  - `cd rest && ../mvnw clean install` → BUILD SUCCESS, Tests run: 142, Failures: 0
  - `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS, Tests run: 88, Failures: 0
- downstream_checked: none   # or [modules/perc-toolkit]
```

If any section is missing, host rewrites status to `failed /
blocked=missing_build_evidence` and skips human-QA handoff.

### C5 — UI live proof (HARD for UI)

Triggers: WebUI / SPA / product chrome / user-visible browser flows change.

1. `python docker/scripts/perc-devctl.py qa-up` then `qa-health`
   (H2 Docker cell; freeport `TEST_CMS_URL`).
2. Deploy **every** C1 `modules_built` jar **and reverse-deps** into
   `webapps/Rhythmyx/WEB-INF/lib/` — not `deploy-jar --target cms` /
   `jetty/base/lib`. Restart Jetty **inside** the cell (do **not**
   `docker restart`). **`qa-health` again**.
3. Run **surface-filtered Playwright** for the feature
   (`npm run test:surface -- --path …`) and golden smoke when shell /
   login / explorer is touched.
4. **Zero** JS console errors and **zero** related `server.log` ERROR /
   FATAL during the run.
5. Record commands + pass counts + console-clean + server.log-clean in
   `build_evidence` / PR body.

**No** human-QA handoff and **no** UI `pr_opened` without C5.

**Never** HTTP-poll `/Rhythmyx/login` after `server.log` shows
`Failed startup of context` / `NoClassDefFoundError` — use `qa-health` and
stop.

`docker restart perc-matrix-cms-h2` after a jar copy wipes copies. Do not.

## Cluster build gates (B1–B3, HARD)

Cycle verify runs later and can be skipped. The cluster PR is the merge
candidate, so it must compile and test **before** it exists.

| Gate | Requirement |
|------|-------------|
| **B1 — touched modules** | Every Maven module touched by any absorbed PR or by conflict-resolution edits: standalone `mvnw clean install`. |
| **B2 — API shape** | Same C2 reverse-dep greps + clean install if the union changes signatures. |
| **B3 — evidence** | Cluster PR body lists `modules_built` and `build_evidence` (exact commands + `BUILD SUCCESS` + `Tests run: N`). Docs-only unions use `modules_built=none`. |

**Host fail-close.** If cluster PR is opened without `BUILD SUCCESS` in
`build_evidence`, host rewrites status to `failed /
blocked=missing_build_evidence`.

## Human-QA handoff gates (Q1–Q8, HARD)

Apply **after** Cycle verify only. The host never assigns Q1–Q8 work; the
`human-qa` sub-agent does, and only when all gates pass.

| Gate | Required |
|------|----------|
| **Q1** | PR exists, not draft, not superseded, mergeable. |
| **Q2** | Independent review **APPROVE** (human or peer). Self-review does not count. |
| **Q3** | Required checks **green** (one snapshot). |
| **Q4** | C1 Maven clean-install evidence on every changed module. |
| **Q5** | UI: C5 with **commands** in the PR body (not a self-claim). |
| **Q6** | Slice complete enough for one QA session (not a fragment while siblings still break). |
| **Q7** | No overlapping open QA ticket for the same surface. |
| **Q8** | Cycle verify did **not** fail this PR. If `failed` / `skipped_budget` / agent failed: assign **nobody**. If `failures_filed`: do not assign PRs in `build_failures` / `playwright_failures`. If `skipped_disabled`: Q8 N/A (still require Q1–Q7). |

If any gate fails: **no QA issue, no assignee.** Leave the PR open;
comment `qa_deferred_quality`.

When all gates pass, the `human-qa` sub-agent creates a GitHub issue:

- **Title:** `QA (#N): <what to verify>`
- **Assignee:** `${qa_assignee}` (default `vijaya-boddipudi`)
- **Label:** `${qa_label}` (default `qa task`)
- **Body:** Parent, PR URL(s), **numbered test plan**, pass / fail
  criteria, out of scope, agent evidence.

## Cross-platform reminders (HARD)

- **Paths**: `pathlib.Path` (Python) / `Path.of` (Java) / `path.join`
  (TS). No `/` or `\` in joins. `File.separator` only when a `char` is
  required.
- **Line endings**: don't assert exact `\r\n` vs `\n` in tests.
- **Temp dirs**: never `%TEMP%` / `$TMPDIR`. Use `./tmp` or
  `Files.createTempDirectory`.
- **Shell scripts**: ops scripts in `scripts/` are cross-platform Python
  (spec 994 FR-001). Repo-root `mvnw` / `mvnw.cmd` are the cross-platform
  Maven entry points.

## Related skills (load on demand)

- `codeql-pr` — CodeQL disposition ladder, suppression placement, default
  setup off.
- `erlang-review` — pre-commit / pre-PR code review persona.
- `percussioncms-dev` — repo conventions, module map, build entry points.
- `java-unit-testing` — JUnit 5 + Mockito patterns, given-when-then.
- `maven-integrity-validator` — Maven enforcer / version pinning.
- `javadoc` — Javadoc authoring conventions.

## Why this skill exists

The rhai workflow records these gates in `.grok/workflows/README.md` so
they survive model / tool migrations. opencode has no equivalent runtime
that injects them. The host agent prompts reference this skill by name;
sub-agent prompts load it explicitly when they touch a gate.

When in doubt: re-derive from `.grok/workflows/README.md`. The rhai README
is the source of truth; this file is the opencode mirror.
