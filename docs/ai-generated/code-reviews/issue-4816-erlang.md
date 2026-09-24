# Erlang review — #4816 EditorHost translation variant

**Branch:** `fix/issue-4816-editor-translation-variant`  
**Base:** `origin/main` (`git diff origin/main...HEAD`)  
**HEAD:** `940ff7d84e` — `fix(editor): open a translation variant from EditorHost (#4816)`  
**Date:** 2026-09-24  
**Persona:** erlang 0.1.1  
**Status:** cli-unavailable, manual review (task instruction: do not run `mkd-code-review`)  
**Independence:** reviewer is not the implementer of this slice.

Issue #4816: EditorHost lists translation locales, opens one variant, or creates one locale and opens it. Errors must not blank the host.

## Summary

EditorHost mounts the existing Explorer `TranslationsPanel` when an item is open (`contentId` set, not promote). Open-variant updates `contentId` in the search params (keeps view vs edit). Create-variant navigates only when the POST returns exactly one new `contentId`, then forces `mode=edit`. Panel 403/409 handling stays inside `TranslationsPanel`; the host root (`data-testid="editor-host"`) remains mounted.

Behavioral coverage matches the ticket: Vitest list/open, create-one-and-open, and auth/conflict without blanking; Playwright companions for the same three paths. Product-docs `8.2/admin/content-explorer.md` records the host surface.

No blocking bugs, missing behavioral tests, or non-portable filesystem paths in this diff.

## Scope

| Item | Value |
|------|--------|
| Files | 5 (WebUI host + Vitest, perc-qa-automation spec + helper, product-docs) |
| Stat | +464 / −1 |
| Prior report | none for #4816 (`docs/ai-generated/code-reviews/` has no `issue-4816*` / `4816-*`) |
| Related memory | #3545 / #3703 Translations GUID reviews (panel GET/POST contract; this slice reuses the panel) |
| Uncommitted product edits | none (review file is the only write) |
| Rule / AGENTS files | none |

Memory patterns hit: missing behavioral tests (covered); WebUI screen companions (Vitest + Playwright + product-docs); empty-catch / user-facing swallow (panel maps 403/409 to panel state); URL `/` is protocol form, not OS path.

## Recommendation

**approve**

## Gate

**PASS**

May commit/push: yes

## Change-class closure

Change class: **WebUI product screen (React Content Editor host — translation variants).**

| Companion | Status |
|-----------|--------|
| Production: `EditorHost` mounts `TranslationsPanel` + search-param navigation | present (`EditorHost.tsx` ~2718–2749) |
| Test seams on `EditorHostProps` | present (`loadTranslationVariants` / `loadTranslationLocales` / `createTranslationVariants`) |
| Vitest host behavior | present (`WebUI/src/test/ts/editor/EditorHostTranslations.test.tsx`) |
| Playwright screen spec | present (`modules/perc-qa-automation/frontend/tests/editor-host-translations.spec.js`) |
| product-docs 8.2 admin | present (`product-docs/8.2/admin/content-explorer.md` Edit/View row) |
| New chrome strings | none — panel already uses `EXPLORER_MSG` |
| REST / sitemanage adaptor | N/A — reuses existing translations façade |
| Copyright on new sources | Intersoft Data Labs 2026 + Apache 2.0 |

Playwright a11y helper (`expectNoSeriousA11yViolations`) is present on peer `editor-host-*.spec.js` files and is absent here. That is convention drift, not a missing #4816 behavior test. See suggestion below.

## Cross-platform path review

No filesystem path construction, temp roots, or OS path assertions.

- REST/SPA URLs correctly use `/` (`/Rhythmyx/rest/content-explorer/translations`, `spa.jsp?entry=editor`).
- Helper `editorSpaUrl` joins a URL root with `/Rhythmyx/cm/app/spa.jsp` (protocol path).
- Playwright glob `**/rest/content-explorer/translations/**` is a URL matcher.

**Outcome: clean.**

## Tests (behavioral)

Issue acceptance is exercised, not only token-grepped:

1. **List + open** — variants row `900` → `editor-content-id` matches 900; `loadFields("900")`.
2. **Create one + open** — locale `de-de` POST `{ itemIds: [42], locales: ["de-de"] }` → host shows 901; `loadFields("901")`.
3. **Errors do not blank the host** — `TranslationAuthError` → panel `data-testid-state="auth"` with `editor-host` still present; `TranslationConflictError` → `translations-create-error` with field form still present. Playwright 403 GET + 409 POST keep `editor-host` visible.

`TranslationsPanel` already catches load/create failures (403/404/409/generic) and renders panel error/auth regions. Host wiring does not wrap those throws in a parent catch; it does not need to — the panel never rethrows into render.

## Issues

None blocking.

### suggestion — Playwright a11y peer gap

`modules/perc-qa-automation/frontend/tests/editor-host-translations.spec.js` (describe ~170)

Peer editor-host specs import `expectNoSeriousA11yViolations` and run it against `[data-testid="editor-host"]` after the happy path. This spec covers the ticket flows and a console-error watch, and skips that a11y call.

**Suggestion:** add the same helper after the host is visible (open-variant or create-one test). Do not treat as a #4816 functional hole.

### suggestion — create-many stays on the source item

`WebUI/src/main/ts/editor/EditorHost.tsx:2734-2738`

`onCreated` returns without navigating when `created.length !== 1`. That matches the issue (“creates **one** locale and opens it”) and the product-docs sentence. Selecting two locales still POSTs (panel allows a set) and then leaves the source editor open.

**Suggestion:** if operators should land on the first created copy when several succeed, take `created[0]` regardless of length. Otherwise the current guard is the documented contract.

### nit — Playwright 403 and 409 share one test

`editor-host-translations.spec.js:194-212` unroutes, re-stubs, and `goto`s again in the same case. Isolation is weaker than two tests. Not a false-green: both asserts still run.

### nit — `variantsFor` uses `endsWith("900"|"901")`

A stubbed id `1901` would take the 901 branch. Fine for this fixture set.

## Notes (non-blocking)

- Panel is shown in **view** as well as **edit** (`contentId != null && !promote`). Open preserves `mode=view`; create forces `edit`. Reasonable for a new copy.
- `itemLabel` is omitted (Explorer passes name/title). Chrome title stays generic.
- Host does not add an error boundary; panel load/create errors are stateful, not render throws.
- Live H2 Playwright was not executed in this review (static diff review only).

## Handoff

Gate is PASS for the committed slice. Reviewer did not implement fixes, commit, or push.
