# Issue #1919 — Publisher / pubserver domain Jackson deviations

> Parent: #1892 / #1823 / epic #505. Companion to filter (#1915) and sitemgr (#1918) slices.

## Types in this slice

|             Class             |          Root element          |                                                   Nested `addType`                                                    |
|-------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `PSContentList`               | `content-list`                 | `content-list-generator-param` → `PSContentListGeneratorParam`, `template-expander-param` → `PSTemplateExpanderParam` |
| `PSContentListGeneratorParam` | `content-list-generator-param` | parent `contentList` suppressed                                                                                       |
| `PSTemplateExpanderParam`     | `template-expander-param`      | parent `contentList` suppressed                                                                                       |
| `PSEdition`                   | `edition`                      | —                                                                                                                     |
| `PSDeliveryType`              | `delivery-type`                | —                                                                                                                     |
| `PSPubServer`                 | `pub-server`                   | `pub-server-property` → `PSPubServerProperty`                                                                         |
| `PSPubServerProperty`         | `pub-server-property`          | raw stored `value` on wire (no re-encrypt on XML restore)                                                             |

## Approved / intentional deviations vs historical Betwixt

|                      Deviation                      |                                                                             Notes                                                                              |
|-----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No Betwixt graph-identity `id="…"` attributes       | Same as #1887 facade / other domain batches.                                                                                                                   |
| No XML declaration                                  | `PSJacksonXmlSerializationHelper` default.                                                                                                                     |
| `PSContentList` map forms suppressed                | `generator-params` / `expander-params` maps omitted; bean collections used instead.                                                                            |
| Nested params omit `id` / `version` / `contentList` | Avoid circular graph and unstable identity; equality is name+value.                                                                                            |
| Stable collection order                             | Generator/expander args and pub-server properties sorted by name for golden parity.                                                                            |
| `PSEdition.guid` suppressed                         | Historical `@IPSXmlSerialization(suppress=true)`; identity is `id`. `name` is alias of `display-title` and is omitted.                                         |
| Null-safe `setSiteId` / `setPubServerId`            | Allow design XML restore when site/pub-server unset.                                                                                                           |
| Offline-safe `PSGuid` assemble                      | `PSDeliveryType` / edition site+pubserver / content-list filter no longer require GuidManager locator for XML.                                                 |
| `PSPubServer` Optional API                          | Interface returns `Optional` for description/serverType; XML uses String accessors + class-based `fromXML` field copy (BeanUtils cannot copy Optional→String). |
| Computed pub-server flags omitted                   | `xml-format`, `database-type`, `ftp-type`, runtime `publish-server` (DTS lookup) suppressed.                                                                   |
| Password property wire form                         | Design XML stores raw encrypted `value`; `setValueXml` does not re-encrypt. Unit tests use non-password properties only.                                       |

## Out of scope (follow-ups)

- Catalog/ui leftovers (#1920)
- Content leftovers (#1921)
- Betwixt POM removal (#1824)
- Production `.betwixt` files for these types were not present (nothing to drop)

## Tests

- `system/.../publisher/data/PSPublisherXmlSerializationTest` — content list, edition, delivery type golden + round-trip + legacy null root.
- `system/.../pubserver/data/PSPubServerXmlSerializationTest` — pub server golden + round-trip + legacy null root.

