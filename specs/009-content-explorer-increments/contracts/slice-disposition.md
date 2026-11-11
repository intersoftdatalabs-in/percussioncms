# Contract: Slice disposition

## Purpose

Define the terminal states for QA tickets and body-table rows so SC-002/SC-004 and FR-004/006/008 are auditable.

## Rules

1. A QA ticket is closed only as pass-recorded (with environment) or failure-filed-as-bug (with bug link).
2. A body-table row is closed only as merged or re-tracked (new slice issue with owner, scope, acceptance criteria).
3. Table edits alone never close a row that represents unfinished work.
4. The closeout note (FR-008) links every piece of evidence above back to #4530 disposition.

## Verification

SC-002: ticket count from the QA queue under the strict-plus-named rule. SC-004: row walk of the body table with zero bare `PR_OPENED` rows.
