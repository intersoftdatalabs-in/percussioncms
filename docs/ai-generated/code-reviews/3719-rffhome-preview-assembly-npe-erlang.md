# Erlang review — #3719 rffHome Preview assembly NPE + error.jsp

**Scope:** uncommitted branch `fix/issue-3719-rffhome-preview-assembly-npe` vs `origin/main`  
**Change class:** FastForward site-path Preview assembly (legacy rffHome) + assembly error JSP + Playwright companion  
**Memory patterns hit:** behavioral unit tests; incomplete change-class (Playwright + product-docs); CMS `/` URL paths (false-positive guard); missing-template NPE from Validate.notNull

## Summary

H2 FastForward Preview of Corporate Investments Home (rffHome / content id 551) assembled with `perc.base.plain`. `PSAssemblyItemBridge` then `Validate.notNull(templateId)` NPEd (`The validated object is null`). `ui/assembly/error.jsp` imported lang3 `StringEscapeUtils.escapeHtml` (removed) so the error page failed to compile.

This change:

- Detects non-`percPage` items on `/Sites/*` and builds `/assembler/render` with the site default page template.
- Uses the same default-template pick on `PSRenderAssemblyBridge` GET render.
- Replaces the Validate NPE with a clear `IllegalStateException` if percPage dispatcher is still hit without a template id.
- Replaces nav `Validate.notNull(self)` with a RepositoryException.
- Fixes error.jsp to commons-text `escapeHtml4`.
- Playwright + product-docs companions.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs. Behavioral tests cover template pick, assembler URL, request path stripping, percPage template-id guard, navon-null, JSP escape contract, and Playwright HTML-not-NPE helpers. CMS paths correctly use `/` (URL/finder, not OS filesystem). Product docs updated. Playwright live C5 is required before treating the PR as complete (run after this review).

## Issues

None blocking.

### Notes (not gates)

- `PSPreviewItemContent` still uses locators (`PSAssemblyServiceLocator`, `PSSiteManagerLocator`) consistent with the existing servlet.
- Live H2 assembly of rffHome still depends on FastForward default templates being associated with the sample site (fallback is type-level Default page templates).
- Explorer Preview *button* on `main` may still open the editor host until #3718 merges; this slice proves site-path GET assembly independently.
- Preview `sys_context=0` must not mark `IPSAssemblyResult` FAILURE for tracked inline-link warnings (`PSTrackAssemblyError.handleItem`); that was HTTP 500 with assembled HTML as the error body.

## Re-review (C5)

**Gate:** approve. Live H2 after FastForward assembler URL (no dual authtype+filter), site id from folder root, and preview keep-SUCCESS: Playwright `explorer-preview-view.spec.js` **3 passed**. error.jsp compiles (HTTP 500 error page no longer JSP-fails). Site-path GET for Corporate Investments Home is HTTP 200 assembled HTML.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] CMS/URL/finder paths correctly use `/`
- [x] Tests do not assert Unix-only OS path shapes
- [x] Line-ending sensitive assertions: N/A (HTML/URL strings)

## Tests / builds

- `system` clean install: BUILD SUCCESS; Tests run: 2285, Failures: 0; `PSNavHelperTypedTest` 4 passed
- `projects/sitemanage` clean install: BUILD SUCCESS; Tests run: 1327, Failures: 0; new tests 7+3 passed
- `WebUI` clean install: BUILD SUCCESS; Java Tests run: 63, Failures: 0
- `modules/perc-qa-automation` clean install: BUILD SUCCESS; `npm run test:unit` 441 passed

Operator: Grok: night-issue-prs (model grok-4.6)
