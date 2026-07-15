# CodeQL Suppression Index — intersoftdatalabs-in/percussioncms

**Branch**: `004-zero-code-scanning-alerts`
**Generated**: 2026-07-11 (initially empty; rows added per US4 closure)

Schema follows `specs/004-zero-code-scanning-alerts/contracts/README.md` C3. Every
inline `// codeql[rule-id]` suppression applied under this feature MUST have
exactly one row here, with `justification` matching the inline comment
verbatim. Path-level exclusions (`.github/codeql/codeql-config.yml`
`paths-ignore` / `query-filter` additions) MUST also have a row here with
`file_path = .github/codeql/codeql-config.yml`.

| alert_id | rule_id | file_path | line | justification | applied_on | applied_by | review_by |
|----------|---------|-----------|------|---------------|------------|------------|-----------|
| 1724 | `js/xss-through-dom` | `WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js` | 609 | `parseFromString(html, "text/html")` parses untrusted TinyMCE HTML into a detached Document; the body is consumed only via `stripDangerousElements` + `stripDangerousAttributes` (script/iframe/object/embed/form/style/base/link/meta removed, `on*` and `javascript:`/`data:`/`vbscript:` URL attributes stripped), then surviving children are imported into the host document via `document.importNode` + `appendChild` -- never via `innerHTML` / jQuery `.html()`. Covered by `WebUI/src/main/frontend/src/test/js/perc_page_edit_sanitizer.test.js`. | 2026-07-15 | vijaya-boddipudi | T044 PR #1218 |

