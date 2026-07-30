# Erlang-style pre-commit review — PR #1594 follow-up (Kilo open threads)

**Scope:** rest `PipelinesResource` null-guard + cause-preserving tests; sitemanage `PipelinesAdaptorTest` pure helper coverage.
**Verdict:** PASS — no merge-blocking defects.

## Findings

| Severity |                                     Finding                                      |    Disposition     |
|----------|----------------------------------------------------------------------------------|--------------------|
| none     | `requireAdaptor()` guards no-arg constructor path; wraps as 500 with cause       | OK                 |
| none     | `PipelinesAdaptorTest` covers limit clamp, offset, name filter, hidden map, sort | OK                 |
| none     | `listApplicationsWrapsFailures` asserts `assertSame` on cause                    | OK                 |
| none     | Cross-platform / file I/O                                                        | N/A (no path work) |

## Tests run

- `cd rest && ../mvnw -Dtest=PipelinesResourceTest test` → Tests run: 4, Failures: 0
- `cd projects/sitemanage && ../../mvnw -Dai.integrity.skip=true -Dtest=PipelinesAdaptorTest test` → Tests run: 9, Failures: 0  
  (integrity skip: worktree seal drift vs sibling #1595 / system notify map not on this branch)

