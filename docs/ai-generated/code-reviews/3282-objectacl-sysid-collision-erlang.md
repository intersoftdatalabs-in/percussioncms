# Erlang review — #3282 PSX_OBJECTACL SYSID 1001 / qa-up H2

**Scope:** uncommitted branch `fix/issue-3282-objectacl-sysid-collision` vs `origin/main`.
**Reviewer persona:** Erlang (independent of implementer).
**Memory patterns hit:** behavioral unit tests for new logic; seed/installer lockstep; swallowed exceptions must log; portable Path in tests; incomplete change-class (seed + runtime + tests).

## Summary

H2 qa-up failed because `NEXTNUMBER.PSX_OBJECTACL.NEXTNR=1000` allocates SYSID `1001`, already seeded as Everyone on CONTENTID=301. Folder 7 (Assets) ACL rewrite on `CORE_SERVER_INITIALIZED` then hits `PK_PSX_OBJECTACL`.

The change:

1. Bumps seed NEXTNR for `PSX_OBJECTACL` and `PSX_PROPERTIES` to 2000 (last-issued; first `createId` is 2001).
2. Adds `PSNextNumberAligner` + `PSObjectAclNextNumberReconciler` so runtime still advances NEXTNUMBER past `max(SYSID)` when seed is stale.
3. Makes `setDefaultPermissions` skip save when virtual Everyone already has ADMIN.
4. Tests reproduce NEXTNR=1000 → 1001 collision, prove align to 1007, seed XML invariant, and idempotent skip.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover the collision arithmetic, reconcile decision, seed invariant, and skip-rewrite. Paths use `Path.of`. `reconcileOnServer` logs and swallows locator/JDBC failures (justified: folder persist still attempted; seed bump is the primary H2 path).

## Cross-platform path checklist

- [x] No new `".../" +` filesystem joins
- [x] Tests use `Path.of(...)` / `Files.isRegularFile`
- [x] No Unix-only absolute path assertions
- [x] Schema qualification uses connection origin, not hardcoded `/`

## Issues

None (hard-gate).

### Suggestion (non-blocking)

`reconcileOnServer` peeks NEXTNUMBER (may allocate a block) then `fixNextNumber`. Correct if `fix` wins before any insert; document that order in the listener if this class is reused outside folder save.

## Product documentation

N/A — allocator/seed fix; no operator-facing ACL semantics change (Everyone ADMIN on Assets is existing intent).
