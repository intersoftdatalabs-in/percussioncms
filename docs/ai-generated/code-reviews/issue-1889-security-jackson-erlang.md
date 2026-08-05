# Erlang self-review — issue #1889 (security domain Jackson)

**Change class:** design-object XML domain annotations + golden/round-trip tests (companion to #1888 keyword slice).

## Checklist

|         Gate         |                                                                                    Result                                                                                     |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs / logic errors  | None found after green unit suite; dual `id` attribute/element package XML handled via last-write-wins `setId`                                                                |
| Behavioral tests     | `PSSecurityXmlSerializationTest` (10 tests): community golden/round-trip/legacy-null; ACL golden/round-trip/package smoke; login golden/round-trip; community-visibility guid |
| Portable paths       | Classpath resource loads only; no OS path construction                                                                                                                        |
| Companions           | Jackson annotations + `jackson-dataformat-xml` on system; IPSGuid serde in utils; deviations doc                                                                              |
| Betwixt hide parity  | `roleAssociations`/`siteAssociations` suppressed; roles via scalar longs                                                                                                      |
| Spotless             | apply → check (root) before commit                                                                                                                                            |
| Module clean install | `system` (+ `utils` if IPSGuid companion shipped)                                                                                                                             |

## Residual

- Betwixt `idref` permission sharing in package ACL XML not expanded under Jackson — residual #1899.

