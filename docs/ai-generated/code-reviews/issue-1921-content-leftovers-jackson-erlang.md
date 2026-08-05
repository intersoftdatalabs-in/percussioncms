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
| Behavioral unit tests for new/changed logic | PASS — 12 offline tests (8 content leftovers + 4 node-def)                                                            |
| Portable paths                              | PASS — no new filesystem path construction                                                                            |
| Change-class companions                     | PASS — annotations + goldens + round-trip; type registration for nested fields/children; deviations doc               |
| `.betwixt` drop only when proven            | N/A — no production betwixt for these types                                                                           |
| Helper registration conflicts               | LOW — static `addType` for nested names only; coordinates with existing auto-translation / variant-guid registrations |

## Findings

### Non-blocking notes

1. **PSNodeDefinition template restore** still requires live content manager (`addTemplateId` → locator). Offline tests intentionally cover write shape + scalar restore only; live path remains in `PSContentTypeMgrTest`. Documented in deviations.
2. **setWorkflowIds** is a no-op for non-empty input (historical surface had no workflow adder). Acceptable for this slice; residual only if package install proves workflow-id restore is required offline.
3. **PSFolderProperty** gained `toXML`/`fromXML` (inventory marked RW; previously missing). Low risk additive API.

## Hard-gate bugs

None.
