---
description: Pick up the highest-priority open GitHub issue that is not already in progress, mark it in-progress, swap the operator labels to Kilo, and start a feature branch + PR for it. Iterates priorities p1 to p8 and stops at the first available issue.
---

## Goal

Find the **highest-priority** open issue that is **not** already being
worked (no `in progress` label) and pick it up.

1. **Search order:** `p1`, `p2`, `p3`, … `p8`. Stop at the first
   priority that yields at least one available issue; if a priority
   yields none, move to the next. Stop entirely only when **every**
   priority returned zero issues.
2. **Available =** state == `OPEN` **AND** does **not** carry the
   `in progress` label. The `operator:*` and `model:*` labels are
   workflow attribution, not assignment (see
   `.kilo/rules/operator-pr-labels.md`); do **not** filter on them.
3. Among available issues at the chosen priority, pick the **first**
   `gh issue list` row (oldest `createdAt`). Do not re-sort.

## Discovery

For each priority `p<N>` in `p1..p8`:

```bash
gh issue list \
  --state open \
  --label "p<N>" \
  --json number,title,labels,createdAt \
  --limit 100
```

For each candidate, filter out any issue whose `labels[].name` equals
`in progress`. The first survivor at the highest non-empty priority is
the chosen issue.

If every `p1..p8` query returns an empty filtered list, report
`No open p1..p8 issue is available.` and **stop** — do not invent work.

## Take the issue

On the chosen issue, perform the **state transition** before any code
work. Use `gh issue edit`:

```bash
ISSUE=<number>

# 1. Make sure the in-progress label exists (idempotent).
gh label create "in progress" --force --color "5CD5E1" \
  --description "Agent or human actively working"

# 2. Remove stale operator/model attribution so the agent that picks
#    it up does not appear as Grok / Minimax / etc.
gh issue edit "$ISSUE" \
  --remove-label "operator:grok" \
  --remove-label "operator:night-issue-prs" \
  --remove-label "operator:minimax" \
  --remove-label "operator:nate" \
  --remove-label "model:grok-4.5" \
  --remove-label "model:minimax" \
  --remove-label "model:claude-sonnet-4" \
  --remove-label "model:gpt-4.1" \
  || true   # ok if a label is absent

# 3. Apply Kilo attribution for this session's model.
MODEL="${KILO_MODEL:-unknown}"
gh label create "operator:kilo" --force --color "d73a4a" \
  --description "Work produced by Kilo Code agent"
gh label create "model:$MODEL" --force --color "1d76db" \
  --description "Model: $MODEL"

gh issue edit "$ISSUE" \
  --add-label "in progress" \
  --add-label "operator:kilo" \
  --add-label "model:$MODEL"
```

`$KILO_MODEL` is the **session model id** reported by the host (e.g.
`minimax-coding-plan/MiniMax-M3`). Use a stable lowercase slug with no
spaces. Never invent a label name.

> **Never** force-push to `main` and never commit directly to `main`.
> Create a feature branch first — see
> `.kilo/rules/no-force-push-development.md`.

## Start the feature branch

```bash
git fetch origin
git worktree list --porcelain          # see whether to reuse a worktree
BRANCH="fix/${ISSUE}-$(echo "$TITLE" | tr '[:upper:]' '[:lower:]' \
  | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//' | cut -c1-60)"

git checkout -b "$BRANCH" origin/main
```

Prefer a fresh worktree under `.kilo/worktrees/<branch>/` if one does
not already exist; otherwise work in the current checkout. Record the
worktree path in the session summary so
`.kilo/rules/worktree-hygiene.md` cleanup can find it later.

## Work the issue to a PR

Follow the standard Kilo flow for this repo (see root `AGENTS.md`):

- **Companion closure** for the change class (peer production + test +
  docs; see root **Change-class completeness**).
- **Pre-PR build:** `cd <module> && …/mvnw[.cmd] clean install` (not a
  default root `-pl -am` reactor build).
- **Pre-commit Erlang review** before `git commit` / `git push` /
  `gh pr create`.
- **Operator + model labels** on the PR (see
  `.kilo/rules/operator-pr-labels.md`).
- **Co-Authored footer** on commits (see
  `.kilo/rules/co-author-attribution.md`).

## Close the loop — remove `in progress` on PR submission

When the PR is **opened or updated** (not just when work ends), remove
the `in progress` label so other agents and humans see the issue is no
longer available:

```bash
gh issue edit "$ISSUE" --remove-label "in progress"
```

If the PR is later closed without merging, **re-add** `in progress` so
the next pickup pass does not re-pick it as fresh work.

## Output

Report back to the user with:

- Chosen issue number, title, priority, and link
- Branch name and worktree path
- One-line summary of the implementation plan
- PR URL once opened

## Do **not** do

- Do **not** filter on `operator:*` labels — they are workflow
  attribution, not assignment.
- Do **not** invent a `daily-status` label or any new label outside this
  command's allowlist.
- Do **not** skip the empty-check when a priority returns zero issues;
  continue to the next priority.
- Do **not** commit or push to `main` directly — always branch.
- Do **not** skip the pre-PR Maven `clean install` for changed modules.
- Do **not** mark `in progress` removed without first opening or
  updating the PR (i.e. keep the label while still drafting locally).
