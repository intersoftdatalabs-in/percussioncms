---
description: Triage the open GitHub issue backlog and select up to N (default 5) high-priority candidates that have actionable engineering work for a Kilo session. Also runs a PR review-and-merge pass (own-thread clear + other-agent reviews) before triage. Outputs a triage dataset and stops; does not pick, branch, or commit on its own during triage.
---

## Goal

Produce a **triage dataset** -- a ranked list of the first `MAX_TASKS`
(default `5`) open p1..p8 issues that have actionable engineering
work for a Kilo session. The output is a list, not a single pick,
so the human (or a follow-up session) can decide which candidate to
actually drive to a PR.

- **`MAX_TASKS`** (default `5`): cap on candidates returned per
  workflow run. Workflow stops once the cap is reached.
- **`MAX_PRIORITY`** (default `p1`): highest priority bucket the
  walk starts from. Discovery walks `p1` -> `p2` -> ... and stops once
  `MAX_TASKS` survivors are accumulated **or** every priority is
  exhausted, whichever comes first.

The **triage** path **reads only**. It does **not** apply `in progress`
labels, does **not** open branches, and does **not** commit or push.
A separate `pick-and-work` invocation (or the human) picks one row
from the dataset and drives it to a PR.

**Run order for a full session:** (1) Kilo own-PR comment resolution,
(2) PR review pass for other agents / unlabeled PRs, (3) issue triage.
Do **not** chain PR review and issue triage in the same run -- after
the review pass emits its summary, **stop**; triage runs in a
subsequent invocation.

## Kilo PR comment resolution -- clear threads on YOUR OWN open PRs (run BEFORE the PR review pass)

Before reviewing other agents' PRs, scan **your own** open PRs
(any PR carrying the `operator:kilo` label) for unresolved review
threads and resolve them per root `AGENTS.md` -> **PR Review
Comment Resolution**. The PR review pass downstream assumes Kilo's
own backlog is already clean.

### Discover (own PRs)

```bash
gh pr list --state open --json number,title,labels --limit 200 \
  | jq -r '.[]
      | select((.labels | map(.name) | any(. == "operator:kilo")))
      | .number'
```

### Per PR -- resolve every unresolved thread

For each open PR with `operator:kilo`:

1. **Fetch the review threads** (GraphQL -- the REST comments API
   does not expose thread-level resolution state):

   ```bash
   gh api graphql -H "X-GitHub-Api-Version: 2022-11-28" \
     -f query='query($owner: String!, $repo: String!, $n: Int!) {
       repository(owner: $owner, name: $repo) {
         pullRequest(number: $n) {
           reviewThreads(first: 50) {
             nodes {
               id isResolved isOutdated
               comments(first: 1) {
                 nodes { databaseId path line body }
               }
             }
           }
         }
       }
     }' \
     -f owner=<owner> -f repo=<repo> -F n=<n>
   ```
2. For **each unresolved** thread (regardless of who authored the
   first comment -- your own, another agent, a human, or the
   kilo-code-bot):
   - Read the finding body and the file:line context.
   - Decide the mitigation: code fix in a follow-up commit on the
     PR's branch, doc fix, or a documented "noted -- won't fix
     because X".
   - **Reply inline** with the mitigation, citing the commit hash
     (e.g. `f1908b961e`):

     ```bash
     gh api -X POST repos/<owner>/<repo>/pulls/<n>/comments/<databaseId>/replies \
       -f body='**Mitigation (commit <hash>):** <one-paragraph fix description + test/script/doc pointer>'
     ```
   - **Resolve the thread** via GraphQL `resolveReviewThread`:

     ```bash
     gh api graphql -H "X-GitHub-Api-Version: 2022-11-28" \
       -f query='mutation($threadId: ID!) {
         resolveReviewThread(input: { threadId: $threadId }) {
           thread { id isResolved }
         }
       }' -f threadId="<thread-id-PRRT_...>"
     ```
3. **Outdated threads** (where the diff no longer contains the
   offending line -- `isOutdated: true`) still need an inline
   reply explaining the mitigation AND a `resolveReviewThread` call.
   `isOutdated: true` is informational; it does **not** auto-resolve.
4. **Do not** mark a thread as resolved without first replying
   inline with a mitigation statement. A bare `resolveReviewThread`
   call is not a substitute for a documented fix.

### Sequence

Run **before** the PR review pass -- clear Kilo's own backlog first,
then review others. This ensures the PR review pass sees a clean
review-thread state on Kilo's PRs before applying the same gate to
incoming reviews.

### Stop condition

When **every** `operator:kilo` open PR has zero unresolved review
threads, proceed to the PR review pass. Otherwise emit a one-line
per-PR summary showing remaining unresolved count and continue
working through the list -- never skip a thread.

## PR review pass -- review-and-merge queue (run BEFORE triage)

Before any new triage picks up engineering work, scan the open PR list
for PRs authored by **another model** (or by **no model** at all) that
need a code review from Kilo and are candidates for squash-merge on
a clean review. The pass clears the review queue so new engineering
starts on a clean tree.

### Discover (other-agent / unlabeled PRs)

```bash
gh pr list --state open \
  --sort created --direction asc \
  --json number,title,author,labels,headRefName,baseRefName \
  --limit 200 | jq -r '
    .[]
    | select(
        ((.labels | map(.name) | any(. == "operator:kilo")) | not)
        and (
          (.labels | map(.name) | any(startswith("model:")))
          or ((.labels | map(.name) | any(startswith("model:"))) | not)
        )
      )
    | "\(.number)|\(.author.login)|\(.title)"
  '
```

`--sort created --direction asc` is required so processing order is
oldest-first (matches the Review instructions below). Without it,
GitHub's default sort does not guarantee age order.

The filter (per the user's direction) is **model-based**, not
operator-based:

| Condition | Meaning |
|-----------|---------|
| Not `operator:kilo` | Kilo has not claimed it; an independent review is owed. |
| Has **any** `model:*` label | Another model wrote it -- Kilo reviews it. |
| Has **no** `model:*` label | Per the user's rule: "if no model is listed then you should select it for review." |

Note: `operator:*` is workflow attribution (which agent produced the
work), not assignment (who should review it). Per
`.kilo/rules/operator-pr-labels.md`, it does **not** gate Kilo's
review pick -- only the `model:*` label determines whether Kilo
takes the PR.

PRs authored by the human owner (`natechadwick-intsof`) without an
agent attribution fall into the "no model listed" branch and are
selected for review unless the human has explicitly delegated them.

### Review

For each candidate PR, in the order returned by
`gh pr list --sort created --direction asc` (oldest first):

> **All review comments must be inline** -- per the user's rule
> (2026-08-08). Use `gh api -X POST .../pulls/<n>/comments` with
> `path` + `line` + `commit_id` to attach the comment to the
> specific file:line in the diff. Top-level PR comments via
> `gh pr review` are reserved for summary verdicts only (see the
> flag table below); substantive findings always go inline. The
> inline-comment helper:
>
> ```bash
> # Replace <owner>/<repo> with the actual values, <n> = PR number,
> # <file> = repo-relative path, <line> = line in the diff,
> # <head-sha> = SHA from `gh pr view <n> --json headRefOid --jq .headRefOid`.
> gh api -X POST repos/<owner>/<repo>/pulls/<n>/comments \
>   -f body='<finding>' \
>   -f path='<file>' \
>   -F line=<line> \
>   -F side='RIGHT' \
>   -F commit_id='<head-sha>'
> ```
>
> For multi-line selections use `start_line` +
> `start_side='RIGHT'` + `line` (end) on the same call.

1. **Erlang review** -- fetch `gh pr diff <n>` and run a strict Erlang
   review against the
   `.kilo/workflows/erlang-review` persona. Look for: bug findings,
   missing behavioral tests on new/changed non-trivial logic,
   non-portable path / file I/O (Windows/Unix), security issues
   (CodeQL-style), missing or wrong copyright headers on **new**
   files (>= 2023 must use `Intersoft Data Labs`), missing
   Co-Authored footer on agent commits, missing
   `operator + model` labels on the PR. Post each finding
   **inline** at the relevant file:line.
2. **Pre-PR build evidence** -- read the PR body for standalone
   module clean-install results (`cd <module>` then repo-root
   `mvnw` / `mvnw.cmd clean install`). If absent, re-run on a
   fresh worktree pointing at the PR head; if it fails, post an
   inline comment on the relevant test/build file and request
   changes. If the PR is docs-only / non-Maven, the body should
   say so explicitly.
3. **CI checks** -- `gh pr checks <n>` -- confirm required checks
   pass or are explicitly skipped. Failures block the merge.
4. **Review-thread state** -- `gh api graphql ... reviewThreads` --
   confirm every prior review comment has an inline reply AND a
   `resolveReviewThread` mutation. Unresolved threads block the
   merge per root `AGENTS.md` -> **PR Review Comment Resolution**.

### Squash-merge decision

The PR is **squash-merged** if **all** of the following hold:

- Erlang review verdict: APPROVE (no bug findings; non-portable path
  / file I/O absent or already justified; behavioral tests present
  for new/changed non-trivial logic).
- Pre-PR build evidence: present (or skipped with a documented
  reason in the PR body).
- CI: every required check passes or is explicitly skipped.
- Review threads: zero unresolved threads (each prior finding has an
  inline reply + `resolveReviewThread` call).
- PR carries `operator + model` labels per
  `.kilo/rules/operator-pr-labels.md`.
- `mergeable == MERGEABLE`. If `mergeable == CONFLICTING`, see
  **Conflict resolution** below -- the reviewer attempts to clear
  the conflict before posting a rebase request.

If **any** condition fails, post the substantive finding as an
**inline** review comment at the relevant file:line, then post a
short top-level **summary** verdict with the right `gh pr review`
flag (do **not** always use `--comment` -- that submits a neutral
review, not a blocking one):

| Verdict | Command |
|---------|---------|
| APPROVE (clean) | `gh pr review <n> --approve -b "<one-line summary>"` |
| REQUEST CHANGES (blocker) | `gh pr review <n> --request-changes -b "<inline-finding pointers + one-line summary>"` |
| DUPLICATE | `gh pr review <n> --comment -b "DUPLICATE of #<other>: <reason>"` |
| NEEDS REBASE | `gh pr review <n> --comment -b "NEEDS REBASE: <conflict-summary>"` |

Do **not** merge and move to the next candidate.

### Conflict resolution (when `mergeable == CONFLICTING`)

When a candidate PR reports `CONFLICTING`, attempt to resolve the
conflict locally before blocking on the author. The attempt
covers three cases and is ordered least-invasive first:

1. **Trivial rebase** -- fetch the PR head, attempt `git rebase
   origin/main` in a disposable worktree. If the rebase applies
   cleanly (no text conflicts), run the module's pre-PR build
   (see portable `mvnw` snippet below), push the result with
   `--force-with-lease`, and proceed to the squash-merge step.
   This unblocks PRs that just need a refresh against current
   `main`.
2. **Duplicate of an open Kilo PR** -- if the rebase fails and the
   conflicting hunks overlap with an open Kilo PR on the same
   files (see `.kilo/rules/no-force-push-development.md` ->
   exception for review-driven rebases), post a comment recommending
   closure as duplicate of the Kilo PR and skip. Do **not** attempt
   a force-push resolution that would clobber the Kilo PR. The
   duplicate-detection heuristic computes **set overlap percentage**
   (not a contiguous-subarray match):

   ```bash
   # High file-overlap = |intersection| / |target files| >= 0.5
   TARGET=$(gh pr view <n> --json number --jq .number)
   gh pr list --state open --json number,title,files --limit 200 \
     | jq --argjson target "$TARGET" '
       (map(select(.number == $target) | .files[].path) | unique) as $tf
       | .[]
       | select(.number != $target)
       | (.files | map(.path) | unique) as $cf
       | ($tf | map(select(. as $p | $cf | index($p) != null)) | length) as $inter
       | select(($tf | length) > 0 and ($inter / ($tf | length)) >= 0.5)
       | {
           number, title,
           overlap_count: $inter,
           target_count: ($tf | length),
           overlap_pct: (($inter / ($tf | length)) * 100 | floor)
         }'
   ```
   `<n>` is the PR being reviewed; `TARGET` resolves it to the
   authoritative `number` from the GitHub API.

   If a candidate surfaces with high file-overlap (>= 50% of the
   target's touched files), post the close-as-duplicate comment.
   Do **not** use jq `inside()` for this check -- `inside()` is a
   contiguous subarray match and would flag any single shared file.
3. **Non-trivial conflict** -- if the rebase fails with text conflicts
   that are **not** a duplicate signal, post a structured review
   comment with the conflict markers (`<<<<<<<` / `=======` /
   `>>>>>>>`) excerpted from `git diff` and request a rebase from
   the author. Do **not** push a partial resolution. Do **not**
   leave the worktree in a half-rebased state -- `git rebase --abort`
   and remove the worktree (portable path below) before moving on.

#### Trivial rebase command (portable worktree + mvnw)

Use a **repo-local** worktree under `.kilo/worktrees/` (gitignored)
rather than OS-specific temp roots such as `/tmp` or `%TEMP%`. This
matches root `AGENTS.md` -> **Cross-Platform File I/O & Paths** and
**Git worktree hygiene**.

```bash
# Portable: repo-local disposable worktree (Windows + Unix) -- never /tmp or %TEMP%
PR_BRANCH="<pr-branch>"            # e.g. feat/foo
PR_NUMBER=<n>                      # PR number (int)
REPO_ROOT="$(git rev-parse --show-toplevel)"
WORKTREE_DIR="${REPO_ROOT}/.kilo/worktrees/review-${PR_NUMBER}"
git worktree remove --force "$WORKTREE_DIR" 2>/dev/null || true   # idempotent
git fetch origin main
git fetch origin "$PR_BRANCH"
git worktree add "$WORKTREE_DIR" "origin/$PR_BRANCH"
cd "$WORKTREE_DIR"
if git rebase origin/main; then
  # Resolve the monorepo-root Maven wrapper once (works for any module depth).
  if   [ -f "$REPO_ROOT/mvnw.cmd" ]; then WRAPPER="$REPO_ROOT/mvnw.cmd"
  elif [ -f "$REPO_ROOT/mvnw" ];     then WRAPPER="$REPO_ROOT/mvnw"
  else echo "FATAL: no mvnw / mvnw.cmd at $REPO_ROOT" >&2; exit 1
  fi
  # Run pre-PR build per changed module. Discover modules from the diff
  # vs origin/main (post-rebase) so we cover every module the PR touched.
  mapfile -t MODULES < <(git diff --name-only origin/main..HEAD \
    | xargs -I{} dirname {} | sort -u \
    | awk -F/ 'NF==1{print "."} NF>1{print $1}' | sort -u)
  for m in "${MODULES[@]}"; do
    [ -f "$m/pom.xml" ] || continue
    ( cd "$WORKTREE_DIR/$m" && "$WRAPPER" clean install )
  done
  git push --force-with-lease origin "HEAD:$PR_BRANCH"
  git worktree remove --force "$WORKTREE_DIR"
  # proceed to squash-merge
else
  git rebase --abort
  git worktree remove --force "$WORKTREE_DIR"
  # post duplicate-or-conflict comment; skip
fi
```

`--force-with-lease` is permitted here only because the rebase is
against `origin/main` (not `main`) and the push target is the PR's
own branch (never `main`). Per `.kilo/rules/no-force-push-development.md`
this is the explicitly documented exception for **review-driven
rebases of third-party PRs**; the comment "log the rebase in the
review comment" is mandatory so the original author knows their
branch tip moved. **Never** force-push to `main`.

### Squash-merge command

```bash
gh pr merge <n> --squash --delete-branch \
  --subject "<concise commit subject -- match the PR title or scope>" \
  --body "<PR summary; preserve original PR body unless empty>"
```

After merge, post a brief confirmation comment on the issue the PR
closes (if the PR body references one). Apply `Reviewed-by: Kilo`
trailer to the merge commit via `--body` if the GitHub UI does not
auto-fill it.

### Stop (after review pass)

After the review pass, emit the summary block and **stop** before
running the triage pass:

```
PR REVIEW -- Kilo (model: $KILO_MODEL)

1.  #<n>  <title>  ->  SQUASH-MERGED | REQUESTED CHANGES | SKIPPED
2.  #<n>  <title>  ->  ...

(<x> reviewed, <y> merged, <z> needs changes, <w> skipped)
```

If the summary is empty (no candidates), report
`No open PRs from other models need a code review.` and **stop**.
The triage pass runs in a subsequent invocation -- do not chain
PR review and issue triage in the same run.

---

## Discovery (issue triage)

For each priority `p<N>` in `p1`..`p8`, query with explicit
ascending-`createdAt` order so "first row" = "oldest":

```bash
gh issue list \
  --state open \
  --label "p<N>" \
  --json number,title,labels,createdAt \
  --limit 100 \
  --sort created \
  --direction asc
```

For each candidate, filter out any issue whose `labels[].name` equals
`in progress`. The first survivor at the highest non-empty priority
becomes the next candidate to scope-check. Capture `ISSUE=<number>`
and `TITLE=<title>` for the steps below.

### Already-picked-up check (per candidate)

Before the candidate becomes the chosen issue, run **three** pickup
checks. Any one of them signals "someone is already on it" and the
candidate is dropped from the candidate set; Discovery resumes with
the next oldest survivor at the same priority. This contract is
**shared with the grok nightly-PR workflow** (host-local
`.grok/workflows/`): both sides honor the same three checks so an
agent on one side never starts parallel work the other side has
already claimed.

1. **Label check.** Issue carries `in progress` label -> skip. Both
   the Kilo pickup workflow and the grok nightly-PR workflow apply
   this label when they start work and remove it when a PR is opened
   so an issue is available for handoff only when no PR is in flight.
2. **PR-reference check.** Any PR whose body references the issue
   number (via `#<n>` / "closes #N" / "fixes #N") or whose
   `closingIssuesReferences` includes the issue, in state `OPEN`
   or `MERGED` -> skip. (`CLOSED` without merge is **not** a
   pickup signal -- the work was abandoned; defer to check 3.)

   ```bash
   gh pr list --state all --json number,state,body,closingIssuesReferences \
     --limit 500 \
     | jq --arg issue "$ISSUE" '
       [.[] | select(
         (.body // "" | test("(^|[^0-9])#" + $issue + "($|[^0-9])"))
         or (((.closingIssuesReferences // []) | map(.number // empty)) | any(. == ($issue | tonumber)))
       ) | {number, state}]'
   ```
   Note: jq `test` is POSIX ERE (no `\b`), so the regex anchors with
   explicit non-digit boundaries to avoid `#1` matching `#10`, `#11`, ....
   `closingIssuesReferences` is an array of `{number, ...}` objects, so
   we map to `.number` before the integer comparison.
3. **Comment-based claim check.** The latest 5 comments include an
   agent-attributed pickup signal -- any of: `picking this up`,
   `starting work`, `picked up`, `agent progress`, `pr_opened`,
   `pr opened`, `in flight`, or a status table entry with value
   `in_progress` / `pr_opened` / `done` / `merged`. Authored by
   a known agent handle (`grok`, `minimax`, `kilo`, or any
   `operator:*`-attributed handle).

   ```bash
   gh issue view "$ISSUE" --comments --json comments --jq '
     .comments | sort_by(.createdAt) | reverse | .[0:5]
     | map({author: .author.login, body: .body})'
   ```
   The full comment body is preserved (no truncation) so tracker
   directives at any character offset are still visible.

When a candidate is skipped on any of the three checks, log:

```
SKIP #<n>: <which-check> triggered -- <one-line evidence>
```

...and resume Discovery with the next oldest survivor at the same
priority. If every candidate at every priority skips, report
`No open p1..p8 issue is available to pick up.` and **stop** -- do
not invent work.

If every `p1..p8` query returns an empty filtered list, report
`No open p1..p8 issue is available.` and **stop** -- do not invent work.

## Scope check -- there must be engineering to do

After the candidate row is chosen, read the issue body and recent
comments to confirm there is real engineering work a Kilo worktree
can execute in this session. **Skip the candidate** (and resume
Discovery with the next oldest survivor at the same priority) if
**all** of the following hold:

1. The issue body or recent comments list the slice PRs that
   implement the acceptance criteria and every one of those PRs is
   `MERGED` (or the linked child issues are closed with no open
   residual).
2. The comments contain a tracker directive -- any of:
   `leave open as tracker`, `parent stays open until residual`,
   `close gate: #<n>`, `no further engineering slices scheduled`,
   `all AC met`. The directive must be from the issue's own audit
   comment, not a one-off remark.
3. The remaining open child issues (if any) are documented as
   blocked on customer data (`requires customer env`, `verify ops
   path`, etc.) -- i.e. nothing in this issue is actionable from a
   Kilo worktree.

If the candidate is a **research / spec parent** (e.g. #2400), the
scope check passes only if there is a deliverable to author in this
session: a checked-in spec/plan file under `specs/`, a capability
matrix, or an open engineering slice child. Bare "research"
parents with no concrete artifact to ship in this session are also
skipped.

Use this query to fetch the audit comment and the open children:

```bash
# Latest 5 comments -- to find the tracker directive
gh issue view "$ISSUE" --comments --json comments --jq '.comments
  | sort_by(.createdAt) | reverse | .[0:5]
  | map({author: .author.login, body: .body[0:200]})'

# Open children -- to see if any are engineering slices
gh issue list --state open --json number,title --limit 200 \
  | jq --arg issue "$ISSUE" '[.[]
      | select(.title | test("issue " + $issue + "( |$)", "i"))
      | {number, title}]'
```

When a candidate passes the scope check, summarize **why it is
actionable** in one or two sentences for the triage dataset:

- Open slice count + names (e.g. "2 open engineering slices
  #2435/#2436; #2435 needs `@Lazy` ctor-param fix").
- For research parents, the concrete artifact to ship (e.g.
  "spec/plan file under `specs/2400-dce-explorer-parity.md`").
- For unknown / under-specified bodies, "needs scoping" -- the
  next session must read the issue body and decide.

## Accumulate, then stop

After each successful scope check, append the candidate to the
in-memory dataset and stop once `MAX_TASKS` entries are present:

```bash
MAX_TASKS="${MAX_TASKS:-5}"
MAX_PRIORITY="${MAX_PRIORITY:-p1}"
```

If the priority walk reaches `p8` with fewer than `MAX_TASKS`
candidates, the workflow still stops -- a short dataset is more
useful than padding it with low-priority work.

## Triage dataset output

Emit the dataset as both a human-readable block and a JSON object
so downstream tooling can parse it:

### Human-readable

```
TRIAGE -- Kilo pickup (model: $KILO_MODEL, max: $MAX_TASKS)

1.  #1234  p1  <title>
        scope: <one-line summary>
        link:  https://github.com/<owner>/<repo>/issues/1234

2.  #5678  p1  <title>
        scope: <one-line summary>
        link:  https://github.com/<owner>/<repo>/issues/5678

(2 candidates, 3 priorities scanned)

Skipped during this run:
  - #804  tracker-only -- all engineering merged, audit says leave open
  - #934  tracker-only -- gap-matrix says AC1/2/3/4/5/6 met
```

### JSON

```json
{
  "workflow": "pickup-issue",
  "model": "$KILO_MODEL",
  "max_tasks": 5,
  "max_priority": "p1",
  "priorities_scanned": 3,
  "candidates": [
    {
      "number": 1234,
      "title": "<title>",
      "priority": "p1",
      "createdAt": "<iso8601>",
      "labels": ["p1", "bug"],
      "url": "https://github.com/<owner>/<repo>/issues/1234",
      "scope": "<one-line summary>"
    }
  ],
  "skipped": [
    { "number": 804, "reason": "tracker-only -- all engineering merged, audit says leave open" },
    { "number": 934, "reason": "tracker-only -- gap-matrix says AC1/2/3/4/5/6 met" }
  ]
}
```

The session's final response should include **only** the human-
readable block (so it is visible in the chat) and offer the JSON
on request. Do **not** auto-write the dataset to disk unless the
human asks; keep the chat output as the source of truth.

## Stop (after triage)

After emitting the dataset, the workflow is done. Do **not**:

- Apply `in progress` to a candidate.
- Open a feature branch, commit, push, or create a PR.
- Re-run Discovery within the same session.

The human reads the triage, picks one row (or none), and either
runs a follow-up `pick-and-work` workflow on it or directs the
agent to drive that one issue to a PR.

## Do **not** do

- Do **not** filter on `operator:*` labels -- they are workflow
  attribution, not assignment, and must not disqualify a candidate.
- Do **not** pick, branch, commit, push, or open a PR during the
  triage path. This workflow's triage phase is triage-only.
- Do **not** pad the dataset to `MAX_TASKS` if the priority walk
  ran out earlier. Short dataset beats low-priority padding.
- Do **not** write the dataset to disk unless the human asks. The
  chat output is the source of truth.
- Do **not** invoke Discovery more than once per session. If the
  human asks for a refresh, start a new workflow run.
- Do **not** silently default `MAX_TASKS` to a value the human did
  not set; respect the env var if it is provided, otherwise use
  `5`.
- Do **not** silently default `KILO_MODEL` to a placeholder. If it
  is unset, stop.
- Do **not** invent a `daily-status` label or any new label outside
  this workflow's allowlist.
- Do **not** hardcode OS-specific temp paths (`/tmp`, `%TEMP%`) for
  review worktrees -- use `.kilo/worktrees/review-<n>` under the
  repo root.
- Do **not** use a literal path `mvnw[.cmd]` in shell examples --
  always branch on `mvnw.cmd` vs `mvnw`.
