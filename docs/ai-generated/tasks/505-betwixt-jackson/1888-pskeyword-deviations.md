# Issue #1888 — PSKeyword / PSKeywordChoice Jackson wire deviations

Parent: #1823 · Epic: #505 · Facade: #1887 · Pilot: #1822

## Scope delivered

- Production `PSKeyword` / `PSKeywordChoice` annotated for Jackson-backed
  `PSXmlSerializationHelper` (default engine).
- Nested package element name pinned to **`choice`** (not `keyword-choice`).
- Golden fixture + round-trip tests + offline package smoke (`Adhoc_Type.keyword`).
- `PSKeyword.betwixt` **retained** for dual-engine rollback
  (`-Dcom.percussion.xml.serialization.engine=betwixt`) until #1824.

## Approved XML deviations (Jackson write vs historical Betwixt packages)

|                         Historical Betwixt / package dump                          |                              Jackson default write                              |                                      Notes                                       |
|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Graph-identity `id="…"` attributes on complex elements                             | Not emitted                                                                     | Property values live in child elements; documented in #1887                      |
| Root `<null>` on package archives                                                  | Root `<keyword>` on modern write                                                | Read path still rewrites legacy `<null>` → mapped root                           |
| Nested item `<choice>`                                                             | Nested item `<choice>`                                                          | **Parity** via `@JacksonXmlProperty(localName="choice")`                         |
| Mapped type name `keyword-choice` (if type-mapped write)                           | Not used for nested items                                                       | Standalone `PSKeywordChoice.toXML()` root is still `keyword-choice`              |
| `<guid>0-14-…</guid>`                                                              | Emitted/read as string                                                          | Shared `IPSGuid` converter in `PSJacksonXmlSerializationHelper` (Betwixt parity) |
| `<name>` (alias of label)                                                          | Omitted                                                                         | Package read tolerates extra `name`                                              |
| `<type>KEYWORD_DEF</type>`                                                         | Omitted                                                                         | Catalog summary; not needed for install                                          |
| Catalog default methods (`description-optional`, `display-string`, `type-enum`, …) | Omitted via `@JsonAutoDetect(getterVisibility=NONE)` + explicit `@JsonProperty` | Interface defaults otherwise leak into Jackson XML                               |
| `<version>`                                                                        | Suppressed (`@IPSXmlSerialization`)                                             | Same as Betwixt annotation path                                                  |
| Optional wrapper getters (`*Optional` on choice)                                   | Suppressed                                                                      | Avoid spurious elements                                                          |
| Property order                                                                     | `choices` first then scalars + guid (`@JsonPropertyOrder`)                      | Packages interleave name/type; golden captures Jackson order                     |

## Package install smoke (offline)

Fixture: `system/src/test/resources/.../Adhoc_Type.keyword` (copy of
`modules/perc-packages/.../perc.widget.blogIndexPage/Adhoc_Type.keyword`).

`PSKeyword.fromXML` restores id, label, value, keyword-type, sequence, description,
and all three choices with element name `choice` under legacy `<null>` root.

## Utils companion (shared)

`PSJacksonXmlSerializationHelper` registers `IPSGuid`/`PSGuid` string serde (Betwixt
`PSBetwixtObjectConverter` parity). Unblocks package XML with `<guid>` for keywords and
pre-existing assembly slot restore tests after the #1887 Jackson facade default.

## Not in this PR

- Full assembly/security/workflow domain annotation batches
- Betwixt POM removal (#1824)
- Live CMS package install

