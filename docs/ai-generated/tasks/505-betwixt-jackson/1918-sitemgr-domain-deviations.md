# Issue #1918 — Sitemgr domain Jackson wire deviations

Parent: #1892 · Grandparent: #1823 · Epic: #505 · Filter sibling: #1915 / PR #1922

## Scope delivered

- Production design objects annotated for Jackson-backed `PSXmlSerializationHelper`:
  - `PSSite` / `PSSiteProperty`
  - `PSPublishingContext`
  - `PSLocationScheme` / `PSLocationSchemeParameter`
- Nested element names via `addType`: `site-property`, `template-id`, `context` (type map),
  `default-scheme` (type map), `location-scheme-parameter`
- Golden + round-trip tests (`PSSitemgrXmlSerializationTest`) + legacy null-root smoke for site
- No production `.betwixt` files for these types (none present under sitemgr)

## Approved XML deviations (Jackson write vs historical shapes)

|                                 Historical shape                                 |                        Jackson default write                        |                                        Notes                                        |
|----------------------------------------------------------------------------------|---------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| Graph-identity `id="…"` attributes                                               | Not emitted                                                         | Same as #1887 / peers                                                               |
| `PSSite` attribute root `PSXSite` (Java-11-era DOM helper)                       | Element root `site` with children                                   | Restored helper path used pre-DOM simplification; modern design-object element tree |
| `PSPublishingContext` attribute root `PSXPublishingContext`                      | Element root `publishing-context`                                   | Scalar `default-scheme-id` GUID string; nested `default-scheme` object suppressed   |
| `PSLocationScheme` attribute root `PSXLocationScheme` + `PSXLocationSchemeParam` | Element root `location-scheme` + nested `location-scheme-parameter` | Parameters under `parameter-set`, ordered by sequence                               |
| Full `associatedTemplates` / `IPSAssemblyTemplate` graphs                        | Omitted                                                             | Wire form is `template-ids` / `template-id` strings; restore needs assembly service |
| Circular `site` on `PSSiteProperty`                                              | Omitted                                                             | Restored via `PSSite#setProperties`                                                 |
| Circular `scheme` on parameter                                                   | Omitted                                                             | Restored via `PSLocationScheme#setParameterSet`                                     |
| Hibernate `version`                                                              | Omitted                                                             | `@IPSXmlSerialization(suppress=true)` + `@JsonIgnore`                               |
| Catalog `label` on site                                                          | Omitted                                                             | Alias of `name`                                                                     |
| Null scalars                                                                     | May emit `xsi:nil`                                                  | Documented Jackson default; package read ignores unknown extras                     |
| No XML declaration                                                               | Default                                                             | `PSJacksonXmlSerializationHelper`                                                   |

## Offline test notes

- **No live CMS.** Template association restore is not exercised offline (assembly service load).
- Site property parent is re-linked after restore by `setProperties`.
- `PSPublishingContext#setDefaultScheme(null)` and `PSLocationScheme#setContext(null)` tolerate
  BeanUtils null property-copy so scalar id restore is not wiped.

## Not in this PR

- filter (#1915), publisher/pubserver (#1919), catalog/ui leftovers (#1920), content leftovers (#1921)
- Betwixt POM removal (#1824)
- Live CMS package install

