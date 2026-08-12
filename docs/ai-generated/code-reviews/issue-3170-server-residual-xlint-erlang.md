# Erlang review — issue #3170 server residual Xlint (non-cache/non-webservices)

**Reviewer persona:** Erlang (pre-commit hard gate)  
**Branch:** `fix/issue-3170-server-residual-xlint`  
**Date:** 2026-08-12  
**Change class:** Legacy raw-collection typing / Xlint residual (no product behavior surface)

## Scope reviewed

| Path | Role |
|------|------|
| `PSPersistentPropertyManager.java` | Nested typed caches; public Collection APIs |
| `PSUserSession.java` | Session maps/lists; property metadata APIs |
| `PSCompareRequestHandler.java` | Loadable handler roots + user props |
| `job/PSJobHandler.java`, `PSJobRunner.java`, `PSJobHandlerConfiguration.java` | Job handler maps/listeners/roots |
| `PSServerResidualTypedTest.java` | Behavioral + API-signature tests |

Out of scope (owned elsewhere): `server.cache` (#2877 / PR #3161), `server.webservices` (#3160 / PR #3171), `services.*` (#3181), `security` residual (#3182).

## Checklist

### Bugs / correctness
- **resetMetaCache:** previous loop recreated iterators incorrectly; replaced with `keySet().removeIf` (same intent: drop matching user meta cache entries then repopulate). **OK.**
- **save() null-action loop:** dropped redundant early-return on last null-action element; finally still syncs cache. Behavior equivalent. **OK.**
- **ConcurrentHashMap null keys/values:** pre-existing CHM constraints (null category / null session values); not introduced. **OK (no change).**
- **CategoryMap/LeafMap nesting:** matches prior structure username→category→propName→object. **OK.**
- **getUserProperties typing:** callers (`PSCompareRequestHandler`, `PreferencesAdaptor`) remain binary-compatible; generic refinement only. **OK.**

### Generics / rawtypes
- Real generics preferred; no new class-level blanket `@SuppressWarnings`.  
- Narrow unchecked cast retained only for private-object community lists (`List<String>` from Object map). **OK.**

### Portable paths / I/O
- No path or file I/O changes. **N/A.**

### Tests
- `PSServerResidualTypedTest` (10): arg validation, compare roots, job config typed maps, job listeners, API generic signatures.  
- Existing proxy-cast + job config tests still green.  
- Full module: `mvnw clean install` **BUILD SUCCESS**, Tests run: **1965**, Failures: 0, Errors: 0, Skipped: 241.

### API blast radius (C2)
- Public return/param types refined to parameterized collections (erasure-compatible).  
- Grep: no monorepo caller needs recompile beyond `system` for binary; `PreferencesAdaptor` uses raw `.contains` on Collection.  
- No `final`/`sealed` on types; no reverse-dep module install required.

### Product docs
- N/A — pure tech-debt typing.

## Verdict

**PASS** — safe to commit and open PR.

> Co-Authored by Grok Build using grok-4.5 with agent main.
