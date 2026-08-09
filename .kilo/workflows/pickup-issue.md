---
description: Triage the open GitHub issue backlog and select up to N (default 5) high-priority candidates that have actionable engineering work for a Kilo session. Outputs a triage dataset and stops; does not pick, branch, or commit on its own.
---

## Goal

Produce a **triage dataset** — a ranked list of the first `MAX_TASKS`
(default `5`) open p1..p8 issues that have actionable engineering
work for a Kilo session. The output is a list, not a single pick,
so the human (or a follow-up session) can decide which candidate to
actually drive to a PR.

- **`MAX_TASKS`** (default `5`): cap on candidates returned per
  workflow run. Workflow stops once the cap is reached.
- **`MAX_PRIORITY`** (default `p1`): highest priority bucket the
  walk starts from. Discovery walks `p1` → `p2` → … and stops once
  `MAX_TASKS` survivors are accumulated **or** every priority is
  exhausted, whichever comes first.

The workflow **reads only**. It does **not** apply `in progress`
labels, does **not** open branches, and does **not** commit or push.
A separate `pick-and-work` invocation (or the human) picks one row
from the dataset and drives it to a PR.

## Discovery

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

## Scope check — there must be engineering to do

After the candidate row is chosen, read the issue body and recent
comments to confirm there is real engineering work a Kilo worktree
can execute in this session. **Skip the candidate** (and resume
Discovery with the next oldest survivor at the same priority) if
**all** of the following hold:

1. The issue body or recent comments list the slice PRs that
   implement the acceptance criteria and every one of those PRs is
   `MERGED` (or the linked child issues are closed with no open
   residual).
2. The comments contain a tracker directive — any of:
   `leave open as tracker`, `parent stays open until residual`,
   `close gate: #<n>`, `no further engineering slices scheduled`,
   `all AC met`. The directive must be from the issue's own audit
   comment, not a one-off remark.
3. The remaining open child issues (if any) are documented as
   blocked on customer data (`requires customer env`, `verify ops
   path`, etc.) — i.e. nothing in this issue is actionable from a
   Kilo worktree.

If the candidate is a **research / spec parent** (e.g. #2400), the
scope check passes only if there is a deliverable to author in this
session: a checked-in spec/plan file under `specs/`, a capability
matrix, or an open engineering slice child. Bare "research"
parents with no concrete artifact to ship in this session are also
skipped.

Use this query to fetch the audit comment and the open children:

```bash
# Latest 5 comments — to find the tracker directive
gh issue view "$ISSUE" --comments --json comments --jq '.comments
  | sort_by(.createdAt) | reverse | .[0:5]
  | map({author: .author.login, body: .body[0:200]})'

# Open children — to see if any are engineering slices
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
- For unknown / under-specified bodies, "needs scoping" — the
  next session must read the issue body and decide.

## Accumulate, then stop

After each successful scope check, append the candidate to the
in-memory dataset and stop once `MAX_TASKS` entries are present:

```bash
MAX_TASKS="${MAX_TASKS:-5}"
MAX_PRIORITY="${MAX_PRIORITY:-p1}"
```

If the priority walk reaches `p8` with fewer than `MAX_TASKS`
candidates, the workflow still stops — a short dataset is more
useful than padding it with low-priority work.

## Triage dataset output

Emit the dataset as both a human-readable block and a JSON object
so downstream tooling can parse it:

### Human-readable

```
TRIAGE — Kilo pickup (model: $KILO_MODEL, max: $MAX_TASKS)

1.  #1234  p1  <title>
        scope: <one-line summary>
        link:  https://github.com/<owner>/<repo>/issues/1234

2.  #5678  p1  <title>
        scope: <one-line summary>
        link:  https://github.com/<owner>/<repo>/issues/5678

(2 candidates, 3 priorities scanned)

Skipped during this run:
  - #804  tracker-only — all engineering merged, audit says leave open
  - #934  tracker-only — gap-matrix says AC1/2/3/4/5/6 met
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
    { "number": 804, "reason": "tracker-only — all engineering merged, audit says leave open" },
    { "number": 934, "reason": "tracker-only — gap-matrix says AC1/2/3/4/5/6 met" }
  ]
}
```

The session's final response should include **only** the human-
readable block (so it is visible in the chat) and offer the JSON
on request. Do **not** auto-write the dataset to disk unless the
human asks; keep the chat output as the source of truth.

## Stop

After emitting the dataset, the workflow is done. Do **not**:

- Apply `in progress` to a candidate.
- Open a feature branch, commit, push, or create a PR.
- Re-run Discovery within the same session.

The human reads the triage, picks one row (or none), and either
runs a follow-up `pick-and-work` workflow on it or directs the
agent to drive that one issue to a PR.

## Do **not** do

- Do **not** filter on `operator:*` labels — they are workflow
  attribution, not assignment, and must not disqualify a candidate.
- Do **not** pick, branch, commit, push, or open a PR. This
  workflow is triage-only.
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
