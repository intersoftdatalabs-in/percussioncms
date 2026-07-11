# Data Model: Javadoc Cleanup for Content Explorer Module

**Spec**: `spec.md` | **Date**: 2026-07-11

This feature does not introduce or modify runtime data entities. The build pipeline and
the JDK javadoc tool do not exchange structured data with the module beyond the source
files already in the repository.

The only entities relevant to this feature are verification artifacts stored under
`specs/003-javadoc-cleanup/`:

| Entity | Purpose | Attributes | Source of truth |
|--------|---------|------------|-----------------|
| `BaselineReport` | Captures pre-cleanup javadoc diagnostics so SC-001 is measurable | Tool summary ("N errors", "N warnings"), raw stderr, capture date, JDK version, javadoc plugin version | `baseline-raw.txt` (this directory) |
| `PostCleanupReport` | Captures the same metrics after the cleanup so deltas vs. baseline are computable | Same attributes as `BaselineReport` plus a generated delta vs. baseline | produced by the implementation phase and stored at `post-cleanup.txt` (this directory) |
| `JavadocSuppressionRecord` | Optional inline audit trail for every `@SuppressWarnings("javadoc")` that gets introduced | File path, symbol line, justification comment text | Inline comment in the modified source file; surfaced in `PostCleanupReport` |

### State transitions (verification artifacts)

```text
[none] --capture--> BaselineReport
BaselineReport + apply-fix-loop --capture--> PostCleanupReport
PostCleanupReport --compare-deltas--> [acceptance: SC-001 met?]
```

No runtime entities; no migration; no schema impact (consistent with FR-004, FR-005 and
the Module Scope section of the spec).

## Validation

There are no runtime validators. The only validation is:

1. `BaselineReport` exists and is reproducible on a clean checkout.
2. `PostCleanupReport` exists and shows the required delta (≥ 80% warning reduction, 0
   errors).
3. `git diff` shows only comment/whitespace changes in tracked Java files (SC-004).
