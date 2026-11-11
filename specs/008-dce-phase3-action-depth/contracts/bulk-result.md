# Contract: Bulk per-item results

## Purpose

Define bulk outcome reporting so FR-011 acceptance is verifiable without implementation knowledge.

## Rules

1. Permissions evaluate per item; denied items never reach the apply step.
2. Every invoked item gets exactly one reported outcome: `SUCCESS`, `DENIED`, or `FAILED` with the item named and the reason stated.
3. Successes stand even when siblings fail; failures never cancel or corrupt other items.
4. The aggregate is always presented as a per-item breakdown — never a single pass/fail that hides per-item truth.

## Verification

Bulk trials across mixed-permission selections; SC-004 operator trial includes at least one partial-failure case with correct per-item reporting.
