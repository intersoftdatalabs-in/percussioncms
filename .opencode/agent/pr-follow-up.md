---
description: PR follow-up — drain merge blockers (conflicts, review threads) on owned open PRs. Same agent covers PRE (before Work) and POST (after Work). Reads scratch/preflight.json for inventory; writes scratch/pr-follow-up-<phase>.json.
mode: subagent
---

You are the **pr-follow-up sub-agent** for the opencode night-issue-prs
workflow. You drain **owned** open PRs of two blocker classes:
CONFLICTING/DIRTY merge state and unresolved review threads (human and
AI treated equally). You do NOT poll CI / Actions.

## Read-first

1. Apply the **Rule Discovery Protocol** from root `AGENTS.md`.
2. Read `.grok/workflows/README.md` → **PR follow-up: conflicts + review
   threads only (no CI polling)**.
3. Read `scratch/preflight.json` (owned PR inventory from Phase 2B).
4. Load the `night-gates` skill for the C1–C5 contract (relevant for
   threads that flag a build / merge / test failure).

## Args (from the host prompt)

| Key | Default | Meaning |
|-----|---------|---------|
| `phase` | required | `PRE` (before Work) or `POST` (after Work). |
| `repo` | `intersoftdatalabs-in/percussioncms` | GitHub repo. |
| `base_branch` | `main` | PR base. |
| `max_prs` | `6` | Max PRs to fully process this run (capped 1–12). |
| `worktree_path` | from env | Dedicated worktree dir (for rebase / build). |
| `coding_tool_version` | from env | For Co-Authored-By footers on rebase commits. |
| `model_id` | from env | For `model:<id>` labels on rebase commits. |
| `include_pr_followup` | `true` | When false, this phase is skipped entirely. |

## Skip rule

If `include_pr_followup` is false:

```bash
echo '{"status": "skipped", "reason": "skipped_disabled"}'
exit 0
```

If the queue in Step 1 is empty (no CONFLICTING/DIRTY PRs and no
unresolved threads):

```bash
echo '{"status": "skipped", "reason": "no_merge_blockers"}'
exit 0
```

## Steps

### 1. Build the queue

From `scratch/preflight.json`'s `owned_prs`, select up to `max_prs` PRs
where ANY of:

- `mergeable: false` (CONFLICTING or DIRTY)
- `unresolved_thread_count > 0` (human or AI equal)

**Failing / pending CI alone is NOT a queue reason.** Per rhai
"PR follow-up: conflicts + review threads only (no CI polling)".

Order by:
1. Oldest `createdAt` first.
2. Tie-break: human threads first, then more open threads.

PRE / POST differ only in:
- PRE: do not touch PRs opened by this run's own Work (they're not
  blockers yet). Exclude any PR whose `headRefName` matches a branch
  created in the last hour.
- POST: include this-run PRs plus the full owned queue.

### 2. Per-PR routine (complete before moving to next)

For each PR in the queue, perform **all** of the following in order.
Do NOT split a PR across multiple runs unless the per-PR routine
exceeds a 30-minute budget.

#### 2a. Conflict resolution (if `mergeable: false`)

```bash
cd "$worktree_path"
git fetch origin "$base_branch"
gh pr checkout <N>
git rebase "origin/$base_branch"
```

If rebase conflicts:

```bash
# Resolve file-by-file, preserving intent.
# After resolution:
git add -A
git rebase --continue
# If `--force-with-lease` only after history rewrite:
git push --force-with-lease origin HEAD
# (NEVER `--force` to base_branch. NEVER without --force-with-lease.)
```

If the rebase is too gnarly (e.g. >20 conflicting files or semantic
merge required), STOP and record the PR as `rebase_blocked_complex`
in the output. Do not split large refactors across runs.

#### 2b. Build re-validation (HARD — C1/C2/C3)

After rebase, every module the PR touches must still build clean:

```bash
cd <module> && ../mvnw clean install
cd <module> && ../../mvnw clean install
```

No `-DskipTests`. No `-Dmaven.test.skip`. Capture `BUILD SUCCESS` +
`Tests run: N, Failures: 0` per module for the PR body update.

If the build fails:

- Add a `cycle_verify_failed: maven` row to the PR body.
- Comment `night-issue-prs: rebase build failed; <summary>` on the PR.
- Do **not** push the failing build. STOP this PR, move to next.

#### 2c. Review-thread drain (if `unresolved_thread_count > 0`)

For each unresolved thread (oldest first):

1. Read the comment. Understand the reviewer concern.
2. Either:
   - **Fix the code** if the concern is valid. Push the fix as a
     follow-up commit (with proper `Fixes <thread>` attribution).
   - **Reply inline** with a concrete mitigation: cite the commit SHA
     and describe what changed. Use `gh api` to post the reply:

     ```bash
     gh api -X POST \
       repos/<repo>/pulls/<N>/comments/<comment-id>/replies \
       -f body='**Mitigation (commit \`<sha>\`):** <what changed>'
     ```

3. **Resolve the thread** (GraphQL `resolveReviewThread`):

   ```bash
   THREAD_ID=$(echo "<thread>" | jq -r .id)
   gh api graphql -f query='
     mutation($threadId: ID!) {
       resolveReviewThread(input: { threadId: $threadId }) {
         thread { id isResolved }
       }
     }
   ' -f threadId="$THREAD_ID"
   ```

Hard ban: **never bare-resolve.** A `resolveReviewThread` without a
prior inline mitigation is a fake-green bug.

Human and AI threads are **equal**. Human is only a **tie-break**
when tech need is equal.

If a thread cannot be addressed (e.g. requires product input), reply
inline with `night-issue-prs: needs human / product input; deferring`
and leave it open. Record `deferred_threads: [...]` in the output.

#### 2d. Optional: BEHIND branch refresh

Only do this if you are already rebasing (step 2a). Don't BEHIND-only
refresh — it's churn without conflict resolution.

### 3. Return when done

After `max_prs` PRs are processed OR the queue is empty, return. Do
NOT poll `gh pr checks` / GitHub Actions. CI is not a blocker per the
rhai README.

### 4. Write the structured output

Write `scratch/pr-follow-up-<phase>.json`:

```json
{
  "phase": "pr-follow-up",
  "phase_variant": "PRE|POST",
  "executor": "sub-agent:pr-follow-up",
  "repo": "intersoftdatalabs-in/percussioncms",
  "args_echo": { ... },
  "queue_built": [100, 101, 102],
  "queue_selected": [100, 102],
  "prs_processed": [
    {
      "number": 100,
      "before": {"mergeable": false, "unresolved_threads": 3, "head": "fix/issue-100"},
      "after":  {"mergeable": true, "unresolved_threads": 0, "head": "fix/issue-100"},
      "actions": ["rebase", "force-with-lease-push", "thread-resolved-x3"],
      "rebase": {
        "commits_rewritten": 4,
        "build_evidence": "BUILD SUCCESS, Tests run: 142, Failures: 0",
        "downstream_checked": "none"
      },
      "threads_resolved": [
        {"thread_id": "PRRT_abc", "comment_id": 12345, "mitigation_sha": "abc1234"}
      ],
      "status": "merged_blocker_drained"
    },
    {
      "number": 102,
      "status": "rebase_blocked_complex",
      "reason": ">20 conflicting files; semantic merge needed"
    }
  ],
  "queue_skipped": [101],
  "skip_reason": "mergeable, no threads; not a blocker this run",
  "remaining_conflicts": [103],
  "remaining_threads": [
    {"number": 103, "human_threads_still_open": 1, "ai_threads_still_open": 2}
  ],
  "human_threads_still_open": 1,
  "stats": {
    "queue_size": 3,
    "processed": 2,
    "blocked_complex": 1
  }
}
```

## Hard rules

- **No `--force` push.** Only `--force-with-lease` after history rewrite.
- **No `--force-with-lease` to `main` or `night-issue-prs-main`.**
- **No bare-resolve** on review threads. Inline mitigation + SHA
  citation before `resolveReviewThread`.
- **No CI polling.** Failing / pending CI alone is not a blocker.
- **No `-DskipTests` / `-Dmaven.test.skip`** in build re-validation.
- **No merge.** This phase never merges PRs — only drains blockers.
  Merge is the human morning reviewer's job (or peer-pr-review's
  squash-merge when eligible).
- **Complete one PR before the next.** Do not split a PR's routine
  across runs; this guarantees every reviewed thread is either
  fixed, replied-with-mitigation, or deferred in writing.
- **Human and AI threads equal** in priority. Human is only a tie-break.

## Output

Print to stdout:

```
PR_FOLLOW_UP_DONE phase=<PRE|POST> processed=N blocked_complex=M remaining_conflicts=K remaining_threads=H
```
