# Issue #1889 — Security domain Jackson wire deviations

Parent: #1823 · Epic: #505 · Facade: #1887 · Keyword slice: #1888 · Pilot: #1822

## Scope delivered

- Production design objects annotated for Jackson-backed `PSXmlSerializationHelper`:
  - `PSCommunity` (honors `PSCommunity.betwixt` hide of `roleAssociations` / `siteAssociations`)
  - `PSAclImpl` / `PSAclEntryImpl` / `PSAccessLevelImpl`
  - `PSLogin`, `PSCommunityVisibility`
- Golden fixtures + round-trip tests + offline package smoke (`percPage.contentType.aclDef`)
- Shared `IPSGuid`/`PSGuid` string converters in `PSJacksonXmlSerializationHelper` (same companion as #1888)
- `jackson-dataformat-xml` declared on `system`
- `PSCommunity.betwixt` **retained** for dual-engine rollback until #1824

## Approved XML deviations (Jackson write vs historical Betwixt packages)

|                 Historical Betwixt / package dump                 |       Jackson default write        |                                                                                                                     Notes                                                                                                                     |
|-------------------------------------------------------------------|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Graph-identity `id="…"` attributes on complex elements            | Not emitted                        | Property values live in child elements; documented in #1887                                                                                                                                                                                   |
| Shared permission nodes via Betwixt `idref="…"`                   | Not resolved                       | Jackson does not expand graph idrefs; only fully-inlined `<ps-permission>` blocks restore. Package smoke asserts inline perms (first community entry + Default owner). Residual: #1899.                                                       |
| Entry derived flags (`community`/`user`/`group`/`role`/`owner`/…) | Omitted                            | Derived from `type` + permissions; package read ignores extras (`FAIL_ON_UNKNOWN_PROPERTIES=false`)                                                                                                                                           |
| Nested `<typed-principal>…</typed-principal>`                     | Omitted                            | Redundant with entry `name` + `type`                                                                                                                                                                                                          |
| `<first-owner>`                                                   | Omitted                            | Computed; getter can throw without owners                                                                                                                                                                                                     |
| `<object-guid>`                                                   | Omitted                            | Derived from `object-id` + `object-type`                                                                                                                                                                                                      |
| Catalog `<label>` on ACL                                          | Omitted                            | Alias of `name`                                                                                                                                                                                                                               |
| Community `<role-associations>` / `<site-associations>`           | Omitted                            | Historical betwixt **hide**; wire form uses scalar `<roles><long>…`                                                                                                                                                                           |
| Community catalog `<type>` / `<label>` / `<version>`              | Omitted                            | Catalog aliases / Hibernate version                                                                                                                                                                                                           |
| Login nested `PSRole` / `PSLocale` graphs                         | Omitted                            | Legacy objectstore types not Jackson-annotated in this slice; scalars + nested `PSCommunity` covered                                                                                                                                          |
| Entry/permission set iteration order                              | Sorted (name / permission ordinal) | Deterministic package export + golden parity                                                                                                                                                                                                  |

## Package install smoke (offline)

Fixture: `system/src/test/resources/.../percPage.contentType.aclDef` (copy of
`modules/perc-packages/.../perc.Baseline/percPage.contentType.aclDef`).

`PSAclImpl.fromXML` restores name, object-id/type, guid string form, entry names/types, and
permissions for fully-inlined permission blocks. Entries that only reference permissions via
`idref` do not regain those permissions under Jackson (documented above).

## Utils companion (shared)

`PSJacksonXmlSerializationHelper` registers `IPSGuid`/`PSGuid` string serde (Betwixt
`PSBetwixtObjectConverter` parity). Required for package `<guid>` on ACL/community.

## Not in this PR

- Keywords / workflow / assembly domain batches
- Betwixt POM removal (#1824)
- Live CMS package install
- Betwixt `idref` graph expansion for ACL permissions (follow-up residual if needed)

