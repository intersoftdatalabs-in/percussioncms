# Task Plan: Fix Database Lock Timeout Error During Rich Text Page Updates (Issue #828)

## Problem Description

When saving a Rich Text widget, the application updates the asset and notifies listeners of page changes via `PSPageChangeHandler`. Inside `PSPageChangeHandler.pageChanged()`, multiple updates are performed on the page (updating link text, author, and page summary). In the original implementation, each of these helper methods independently triggered a transaction by calling `contentItemDao.save(page)` sequentially on separate transactions. Under Derby (or under concurrent load), this frequently resulted in a lock timeout (SQL error code 5,202) on the `CMDB.CONTENTSTATUS` table because multiple sequential transactions in the same HTTP request thread would compete or conflict on locks.

## Resolution Design

1. **Transaction Wrapping**: Wrap the entire logic within `pageChanged()` in a single transaction context using programmatic `TransactionTemplate` (backed by the Spring context's `sys_transactionManager`). This ensures all database reads and writes reuse the same database connection and transaction context, eliminating internal lock contentions and ensuring atomic success or failure.
2. **Batching Modifications**: Refactor the helper methods (`updateLinkText`, `updateAuthor`, and `updateSummary`) to update page fields in-memory and return a boolean flag indicating if any modification actually occurred.
3. **Single-Save Execution**: At the end of `pageChanged()`, execute `contentItemDao.save(page)` exactly once if and only if any modifications were made to the page fields.

## Validation

- Verified all sitemanage unit tests pass (295 tests successfully run, 0 failures).
- Applied Google Java Format guidelines via spotless formatter.

