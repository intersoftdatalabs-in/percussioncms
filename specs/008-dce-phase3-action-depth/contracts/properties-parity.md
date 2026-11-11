# Contract: Properties parity

## Purpose

Define dialog completeness so US2 acceptance is verifiable without implementation knowledge.

## Rules

1. Folder dialogs present every DCE-shown property group beyond access control, with equal values.
2. Item dialogs present every DCE-shown property group, with equal values.
3. Groups the actor may not edit render read-only; attempted edits without permission produce a clear message (FR-009).
4. Saved permitted edits persist and are visible on dialog reopen.

## Verification

SC-002 sample matrix: ≥10 folders + ≥10 items, 100% group/value equality with DCE.
