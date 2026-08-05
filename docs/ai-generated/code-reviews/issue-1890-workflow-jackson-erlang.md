# Erlang-style review — issue #1890 (workflow Jackson)

**Scope:** Jackson-migrate `PSWorkflow` tree (`PSState`, `PSTransition`/`PSTransitionBase`,
`PSAgingTransition`, `PSNotification`/`PSNotificationDef`, `PSAssignedRole`,
`PSTransitionRole`, `PSWorkflowRole`) + IPSGuid string converters on Jackson helper.

## Hard gates

|          Gate           |                                               Result                                                |
|-------------------------|-----------------------------------------------------------------------------------------------------|
| Bugs                    | No blocking bugs found after golden/round-trip + fixture smoke                                      |
| Behavioral unit tests   | `PSWorkflowXmlSerializationTest` covers multi-state golden, graph RT, leaves, design-export fixture |
| Cross-platform paths    | No new filesystem path I/O; classpath resources only                                                |
| Change-class companions | Nested `addType` aliases; shared GUID converters in utils; deviations doc                           |

## Notes

- Recipient list dual-collection item naming limited by Jackson XML introspector; documented.
- `PSAgingTransition` exposes `setType(String)` for BeanUtils copy after Jackson (getter is String).
- `PSTransitionHib` not on design-export wire path (state exposes converted `PSTransition` /
  `PSAgingTransition`); left unannotated beyond existing `toXML`/`fromXML` + shared notification type reg.
- Do not absorb assembly domain in this PR.

**Verdict:** PASS for commit/PR gates.
