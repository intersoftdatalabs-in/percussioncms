# Issue #1890 — Workflow domain Jackson deviations

> Parent: #1823 / epic #505. Companion to `PSWorkflow` tree migration.

## Approved / intentional deviations vs historical Betwixt

|                   Deviation                   |                                                                                                                                                                                                                                         Notes                                                                                                                                                                                                                                         |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No Betwixt graph-identity `id="…"` attributes | Same as #1887 facade / other domain batches. Values live in child elements.                                                                                                                                                                                                                                                                                                                                                                                                           |
| Recipient list item element                   | Historical Betwixt used `<string>` items under `recipients` / `ccrecipients`. Jackson 3 cannot attach `@JacksonXmlProperty(localName="string")` to two list properties without a property-name conflict; wrappers stay `recipients` / `ccrecipients` (not kebab `cc-recipients`). Item element follows Jackson `String` mapping. Design-export fixtures with empty wrappers still round-trip; multi-value read of historical `<string>` items is accepted when content binds as text. |
| `PSWorkflow.version` omitted on write         | Suppressed (`@IPSXmlSerialization` + `@JsonIgnore`); not present on shipped design exports.                                                                                                                                                                                                                                                                                                                                                                                           |
| Catalog aliases                               | Computed `isAdhocEnabled` is emitted (Betwixt parity).                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `setGUID` overwrite                           | Allowed for BeanUtils property-copy after Jackson deserialize (same as #1888/#1889).                                                                                                                                                                                                                                                                                                                                                                                                  |

## Nested `addType` registration (wire + historical aliases)

|   Element (wire)   |        Class        | Historical alias also registered |
|--------------------|---------------------|----------------------------------|
| `state`            | `PSState`           | —                                |
| `role`             | `PSWorkflowRole`    | —                                |
| `notification-def` | `PSNotificationDef` | `notificationdef`                |
| `assigned-role`    | `PSAssignedRole`    | `assignedrole`                   |
| `transition`       | `PSTransition`      | —                                |
| `aging-transition` | `PSAgingTransition` | `agingTransition`                |
| `transition-role`  | `PSTransitionRole`  | `transitionrole`                 |
| `notification`     | `PSNotification`    | —                                |

## Shared helper companion

`PSJacksonXmlSerializationHelper` registers `IPSGuid` / `PSGuid` string converters (Betwixt `PSBetwixtObjectConverter` parity). Same companion as #1888; required for workflow `<guid>` elements on `main` before that PR merges.

## Tests

- `system/.../PSWorkflowXmlSerializationTest` — golden multi-state write, full graph round-trip, leaf round-trips, `testWorkflow1.xml` design-export smoke.
- `modules/utils/.../PSJacksonXmlSerializationHelperTest#ipsGuidSerializesAndDeserializesAsBetwixtStringForm`.

