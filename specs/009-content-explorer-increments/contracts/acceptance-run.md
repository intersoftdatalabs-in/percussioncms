# Contract: Acceptance run evidence

## Purpose

Define what counts as proof for US1–US3 so SC-001–SC-003 are auditable.

## Rules

1. Every acceptance run records flow, selection size, path (success/named-failure), H2 cell identity, and outcome.
2. A `PASS` claims the observed behavior matched the product-docs row (Take Down/Stage/Schedule/Clear) exactly.
3. A `FAIL` links the filed bug; the run does not count toward the SC until the bug is fixed and the run repeated.
4. Environment differences (H2 vs golden) are recorded, never silently waived.

## Verification

SC-001: 3/3 flows with complete run records. SC-003: #6119 fix run + #6120 row-loss-free run, both recorded.
