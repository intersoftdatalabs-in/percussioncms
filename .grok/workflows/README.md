# Grok workflows (Percussion CMS)

Project workflows live here and are invocable by name (e.g. `/night-issue-prs` or the workflow tool with `name: "night-issue-prs"`).

**Git:** only `*.rhai` and `README.md` under this folder are versioned. The rest of `.grok/` (memory, sessions, rules, worktrees, auth, caches, etc.) stays gitignored.

## `night-issue-prs`

Unattended overnight worker:

1. **Discover** open GitHub issues  
2. **Triage** (implement / split / skip)  
3. **Work** sequential implement or split — **file residual follow-up issues** for leftover work  
4. **PR follow-up** (optional, default on) — **human review threads first** (never starve/fake-resolve), then merge conflicts, CI, then bot review threads on *our* open PRs only  
5. **Report** → `scratch/night-report.md`

Opens **PRs only** (never merges). Oversized issues become child issues, not mega-PRs.

### Why residual issues + PR review follow-up

| Gap without it | What we do |
|----------------|------------|
| Partial PRs leave “rest of the work” only in chat | **Always** log residual work as GitHub issues (or plan them in dry_run) linked to parent + PR |
| Split plans disappear if only a comment | Child issues for each slice; residual URLs in structured result |
| Same-area overnight PRs (i18n/TMX/gadgets) go **CONFLICTING** and block each other | **PR follow-up** rebases onto base **oldest-first**, resolves conflicts, then CI/review |
| Human review comments lose to conflict/CI `max_prs` budget | **HUMAN threads are mandatory** — always inventoried; not capped out by `max_prs`; report `human_threads_still_open` |
| Agent PRs sit blocked on review/CI | **PR follow-up** phase fixes + **inline reply + `resolveReviewThread`** per root `AGENTS.md` |
| Fake-green: resolve without fix | Never resolve without mitigation reply citing commit; open residual issue if judgment needed; **human threads stay OPEN** if deferred |

### When to use

- Overnight / long session: burn down **unassigned** issues and leave PRs for morning review  
- Clean up review debt on PRs this account already owns  
- Prefer small tech-debt; skip live-CMS / Playwright when `agent_safe_only` is true (default)

### Args

| Arg | Type | Default | Meaning |
|-----|------|---------|---------|
| `max_issues` | int | `3` | Max items fully processed (capped 1–8) |
| `issue_numbers` | int[] | — | Only these issues (still triaged) |
| `labels` | string | — | Optional label filter for discovery |
| `repo` | string | `intersoftdatalabs-in/percussioncms` | GitHub repo |
| `base_branch` | string | `main` | PR base |
| `dry_run` | bool | `false` | **Cheap plan only:** discover + triage + report (no work agents, no PR follow-up, no git/gh writes) |
| `prefer_easy` | bool | `true` | Prefer tech-debt / javadoc / small bugs |
| `agent_safe_only` | bool | `true` | Skip work needing live CMS / E2E / secrets |
| `unassigned_only` | bool | `true` | Only issues with **no assignees** |
| `include_pr_followup` | bool | `true` | Run PR babysit phase after issue work (conflicts + CI + review) |
| `max_prs` | int | `3` | Max open PRs to follow up (capped 1–8). Raise (e.g. 5–8) when same-area conflict chains build up |

### What is `agent_budget`?

Not a money budget. It is the **maximum number of child agents** this workflow run may spawn.

| Concept | Meaning |
|---------|---------|
| **1 slot** | One `agent()` call, or one item in a `parallel()` panel |
| **Default** | 128 slots for the whole run |
| **Range** | 1–1,024 if you set `agent_budget` when launching |
| **Does not count** | Schema-correction retries on the same agent |

Rough agent use:

| Phase | Agents |
|-------|--------|
| Discover | 1 |
| Triage | 1 |
| Work | 1 per queued issue |
| PR follow-up | 0–1 |
| Report | 1 |

**3 issues + PR follow-up ≈ 7 agents.** Default 128 is plenty.

### How to run

```text
/night-issue-prs
```

Examples:

```text
# Dry run — triage plan only (~2–3 agents, a few minutes). No work explorers.
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

### PR follow-up: human reviews + conflicts + order

1. **Inventory first:** GraphQL `reviewThreads` on **every** owned open PR; classify HUMAN vs bot  
2. **HUMAN threads (sacred):** always in the work set (not excluded by `max_prs`); process oldest first  
   - Fix + mitigation reply + `resolveReviewThread`, **or**  
   - Inline deferral + residual issue and **leave thread OPEN** (never bare-resolve)  
3. **Then fill `max_prs`:** CONFLICTING/DIRTY → failing CI → unresolved bot threads → optional BEHIND  
4. **Process oldest first** among non-human work so same-area stacks unstick bottom-up  
5. **Rebase** conflicted PRs onto `origin/<base_branch>`; resolve markers carefully  
6. **Push** with `git push --force-with-lease` only after history rewrite (never bare `--force`)  
7. Bot review reply+`resolveReviewThread` only after humans on that PR are handled or deferred  
8. Report **must** list `human_threads_still_open` after a fresh GraphQL re-query of all owned PRs  

**What went wrong on #1955 (example):** a human freeport review sat open while follow-up reported “all MERGEABLE / no threads” — conflict-first + `max_prs` starved the PR, and inventory was incomplete. Human priority above closes that gap.

### Safety model

- Sequential implementers (shared workspace)  
- Fresh branch per issue from `origin/<base_branch>`  
- No merge, no direct push to `main`  
- **No bare `--force`**; rebase after conflict resolution may use **`--force-with-lease` only**  
- Spotless → clean install → tests before PR  
- `unassigned_only` avoids stepping on humans  
- PR follow-up only on **our** open PRs; hard gate reply+resolve (never bare resolve)  
- **Human review threads never starved or fake-resolved**; deferred humans stay open with residual issue  
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
| `model:<id>` | **Required with every agent operator** — e.g. `model:grok-4.5`, `model:claude-…`, whatever the tool reports |

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
`operator:kilo` + `model:<session model>` on every agent-authored PR — keep that
file in lockstep with this table.

### Note on in-flight runs

A run already started uses the **immutable script from launch**. Edits to this file apply to the **next** run only.

### Known Grok Build limitation (scheduled overnight)

**As of 2026-08-04**, multi-hour `night-issue-prs` runs are **reliable only when launched from the durable main chat session**.

| Path | What happens |
|------|----------------|
| Manual / chat `workflow` tool | Run appears in **this** session’s `/workflows`; can complete (~45–90 min). |
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

Canned path only — not live `gh` proof.
