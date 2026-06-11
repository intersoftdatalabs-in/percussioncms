# Implementation Notes – Issue #777 Asset Status Border

**Status**: Spec complete. Awaiting team review + implementation start.

## Decisions Made During Spec Creation

- We will **add** a new higher-level JEXL method rather than changing the semantics of the existing `isInRecycler(...)` (many other callers depend on the strict "has recycle rel" behavior).
- Class name strategy: Keep `.perc-recycled-asset` working indefinitely for backward compatibility. Introduce `.perc-problem-asset` as the new primary semantic class. The VM can emit both when appropriate.
- Tooltip will become dynamic (reason-based).
- Initial scope focuses on the widget container level. Per-item highlighting inside auto-lists is noted as desirable future work.

## Open Questions for Implementation

- Should the new check respect the current assembly "staging vs production" context for publishability? (Current proposal starts with the common non-staging approve-state rule.)
- Do we want a small `AssetProblemInfo` DTO or just return structured data via a Map for Velocity?
- Any performance concerns with the extra relationship query per widget? (Likely negligible.)

## Files Expected to Change (see SPEC.md for details)

- PSPageUtils.java (new method + helpers)
- sys_assembly.vm
- perc_decoration.css
- pageutils.extension XML
- Tests + this task folder (add PR_DESCRIPTION.md etc.)

## How to Verify Manually (from the GitHub issue)

1. Select EDITOR.
2. Open widget-test-page/file/index.html (or equivalent test page).
3. Add two File widgets in Layout tab, browse + select two different file assets.
4. Go to Assets, locate the two files, approve + publish them.
5. Refresh Layout tab, Content tab, and Preview.
6. **Expected**: Both file widgets are clean (no red dotted border). Tooltips reflect normal state.
7. Additional manual tests: put one asset back into Draft, truly recycle one (with vs. without folder rel), etc.

---

Update this file with actual implementation choices, gotchas, and links to the PR once work begins.
