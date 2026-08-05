# Issue #1915 — Filter domain Jackson wire deviations

Parent: #1892 · Grandparent: #1823 · Epic: #505 · Facade: #1887

## Scope delivered

- Production design objects annotated for Jackson-backed `PSXmlSerializationHelper`:
  - `PSItemFilter` (root `item-filter`)
  - `PSItemFilterRuleDef` (nested package element `rule-def`)
  - `PSItemFilterRuleParam` (registered type name `parameters`; package wire usually uses parent string map `params`)
- Golden fixture + round-trip tests + offline package smoke (`perc_public.filterDef`, `perc_staging.filterDef`)
- Keep `PSXmlSerializationHelper.addType("rule-def", …)` / `addType("parameters", …)`
- No `.betwixt` files existed for filter types (nothing to drop)

## Approved XML deviations (Jackson write vs historical Betwixt packages)

|           Historical Betwixt / package dump            |           Jackson default write            |                             Notes                              |
|--------------------------------------------------------|--------------------------------------------|----------------------------------------------------------------|
| Graph-identity `id="…"` attributes on complex elements | Not emitted                                | Property values live in child elements; documented in #1887    |
| Nested `<filter idref="…"/>` on each `rule-def`        | Omitted                                    | Circular parent; restored via `PSItemFilter.setRuleDefs`       |
| Empty `<parent-filter-id/>`                            | May emit `xsi:nil="true"` when null        | Read accepts empty / nil / absent                              |
| Nested `parameters` bean elements                      | String map under `params` (key-as-element) | Matches package empty `<params/>`; non-empty uses key children |
| Mapped type name `item-filter-rule-def` as nested item | Nested uses package name `rule-def`        | Standalone root still `item-filter-rule-def`                   |
| Version / Hibernate fields                             | Omitted                                    | `@IPSXmlSerialization(suppress=true)` + `@JsonIgnore`          |

## Package install smoke (offline)

Fixtures (copied under test resources from `modules/perc-packages/.../perc.Baseline/`):

- `perc_public.filterDef` — two rules, empty params, Betwixt filter idrefs ignored
- `perc_staging.filterDef` — one staging rule

`PSItemFilter.fromXML` restores name, description, legacy-authtype-id, guid string form, and rule-name set. Circular filter idrefs are not required on restore.

## Residual (siblings of #1915 under #1892)

Not in this PR — file separate children:

1. sitemgr (`PSSite` / `PSSiteProperty` / `PSPublishingContext` / `PSLocationScheme`)
2. publisher / pubserver (`PSContentList` / `PSEdition` / `PSDeliveryType` / `PSPubServer`)
3. catalog / system / ui leftovers (beyond `PSObjectSummary` #1903)
4. content leftovers beyond keywords + contentmgr `PSNodeDefinition` if needed

## Not in this PR

- Betwixt POM removal (#1824)
- Live CMS package install
- Mass rewrite of package `*.filterDef` files

