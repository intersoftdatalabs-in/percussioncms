# Issue #829 — Directory Index Page Not Updated on Person/Department Asset Changes

## Summary

Directory Index pages (pages using the `percDirectory` widget) are not consistently re-published with updated information when `percPerson`, `percDepartment`, or `percOrganization` assets are approved/published. Editors were forced to save/approve the assets multiple times to see updates.

## Root Cause

1. **Query-Based Widget**: The `percDirectory` widget compiles its list of people/departments using **JCR queries** at assembly time (e.g. `select rx:sys_contentid from rx:percPerson ...`). There are no Active Assembly (AA) relationships between the Directory Index page and the dynamic asset items it displays.
2. **Workflow Action Timing**: The original fix introduced `sys_DirectoryIndexTouchWorkflowAction` (`PSDirectoryIndexTouchWorkflowAction.java`) which touches the pages *during the workflow transition transaction* (pre-commit).
3. **Asynchronous Indexing Race Condition**: JCR indexing (Apache Jackrabbit / Lucene) runs asynchronously via a background thread with a delay (minimum 20 seconds).
4. **Stale Publishing**: If a publish job runs immediately after approval, the JCR query executed during page assembly runs against a Lucene index that hasn't yet indexed the new person/department asset's changes.
5. **Modified Date Reset**: Once published, the page's modified date is reset, and subsequent incremental publishes ignore it until it is touched again.

## Fix

We resolve the race condition by adding a **post-indexing touch mechanism** that runs immediately after the search indexer completes indexing the changed assets.

1. **Pre-commit Workflow Action (Existing)**: The `sys_DirectoryIndexTouchWorkflowAction` still runs during workflow transition to mark the pages as modified.
2. **Post-indexing Event Handler (New)**: In `PSSearchIndexEventQueue.java`, we catch when the background thread successfully indexes a `percPerson`, `percDepartment`, or `percOrganization` asset.
3. **Post-indexing Touch**: Once indexing is completed and committed to Lucene, we call `IPSPublisherService.touchContentTypeItems([percDirectory])` to touch all directory pages again.
   - This ensures that if the page was assembled with stale data during an immediate publish run, it is marked as modified *again* after the index is updated.
   - Eventual consistency is guaranteed: the next publish run (scheduled, manual, or subsequent incremental run) will republish the directory index page with the correct, up-to-date query results.

## How the Mechanism Works

```
     percPerson approved (Workflow Transition)
                  │
                  ├─────────────────────────────────────────┐
                  ▼                                         ▼
sys_DirectoryIndexTouchWorkflowAction            Item queued for search index
                  │                                         │
                  ▼                                         ▼
Touches Directory Index pages (pre-commit)       Background thread processes queue (20s delay)
                  │                                         │
                  ▼                                         ▼
  [Optional Immediate Publish Run]                Lucene index updated & committed
                  │                                         │
  Processes Directory Index page                            ▼
(Assembled with stale JCR index results)         checkAndTouchDirectoryIndex() fires
                  │                                         │
                  ▼                                         ▼
       Published with stale data                  Touches Directory Index pages (post-indexing)
                  │                                         │
                  └────────────────────────┬────────────────┘
                                           │
                                           ▼
                           Directory Index pages modified
                                           │
                                           ▼
                          [Next Incremental Publish Run]
                                           │
                                           ▼
                           Re-published with fresh data
```

## Files Changed

|                                     File                                      |                                          Change                                          |
|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `system/src/main/java/com/percussion/search/PSSearchIndexEventQueue.java`     | Modified `processNextEventSet` to trigger `checkAndTouchDirectoryIndex()` post-indexing. |
| `docs/ai-generated/tasks/829-directory-index-touch-on-asset-update/README.md` | Updated documentation to explain JCR indexing race condition and post-indexing touch.    |

## Verification and Testing

1. Edit a `percPerson` or `percDepartment` asset and transition to approved.
2. The asset is placed in the FTS queue (`PSX_SEARCHINDEXQUEUE`).
3. Check the server logs to verify `PSSearchIndexEventQueue` log output:
   - `PSSearchIndexEventQueue: content type id ... indexed successfully. Touching Directory Index pages.`
   - `PSSearchIndexEventQueue: touched ... Directory Index pages/assets post-indexing.`
4. Verify that the `LASTMODIFIEDDATE` of the pages containing `percDirectory` is updated after the search indexer completes.

