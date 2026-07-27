# Erlang review — 989-react-cui-widget-builder (pre-push)

**Date:** 2026-07-17  
**Reviewer:** Erlang (strict independent pre-commit/pre-push)  
**Branch:** `989-react-cui-widget-builder`  
**PR:** #1337

## Summary

Pre-push review of **uncommitted** work only (branch is even with
`origin/989-react-cui-widget-builder` except local modifications).

The pending change is a message-stability fix for `PSAclService.createAclImpl`
null-arg `IllegalArgumentException` text, responding to a Kilo PR suggestion
after thin `createAcl` → `createAclImpl` delegation. No logic, path, or
transaction boundary change.

## Scope

|            Item            |                                     Detail                                     |
|----------------------------|--------------------------------------------------------------------------------|
| Uncommitted                | `system/services/.../PSAclService.java` — exception message alignment          |
| Unpushed commits           | none (`origin...HEAD` = 0/0)                                                   |
| Intent                     | Keep public null-arg messages stable (`cannot be null`) after thin proxy entry |
| Prior report               | none for this slug                                                             |
| Memory patterns hit        | none blocking (no path I/O; no new non-trivial logic)                          |
| Cross-platform path review | N/A — no filesystem path handling in this diff                                 |

## Diff under review

```diff
// createAclImpl null checks only
- throw new IllegalArgumentException("objGuid may not be null");
+ throw new IllegalArgumentException("objGuid cannot be null");
- throw new IllegalArgumentException("owner may not be null");
+ throw new IllegalArgumentException("owner cannot be null");
+ // comment: Messages match IPSAclService.createAcl contract
```

Aligned with `IPSAclService.createAcl` default (`Objects.requireNonNull(..., "… cannot be null")`).

## Recommendation

**approve**

## Gate

|        Field        |   Value   |
|---------------------|-----------|
| Gate                | `approve` |
| **May commit/push** | **yes**   |

No bugs, no missing behavioral tests required for a message-string alignment,
no path/portability issues.

## Issues

_None._

### Notes (non-blocking)

1. **Historical note (already on origin, not in this uncommitted pack):** commits
   `02804a0922` / `84bf73e8ee` (ACL `@Transactional` proxy entry points, XXE
   hardening on packaging tests) were pushed before this Erlang pass. They are
   not re-gated here; no open issues found in the uncommitted slice that depend
   on reopening them. Future pushes should run Erlang **before** `git push`.
2. Exception type remains `IllegalArgumentException`; only the message string
   changes. No test in-tree asserts the old `"may not be null"` wording for
   these args.

## Re-review

_N/A — first report for this uncommitted pack._
