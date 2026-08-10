# Erlang review — issue #1903 PSObjectSummary Jackson equality RT

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** catalog/security domain Jackson bean surface for object-summary XML  
**Modules:** `system` only

## Verdict: **APPROVE**

### Checklist

|            Gate             |                                                                     Result                                                                     |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs / incorrect assertions | Pass — historical suppress of nested `permissions` restored; `permission-value` string path + `setGUID` restore wire form                      |
| Behavioral unit tests       | Pass — `PSObjectSummaryTest` write-shape + both equality RTs green (3/3)                                                                       |
| Cross-platform paths        | Pass — no filesystem path construction                                                                                                         |
| Change-class companions     | Pass — peer suppress/`@JsonIgnore` + `setGUID` pattern from #1888/#1889/#1915; SOAP orphan-brace compile fix required for module clean install |
| Spotless / clean install    | Pass — in-scope only; `cd system && ../mvnw clean install` BUILD SUCCESS (Tests run: 1035, Failures: 0)                                        |

### Findings

1. **Root cause** — Java 11 modernization dropped Betwixt bean surface (`setGUID`, suppressed `getPermissions`, `permissionValue`, `getId`). Nested empty `PSUserAccessLevel` then failed Jackson deserialize.
2. **Fix** — Restore suppress on nested permissions + `permission-value` string; restore GUID/label/type setters; add `PSUserAccessLevel` no-arg + `setPermissions` and suppress derived `has*Access`.
3. **Equals** — Restored fuller historical equality (id/type/name/label/description/locked/permissions) so complete RT is meaningful.
4. **Drive-by** — Remove orphan class-body braces in `AssemblySOAPImpl` / `AssemblyDesignSOAPImpl` introduced by #1932 (blocked `perc-system` compile).

### Memory patterns hit

- Jackson domain slices: suppress derived getters with `@IPSXmlSerialization` + `@JsonIgnore`
- GUID as string property via shared `IPSGuid` converter
- Do not fold monorepo Spotless noise into feature PR

