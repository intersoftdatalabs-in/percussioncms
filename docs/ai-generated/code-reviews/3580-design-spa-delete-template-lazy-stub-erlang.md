# Erlang review — PR #3597 `@Lazy` on `TestTemplatesAdaptor`

## Summary

Add `@Lazy` next to `@Component` on the rest Spring test stub
`TestTemplatesAdaptor`, matching `rest/AGENTS.md` and peers (`TestLocalesAdaptor`,
`TestSystemDefAdaptor`). Addresses kilo-code-bot WARNING on PR #3597.

## Scope

- Branch: `feat/issue-3580-design-spa-delete-template` vs `HEAD`
- Files: `rest/src/test/java/com/percussion/rest/test/apibridge/TestTemplatesAdaptor.java`
- Memory: shared Spring test stubs must use `@Component` + `@Lazy`; `MainTest`
  needs the adaptor bean without eager-init side effects
- Cross-platform path review: no filesystem I/O in this diff

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None at bug severity.

Annotation-only change; no new production logic. `cd rest && ../mvnw.cmd clean
install` BUILD SUCCESS, Tests run: 539, Failures: 0 (`MainTest` 2 passed).
