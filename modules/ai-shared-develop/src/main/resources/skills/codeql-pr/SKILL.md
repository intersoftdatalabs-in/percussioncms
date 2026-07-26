---
name: codeql-pr
description: >-
  Stop CodeQL residual thrashing on Percussion CMS PRs. Use when working
  code-scanning alerts, security advisories, SSRF/LDAP/path/XXE fixes,
  // codeql suppressions, query-filters, model packs, or PR checks named
  CodeQL / Analyze (java-kotlin).
---

# CodeQL PR skill (analyzer of record)

## Read first

`docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md`

## Hard rules

1. **Analyzer of record** is **CodeQL Advanced** (`.github/workflows/codeql.yml` + `codeql-config.yml` + `.github/codeql/models`). Scope: **Java + JavaScript/TypeScript only**.
2. **Default CodeQL setup** must stay `not-configured`. **GitHub Code Quality** (dynamic `Code Quality: CodeQL Setup`) must stay **disabled** (Settings → Code quality) — it ignores advanced config and can mass-close alerts.
3. **Disposition ladder**: runtime fix + test → model pack barrier → sink-line `// codeql[rule-id]` → path `query-filters` → dismiss last.
4. **Sink-line only**: put `// codeql[...]` on the alert line or the single line immediately above a one-line sink. Multi-line builders with a comment three lines up **fail**.
5. **Do not** open dismiss-only PRs. **Do not** re-enable default CodeQL setup or Code Quality without the same config/models.
6. After addressing a CodeQL review comment: **reply with mitigation (commit SHA)** then **`resolveReviewThread`** (root `AGENTS.md`).

## Verify default setup off

```bash
gh api repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup --jq .state
# expected: not-configured
```

If configured:

```bash
gh api --method PATCH repos/intersoftdatalabs-in/percussioncms/code-scanning/default-setup \
  -f state=not-configured
```

## When adding a sanitizer

| Kind | Model kind string |              Example type              |
|------|-------------------|----------------------------------------|
| SSRF | `request-forgery` | `URLValidation`                        |
| LDAP | `ldap-injection`  | `PSJndiUtils.escapeLdapFilter`         |
| Path | `path-injection`  | `PSPathInjectionGuard`                 |
| XXE  | `xxe`             | `PSSecureXMLUtils.getSecuredSaxSource` |

Edit `.github/codeql/models/models/*.model.yml`, bump pack `version` in `codeql-pack.yml`, document in `suppressions.md` if replacing a path exclude.

## PR body checklist

- [ ] Runtime fix + `./mvn-env.sh` tests green
- [ ] Model pack updated (or path exclude + suppressions.md row justified)
- [ ] Sink-line suppressions only
- [ ] Default setup still `not-configured`
- [ ] CodeQL threads replied + resolved

## Full playbook

See `docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md` for architecture, metrics, and dismiss API details.
