# AGENTS.local.md from primary checkout (worktrees)

Applies to **every** agent session in this repository (Kilo Code implementers,
reviewers, and other tools that load `.kilo/rules/`).

## Problem

`AGENTS.local.md` is **gitignored** personal/operator override. Git worktrees
created for agent work (`.kilo/worktrees/*`, `.worktrees/*`,
`~/.grok/worktrees/*`, etc.) **do not copy** that file from the primary
checkout. Sessions that only read the worktree root silently miss GH identity,
commit footers, machine shell notes, and other hard preferences.

## Rule (HARD GATE — session start)

At the **start of the session** (before implement / PR / `gh` work that depends
on personal overrides):

1. Resolve the **current worktree root**:
   ```bash
   git rev-parse --show-toplevel
   ```
2. Look for `AGENTS.local.md` in this order (stop at first readable file):
   1. `<worktree-root>/AGENTS.local.md`
   2. **Primary checkout** (main worktree) `/AGENTS.local.md` — see resolution
      below
   3. Optional: walk parents only if they are still the same monorepo root
3. **Read** that file with the file tool and treat it as higher priority than
   root `AGENTS.md` for personal/operator overrides (same hierarchy as
   **Rule Discovery Protocol** in root `AGENTS.md`).
4. If no `AGENTS.local.md` exists anywhere in that chain, continue with
   `AGENTS.md` only — do **not** invent contents.

### Resolving the primary (parent) checkout

Prefer:

```bash
git worktree list --porcelain
```

- The first `worktree <path>` entry is the **primary** checkout for this
  repository (the main working tree that owns `.git`).
- Use `<primary>/AGENTS.local.md` when the current toplevel’s copy is missing.

Fallback when porcelain is unavailable:

```bash
# common dir is usually <primary>/.git (or a .git file’s real common path)
git rev-parse --path-format=absolute --git-common-dir
# primary root ≈ parent of that .git directory
```

Examples (illustrative):

| Session cwd | Missing local file | Read instead |
|-------------|--------------------|--------------|
| `…/percussioncms/.kilo/worktrees/feat-foo` | worktree has no `AGENTS.local.md` | `…/percussioncms/AGENTS.local.md` |
| `~/.grok/worktrees/…/night-issue-prs` | worktree has no `AGENTS.local.md` | primary clone’s `AGENTS.local.md` |

## Hard bans

* **Do not** assume “no `AGENTS.local.md` in this worktree” means none exists.
* **Do not** copy `AGENTS.local.md` into the worktree and **commit** it (it is
  gitignored and personal).
* **Do not** skip this read because the worktree has a root `AGENTS.md` only.

## Why

Personal overrides (GitHub account for `gh`, commit email, co-author footers,
host shell/WSL notes) live only on the developer machine’s primary tree.
Disposable agent worktrees must still honor them.

Canonical policy: root `AGENTS.md` → **Rule Discovery Protocol** and
**AGENTS.local.md across git worktrees**.
