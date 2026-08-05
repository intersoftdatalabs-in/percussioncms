# Erlang pre-commit review — issue #1918 (sitemgr Jackson domain)

**Change class:** Jackson design-object domain batch (sitemgr)  
**Scope:** `PSSite`, `PSSiteProperty`, `PSPublishingContext`, `PSLocationScheme`,
`PSLocationSchemeParameter` + golden/round-trip tests + deviations doc  
**Reviewer persona:** independent Erlang (pre-commit)

## Verdict

**PASS** for commit / PR of in-scope files.

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new/changed logic | No critical bugs found after BeanUtils null-restore fixes (`setDefaultScheme(null)`, `setContext(null)` must not wipe scalar ids) |
| Behavioral unit tests | `PSSitemgrXmlSerializationTest` (11) + updated `PSPublishingContextTest` (2) |
| Cross-platform paths | No filesystem path construction; classpath resources only |
| Companions (peer filter #1915 / keyword / security) | Jackson opt-in + `addType` + `@IPSXmlSerialization` suppress + golden + round-trip + deviations doc |
| Out of scope | filter / publisher / catalog leftovers / Betwixt POM (#1824) not touched |

## Notes

- `template-ids` restore still requires assembly service (same as historical helper path); offline tests leave `template-ids` empty.
- Historical attribute roots (`PSXSite`, `PSXPublishingContext`, `PSXLocationScheme`) documented as deviations.
- No production `.betwixt` to drop under sitemgr.
