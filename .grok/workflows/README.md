# Grok workflows (Percussion CMS)

Project workflows live here and are invocable by name (e.g. `/night-issue-prs` or the workflow tool with `name: "night-issue-prs"`).

**Git:** only `*.rhai` and `README.md` under this folder are versioned. The rest of `.grok/` (memory, sessions, rules, worktrees, auth, caches, etc.) stays gitignored.

## `night-issue-prs`

Unattended overnight worker:

1. **Discover** open GitHub issues  
2. **Triage** (implement / split / skip)  
3. **PR follow-up PRE** (optional, default on) - **merge-blocker drain** before new issue PRs (conflicts + open review threads + failing CI, co-equal; oldest first)  
4. **Work** sequential implement or split - **file residual follow-up issues** for leftover work  
5. **PR follow-up POST** - catch PRs opened this run + remaining merge blockers  
6. **Report** -> `scratch/night-report.md`

Opens **PRs only** (never merges). Oversized issues become child issues, not mega-PRs.

### Multi-phase status → parent GitHub issue (not repo markdown)

Concurrent overnight runs must **not** keep epic/slice progress in committed files under
`docs/ai-generated/tasks/` (or similar). Those PRs fight each other on every tick.

| Store | Purpose |
|-------|---------|
| **Parent issue body** — section `## Agent progress (night-issue-prs)` | Living slice table (see columns below) |
| **Parent issue comments** | Append-only run history (split created, PR opened, blocked) |
| **Child / residual issues** | One PR-sized unit; body links `Parent: #N` |
| **`scratch/night-report.md`** | **This run only** (workflow UI). Never commit as the epic tracker |

Parent body table columns (must match the work-agent prompt):

```markdown
| Slice | Issue | Status | PR | Notes | Updated |
```

Statuses: `open` | `in_progress` | `pr_opened` | `blocked` | `done` | `skipped`.

Workers **upsert** the parent body section (`gh issue view` → edit section → `gh issue edit --body-file`) and post a short comment after each meaningful step.

### Why residual issues + PR review follow-up

| Gap without it | What we do |
|----------------|------------|
| Partial PRs leave “rest of the work” only in chat | **Always** log residual work as GitHub issues (or plan them in dry_run) linked to parent + PR |
| Split plans disappear if only a comment | Child issues for each slice; residual URLs in structured result |
| Multi-agent runs thrash merge conflicts on `docs/ai-generated/tasks/**` status markdown | Multi-phase status lives on the **parent GitHub issue** (`## Agent progress (night-issue-prs)` + comments), not committed trackers |
| Same-area overnight PRs (i18n/TMX/gadgets) go **CONFLICTING** and block each other | **PR follow-up** rebases onto base **oldest-first**, resolves conflicts |
| Review threads (human or AI) sit forever while newer conflicts win `max_prs` | Threads are **merge blockers equal to conflicts**; selection is **oldest first** |
| Empty issue queue early-complete skipped babysit | Empty triage still runs PR follow-up |
| Agent "touches" a PR (rebase) but leaves 5-7 Kilo threads open | **Completeness:** finish **all** threads on a selected PR before the next PR |
| Agent PRs sit blocked on review/CI | PRE + POST follow-up; **inline reply + `resolveReviewThread`** per root `AGENTS.md` |
| Fake-green: resolve without fix | Never bare-resolve (human or bot); mitigation + resolve, or residual issue and **leave OPEN** |
| Human cannot merge with open conversations | `merge_blockers_still_open` reported every run |

### When to use

- Overnight / long session: burn down **unassigned** issues and leave PRs for morning review  
- Clean up review debt on PRs this account already owns  
- Prefer small tech-debt; skip live-CMS / Playwright when `agent_safe_only` is true (default)

### Args

| Arg | Type | Default | Meaning |
|-----|------|---------|---------|
| `max_issues` | int | `3` | Max items fully processed (capped 1-12) |
| `issue_numbers` | int[] | - | Only these issues (still triaged) |
| `labels` | string | - | Optional label filter for discovery |
| `repo` | string | `intersoftdatalabs-in/percussioncms` | GitHub repo |
| `base_branch` | string | `main` | PR base |
| `dry_run` | bool | `false` | **Cheap plan only:** discover + triage + report (no work agents, no PR follow-up, no git/gh writes) |
| `prefer_easy` | bool | `true` | Prefer tech-debt / javadoc / small bugs |
| `agent_safe_only` | bool | `true` | Skip work needing live CMS / E2E / secrets |
| `unassigned_only` | bool | `true` | Only issues with **no assignees** |
| `include_pr_followup` | bool | `true` | Run PR merge-blocker drain **before and after** issue Work |
| `max_prs` | int | `6` | Max open PRs per follow-up pass (capped 1-12). Raise on heavy conflict/thread debt nights |

### What is `agent_budget`?

Not a money budget. It is the **maximum number of child agents** this workflow run may spawn.

| Concept | Meaning |
|---------|---------|
| **1 slot** | One `agent()` call, or one item in a `parallel()` panel |
| **Default** | 128 slots for the whole run |
| **Range** | 1-1,024 if you set `agent_budget` when launching |
| **Does not count** | Schema-correction retries on the same agent |

Rough agent use:

| Phase | Agents |
|-------|--------|
| Discover | 1 |
| Triage | 1 |
| Work | 1 per queued issue |
| PR follow-up pre | 0-1 |
| PR follow-up post | 0-1 |
| Report | 1 |

**3 issues + dual PR follow-up ~ 8 agents.** Default 128 is plenty.

### How to run

```text
/night-issue-prs
```

Examples:

```text
# Dry run - triage plan only (~2-3 agents, a few minutes). No work explorers.
name=night-issue-prs args={"dry_run": true, "max_issues": 5, "labels": "tech-debt"}

# Live overnight: unassigned tech-debt + PR follow-up
name=night-issue-prs args={"labels": "tech-debt", "max_issues": 4, "include_pr_followup": true}

# Issues only (skip PR babysit)
name=night-issue-prs args={"max_issues": 3, "include_pr_followup": false}

# PR follow-up heavy night (conflict chains / i18n backlog)
name=night-issue-prs args={"max_issues": 1, "max_prs": 6, "include_pr_followup": true}

# Conflicts + review only (no new issue work)
name=night-issue-prs args={"max_issues": 1, "max_prs": 8, "include_pr_followup": true, "labels": "does-not-match-anything-xyz"}
```

For a pure PR-babysit night, prefer `max_issues: 1` with a label filter that matches nothing (or a known empty set) so Work is nearly empty, and raise `max_prs`. Watch in `/workflows`. Result path: `scratch/night-report.md`.

### PR follow-up: merge blockers (anti-starvation)

**Human cannot merge** a PR that has conflicts **or** any unresolved review conversation (bot or human). Those are **merge blockers**.

1. **Inventory** GraphQL `reviewThreads` + `mergeable` / `mergeStateStatus` + CI on every owned open PR  
2. **Select up to `max_prs` merge blockers**, ordered by:  
   - **Oldest `createdAt` first** (anti-starvation - old Kilo debt must not lose forever to new CONFLICTING PRs)  
   - Tie-break: unresolved **human** threads before bot-only  
   - Tie-break: more open threads before fewer  
3. Conflicts, open threads, and failing CI are **co-equal for inclusion** (interleave by age)  
4. **Completeness:** on each selected PR, finish **all** unresolved threads (or residual + leave OPEN) before the next PR  
5. Rebase carefully; push **`--force-with-lease` only** after history rewrite  
6. Never bare-resolve: fix + mitigation + resolve, or residual + **leave OPEN**  
7. Report `merge_blockers_still_open` + `human_threads_still_open` after a full re-query  

**Phase order:** PRE follow-up -> issue Work -> POST follow-up. Empty issue queue still runs PRE/POST.

**#1955 lesson:** MERGEABLE + human freeport thread under-reported.  
**#1974 lesson:** MERGEABLE + open Kilo threads starved by `max_prs` + conflict-first tiering.

### Safety model

- Sequential implementers (shared workspace)  
- Fresh branch per issue from `origin/<base_branch>`  
- No merge, no direct push to `main`  
- **No bare `--force`**; rebase after conflict resolution may use **`--force-with-lease` only**  
- Clean install (changed modules) -> tests before PR  
- `unassigned_only` avoids stepping on humans  
- PR follow-up only on **our** open PRs; hard gate reply+resolve (never bare resolve)  
- Human and AI review comments **equal**; human is a **tie-break** when tech need is equal  
- Residual / child issues left **unassigned** for the backlog  

### Operator + model labels (daily status)

Do **not** use a `daily-status` label. Status tables are built from **last 24h PR activity** + these labels (and Dependabot author).

| Label | Who |
|-------|-----|
| `operator:grok` | Grok Build / Grok CLI agent work |
| `operator:night-issue-prs` | This overnight workflow specifically (also apply `operator:grok`) |
| `operator:kilo` | Kilo Code agent |
| `operator:minimax` | Minimax agent |
| `operator:nate` | Human Nate only (optional; default is “no operator: label = Nate”) |
| `model:<id>` | **Required with every agent operator** - e.g. `model:grok-4.5`, `model:claude-…`, whatever the tool reports |

**Daily status Operator column** (derived):

| Labels present | Operator cell |
|----------------|---------------|
| `operator:night-issue-prs` (+ `model:X`) | `Grok: night-issue-prs` (`X`) |
| `operator:grok` (+ `model:X`) | `Grok` (`X`) |
| `operator:kilo` (+ `model:X`) | `Kilo` (`X`) |
| `operator:minimax` (+ `model:X`) | `Minimax` (`X`) |
| Dependabot author | `Dependabot` |
| None of the above (human Nate account) | `Nate` |

Exclude Vijay’s PRs by author filter (`-author:vijaya-boddipudi`), not labels.

Implementers **must** `gh pr create --label operator:… --label model:…` (and create labels if missing). Residual/child issues get the same labels.

**Kilo (Nate + Vijay):** project rule `.kilo/rules/operator-pr-labels.md` requires
`operator:kilo` + `model:<session model>` on every agent-authored PR - keep that
file in lockstep with this table.

### Note on in-flight runs

A run already started uses the **immutable script from launch**. Edits to this file apply to the **next** run only.

### Known Grok Build limitation (scheduled overnight)

**As of 2026-08-04**, multi-hour `night-issue-prs` runs are **reliable only when launched from the durable main chat session**.

| Path | What happens |
|------|----------------|
| Manual / chat `workflow` tool | Run appears in **this** session’s `/workflows`; can complete (~45-90 min). |
| `scheduler_create` every N hours | Kickoff is a **side session**. Run often **does not show** in parent `/workflows`; may **cancel in ~3s** or **orphan** as `status=active` after parent exits. |

Product feedback logged:

- `docs/ai-generated/feedback/grok-build-scheduled-workflows-lifecycle-2026-08-04.md`
- Session `feedback.jsonl` (this Grok session)

**Workaround:** launch overnight batches from the open TUI session (or re-request a run when you’re at the keyboard). Do not trust 2h scheduler alone for multi-hour workflows until Grok supports durable detached runs.

### Folder trust (project workflows)

Project workflows under `.grok/workflows/` require **folder trust** for this repo.
If you see `workflow path is not trusted … project workflows require folder trust`:

```text
/hooks-trust
```

Or relaunch Grok with `--trust`. That writes `~/.grok/trusted_folders.toml` (same gate as project hooks/MCP/LSP).

**User-global copy** (always trusted, no project grant needed):

```text
~/.grok/workflows/night-issue-prs.rhai
```

Keep both in sync after edits, or edit only the user copy for overnight schedulers.

### Smoke check

```text
workflow validate_only name=night-issue-prs args={"dry_run": true}
```

Canned path only - not live `gh` proof.
