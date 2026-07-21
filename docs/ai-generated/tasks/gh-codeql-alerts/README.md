# GH Code Scanning Alerts (CodeQL)

This folder holds the raw alert fetch, the triage inventory, and the per-disposition indexes used by the `004-zero-code-scanning-alerts` feature (`specs/004-zero-code-scanning-alerts/spec.md`).

## Files

| File | Purpose | Owner |
|------|---------|-------|
| **`codeql-pr-playbook.md`** | **Operational playbook** — analyzer of record, default-setup off, model pack, sink-line suppressions, disposition ladder, PR checklist. **Read this before any CodeQL/security PR work.** | Human-edited |
| `alerts.md` | Raw fetch — one section per open alert, produced by `scripts/fetch-gh-code-scanning-alerts.sh`. | Generated |
| `triage.md` | Triage inventory — one row per open alert with disposition, module owner, target action, target milestone. Seeded by the initial scan; refined per-finding by the module owner. | Human-edited |
| `suppressions.md` | Index of inline `// codeql[rule-id]` suppressions for `false-positive` dispositions. See `specs/004-zero-code-scanning-alerts/contracts/README.md` C3. | Human-edited |
| `accepted-risks.md` | Accepted-risk register for findings that cannot be remediated in `8.2`. See `specs/004-zero-code-scanning-alerts/contracts/README.md` C4. | Human-edited |
| `release-readiness-8.2.md` | Per-release sign-off report. See `specs/004-zero-code-scanning-alerts/contracts/README.md` C6. | Generated at sign-off |

## Analyzer of record (do not re-enable default setup / Code Quality)

- **Workflow**: `.github/workflows/codeql.yml` (push + **pull_request** to `development` + schedule + `workflow_dispatch`)
- **Languages**: **Java** + **JavaScript/TypeScript** only
- **Config**: `.github/codeql/codeql-config.yml` (`paths-ignore`, Java `packs`, `query-filters`)
- **Models**: `.github/codeql/models/` (custom Java sanitizer barriers)
- **Default setup**: must remain `not-configured` — verify with  
  `gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state`
- **Code Quality**: must stay **disabled** (Settings → Code quality). Dynamic workflow `dynamic/github-code-scanning/codeql` ignores advanced config and can wipe default-branch alerts.

Agent skill: `modules/ai-shared-develop/src/main/resources/skills/codeql-pr/SKILL.md`

## Initial scan (seeded 2026-07-11)

- **Repository**: `intersoftdatalabs-in/percussioncms`
- **Branch**: `development` (target: `8.2`)
- **Total open alerts**: 866 (38 distinct rules)
- **By severity**: 13 critical, 535 high, 318 medium
- **By language**: predominantly JS (`js/*` — WebUI/JSP) + Java (`java/*` — server)

### Top rules (long-tail concentration)

| Rule | Severity | Count |
|------|----------|-------|
| `js/xss-through-dom` | high | 168 |
| `js/incomplete-sanitization` | high | 164 |
| `js/html-constructed-from-input` | medium | 96 |
| `js/unsafe-jquery-plugin` | medium | 84 |
| `java/path-injection` | high | 58 |
| `js/functionality-from-untrusted-source` | medium | 55 |
| `java/xss` | high | 35 |
| `js/prototype-pollution-utility` | medium | 32 |
| `js/incomplete-multi-character-sanitization` | high | 27 |
| `js/xss` | high | 23 |

The top 10 rules account for 742 of 866 alerts (~86%).

### Triage seed disposition counts

| Disposition | Count | Primary target module |
|-------------|-------|-----------------------|
| `obsolete` | 485 | `WebUI/` (vendored 3rd-party: knockout, bootstrap, jquery, dojo, requirejs, less, highlight, datatables, qunit, jstree, trinidad, adf, debug builds) |
| `valid` | 380 | `system/`, `projects/sitemanage/`, `modules/perc-packages/`, `modules/perc-toolkit/`, `deliverytiersuite/.../p13n-ds/`, etc. |
| `false-positive` | 1 | `deliverytiersuite/.../feeds/` (test perf cast) |
| `accepted-risk` | 0 | — |

### Implication for the `0 active alerts for 8.2` goal

- **485 obsolete**: removing vendored 3rd-party files is the highest-leverage work item and is the cheapest path to a large reduction.
- **380 valid**: requires per-finding engineering; 13 critical findings are all in Java (`java/ssrf`, `js/code-injection`, `java/xxe`, `java/ldap-injection`) — these MUST be addressed before any release can be signed off.
- **1 false-positive**: minimal noise.
- **Realistic 8.2 outcome**: PASS-WITH-EXCEPTIONS if any `accepted-risk` filings remain; otherwise PASS if the obsolete + valid work completes.

## Refresh procedure

```bash
# Re-fetch raw alerts
scripts/fetch-gh-code-scanning-alerts.sh intersoftdatalabs-in/percussioncms

# Refresh triage disposition counts (only after editing triage.md)
awk -F'|' '/^\| [0-9]+ / {print $7}' docs/ai-generated/tasks/gh-codeql-alerts/triage.md | sort | uniq -c
```

See `specs/004-zero-code-scanning-alerts/quickstart.md` for the end-to-end triage / mitigation / release-readiness workflow.
