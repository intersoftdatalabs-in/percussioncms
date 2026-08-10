# Erlang review — #2673 Design-object auditor dual-write (DESN)

**Date:** 2026-08-10  
**Branch:** `fix/issue-2673-design-object-auditor-dual-write`  
**Scope:** uncommitted `system` changes vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Slice 1 of parent #2617 wires `PSDesignObjectAuditor` so design save/delete AOP audits dual-write through `PSSystemAuditLogger` / `DesignErrorCodes` (DESN-2902 update, DESN-2903 delete) into the system audit dual-write path, while retaining legacy `PSX_DESIGN_AUDIT_LOG` rows. When design auditing is disabled, neither path is written. CADF/jcadf untouched.

## Memory patterns hit

- Dual-write must not break primary business path (audit sink failures swallowed)
- Static/locator test hooks must restore state in `@AfterEach`
- Blank/missing actor → `"unknown"` for audit trails
- No mega-PR with jcadf removal in design-auditor slice

## Cross-platform path checklist

N/A — no new file I/O or path construction. Result: clean.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

### Nits (non-blocking)

1. **SAVE always maps to `DesignErrorCodes.UPDATE` (not `CREATE`)** — legacy AOP only distinguishes save vs delete via method name; version-based create detection is out of scope for this slice. `designCreate` helper exists for future call sites.
2. **`m_auditEnabled` cache remains sticky for the AOP bean lifetime** — pre-existing behavior; tests force fresh auditor instances / mock config.

## Tests / verification

- `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**
- Surefire total: **Tests run: 1500, Failures: 0, Errors: 0**
- Focused: `PSDesignObjectAuditorTest` 12/0/0; `PSSystemAuditLoggerTest` 26/0/0
- Coverage: enabled save/delete dual-write, multi-object collection, disabled no-op, blank user → unknown, createAuditData extract logic, design helpers blank actor / null code

## Gate

**approve** — no bugs, behavioral tests present, no CADF removal, module clean install green.
