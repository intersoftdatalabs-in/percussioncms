# Issue #1893 — System serialization tests (Jackson default)

Parent: #1823 slice 7 / epic #505. Depends on facade #1887 (merged). Domain slices #1888–#1891 may still refine wire goldens.

## Scope landed

|               Suite               |                                                                   Change                                                                    |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `PSObjectSerializerTest`          | Jackson engine assert; wire-shape (no Betwixt graph `id`); enable PersonList collection RT (`setPersons` + `person-list` root registration) |
| `PSObjectSerializerRoundTripTest` | Re-enabled offline assembly template RT (no live CMS / no slot service)                                                                     |
| `PSSerializationTest`             | JUnit 5; Guid / Community / Locale behavioral RTs + no-graph-id shape                                                                       |
| `PSObjectSummaryTest`             | Enabled write-shape; full equality RT still `@Disabled` (residual)                                                                          |

## Approved XML deviations (asserted)

- No Betwixt graph-identity `id="…"` attributes on complex elements under Jackson write.
- Unannotated collection items use property name as item tag (e.g. nested `<books>`), not type-mapped `<book>`, until domain annotations (#1888+).
- Unannotated catalog-style beans may still emit derived `*-optional` / `display-string` fields until domain suppress slices land.

## Residual

- `PSObjectSummary` full round-trip fails because nested empty `permissions` / `PSUserAccessLevel` has no default constructor. Needs catalog/security domain suppress or bean support — not production domain migration in this PR.

## Out of scope

- New domain migrations / `.betwixt` removal (#1824)
- Guid string serde module polish (#1888) — Guid scalar RT already works on main facade for these tests

