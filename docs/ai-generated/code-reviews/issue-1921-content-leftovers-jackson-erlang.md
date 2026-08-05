# Erlang review — issue #1921 content leftovers + PSNodeDefinition Jackson

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** Jackson-migrate design-object XML surface (content + contentmgr)  
**Verdict:** PASS (no hard-gate bugs)

## Scope reviewed

- `PSAutoTranslation`, `PSContentTypeSummary`/`Child`, `PSFieldDescription`, `PSFolderProperty`, `PSItemStatus`
- `PSNodeDefinition` (contentmgr)
- Golden + round-trip tests under `system/src/test/...`
- Deviations note: `docs/ai-generated/tasks/505-betwixt-jackson/1921-content-leftovers-deviations.md`

## Checklist

|                    Gate                     |                                                        Result                                                         |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Behavioral unit tests for new/changed logic | PASS — 17 offline tests (10 content leftovers + 7 node-def, including review mitigations)                             |
| Portable paths                              | PASS — no new filesystem path construction                                                                            |
| Change-class companions                     | PASS — annotations + goldens + round-trip; type registration for nested fields/children; deviations doc               |
| `.betwixt` drop only when proven            | N/A — no production betwixt for these types                                                                           |
| Helper registration conflicts               | LOW — static `addType` for nested names only; coordinates with existing auto-translation / variant-guid registrations |

## Findings

### Non-blocking notes

1. **PSNodeDefinition template restore** still requires live content manager (`addTemplateId` → locator). Offline tests intentionally cover write shape + scalar restore only; live path remains in `PSContentTypeMgrTest`. Documented in deviations.
2. **setWorkflowIds** rebuilds offline `PSContentTypeWorkflow` rows via `addWorkflowGuid` (association PKs left unset for Hibernate). Live package install may still re-merge existing DB rows via `PSContentTypeHelper`. Covered by `PSNodeDefinitionXmlSerializationTest` workflow restore.
3. **PSFolderProperty** gained `toXML`/`fromXML` (inventory marked RW; previously missing). Low risk additive API.
4. **PR #1974 review mitigations preserved:** fail-fast `getId`/`getRawContentType` (no synthetic `0L`); real `setWorkflowIds`; intentional null-allowed `PSItemStatus` from/to state setters (empty still rejected).

## Hard-gate bugs

None.
