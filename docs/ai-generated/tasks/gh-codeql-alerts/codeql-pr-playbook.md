# CodeQL PR Playbook — stop residual thrashing

**Audience**: humans and agents working security / CodeQL PRs  
**Repo**: `intersoftdatalabs-in/percussioncms`  
**Analyzer of record**: **CodeQL Advanced** workflow (`.github/workflows/codeql.yml`)

---

## Problem we are solving

Default CodeQL setup and advanced setup used to run in parallel:

| Analyzer | Loaded `codeql-config.yml` / model pack? | Effect on PRs |
|----------|------------------------------------------|---------------|
| **Default setup** | No | Re-opened critical residuals as new alert IDs (#1733 → #1735 → …) |
| **Advanced workflow** | Yes (`query-filters`, `packs`) | Honored documented residuals |

Structural fixes were correct; **PR gating used the wrong analyzer**. That caused repeated CodeQL review comments, dismissals, and re-opens.

**Policy (as of 2026-07-17):**

1. **Advanced setup is the analyzer of record** for PRs targeting `development` and for pushes to `development`.
2. **Default CodeQL setup is disabled** (`state=not-configured` on the repo).
3. **GitHub Code Quality** (dynamic workflow `Code Quality: CodeQL Setup` at `dynamic/github-code-scanning/codeql`) must stay **disabled** for this repo. It ignores `codeql-config.yml` / model packs, scans extra languages (C#, Python, Actions, …), and empty/stub analyses on the default branch close open alerts as "fixed".
4. **Languages in scope**: **Java** (`java-kotlin` with `build-mode: none`) and **JavaScript/TypeScript** only.
5. Custom sanitizers are modeled in `.github/codeql/models/` first; path `query-filters` are a fallback only.
6. Dismiss-only is a last resort and must cite model/config + tests.

---

## Architecture (source of truth)

```
.github/workflows/codeql.yml          # Advanced: push + pull_request + schedule + workflow_dispatch
.github/codeql/codeql-config.yml      # paths-ignore, java packs, query-filters
.github/codeql/models/                # Local model pack (Java barrier models)
  codeql-pack.yml
  models/*.model.yml
docs/ai-generated/tasks/gh-codeql-alerts/
  codeql-pr-playbook.md               # This file
  suppressions.md                     # Index of suppressions / path excludes
  triage.md / accepted-risks.md       # Disposition inventory
```

### Verify default setup stays off

```bash
gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state
# expected: not-configured
```

If it shows `configured` again (UI re-enable or org policy), disable immediately:

```bash
gh api --method PATCH repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup \
  -f state=not-configured
```

### Disable GitHub Code Quality (required)

API cannot disable this dynamic workflow (returns 422). Use the UI:

1. Repo **Settings** → **Code quality** (under Security)
2. Click **Disable** → **Save changes**

Confirm the dynamic workflow is idle:

```bash
gh api repos/intersoftdatalabs-in/percussioncms/actions/workflows \
  --jq '.workflows[] | select(.path=="dynamic/github-code-scanning/codeql") | {name, state}'
# Prefer: no active runs on push to development after disable
```

Symptom when Code Quality / default setup races advanced setup:

- Advanced job fails with: `CodeQL analyses from advanced configurations cannot be processed when the default setup is enabled`
- Or default-branch analyses appear with `rules_count: 0`, `tool.version: null` → **open alerts mass-closed as fixed**

Do **not** re-enable default setup or Code Quality without also attaching the same config/models — otherwise residual thrashing returns.

### Re-scan after config changes

```bash
gh workflow run "CodeQL Advanced" --ref development
# or merge a PR that touches .github/workflows/codeql.yml (push trigger)
```

---

## Disposition ladder (use in order)

When CodeQL flags a sink on a PR:

| Step | Action | Done when |
|------|--------|-----------|
| **1. Runtime fix** | Structural sanitizer / safe rebuild; regression test that fails on pre-fix | Exploit path closed |
| **2. Model pack** | Add/update `.github/codeql/models/models/*.model.yml` barrier for the custom sanitizer | Call sites stop tainting after `validate*` / `escape*` return |
| **3. Sink-line suppression** | `// codeql[rule-id]` on the **exact alert line** (or the single line immediately above a one-line sink) | Re-scan does not re-open that line |
| **4. Path `query-filters`** | Exclude path+rule in `codeql-config.yml` + row in `suppressions.md` | Only if model + sink-line still fail (known CodeQL blind spots) |
| **5. Dismiss alert** | API/UI dismiss as false positive with ≤280 char reason, commit SHA, test name | Gate green; document in `suppressions.md` |

Never start at step 5. Never open a parallel PR that only dismisses without steps 1–2.

---

## Sink-line suppression rules

CodeQL only honors:

```java
// codeql[java/ssrf]
someSink(arg);
```

or same line:

```java
someSink(arg); // codeql[java/ssrf]
```

**Preferred form for Jackson/JAXB/CXF REST residuals (`java/xss`):** put the annotation
**on the exact `return` / `write` / `print` line** with a short `justification:` that states
why the flow is not HTML XSS (e.g. JSON/XML DTO serialization, reverse-proxy pass-through,
or an `XSSValidation.*` sanitizer). Prefer this over bulk path-exclude or mass dismiss:

```java
return siteDataService.save(site); // codeql[java/xss] justification: JSON/XML DTO via Jackson/JAXB; not HTML body
```

**Does not work** (comment not adjacent to the alert line):

```java
// codeql[java/ssrf] long justification...
// more comments...
HttpRequest.Builder b =
      HttpRequest.newBuilder(uri)   // ← alert is often HERE
            .GET();
```

**Does work**:

```java
HttpRequest.Builder b =
      HttpRequest.newBuilder(uri) // codeql[java/ssrf]
            .GET()
            .timeout(Duration.ofSeconds(60));
```

---

## Adding a custom sanitizer model

1. Identify package, type, method name, JVM signature (erased generics).
2. Add a row under `barrierModel` in the right `models/*.model.yml` (or a new file matched by `models/**/*.model.yml`).
3. Use the vulnerability kind CodeQL expects, e.g.:
   - SSRF → `request-forgery`
   - LDAP → `ldap-injection`
   - Path → `path-injection`
   - XXE → `xxe`
4. Bump `version` in `.github/codeql/models/codeql-pack.yml` when models change.
5. Note the model in the PR body and in `suppressions.md` if replacing a path exclude.
6. Prefer **return-value barriers** for methods that return sanitized values; **argument barriers** for void validators that throw on bad input.
7. **Local model packs in GHA:** Do **not** pass `./.github/codeql/models` (with or without `+`) via
   workflow `packs:` — init fails with `is not a valid pack`. Local path packs are not reliably
   loaded by CodeQL Action today; keep models in-repo for documentation and future GHCR publish.
   Until then: runtime sanitizers + tests, sink-line `// codeql[...]`, path `query-filters`, then
   dismiss residual as FP (ladder step 5) with a short reason citing tests.


Example (SSRF):

```yaml
- ["com.percussion.security.validation", "URLValidation", false, "validateURLString", "(String)", "", "ReturnValue", "request-forgery", "manual"]
```

---

## Path `query-filters` (fallback only)

Every exclude in `codeql-config.yml` **must** have a matching `suppressions.md` row with `file_path = .github/codeql/codeql-config.yml` (contracts/C3).

Prefer converting long-lived path excludes into model-pack barriers so new call sites inherit protection without new excludes.

---

## PR / agent checklist (security or CodeQL work)

Copy into the PR body or agent task list:

- [ ] Runtime fix + regression test(s) green under `./mvn-env.sh`
- [ ] Custom sanitizer covered by `.github/codeql/models` (or justified why not)
- [ ] Any `// codeql[...]` is on the **sink line** (not three lines above a multi-line builder)
- [ ] `suppressions.md` updated for path excludes / dismissals / new models
- [ ] Default setup still `not-configured` (`gh api .../default-setup`)
- [ ] Advanced workflow is the one that ran on the PR (check name **CodeQL Advanced** / Analyze jobs with config)
- [ ] CodeQL review threads: inline mitigation reply **and** `resolveReviewThread`
- [ ] No overlapping open PRs re-touching the same sinks without rebase onto the latest security base

---

## Resolving CodeQL review threads

Per root `AGENTS.md`:

1. Reply on the comment with mitigation (commit SHA, what changed, tests).
2. Resolve the GraphQL thread (`resolveReviewThread`).
3. Re-query: all addressed threads `isResolved: true`.

A code-only fix without resolve leaves the merge conversation gate red.

---

## Dismissing an alert (last resort)

```bash
gh api -X PATCH "repos/intersoftdatalabs-in/percussioncms/code-scanning/alerts/<N>" \
  -f state=dismissed \
  -f dismissed_reason="false positive" \
  -f dismissed_comment="FP residual (PR #…, <sha>): <sanitizer> + model/config; tests: <TestClass>. Same as #1682."
```

Comment max **280 characters**. Always add a `suppressions.md` row.

---

## Overlapping security PRs

- Prefer one remediation stack; rebase dependents after the base merges.
- If #N already landed structural SSRF/LDAP fixes, do **not** open #N+1 that only re-documents the same residuals — extend the **model pack** instead.
- After merging a security PR to `development`, re-scan once under advanced setup and update triage; do not expect default-setup-style PR comments anymore.

---

## Metrics (monthly / release)

| Metric | Healthy trend |
|--------|----------------|
| Open critical/high on `development` | Down |
| PR residual reopens (same path, new alert ID) | Near zero |
| Residuals with model pack row | Up |
| Residuals that are dismiss-only | Down |

---

## Related docs

- `specs/004-zero-code-scanning-alerts/` — feature spec, contracts C2/C3, quickstart  
- `suppressions.md` — suppression / path-exclude index  
- `triage.md` — per-alert disposition  
- Root `AGENTS.md` — PR review thread resolve protocol  
