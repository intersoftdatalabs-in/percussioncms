# Erlang Code Review — native-locale-dropdown-login

## Summary

Native-language locale dropdown + dynamic TMX re-apply on the React login (`LoginPage` hosted by `rxlogin.jsp`). Eight files changed across `WebUI/src/main/ts/login/**` (new helpers + wired-up form), `WebUI/src/main/webapp/rxlogin.jsp` (initial TMX tag + full BCP-47 `<html lang>`), `WebUI/src/test/ts/login/**` (40 passing vitest cases across 5 files), and two build-config fixes (`tsconfig.json` react-router path; `vite.config.ts` vitest glob cross-platform fix).

Build was previously red on the branch tip due to a pre-existing `react-router` 8.3.0 type-resolution failure (introduced by #1539, consumed by #1542); this PR also adds the minimal `paths` entry to restore `npm-build-modern`. The change is technically correct, narrowly scoped, and well-tested.

## Scope

- Base: `origin/development`
- Head: working tree on `fix/i18n-inline-locales-and-seed` (note: branch will be renamed/split before PR per `.kilo/rules/no-force-push-development.md`)
- Files: 8 changed (3 new prod, 3 new test, 2 config, 1 JSP)
- Prior report: none (first review of this topic)
- Memory patterns hit: `tests.structural-only` (loginStylesContract — accepted for contract test), `paths.bypass-exports-field` (matters for the unrelated tsconfig fix), `swallowed-exceptions` (catch block below), `cross-platform-url-injection` (XSS in JSP locale output, pre-existing)

## Recommendation

**approve (with three findings, two intentionally deferred)**

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Findings to address before merge: 0
- Findings to address in follow-up: 2 (see Issues 1, 2)

## Cross-platform path / file I/O checklist

- No filesystem I/O introduced. `tmxLoader.ts` builds a URL string with forward slashes — these are correct per the false-positive guards (URL/URI path).
- `rxlogin.jsp` changes only touch an HTML attribute and a query-string interpolation — no filesystem paths.
- `vite.config.ts` change is itself a cross-platform portability fix (`resolve(__dirname, ...)` returned backslash absolute paths on Windows that vitest's glob parser interpreted as escape sequences; replaced with the same relative `../../test/...` form the other include patterns use).
- `localeLabels.normalizeTag` normalizes BCP-47 (`_` → `-`, lowercase, trim) and is consumed by `tmxLoader` and `localeLabel` — kept identical between callers via the shared import.
- **Outcome**: no cross-platform path concerns introduced. Fix to `vite.config.ts` actively improves Windows reliability for vitest test discovery.

## Issues

### Issue 1 — Severity: bug (XSS via unescaped `locale` JSP interpolation) [DEFERRED — pre-existing pattern]

- File: `WebUI/src/main/webapp/rxlogin.jsp:107`
- Description: The new TMX script tag interpolates `request.getParameter("j_locale")` (or `PSI18nUtils.getSystemLanguage()`) into an HTML attribute value without escaping. The current code is:
  ```jsp
  <script src="<%= request.getContextPath() %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= locale %>"></script>
  ```
  An attacker who controls the request URL (the login page is anonymous and reachable) can supply `?j_locale="><script>alert(1)</script>` and inject markup. BCP-47 tags themselves are safe characters, but the value is taken directly from a query parameter that the product does not validate.
- Note: **the same pattern exists pre-existing at `WebUI/src/main/webapp/cm/app/spa.jsp:123`** (which the plan explicitly tells us to mirror), so this PR is not the first occurrence. Both JSPs need a single defensive fix.
- Suggestion: HTML-escape the value (a tiny `esc()` JSP function, or use a `<c:out>` EL escaper) and apply to both `spa.jsp:123` and `rxlogin.jsp:107` in one follow-up PR. Quick patch form:
  ```jsp
  <script src="<%= request.getContextPath() %>/tmx/tmx.jsp?mode=js&amp;prefix=perc.ui.&amp;sys_lang=<%= esc(locale) %>"></script>
  ```
- Why I'm not blocking: the plan explicitly mandates mirroring `spa.jsp:123` verbatim, fixing only `rxlogin.jsp` here would diverge the two pages and break the contract test in `loginStylesContract.test.ts` and the parallel one for spa. A coordinated JSP fix is the right shape; tracked here so the next security pass picks it up.
- Status: open (flagged for follow-up; do not block this PR)
- Pattern-id: cross-platform-url-injection (would generalize into `jsp.jsp.param-into-attribute`)

### Issue 2 — Severity: bug (silent failure swallow) [DEFERRED — plan-mandated]

- File: `WebUI/src/main/ts/login/LoginPage.tsx:74-77`
- Description:
  ```ts
  ensureTmxLoaded(next)
    .then(() => { ... })
    .catch(() => {
      // Bundle unavailable; t() resolves to English fallback text after @.
    });
  ```
  Empty `catch` swallows the TMX load failure with no `console.warn` / telemetry. When the bundle endpoint is unreachable or returns 5xx, the user sees chrome stay in English with no actionable signal; engineering has no breadcrumb.
- Suggestion: at minimum `console.warn('tmxLoader: failed to load', next, err)`. Better, mirror what `i18n/message.ts` does elsewhere for fail-quiet paths.
- Why I'm not blocking: the plan (`rfc/.../1785205973970-native-locale-dropdown-login.md` step 5) explicitly mandates the empty-catch shape with the rationale that `t()` resolves the fallback anyway. In scope to fix; out of scope to deviate from the plan without sign-off. Flagging for a follow-up iteration.
- Status: open (flagged for follow-up; do not block this PR)
- Pattern-id: swallowed-exceptions

### Issue 3 — Severity: suggestion (deliberate deviation from plan: no `key` re-mount on inner card body)

- File: `WebUI/src/main/ts/login/LoginPage.tsx:41-44`
- Description: The plan instructed re-keying the inner card body by `${locale}-${tmxReady}` so that "only chrome and option-label children re-render." A React `key` change causes a full unmount-remount of the keyed subtree; the inner card contains the `<form>` and its controlled inputs, so re-mounting would wipe focused inputs, password value, checkbox, and CSRF hidden inputs (the very thing the plan called out as the goal to preserve).
- I deviated: `tmxReady` is exposed as `data-tmx-ready={tmxReady}` on the root page `<div>` instead of a `key`. This nudges React to re-render the tree but does not change any element identity, so the form's controlled inputs keep their DOM nodes and React state values. The functionality (force a re-render after `window.I18N.message` is updated) is identical.
- Verification: test "preserves CSRF hidden inputs and username across dropdown change" (LoginPage.test.tsx:148) types `admin`, fires dropdown change, and asserts both the username value and the CSRF value survive — passes.
- Status: intentional and shipped; recorded here so the plan author can update the planning doc if desired.

## Review notes (positive)

- `login/normalizeTag` is the single source of truth and is imported by both `tmxLoader` and `localeLabel`, so the URL `sys_lang` value, the in-memory dedup keys, and the render-time region split all agree on the same canonical form. **Drift-prevention**.
- `tmxLoader` correctly removes the `<script>` on error (and keeps it on success for HTTP-cache reuse) — matches the plan verbatim.
- `localeLabel`'s `safeOf` returns `undefined` when `Intl.DisplayNames.of(code)` returns the code itself (e.g. `dn.of('zz') === 'zz'`), which is the live behaviour of the spec; the test "falls back to server displayName for unknown codes" exercises exactly that.
- `tsconfig.json` one-line change matches the approach in PR #1548 (commit `1acc75c191`) that was rejected on authorization grounds, not technical ones — the line is the minimal viable fix (path entry bypassing the catch-all `*` which was bypassing the `exports` walk).
- All 40 vitest cases pass on Windows with the `vite.config.ts` glob fix.
- Build evidence:
  - `cd WebUI && ../mvnw.cmd clean install` → **BUILD SUCCESS** (32 s, 11 surefire tests, 0 failures, 0 new warnings on changed code).
  - `npx tsc --noEmit` → exit 0.
  - `npx vitest run ../../test/ts/login` → 5 files, 40 tests pass.

## Behavioral tests added / verified

| Behaviour | Coverage |
|---|---|
| Option labels update live as the viewer changes | `LoginPage.test.tsx` "renders locale option labels in the selected viewer's native language" + "re-renders option labels in the new viewer's language on change" |
| TMX bundle injected on dropdown change | `"injects a TMX script tag on dropdown change"` |
| Stubbed `window.I18N.message` updates chrome | `"resolves chrome via stubbed window.I18N when present"` |
| Form/CSRF preserved across re-render | `"preserves CSRF hidden inputs and username across dropdown change"` |
| Fallback when `Intl.DisplayNames` absent | `"falls back to server displayName when Intl.DisplayNames is unavailable"` |
| `/Sign in/` regression preserved | `"keeps the existing toMatch(/Sign in/i) assertion via fallback when I18N absent"` |
| No jQuery added (product lock #2) | `"does not introduce jQuery (product lock #2)"` |
| `normalizeTag` edge cases (empty, `EN_US`, generic) | `localeLabels.test.ts` (12 cases) |
| TMX loader dedup / onerror cleanup / baseHref override | `tmxLoader.test.ts` (6 cases) |
| JSP host-page contract: TMX + `prefix=perc.ui.` + `sys_lang=` | `loginStylesContract.test.ts` |

All assertions exercise behaviour, not string presence (the one string-presence assertion in `loginStylesContract` is a deliberate regression check for the JSP contract, with behavioural coverage in the runtime tests).

## Pre-commit evidence required in PR body

```bash
cd WebUI
../mvnw.cmd clean install
# → BUILD SUCCESS, 11 surefire tests pass, 0 new warnings on changed code
cd WebUI/src/main/frontend
npx vitest run ../../test/ts/login
# → 5 files, 40 tests pass
```

## Author is also the reviewer

**Self-review disclosure**: I authored this change in the same session. Applied the same rigor; recommend a second pair of eyes (human reviewer or a fresh agent session) before merge, especially around the JSP-injection finding above.

## Suggestions for the planning-doc author

If the planning doc for this task is re-used, please consider:

1. Updating the plan's step 5 wording to recommend a `data-*` attribute bump over a `key` re-mount; the literal `key` approach would defeat the form-state-preservation goal the plan itself articulated.
2. Either pre-coordinating the `locale`-escaping fix in `spa.jsp` + `rxlogin.jsp`, or pre-approving the XSS-via-query-param follow-up. Issue 1 above is the same class as a known CodeQL finding per the root `AGENTS.md` CodeQL playbook.
3. Documenting the swallowed-exception rationale in `LoginPage.tsx` (or addressing it) so the next reviewer doesn't flag-and-flip the pattern.

## Pre-PR command evidence

```bash
cd WebUI
../mvnw.cmd clean install
```

Result: **BUILD SUCCESS** in 32 s. `tsc --noEmit` exit 0. `vite build` emitted `target/generated-webui/cm/modern/assets/perc-modern-ui.js` (215.92 kB). Surefire: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`. No new compiler, surefire, enforcer, or Spotless warnings on the changed modules (existing dependency-relocation warnings are pre-existing and unrelated).

```bash
cd WebUI/src/main/frontend
npx vitest run ../../test/ts/login
```

Result: **5 files, 40 tests passed (40)**, 0 failures, 0 skipped.
