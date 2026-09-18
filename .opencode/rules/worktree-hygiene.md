# OpenCode git worktree hygiene

Applies to **opencode** sessions that create or use git worktrees under
`.opencode/worktrees/` for the `night-issue-prs` workflow (and any future
opencode-hosted overnight worker).

## Rule (HARD GATE)

When your task used a **git worktree** under `.opencode/worktrees/`:

1. **After the PR is opened or updated** for that worktree’s branch, note the
   worktree path in the session summary.
2. **After the PR is merged or closed** (or the human says the worktree is no
   longer needed), **remove the worktree before ending the session**:

   ```bash
   # from the primary (main) checkout, not from inside the disposable worktree
   git worktree remove --force <worktree-path>
   git worktree prune
   # optional: drop the local branch if it is fully merged
   git branch -D <branch-name>
   ```

3. **Do not** leave full-tree worktrees behind “for later.” Each monorepo
   worktree can be multi‑GB and is a common disk-fill failure mode.

4. **Do not** remove:
   - the primary / main worktree
   - a worktree that still has an **open** PR (unless the human ordered cleanup)
   - the worktree you are currently running in (switch to main first)

5. The opencode `night` plugin auto-creates the dedicated worktree at
   `<home>/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs`
   (override via `NIGHT_WORKTREE_PATH` env var) and resets its sync branch
   `night-issue-prs-main` to `origin/<base_branch>` between runs. Treat that
   path the same as any other disposable worktree — clean it up when no PR is
   open against it.

## Session start (related HARD GATE)

Disposable worktrees do **not** receive gitignored `AGENTS.local.md` from the
primary checkout. At session start, read personal overrides from the **primary**
tree when the worktree copy is missing — see
`.kilo/rules/agents-local-from-parent.md` and root `AGENTS.md` →
**AGENTS.local.md across git worktrees**.

## Cross-tool coordination

Opencode, Kilo, and Grok each manage their own worktrees under disjoint roots:

| Tool | Worktree root | Hygiene rule |
|------|---------------|--------------|
| OpenCode | `<home>/.opencode/worktrees/...` | **this file** |
| Kilo | `<home>/.kilo/worktrees/...` | `.kilo/rules/worktree-hygiene.md` |
| Grok | `<home>/.grok/worktrees/...` | `.grok/workflows/README.md` → "Git worktree hygiene" |

When one tool’s cleanup script is run (`scripts/prune-stale-worktrees.py`),
it walks **all three roots** and removes worktrees whose branches have merged
or closed PRs. Per-tool rule files still bind per-session.

## Why

This monorepo is large. Nested agent worktrees are full checkouts. Spotless
and other tools pay a cost if those trees are discoverable. Cleanup is part of
finishing the PR, not optional tidying.

Canonical policy: root `AGENTS.md` → **Git worktree hygiene (HARD GATE)**.
