# Erlang Code Review — fix/perc-web-ui-npm-build

## Summary

Build fix for `perc-web-ui` (WebUI) — the `npm run build:modern` step (`tsc --noEmit && vite build`) was failing on the current `origin/development` tip with 11 `TS2307: Cannot find module 'react-router'` errors. Root cause: the `react-router` v8.3.0 package was added in PR #1539 and started being imported in PR #1542, but the package ships its type declarations only via the `exports.types` subpath condition and not via a root-level `types` field. TypeScript 5.9 with `paths: "*": ["node_modules/*"]` was bypassing the `exports` field, falling back to legacy package-root field resolution, and never finding `react-router/dist/production/index.d.ts`. One-line fix: explicit `paths` mapping for `react-router` to its types file. Standalone module build is now green.

## Scope

- Base: `origin/development` (24ee28acf4)
- Head: `fix/perc-web-ui-npm-build` (uncommitted)
- Files: 1 changed (`WebUI/src/main/frontend/tsconfig.json`, +1 line)
- Prior report: none
- Memory patterns hit: `docs.ts.paths-bypass-exports-field`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Review notes

### Reproduction
- `cd WebUI && ..\..\mvn-env.bat clean install -B` on `origin/development` → BUILD FAILURE in `frontend-maven-plugin:npm-build-modern` (`tsc --noEmit && vite build`). npm step exit value 2.
- `tsc --noEmit` produced exactly 11 errors of the form `error TS2307: Cannot find module 'react-router' or its corresponding type declarations.` across the SPA shell, route shims and feature placeholder files imported in PRs #1531/#1542.

### Diagnosis
- `react-router@8.3.0` package.json has no root-level `types`, `typings`, `main`, or `typesVersions` field; types are exposed solely through `exports[".types"]` pointing at `./dist/production/index.d.ts`.
- TypeScript 5.9 with `moduleResolution: "bundler"` honours `exports` during a normal node-modules walk, but the `tsconfig.json` here uses a catch-all `paths: "*": ["node_modules/*"]` rule. Trace (`tsc --noEmit --traceResolution`) confirmed TS falls back to the legacy package-root field walk (looking for `typesVersions` / `typings` / `types` / `main`) and never reads the `exports` block.
- Secondary contributor: source files live at `WebUI/src/main/ts/`, `node_modules` at `WebUI/src/main/frontend/node_modules/` (sibling layout). Node resolution walks UP from source, so it cannot find the package without an explicit hint even if `exports` is read.

### Fix
- Added `paths` entry `"react-router": ["./node_modules/react-router/dist/production/index.d.ts"]` (kept the existing catch-all `"*"` mapping so no other package changes). The direct `.d.ts` file path bypasses the broken `exports` walk and gives TS the exact type entry point.
- Considered alternatives:
  - Symlink `WebUI/src/main/ts/node_modules/react-router` → real install — rejected, fragile and platform-specific.
  - Move `node_modules` closer to `ts/` — rejected, large dir restructuring outside this issue's scope.
  - Switch to `moduleResolution: "nodenext"` — rejected, would force `.js` extensions on hundreds of relative imports (verified locally — produced ~12 follow-on errors in SiteCopyWizard/HomeShell/index.ts).
  - Add a `@types/react-router` shadow package — rejected, ugly workaround and unnecessary.
  - Switch `paths` wildcard to a per-package style for *every* package — rejected, far more invasive than needed.

### Verification
- `cd WebUI && ..\..\mvn-env.bat clean install -B` → **BUILD SUCCESS** (~3 min cold; full module + 11 tests pass).
- `tsc --noEmit` standalone: 0 errors.
- `vite build` standalone: 0 errors, generated `target/generated-webui/cm/modern/assets/perc-modern-ui.{js,css}` plus the expected vendor chunks.
- `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` (PSWebUiSpaFallbackFilterTest 7 + GadgetRegistryTest 4).
- No new warnings introduced; existing warnings are pre-existing dependency relocations (jakarta artefacts, etc.) unrelated to the diff.

### Diff footprint
- One file, +1 line. No code changes, no API changes, no test changes. Risk surface is minimal.

### Cross-platform path / file I/O
- Diff is TypeScript config only; no file I/O, paths, installers, or packaging touched.
- Cross-platform path review: no issues.

## Pre-PR command evidence

```bash
cd WebUI
..\..\mvn-env.bat clean install -B
```

Result: `BUILD SUCCESS`. `tsc --noEmit` clean. `vite build` clean. 11 tests pass, 0 failures. No new warnings.
