# Erlang review — issue #1993 (PSDependency / PSDependent Jackson)

**Verdict:** PASS (self-review before commit)

**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve

**Scope reviewed:** Jackson opt-in annotations + golden/round-trip tests for
`PSDependency`, `PSDependent`; deviations doc
`docs/ai-generated/tasks/505-betwixt-jackson/1993-dependency-domain-deviations.md`.

## Gates

|          Check          |                                                           Result                                                           |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Bugs / RT correctness   | Pass — `PSSystemDataXmlSerializationTest` 16/16 green; system `clean install` BUILD SUCCESS                                |
| Behavioral unit tests   | Pass — golden + RT + legacy `<null>` root for both types                                                                   |
| Cross-platform paths    | Pass — no new filesystem path construction (classpath resources only)                                                      |
| Change-class companions | Pass — domain annotations, `addType("dependent")`, goldens, deviations doc; peer pattern matches #1920 `PSAuditTrail` nest |
| Spotless                | Pass — apply then check on in-scope files; out-of-scope reformats discarded                                                |

## Notes

- Nested wire: `<dependents><dependent>…</dependent></dependents>` dual-registered
  (`@JacksonXmlProperty` + `PSXmlSerializationHelper.addType`).
- Derived getters suppressed: `display-type`, `dependent-types`.
- Type wire stores `PSTypeEnum.name()` (matches production `PSDependencyHelper`).

## Residuals (not this PR)

- PSMimeContentAdapter (#1994)
- Betwixt POM removal (#1824)

## Memory patterns hit

- Jackson domain batch: opt-in `@JsonAutoDetect`, root element, nested wrapper + item name, golden/RT/legacy-null triad

