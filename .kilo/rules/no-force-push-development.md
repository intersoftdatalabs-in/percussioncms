# Never force-push to `main`; always create a feature branch + PR

## Rule

When the user asks to push, create a PR, or share changes for review:

1. **Never `git push --force`, `git push --force-with-lease`, `git push -f`, or any non-fast-forward push to `origin/main`.** This branch is the project's protected default branch (formerly `development`) and the repository's branch-protection rule denies non-fast-forward pushes anyway — attempting them wastes a round trip and pollutes history.
2. **Never commit directly to `main`.** The root `AGENTS.md` does not list `main` among the pre-approved direct-commit branches, and force-pushing to it is treated as an incident.
3. **Always create a feature branch first.** Convention:
   - Branch name: `fix/<issue>-<short-slug>` for fixes, `feat/<issue>-<short-slug>` for features, `chore/<short-slug>` for tooling.
   - Base off the current `origin/main` tip (fetch first).
   - Example:

     ```bash
     git fetch origin
     git checkout -b fix/548-1500-h2-password-and-startjetty-launcher origin/main
     ```
4. **Always open a PR** with `gh pr create --base main --head <branch>`. The PR description must include the test evidence required by `AGENTS.md` (clean install counts per changed module, Erlang review reference).
5. **Never use `--force-with-lease` even on feature branches unless the user explicitly instructs it** — feature-branch history rewriting still disturbs reviewers who have based work on the branch.

## Why

Incident history (2026-07-27, PR #1522; default branch was then named `development`):

- Author force-pushed 5 commits to the protected default branch after a fast-forward-only fetch rejection, intending to push a "feature branch." The result was that the default remote tip was rewritten, leaving the previous HEAD (commit `a16ae9061`) unreachable except via cherry-pick from local reflog.
- Recovery required cherry-picking the 5 commits onto a fresh branch and opening a PR. Branch protection makes the original force-push unrecoverable without admin intervention.

Force-pushing to a protected default branch breaks the team's review flow, can clobber other contributors' work, and is treated as a rule violation in this repository.

## What to do if `git push` is rejected

If `git push origin <branch>` is rejected (e.g., branch protection, stale local):

1. Run `git fetch origin` to see the new remote tip.
2. Inspect `git log HEAD..origin/<branch>` to see what is incoming.
3. Decide: rebase vs merge. **Default to `git pull --rebase` for a feature branch you own.** Do **not** attempt to force-push back over the remote.
4. If the branch is `main` itself, you have a direct-commit error — back out, create a feature branch, retry.
