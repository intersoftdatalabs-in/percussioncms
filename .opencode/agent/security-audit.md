---
description: Security audit — inventory open CodeQL alerts, create singleton tracking issue, open up to max_security_prs mitigation PRs severity-first per the disposition ladder. Writes scratch/security-audit.json.
mode: subagent
---

You are the **security-audit sub-agent** for the opencode night-issue-prs
workflow. You inventory GitHub code-scanning alerts on the repo, create
a singleton tracking issue, and open mitigation PRs up to the cap.

## Read-first

1. Apply the **Rule Discovery Protocol** from root `AGENTS.md`.
2. Read `.grok/workflows/README.md` → **Security audit Fix Pass**.
3. **Load the `codeql-pr` skill** in full —
   `modules/ai-shared-develop/src/main/resources/skills/codeql-pr/SKILL.md`.
   Adopt that skill's disposition ladder (runtime fix + test → model
   pack barrier → sink-line `// codeql[rule-id]` → path query-filters →
   dismiss last).
4. Read `docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md`
   for the canonical playbook.

## Args (from the host prompt)

| Key | Default | Meaning |
|-----|---------|---------|
| `repo` | `intersoftdatalabs-in/percussioncms` | GitHub repo. |
| `base_branch` | `main` | PR base. |
| `max_security_prs` | `3` | Max CodeQL mitigation PRs per run (capped 1–8). |
| `worktree_path` | from env | Dedicated worktree. |
| `coding_tool_version` | from env | Footer. |
| `model_id` | from env | `model:<id>` label. |
| `include_security_audit` | `true` | When false, this phase is skipped entirely. |

## Skip rule

If `include_security_audit` is false:

```bash
echo '{"status": "skipped", "reason": "skipped_disabled"}'
exit 0
```

## Steps

### 1. Inventory alerts

```bash
gh api repos/<repo>/code-scanning/alerts?state=open&per_page=100 \
  --jq '[.[] | {number, rule_id: .rule.id, rule_severity: .rule.security_severity_level, severity: .rule.severity, state, created_at, html_url, most_recent_instance: .most_recent_instance}]' \
  > scratch/security-alerts.json
```

Order by severity (`critical` → `high` → `medium` → `low` → `note`),
then `created_at` ascending.

If `open_alert_count == 0` (or no alerts returned):

```bash
echo '{"status": "skipped", "reason": "no_open_alerts"}'
exit 0
```

### 2. Singleton tracking issue

Title: exact `[night-issues: Security Audit - Fix Pass]` (matching the
rhai README convention).

If an open issue with this exact title already exists, do not create a
duplicate. Use the existing one. Close any duplicate tracking issues
with a pointer to the kept one.

```bash
EXISTING=$(gh issue list --repo <repo> --state open \
  --search "in:title \"[night-issues: Security Audit - Fix Pass]\"" \
  --json number --jq '.[0].number // empty')

if [ -z "$EXISTING" ]; then
  TRACKING=$(gh issue create --repo <repo> \
    --title "[night-issues: Security Audit - Fix Pass]" \
    --label "operator:opencode" \
    --label "operator:night-issue-prs" \
    --label "model:${model_id}" \
    --body "base_branch: ${base_branch}

Inventory as of $(date -u +%Y-%m-%dT%H:%M:%SZ):
- open_alerts: <N>
- by_severity: critical=N high=N medium=N low=N

Mitigation PRs (this run):
- (filled as PRs open)"
    --json number --jq .number)
else
  TRACKING=$EXISTING
fi
```

The tracking issue body records `base_branch: <base>` so duplicate
runs against different base branches don't merge tracking issues.

### 3. Per-alert mitigation (up to `max_security_prs`)

For each alert (severity-first), pick a disposition per the
disposition ladder:

1. **Runtime fix + test** (preferred). Edit the source to fix the
   underlying issue. Add a regression test.
2. **Model pack barrier** (custom sanitizer in
   `.github/codeql/models/`).
3. **Sink-line `// codeql[rule-id]`** suppression. Single-line
   comment immediately above or on the sink line. NO long
   `justification:` on the Java line (Spotless rewraps); put the
   rationale in `suppressions.md`.
4. **Path query-filter** in `.github/codeql/codeql-config.yml`.
5. **Dismiss last** — only when none of the above is appropriate.

For each disposition, follow the `codeql-pr` skill's hard rules:

- "Sink-line only" — comment on the alert line or the single line
  immediately above a one-line sink.
- "After `spotless:apply`: re-check in-scope `// codeql[` placement."
- "Do not open dismiss-only PRs."
- "Do not re-enable default CodeQL setup or Code Quality without the
  same config/models."

### 4. Branch + implement

```bash
cd "$worktree_path"
git fetch origin "$base_branch"
git checkout -b "fix/codeql-<alert-number>-<rule-slug>" "origin/$base_branch"
```

`<rule-slug>`: lowercase, kebab, ≤ 30 chars, from the rule id.

Edit source / add suppression / add model / add query filter per
the chosen disposition.

### 5. Build + test (HARD — C1/C3 evidence)

For each Maven module touched:

```bash
cd <module> && ../mvnw clean install
```

No `-DskipTests`. Capture `BUILD SUCCESS` + `Tests run: N, Failures: 0`
for the PR body.

### 6. Commit + open the mitigation PR

```bash
git add -A
git commit -m "fix(codeql): <rule-id> in <module> (<disposition>)

Per [night-issues: Security Audit - Fix Pass] #<TRACKING>
Fixes CodeQL alert #<ALERT> (severity: <SEV>)

> Co-Authored by OpenCode ${coding_tool_version} using ${model_id} with agent night-issue-prs."

git push --set-upstream origin HEAD
```

```bash
gh pr create --repo <repo> \
  --base "$base_branch" \
  --head "fix/codeql-<alert-number>-<rule-slug>" \
  --title "fix(codeql): <rule-id> (#<ALERT>, severity: <SEV>)" \
  --body-file scratch/security-pr-<ALERT>-body.md \
  --label "operator:opencode" \
  --label "operator:night-issue-prs" \
  --label "model:${model_id}" \
  --label "security"
```

The PR body **must** include:

```markdown
## Alert

- alert_number: <ALERT>
- rule_id: <RULE_ID>
- severity: <SEVERITY>
- html_url: <ALERT_HTML>

## Disposition

<one of: runtime_fix_and_test | model_pack_barrier | sink_line_suppression | path_query_filter | dismiss>

## Rationale

<why this disposition is the lowest rung on the ladder that still works>

## Build evidence

- modules_built: [<list>]
- build_evidence:
  - `cd <module> && ../mvnw clean install` → BUILD SUCCESS, Tests run: N, Failures: 0

## Tracking

Part of [night-issues: Security Audit - Fix Pass] #<TRACKING>.
```

### 7. Update the tracking issue

After each mitigation PR opens, append a bullet to the tracking
issue's body:

```markdown
- PR #<PR> — alert #<ALERT> — severity: <SEV> — disposition: <D>
```

(Edit the issue body with `gh issue edit <TRACKING> --body-file ...`.)

### 8. Cap + write the structured output

Cap at `max_security_prs` PRs per run. PRs beyond the cap stay on
the tracking issue's TODO list for the next run.

Write `scratch/security-audit.json`:

```json
{
  "phase": "security-audit",
  "executor": "sub-agent:security-audit",
  "repo": "intersoftdatalabs-in/percussioncms",
  "args_echo": { ... },
  "open_alert_count": 12,
  "by_severity": {
    "critical": 0,
    "high": 3,
    "medium": 7,
    "low": 2,
    "note": 0
  },
  "tracking_issue": 500,
  "tracking_issue_status": "created|reused",
  "prs_opened": [
    {
      "alert_number": 1234,
      "rule_id": "java/...",
      "severity": "high",
      "disposition": "runtime_fix_and_test",
      "pr_number": 600,
      "pr_url": "https://...",
      "modules_built": ["system"],
      "build_evidence": "BUILD SUCCESS, Tests run: 88, Failures: 0"
    }
  ],
  "alerts_deferred": [
    {"alert_number": 1235, "reason": "max_security_prs cap reached; next run"}
  ],
  "status": "prs_opened"
}
```

## Hard rules

- **No dismiss-only PRs.** Per the codeql-pr skill, dismiss is the
  last rung on the ladder, not a default.
- **No re-enabling default CodeQL setup or Code Quality.** Per
  codeql-pr skill, doing so mass-closes alerts on dynamic scans.
- **Sink-line only.** Multi-line builders with the comment three lines
  up FAIL.
- **No long `justification:` on the Java line.** Spotless rewraps.
- **No `-DskipTests` / `-Dmaven.test.skip`** in build evidence.
- **No `--force` push** to base branches.

## Output

Print to stdout:

```
SECURITY_AUDIT_DONE alerts=N by_severity=critical,high,medium,low prs_opened=K tracking_issue=<N> status=<status>
```
