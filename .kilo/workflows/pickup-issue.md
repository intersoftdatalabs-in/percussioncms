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
3. Among available issues at the chosen priority, pick the **oldest**
   by `createdAt` (ascending). The discovery query must sort
   `created` ascending so the first survivor is the oldest available.

## Discovery

For each priority `p<N>` in `p1..p8`:

```bash
gh issue list \
  --state open \
  --label "p<N>" \
  --json number,title,labels,createdAt \
  --limit 100 \
  --jq 'sort_by(.createdAt)'
```

Prefer also passing sort flags when the installed `gh` supports them:

```bash
gh issue list \
  --state open \
  --label "p<N>" \
  --json number,title,labels,createdAt \
  --limit 100 \
  --search "sort:created-asc"
```

If neither `--jq` post-sort nor `sort:created-asc` is available, sort the
JSON array by `createdAt` ascending with the host shell before filtering.

For each candidate **in oldest-first order**, filter out any issue whose
`labels[].name` equals `in progress` (case-insensitive match for
`in progress` / `In Progress`). The first survivor at the highest
non-empty priority is the chosen issue.

**Capture fields from that survivor (required for later steps):**

```bash
ISSUE=<number from survivor.number>
TITLE=<title from survivor.title>
```

Do **not** leave `$TITLE` unset. Re-fetch if needed:

```bash
TITLE=$(gh issue view "$ISSUE" --json title --jq .title)
```

If every `p1..p8` query returns an empty filtered list, report
`No open p1..p8 issue is available.` and **stop** — do not invent work.

## Take the issue

On the chosen issue, perform the **state transition** before any code
work. Use `gh issue edit`:

```bash
ISSUE=<number>
# TITLE already set in Discovery

# 1. Make sure the in-progress label exists (idempotent).
gh label create "in progress" --force --color "5CD5E1" \
  --description "Agent or human actively working"

# 2. Remove ALL stale operator/model attribution so the agent that
#    picks it up does not appear as Grok / Minimax / etc.
#    Do not rely only on a hardcoded list — new operator:* / model:*
#    names appear over time (.kilo/rules/operator-pr-labels.md).
#
#    List current labels, then remove every name matching operator:*
#    or model:* (and keep the small fixed allowlist removals for
#    common known labels as a belt-and-suspenders pass).
LABELS=$(gh issue view "$ISSUE" --json labels --jq '.labels[].name')
for lab in $LABELS; do
  case "$lab" in
    operator:*|model:*)
      gh issue edit "$ISSUE" --remove-label "$lab" || true
      ;;
  esac
done
# Belt-and-suspenders for known names (ok if already gone):
gh issue edit "$ISSUE" \
  --remove-label "operator:grok" \
  --remove-label "operator:night-issue-prs" \
  --remove-label "operator:minimax" \
  --remove-label "operator:nate" \
  --remove-label "operator:kilo" \
  --remove-label "model:grok-4.5" \
  --remove-label "model:minimax" \
  --remove-label "model:claude-sonnet-4" \
  --remove-label "model:gpt-4.1" \
  || true

# 3. Apply Kilo attribution for this session's model.
#    KILO_MODEL is required. Do NOT invent model:unknown or any other
#    undocumented model label ("Never invent a label name").
if [ -z "${KILO_MODEL:-}" ]; then
  echo "ERROR: KILO_MODEL is unset. Set the session model id (e.g. minimax-coding-plan/MiniMax-M3) and re-run. Refusing model:unknown." >&2
  exit 1
fi
MODEL=$(echo "$KILO_MODEL" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._/-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$MODEL" ] || [ "$MODEL" = "unknown" ]; then
  echo "ERROR: KILO_MODEL resolved empty or 'unknown'. Refusing to create model:unknown." >&2
  exit 1
fi
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
spaces. **Never invent a label name.** If the model id is unavailable,
**stop** and ask the operator — do not invent `model:unknown`.

> **Never** force-push to `main` and never commit directly to `main`.
> Create a feature branch first — see
> `.kilo/rules/no-force-push-development.md`.

## Start the feature branch

```bash
git fetch origin
git worktree list --porcelain          # see whether to reuse a worktree

# $TITLE must be set from Discovery (or re-fetched via gh issue view).
if [ -z "${TITLE:-}" ]; then
  TITLE=$(gh issue view "$ISSUE" --json title --jq .title)
fi
if [ -z "${TITLE:-}" ]; then
  echo "ERROR: could not resolve issue title for branch slug" >&2
  exit 1
fi

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
  command's allowlist (including **`model:unknown`**).
- Do **not** skip the empty-check when a priority returns zero issues;
  continue to the next priority.
- Do **not** commit or push to `main` directly — always branch.
- Do **not** skip the pre-PR Maven `clean install` for changed modules.
- Do **not** mark `in progress` removed without first opening or
  updating the PR (i.e. keep the label while still drafting locally).
- Do **not** assume `gh issue list` is oldest-first without sorting
  (`createdAt` ascending).
- Do **not** build a branch slug without a resolved `$TITLE`.
