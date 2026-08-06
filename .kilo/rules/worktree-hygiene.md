# Git worktree hygiene

Applies to **implementer** sessions that create or use git worktrees (Kilo,
Grok, and other agents).

## Rule (HARD GATE)

When your task used a **git worktree** (for example under `.kilo/worktrees/`,
`.worktrees/`, or `~/.grok/worktrees/`):

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

3. Prefer the repo helper (dry-run first):

   ```bash
   python3 scripts/prune-stale-worktrees.py
   python3 scripts/prune-stale-worktrees.py --apply --force --delete-local-branches
   # Windows:
   scripts\prune-stale-worktrees.bat --apply --force --delete-local-branches
   ```

4. **Do not** leave full-tree worktrees behind “for later.” Each monorepo
   worktree can be multi‑GB and is a common disk-fill failure mode.

5. **Do not** remove:
   - the primary / main worktree
   - a worktree that still has an **open** PR (unless the human ordered cleanup)
   - the worktree you are currently running in (switch to main first)

## Session start (related HARD GATE)

Disposable worktrees do **not** receive gitignored `AGENTS.local.md` from the
primary checkout. At session start, read personal overrides from the **primary**
tree when the worktree copy is missing — see
`.kilo/rules/agents-local-from-parent.md` and root `AGENTS.md` →
**AGENTS.local.md across git worktrees**.

## Why

This monorepo is large. Nested agent worktrees under `.kilo/worktrees/` are
full checkouts. Spotless and other tools also pay a cost if those trees are
discoverable. Cleanup is part of finishing the PR, not optional tidying.

Canonical policy: root `AGENTS.md` → **Git worktree hygiene (HARD GATE)**.
