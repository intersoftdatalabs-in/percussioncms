# Erlang review — issue #1920 (catalog/system/ui first sub-batch)

**Verdict:** PASS (self-review before commit)

**Scope reviewed:** Jackson opt-in annotations + golden/round-trip tests for
`PSAudit`, `PSAuditTrail`, `PSSharedProperty`, `PSHierarchyNode`,
`PSHierarchyNodeProperty`; drop obsolete `PSHierarchyNodeProperty.betwixt`;
deviations doc.

## Gates

| Check | Result |
|-------|--------|
| Bugs / RT correctness | Pass — 15 focused tests green; system `clean install` green (1078 tests, 2 prior failures were stale utils SNAPSHOT) |
| Behavioral unit tests | Pass — golden + RT + legacy `<null>` root per type |
| Cross-platform paths | Pass — no new filesystem path construction |
| Change-class companions | Pass — domain annotations, `addType("audit")`, goldens, deviations doc, betwixt drop with rationale |
| Spotless | Pass — `mvnw -pl system spotless:apply` then `check`; root `-N` for docs |

## Notes

- `event-time` uses UTC + Betwixt ISO8601 pattern for golden stability (documented deviation).
- `setVersion(null)` ignored so BeanUtils copy after Jackson restore does not NPE.
- `setNodeType` added so BeanUtils property `nodeType` restores enum form.
- Historical betwixt mapped non-existent `parentGuid`; production wire is `node-id`.

## Residuals (child issues)

- PSDependency / PSDependent
- PSMimeContentAdapter
