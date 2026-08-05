# Erlang review — issue #1893 system serialization tests

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** residual system test suite alignment after Jackson facade (#1887)  
**Modules:** `system` (test sources + docs only)

## Verdict: **APPROVE** (with residual documented)

### Checklist

|            Gate             |                                 Result                                  |
|-----------------------------|-------------------------------------------------------------------------|
| Bugs / incorrect assertions | Pass — RTs re-verified under Jackson default                            |
| Behavioral unit tests       | Pass — collections re-enabled; RoundTrip offline; Serialization JUnit 5 |
| Cross-platform paths        | Pass — no filesystem path construction; classpath resources only        |
| Change-class companions     | Pass — peer suite docs + deviations note; no production domain beans    |
| Spotless / clean install    | Required on PR gate after this review                                   |

### Findings

1. **PersonList `setPersons`** — required for Jackson list binding; Betwixt used adder-only. Documented on the setter.
2. **RoundTrip offline** — removes live `PSAssemblyServiceLocator` dependency; keeps equality coverage.
3. **PSObjectSummary RT** — correctly left `@Disabled` with residual reason (not green-washed).
4. **No production domain migration** — matches issue out-of-scope; domain goldens remain #1888–#1891.

### Residual follow-up

- Enable `PSObjectSummary` equality RT after catalog/security suppress for `permissions` / `PSUserAccessLevel`.

