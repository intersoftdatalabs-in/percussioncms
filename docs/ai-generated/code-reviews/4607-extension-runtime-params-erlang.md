# Erlang review — #4607 extension runtime-parameter editing

## Summary

Slice 22 of #1690 closes the last `EXTENSION_DESIGN_GAPS` entry (Workbench
parameter-dialog parity) by making extension runtime parameters editable in
Developer → Extensions detail chrome. REST DTO + `ExtensionAdaptor` already
persisted `runtimeParameters`; the missing piece was SPA editing plus proof.
Live C5 verified add / GET round-trip / clear with zero console and zero
`server.log` errors, including a real JAXB empty-array finding fixed with the
peer-established blank-name sentinel.

## Scope

- Base: `origin/main`
- Head: branch `fix/issue-4607-extension-method-map-editing` (uncommitted at review)
- Files: 11 changed (WebUI SPA + Vitest, sitemanage tests, Playwright spec, product-docs, this report)
- Prior report: none for #4607 (peer: `4543-extension-method-map-erlang.md`)
- Memory patterns hit: change-class closure; behavioral (not structural) tests;
  JAXB empty-array wire sentinel (peer pattern from #4543)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion
- File: `WebUI/src/main/ts/developer/messages.ts`
- Description: `EX_GAP_WORKBENCH` is now unreferenced after the gaps list
  emptied. Dead TMX key, no runtime effect.
- Suggestion: Leave it (TMX tooling keys on stable ids) or remove in a
  follow-up message-key sweep. Not worth churning this PR.
- Status: open (accepted as-is)

### Issue 2 -- Severity: nit
- File: `WebUI/src/main/ts/api/developer/extensionsApi.ts`
  (`wrapExtensionForWire`)
- Description: `Object.values(runtimeParameters)` fallback mirrors the
  methods branch but the declared type is array-only, so the fallback is
  unreachable via types.
- Suggestion: Keep for wire robustness (server may hand back non-array shapes
  elsewhere); harmless.
- Status: open (accepted as-is)

## Verification noted

- `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS,
  Tests run: 2729, Failures: 0, Errors: 0 (`ExtensionAdaptorWriteTest`: 28)
- `cd WebUI && ../mvnw clean install` → BUILD SUCCESS, Vitest 470 files /
  4528 tests passed, Java 69 passed
- C5 QA H2: `qa-up` → `qa-health` OK → `qa-deploy-war-jars --restart-jetty` →
  `qa-deploy-webui` → `qa-health` OK → `test:surface
  tests/developer-extension-runtime-params.spec.js` 1 passed (in-spec console
  clean) + sibling `developer-extension-method-map.spec.js` 1 passed;
  `server.log` 0 ERROR / 0 FATAL
- Cross-platform path review: no file I/O touched — clean
- Copyright headers on 3 new files use `Copyright (c) 2026 Intersoft Data
  Labs, Inc.` + Apache block — clean
- No rule/AGENTS/skill/workflow files in diff — clean

## Re-review

Not required (no blocking findings).
