# Issue #1993 — System dependency domain Jackson wire deviations

Parent: #1920 · Grandparent: #1892 / #1823 · Epic: #505

## Scope delivered

|     Class      | Root element |                         Nested / notes                         |
|----------------|--------------|----------------------------------------------------------------|
| `PSDependent`  | `dependent`  | Scalar `id` + enum-name `type`; derived `display-type` omitted |
| `PSDependency` | `dependency` | `dependents` wrapper / nested `dependent` (`addType`)          |

Golden fixtures + round-trip + legacy `<null>` root tests live in
`system/src/test/java/com/percussion/services/system/data/PSSystemDataXmlSerializationTest`
(alongside #1920 audit / shared-property coverage).

## Approved / intentional deviations vs historical Betwixt

|                   Deviation                    |                                                               Notes                                                                |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| No Betwixt graph-identity `id="…"` attributes  | Same as #1887 facade / other domain batches.                                                                                       |
| No XML declaration                             | `PSJacksonXmlSerializationHelper` default.                                                                                         |
| `PSDependent` omits derived `display-type`     | Computed from `PSTypeEnum.valueOf(type).getDisplayName()`; not design wire.                                                        |
| `PSDependency` omits derived `dependent-types` | Comma-delimited display helper only; rebuilt from nested dependents.                                                               |
| Nested item element name `dependent`           | Matches type mapper (`PSDependent` → `dependent`); dual-registered via `@JacksonXmlProperty` + `PSXmlSerializationHelper.addType`. |
| Type string is `PSTypeEnum.name()`             | Production writers use `childType.toString()` / `.name()` (e.g. `TEMPLATE`, `ITEM_FILTER`).                                        |
| Empty dependents list may still emit wrapper   | Jackson list wrapper present when empty; read accepts empty / absent children.                                                     |

## Residual

Not in this PR:

1. **PSMimeContentAdapter** — system content adapter design XML (tracked as residual of #1920 / separate issue)
2. Betwixt POM removal (#1824)
3. Types already done in #1920 first batch (`PSAudit*`, `PSSharedProperty`, `PSHierarchyNode*`)

## Out of scope

- filter (#1915), sitemgr (#1918), publisher/pubserver (#1919)
- content leftovers (#1921)
- Live CMS package install / MSM dependency graph runtime

## Tests

|               Suite                |                       Coverage                        |
|------------------------------------|-------------------------------------------------------|
| `PSSystemDataXmlSerializationTest` | dependent / dependency golden + RT + legacy null root |

