# Plan: Native-Language Locale Dropdown + Dynamic i18n Re-Apply on the Login Screen

## Goal

Two related UX improvements to the product front-door login (React `LoginPage` hosted by `WebUI/src/main/webapp/rxlogin.jsp`):

1. **Locale dropdown labels in the locale's native language.** Each `<option>` renders as `"<code> - <Native Locale Name>"`, where the native name comes from `Intl.DisplayNames(code, { viewerLocale })` and the viewer is the **currently selected** locale in the dropdown. So a French viewer sees `fr-fr - Français (France)`; selecting `ja-jp` in the dropdown causes all option labels (and form chrome) to re-render in Japanese on the next render pass.
2. **Selecting a different locale re-applies i18n to the login form in place.** When the user changes the `<select>`, fetch the TMX bundle for the new locale (`/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=<new>`), update `window.I18N`, bump a React state key to force re-render, and update `document.documentElement.lang`. The form POST/redirect flow to `/login` is preserved unchanged.

Out of scope: layout, brand, server-side locale registration, anything outside `WebUI/src/main/ts/login/**` + `rxlogin.jsp` + the consumer of `message()` for chrome strings.

## Decisions Locked

- **Strategy:** client-side `Intl.DisplayNames` for option labels **plus** dynamic `tmx.jsp` load for chrome. No Java changes to `PSLocale` or its DB-backed `m_displayName`.
- **Label format:** `<code> - <Native Name>` where `<Native Name>` is `Intl.DisplayNames.of(code)` shown in the **currently selected** viewer locale. Region is not appended unless the option code is already regional (`fr-fr`); generic codes (`es`, `hi`) render as the language name.
- **Re-localization in place:** yes — no round-trip required to see chrome in the new language. Submission still goes to the server and the server's session locale is unchanged behavior.
- **Bundles / keys:** reuse existing `perc.ui.*` keys via TMX. New chrome keys (so `message()` resolves for login) go in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`.
- **Module ownership:** changes are confined to `WebUI` + `modules/perc-i18n` (TMX additions only). No `system` Java changes, no DB schema change, no `RXLOCALE` data change.

## Non-Goals

- Changing the server's `PSLocale.getDisplayName()` value (DB-driven, English-only by design).
- Adding or removing locales from the 17-locale matrix in `modules/perc-i18n/AGENTS.md §1a`.
- Localizing any legacy/rxlogin-classic pages (removed in PR-8 per the Jira-history note in `rxlogin.jsp:11`).
- Translating option labels themselves via TMX — they are intrinsically locale-of-the-option.

## Affected Files

|                          File                          |                                                                                                                                                                Change                                                                                                                                                                |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/webapp/rxlogin.jsp`                    | Emit initial TMX script tag (mirrors `cm/app/spa.jsp:123`) before `perc-modern-ui.js`. Set `<html lang>` to the **full** session locale tag (no `split("-")[0]`). No other JSP changes.                                                                                                                                              |
| `WebUI/src/main/ts/login/i18n.ts` *(new)*              | Tiny wrapper `t(key, args?)` that reads `window.I18N` fresh each call (no stale closures) and falls back to `fallbackLabelFromKey` (reuse from `WebUI/src/main/ts/i18n/message.ts:37`).                                                                                                                                              |
| `WebUI/src/main/ts/login/localeLabels.ts` *(new)*      | Pure module. Module-level `viewerCache: Map<string, Intl.DisplayNames>`. Exports `normalizeTag(code)` and `localeLabel(code, viewer, fallback)`. Handles generic (`es`) and regional (`es-mx`) codes.                                                                                                                                |
| `WebUI/src/main/ts/login/tmxLoader.ts` *(new)*         | Module-level `loaded: Set<string>` + `inFlight: Map<string, Promise<void>>`. Exports `ensureTmxLoaded(locale, baseHref?)`. Tag stays in DOM after `load` for browser HTTP cache.                                                                                                                                                     |
| `WebUI/src/main/ts/login/LoginPage.tsx`                | (a) Replace hardcoded chrome with `t(...)`. (b) Replace `{loc.displayName}` with `{localeLabel(loc.name, locale, loc.displayName)}`. (c) `onChange` calls `ensureTmxLoaded` then bumps `tmxReady` and sets `document.documentElement.lang`. Inner card body is re-keyed by `\`${locale}-${tmxReady}\`` to preserve form input state. |
| `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`  | Six new `<tu>` units under `perc.ui.login.modern@<Key>` (English source). Optional for this iteration — `t()` falls back to `@`-suffix text without them.                                                                                                                                                                            |
| `WebUI/src/test/ts/login/LoginPage.test.tsx`           | Extend with: (a) option labels reflect current viewer locale, (b) changing the dropdown triggers `tmx.jsp` load + rerender with localized chrome, (c) `Intl.DisplayNames` absent → server `displayName` fallback, (d) `t(...)` resolves via stubbed `window.I18N`. Existing `toMatch(/Sign in/i)` continues to pass via fallback.    |
| `WebUI/src/test/ts/login/localeLabels.test.ts` *(new)* | Unit tests: normalization (`EN_US`→`en-us`, `fr-FR`→`fr-fr`), generic vs regional, unknown code fallback, cache reuse across calls.                                                                                                                                                                                                  |
| `WebUI/src/test/ts/login/tmxLoader.test.ts` *(new)*    | Stub `<script>` injection via jsdom `appendChild` spies; assert URL contains `sys_lang=<viewer>` and `prefix=perc.ui.`, deduplication, reject on `onerror`.                                                                                                                                                                          |
| `WebUI/src/test/ts/login/loginStylesContract.test.ts`  | Already asserts `tmx/tmx.jsp` is referenced (line 26–27); extend to also assert `prefix=perc.ui.`.                                                                                                                                                                                                                                   |

## Implementation Steps (ordered)

1. **Initial TMX load on the host page** (`rxlogin.jsp`). Add a `<script src="<%= request.getContextPath() %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>` before the modern bundle, matching `cm/app/spa.jsp:122-123`. Set `<html lang>` (line 102) to `<%= locale %>` (the full tag), not `lang.split("-")[0]`. **No** new `<script type="module">` here — TMX is a classic script per the existing pattern.
2. **New helper `i18n.ts` (under `WebUI/src/main/ts/login/`).** Export `t(key, args?)` that:
   - Reads `window.I18N` fresh each call (no stale closure).
   - If `window.I18N.message` returns a non-empty string different from the key, returns it.
   - Otherwise returns the English text after `@` via `fallbackLabelFromKey` from `WebUI/src/main/ts/i18n/message.ts:37`.
3. **New helper `localeLabels.ts`.** Pure module with a **module-level** `viewerCache: Map<string, Intl.DisplayNames>` (declared at top, not inside the component, so it's shared across renders and won't be GC'd). Exports:
   - `normalizeTag(code: string): string` — `trim()`, lowercase, `_` → `-`. Mirrors `PSTmxResourceBundle.normalizeLang`.
   - `localeLabel(code: string, viewer: string, fallback: string): string` — returns `` `${normalizeTag(code)} - ${nativeDisplayNames(code, viewer)}` `` where `nativeDisplayNames` either returns the language name (generic codes) or `language (region)` for regional codes (region via `Intl.DisplayNames({ type: "region" })`). If `Intl.DisplayNames` is absent or throws, return `fallback`.
4. **New helper `tmxLoader.ts`.** Module-level state: `loaded: Set<string>` + `inFlight: Map<string, Promise<void>>`. Exports `ensureTmxLoaded(locale: string, baseHref?: string): Promise<void>`:
   - `loaded.has(locale)` → return `Promise.resolve()`.
   - `inFlight.get(locale)` → return that promise.
   - Else build URL `${baseHref ?? "/tmx/tmx.jsp"}?mode=js&prefix=perc.ui.&sys_lang=${encodeURIComponent(normalizeTag(locale))}`.
   - Append one `<script data-perc-tmx-locale="${locale}" src="${url}">` to `document.head` and await `load`. On success: `loaded.add(locale)` and resolve. On `error`: remove tag, **do not mark loaded**, reject. Do not remove on success (browser HTTP cache handles repeat selections).
5. **Wire it up in `LoginPage.tsx`.**
   - Add `useState` `tmxReady` (number, default 0).
   - In `onChange(e)`:

     ```ts
     const next = e.target.value;
     setLocale(next);
     ensureTmxLoaded(next)
       .then(() => {
         document.documentElement.lang = next;
         setTmxReady((n) => n + 1);
       })
       .catch(() => { /* keep server fallback; t() resolves to "@<English>" */ });
     ```
   - Replace hardcoded chrome (`Sign in`, `User name`, `Password`, `Locale`, `Use legacy UI`, `Login`) with `t(...)` calls using keys listed in step 6.
   - Replace `{loc.displayName}` with `{localeLabel(loc.name, locale, loc.displayName)}` so labels update live as the user scrubs.
   - Wrap the **inner card body** (not the `<form>`) in a `key={`${locale}-${tmxReady}`}` so only chrome and option-label children re-render — username, password, CSRF hidden inputs, checkbox, and submit button are preserved.
6. **Add TMX units** (`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`). Six English-source `<tu>` entries with `tuid="perc.ui.login.modern@<Key>"`:
   - `Sign in`
   - `User name`
   - `Password`
   - `Locale`
   - `Use legacy UI`
   - `Login`
     Translations for all 17 locales in the canonical matrix are generated by `modules/perc-i18n/scripts/i18n_translate.py --target <code>` (per `perc-i18n/AGENTS.md §2a`). The dropdown label format (`<code> - <Native>`) is **not** in TMX — it's a runtime composition of two `Intl.DisplayNames` calls.
7. **Tests.** See the Affected Files table. Use `vi.stubGlobal`, `Object.defineProperty(window, 'I18N', { configurable: true, value: ... })` for TMX stubs, and `vi.spyOn(document, 'createElement')` if needed for the loader test. The existing `LoginPage.test.tsx:43` assertion `toMatch(/Sign in/i)` keeps working because `t()` falls back to English text.
8. **Validation (per WebUI AGENTS + root AGENTS pre-PR gate):**
   - Frontend tests: `cd WebUI/src/main/frontend && npm run test -- --run login`.
   - Build: `cd WebUI/src/main/frontend && npm run build:modern` (must succeed, no new warnings).
   - WebUI standalone: `cd WebUI && ../mvn-env.sh clean install`.
   - If TMX units added: `cd modules/perc-i18n && ../../mvn-env.sh clean install` then `cd modules/perc-distribution-tree && ../../mvn-env.sh clean install` (so the catalog gets repackaged).
   - Manual smoke (post-deploy): visit `/Rhythmyx/rxlogin.jsp`, change browser locale to `fr-fr`, change dropdown from `en-us` to `fr-fr` → labels and chrome update immediately to French without a page reload.
   - Pre-commit: Erlang review on authored diffs (`.kilo/rules/pre-commit-review.md` + workflow `/erlang-review`). Include as a hard gate before pushing the PR.

## Risks & Edge Cases

- **TMX endpoint requires session.** `/tmx/tmx.jsp` reads `request.getParameter("sys_lang")` and falls back to `PSRoleUtilities.getUserCurrentLocale()` (lines 57–69). Anonymous users on the login screen have no session; passing `sys_lang=<code>` in the query is sufficient. If the bundle has no entries for that locale (some custom `RXLOCALE` rows may lack translations), the response is 200 with no matches and `window.I18N.message` echoes the catalog key, which our `t()` already handles by returning the English text after `@`.
- **`Intl.DisplayNames` browser support.** Modern Chromium/Firefox/Safari and recent jsdom support it; guard with `typeof Intl !== "undefined" && Intl.DisplayNames`. Fallback is the server-provided `displayName`.
- **Generic vs regional codes.** Both `es` and `es-mx` may exist in `RXLOCALE`; the label function handles each (generic → language name only; regional → language + region).
- **Locale tag normalization must mirror `PSTmxResourceBundle.normalizeLang`** (lowercase, `_`→`-`) — done in `localeLabels.ts` and `tmxLoader.ts` via a single shared `normalizeTag()` helper to avoid drift.
- **Race conditions on rapid dropdown scrubbing.** Multiple `tmx.jsp` fetches may be in flight. The loader dedupes via a `data-perc-tmx-locale` marker and a module-level `Map<locale, Promise<void>>` so concurrent calls share the same promise. We **do not** remove the injected `<script>` after `load` (the script may carry state) — we mark it loaded and rely on browser HTTP cache for repeat selections.
- **Preserving user input across re-render.** Use `tmxReady` to re-key the **inner card body** (not the `<form>` or `<input>` elements) so typed username/password survive dropdown changes.
- **CSRF safety.** Only `j_locale`, `j_username`, `j_password` are submitted; locale-change never invalidates CSRF.
- **No jQuery introduced** in `WebUI/src/main/ts/**` (per product lock #2). No new `import "jquery"` and no `window.$` usage.
- **Cross-platform paths / line endings:** no filesystem changes; nothing to harden on this axis.
- **Java / DB unchanged.** No `system/**` Java edits, no `PSLocale` schema change, no `RXLOCALE` data change. Diff is purely TypeScript in `WebUI/src/main/ts/login/**` and one JSP (`rxlogin.jsp`) plus optional TMX unit additions.

## Validation Evidence (required in PR body per `AGENTS.md`)

- `cd WebUI && ../mvn-env.sh clean install` → BUILD SUCCESS, `Tests run: N, Failures: 0`.
- `cd WebUI/src/main/frontend && npm run test -- --run login` → all login suites pass.
- If TMX units added: `cd modules/perc-i18n && ../../mvn-env.sh clean install` plus `cd modules/perc-distribution-tree && ../../mvn-env.sh clean install` → BUILD SUCCESS, test counts.
- Confirmation that no new compiler/surefire/enforcer/Spotless warnings appear in changed modules.
- Manual smoke result confirming the dropdown re-localizes in place.

## Out of Scope

- Adding new locales (the 17-locale matrix already exists in `perc-i18n`).
- Replacing the server `RXLOCALE.displayName` column.
- Localizing the legacy `/Rhythmyx/login`/`rxlogin-classic.jsp` paths (removed in PR-8).
- A11y announcements when locale changes (separate polish task).

