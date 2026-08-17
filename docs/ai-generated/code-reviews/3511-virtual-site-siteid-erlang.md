# Erlang review: #3511 Virtual Site SITEID persist

- **Date:** 2026-08-17
- **Branch:** `fix/issue-3511-virtual-site-siteid`
- **Scope:** uncommitted vs `HEAD` (also vs `origin/main`)
- **Change class:** Hibernate 6 bidirectional site-property FK (Developer Virtual Site save)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** change-class companions (mapping + adaptor persist assertions); exact field types in tests (`PSSite` / `PSSiteProperty`)

## Summary

Developer Git-FS save (`PUT /services/sites/{nameOrId}/virtual`) inserts `RXASSEMBLERPROPERTIES` rows. After #3521 the child `PSSiteProperty.site` JoinColumn is insertable, but the parent collection still owned `SITEID` via `@JoinColumn`. Hibernate 6 then omitted SITEID on INSERT (`NULL not allowed for column SITEID` on H2).

This change makes `PSSite.properties` `mappedBy = "site"` so the child owns the FK, links `prop.setSite(this)` on `addProperty`, and adds mapping + adaptor tests that new `virtual.*` properties carry the parent `siteId`.

## Issues

None.

## Cross-platform path checklist

N/A — no filesystem path construction. Test values such as `C:/docs/product-docs` are stored property strings, not OS path joins.

## Product documentation

N/A — operator Save steps unchanged; backend persist only.

## C5 UI live proof

N/A — no WebUI / Playwright spec change. Existing `developer-site-virtual-source.spec.js` remains the live save surface.

## Build evidence

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2150, Failures: 0; `PSSitePropertySiteIdMappingTest` 4/0/0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1226, Failures: 0; `SitesAdaptorTest` 36/0/0
