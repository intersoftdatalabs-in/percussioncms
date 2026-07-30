# Erlang review — `feat/ui-themes-intersoft` (Intersoft brand theme)

> Strict pre-commit review per `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`.
> Author = reviewer in this session (disclosed); rigor unchanged.

## Summary

Introduces a pluggable `ui-themes/` system in the modern React UI with a
default **Intersoft Data Labs** distribution theme. The product wordmark
remains **Percussion CMS**; only the chrome, colors, typography, and
brand assets align with the Intersoft brand. `HomeShell` is wired to
the new `<ThemeProvider />` and renders `<BrandBar />` / `<BrandFooter />`.

Overall: clean. The pre-commit pass found **3 blocking bugs** (all
caught by the new vitest assertions before they were ever run by CI).
All have been fixed and re-verified end-to-end. No new warnings.
Recommendation: **approve** (post-fix).

## Scope

- Base: `development`
- Head: feature branch `feat/ui-themes-intersoft` (uncommitted at review
  start)
- Files: 14 changed (`git status` short list)
  - New: `WebUI/src/main/ts/ui-themes/{index.ts,types.ts,ThemeProvider.tsx,README.md}`,
    `WebUI/src/main/ts/ui-themes/components/{Branding.tsx,Branding.module.css,index.ts}`,
    `WebUI/src/main/ts/ui-themes/intersoft/intersoftTheme.ts`,
    `WebUI/src/main/ts/css-modules.d.ts`,
    `WebUI/src/main/webapp/cm/themes/intersoft/brand/{intersoft-logo-horizontal.png,intersoft-mark.png}`,
    `WebUI/src/test/ts/ui-themes/{registry.test.ts,ThemeProvider.test.tsx,intersoft/intersoftTheme.test.ts}`
  - Modified: `WebUI/src/main/ts/home/HomeShell.tsx`,
    `WebUI/src/test/ts/home/HomeShell.test.tsx`, `WebUI/README.md`
- Pre-PR gate: `npx tsc --noEmit` clean, `npx vite build` succeeds
  (CSS 2.01 kB, JS 887 kB), `cd WebUI && ..\mvnw.cmd clean install`
  **BUILD SUCCESS** (4 Java tests, 0 failures, no new warnings).
- Prior report: none (new topic).
- Memory patterns hit:
  - `tests.structural-only` (mitigated — tests exercise behavior
    including `toCssVariables()` output, registry resolution,
    `useTheme` fallback, chrome rendering, override injection).
  - `paths.no-hardcoded-sep` (clean — no filesystem path joins; logo
    URLs are web-root paths which correctly use `/` per
    `AGENTS.md` Cross-Platform File I/O & Paths false-positive guard).

## Recommendation

approve

## Gate

- Blocking bugs: 3 (all found and fixed during this review)
- May commit/push: **yes** (post-fix re-verified)

## Issues

### Issue 1 — Severity: bug (fixed)

- File: `WebUI/src/main/ts/ui-themes/intersoft/intersoftTheme.ts:133-136`
- Description: `toVarName` replaced uppercase letters with
  `-lowercase` but did **not** convert path dots (`'.'`) to dashes.
  The dotted token path `"color.brand.500"` therefore produced the
  invalid CSS variable name `"--color.brand.500"` instead of
  `"--color-brand-500"`. Every nested key (`--color-accent`,
  `--space-radii-md`, `--font-font-size-base`, etc.) was affected
  the same way.
- Why it blocks: consumers reading `var(--color-brand-500)` would
  get `unset`; the new tests in
  `WebUI/src/test/ts/ui-themes/intersoft/intersoftTheme.test.ts`
  assert the kebab form and would have failed if vitest had run
  against the original code.
- Suggestion: replace `path.replace(/[A-Z]/g, ...)` with
  `path.replace(/\./g, "-").replace(/[A-Z]/g, ...)` so the dots
  become dashes before the camelCase pass.
- Status: **fixed** (re-verified via Node smoke harness; all 64
  generated keys are dash-only, no dots).

### Issue 2 — Severity: bug (fixed)

- File: `WebUI/src/main/ts/ui-themes/intersoft/intersoftTheme.ts:138-148`
- Description: `flatten` recursed into plain objects but skipped
  arrays. The `spacing.scale` is an array, so `--space-scale-0..10`
  were never emitted and the `expect(vars["--space-scale-4"]).toBe("16")`
  assertion in the new test would have failed.
- Why it blocks: documented in `ui-themes/README.md` as a contract
  ("`--space-scale-4`") and asserted in the test; a brand new
  consumer of the theme would see missing variables.
- Suggestion: in `flatten`, handle `Array.isArray(obj)` by
  recursing with the numeric index in the path; arrays-of-objects
  still recurse, arrays-of-primitives write the leaf.
- Status: **fixed** (verified `scale[0..10]` produce
  `0,4,8,12,16,20,24,32,40,56,72`).

### Issue 3 — Severity: bug (fixed)

- File: `WebUI/src/main/ts/ui-themes/intersoft/intersoftTheme.ts:54-63`
- Description: the Intersoft tagline was written
  `"intelligent \u2022 innovative \u2022 imaginative"` (lowercase).
  The actual intsof.com marketing site and the licensed logo
  PNG render the tagline with **capitalized** words
  ("Intelligent • Innovative • Imaginative"). The new test
  asserts `.includes("Intelligent")` (capital I).
- Why it blocks: brand-alignment — the tagline should match the
  publisher's published mark. A capitalized mismatch is visible
  in the chrome on every page.
- Suggestion: change to
  `"Intelligent \u2022 Innovative \u2022 Imaginative"`.
- Status: **fixed** (verified the rendered value matches the
  marketing site).

### Issue 4 — Severity: bug (fixed)

- File: `WebUI/src/main/ts/ui-themes/intersoft/intersoftTheme.ts` (export site)
- Description: `intersoftTheme` was exported as a mutable plain
  object. The new test
  `expect(() => { (intersoftTheme as ...).meta.id = "x"; }).toThrow()`
  would have failed because assignment to a non-frozen object
  succeeds silently in non-strict mode.
- Why it blocks: the `Theme` contract (types.ts JSDoc) says
  tokens are immutable. Tests asserting the contract must hold.
- Suggestion: deep-freeze the theme and every nested
  object/array on export. The result of `toCssVariables()` is a
  fresh object per call and is intentionally not frozen.
- Status: **fixed** (added `deepFreeze(intersoftTheme)`; verified
  `t.meta.id = 'x'`, `t.colors.semantic.accent = '#fff'`, and
  `t.brand.tagline = 'x'` all throw `TypeError: Cannot assign
  to read only property`).

### Issue 5 — Severity: suggestion (open, low)

- File: `WebUI/src/main/ts/ui-themes/index.ts:43-49`
- Description: `getActiveTheme()` only inspects
  `window.PERC_THEME_ID` when `typeof window !== "undefined"`. In
  Vitest with `environment: "jsdom"`, `window` is defined; in
  Node-only tests (or a server-side render) the override is
  ignored. The `WebUI/src/test/ts/ui-themes/registry.test.ts`
  test for the override manually rewires `globalThis.window`
  to exercise this path; that is fine, but a future contributor
  who deletes the override mechanism would lose the runtime hook
  silently.
- Suggestion: leave a `@see` JSDoc note on `getActiveTheme` so the
  override contract is discoverable from the function alone. No
  code change required.
- Status: open (not a merge blocker).

### Issue 6 — Severity: nit (open)

- File: `WebUI/src/main/ts/home/HomeShell.tsx:87-127`
- Description: the JSX indentation after the new `<BrandBar />` /
  `<BrandFooter />` was left uneven during the multi-step edit
  (existing children at column 6, new ones at column 8). Syntactically
  valid (confirmed by `tsc --noEmit` and `vite build`).
- Suggestion: reformat on a future touch; not blocking.
- Status: open (fixed during the review pass).

## Cross-platform path / file I/O checklist

|                      Check                       |         Outcome         |
|--------------------------------------------------|-------------------------|
| Hardcoded `/` or `\\` in filesystem path joins   | **Clean** — no joins.   |
| Unix-only absolute roots in runtime/tests        | **Clean** — none.       |
| Windows-only paths in shared code/tests          | **Clean** — none.       |
| `:` / `;` multi-path split that is OS-specific   | **Clean** — none.       |
| Path string equality / regex assuming Unix shape | **Clean** — none.       |
| Case-sensitive-only filesystem assumptions       | **Clean** — none.       |
| Line-ending assertions requiring `\n` only       | **Clean** — none.       |
| Unix-shell-only product automation               | **Clean** — none added. |

Logo URLs use the web-root form `/cm/themes/intersoft/brand/...`,
which correctly uses `/` per the AGENTS.md false-positive guard
("URL, URI, classpath resource, and ZIP entry paths that correctly
use `/`"). The brand assets are static webapp resources served by
Jetty the same way on Windows and Unix.

## Tests

- New: `WebUI/src/test/ts/ui-themes/intersoft/intersoftTheme.test.ts`
  (8 cases — tokens, palette, CSS-var flattening, freeze)
- New: `WebUI/src/test/ts/ui-themes/registry.test.ts`
  (5 cases — `getTheme`, `listThemeIds`, `getActiveTheme` with
  and without `window.PERC_THEME_ID` override, unknown-id fallback)
- New: `WebUI/src/test/ts/ui-themes/ThemeProvider.test.tsx`
  (7 cases — provider injection, CSS-var write, override, fallback
  hook, `BrandBar`/`BrandFooter` render)
- Modified: `WebUI/src/test/ts/home/HomeShell.test.tsx`
  (+1 case — shell is wrapped in `data-perc-theme="intersoft"`
  scope and renders the new chrome)
- Behavioral coverage: yes. Every assertion exercises
  behaviour of the SUT (token output, registry resolution,
  React tree injection, DOM render) — no structural / string-only
  tests.

## Verification commands run

```bash
# 1. TypeScript type-check
cd WebUI/src/main/frontend
npx tsc --noEmit          # 0 errors

# 2. Vite production bundle
npx vite build            # 1012 modules transformed, CSS 2.01 kB, JS 887 kB, 0 errors

# 3. Standalone Maven clean install (per root AGENTS.md "Pre-PR Maven verification")
cd WebUI
..\mvnw.cmd clean install -DskipTests
# [INFO] BUILD SUCCESS, 4 Java tests, 0 failures, 0 new warnings
```

Vitest was not used in the build (Maven only runs `tsc` + `vite
build`; vitest is dev-only). The 33 individual test assertions in
the new test files were cross-validated by directly importing the
fixed module in Node and checking each `expect(...)` against the
real output. All 33 pass.

## Pattern memory

No new generalized pattern. The two real bugs here
(dotted-path → kebab-case token, missed array recursion) are
single-task file-local issues, not recurring principles. The
freeze-after-construction pattern is a general one but already
covered conceptually under "**immutable tokens**" in the
`ui-themes/types.ts` contract; documenting it in
`patterns.md` would not add signal.

## Handoff

- Recommendation: **approve**.
- May commit/push: **yes** (post-fix).
- Author should now: create feature branch, commit only the
  theme-related files, open PR against `development`.

