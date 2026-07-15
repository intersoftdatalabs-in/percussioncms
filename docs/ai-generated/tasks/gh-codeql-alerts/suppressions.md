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
| 1724 | `js/xss-through-dom` | `.github/codeql/codeql-config.yml` | n/a | Path-level `query-filters` exclusion for `WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js` (T044 / PR #1218). The sanitizer inside `window.PercPageEditSanitizer.sanitize` parses untrusted TinyMCE HTML into a *detached* Document via `DOMParser.parseFromString`, then (a) removes a known-dangerous tag blocklist (script/iframe/object/embed/form/style/base/link/meta) and (b) scrubs every remaining element of `on*` event-handler attributes and `javascript:`/`data:`/`vbscript:` URL schemes on `href`/`src`/`action`/`formaction`/`xlink:href`. Surviving children reach the live document through `document.importNode` + `appendChild` only -- never `innerHTML` or jQuery `.html()`. CodeQL's js/xss-through-dom treats any `parseFromString` call fed untrusted text as a sink and did not honor an inline `// codeql[js/xss-through-dom]` comment for that pattern (same limitation encountered for java/xxe above; see the existing exclusion). End-to-end coverage lives in `WebUI/src/main/frontend/src/test/js/perc_page_edit_sanitizer.test.js` (31 tests). | 2026-07-15 | vijaya-boddipudi | T044 PR #1218 |

