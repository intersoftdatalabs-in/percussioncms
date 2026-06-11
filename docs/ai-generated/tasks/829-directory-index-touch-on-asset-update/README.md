# Issue #829 — Directory Index Page Not Updated on Person/Department Asset Changes

## Summary

Directory Index pages (pages using the `percDirectory` widget) are not re-published when
`percPerson`, `percDepartment`, or `percOrganization` assets are approved/published.

## Root Cause

The `percDirectory` widget assembles its list of people via **JCR queries** at assembly time
(e.g. `select rx:sys_contentid from rx:percPerson where rx:personOrganization = :orgSearchId`).
There are **no** Active Assembly (AA) relationships between the Directory Index page and the
individual `percPerson`/`percDepartment`/`percOrganization` items it displays.

When a `percPerson` item is approved, the standard workflow action
`sys_TouchItemsWorkflowAction` calls `touchActiveAssemblyParents()`. However, since there is
no AA parent chain from the person item to the Directory Index page, the page's
`LASTMODIFIEDDATE` is never updated. The incremental publishing filter therefore does not
detect the page as modified, and does not re-publish it.

## Fix

### Code Change: New Workflow Action Class

**File**: [`modules/extensions-main/src/main/java/com/percussion/extensions/general/PSDirectoryIndexTouchWorkflowAction.java`](../../../modules/extensions-main/src/main/java/com/percussion/extensions/general/PSDirectoryIndexTouchWorkflowAction.java)

A new workflow action class `PSDirectoryIndexTouchWorkflowAction` was created. When
triggered, it:

1. Resolves the `percDirectory` content type ID via `PSItemDefManager`
2. Calls `IPSPublisherService.touchContentTypeItems(percDirectoryTypeId)` which:
   - Finds **all** `percDirectory` content items in the repository
   - Touches them (updates `LASTMODIFIEDDATE`)
   - Touches their Active Assembly parent pages (the Directory Index pages that contain the
     `percDirectory` widget)
3. The touched Directory Index pages are then picked up by the incremental publishing filter
   and re-published on the next publish run

**File**: [`modules/extensions-main/src/main/resources/Java/Extensions.xml`](../../../modules/extensions-main/src/main/resources/Java/Extensions.xml)

The new extension is registered with the name `sys_DirectoryIndexTouchWorkflowAction` in the
`global/percussion/extensions/general/` context.

### Configuration: Add Workflow Action to Approve Transitions

The new workflow action must be added to the **"Approve"** and **"Quick Approve"** transitions
of the **Default Workflow** (and any other workflows used by `percPerson`, `percDepartment`,
or `percOrganization` content types — typically workflows 4, 5, 6, and 7).

This can be done via:

**Option A: Percussion Workbench UI**

1. Open Percussion Workbench → Workflow → Default Workflow
2. Select the **"Approve"** transition
3. In the **Actions** tab, add:

   ```
   Java/global/percussion/extensions/general/sys_DirectoryIndexTouchWorkflowAction
   ```
4. Repeat for the **"Quick Approve"** transition
5. Repeat for any other workflows used by the directory content types

**Option B: Direct Database Update (MS SQL Server)**

Run the following SQL to append the new workflow action to the Approve and Quick Approve
transitions of the Default Workflow. Adjust `WORKFLOWAPPID` values as needed for your
installation.

```sql
-- Preview current transition actions
SELECT TRANSITIONID, TRANSITIONLABEL, TRANSITIONACTIONS, WORKFLOWAPPID
FROM TRANSITIONS
WHERE WORKFLOWAPPID IN (4, 5, 6, 7)
  AND TRANSITIONLABEL IN ('Approve', 'Quick Approve', 'Publish')
ORDER BY WORKFLOWAPPID, TRANSITIONID;

-- Update: append new action to existing ones (separator is \n)
UPDATE TRANSITIONS
SET TRANSITIONACTIONS =
    CASE
        WHEN TRANSITIONACTIONS IS NULL OR TRANSITIONACTIONS = ''
            THEN 'Java/global/percussion/extensions/general/sys_DirectoryIndexTouchWorkflowAction'
        ELSE TRANSITIONACTIONS + CHAR(10) +
             'Java/global/percussion/extensions/general/sys_DirectoryIndexTouchWorkflowAction'
    END
WHERE WORKFLOWAPPID IN (4, 5, 6, 7)
  AND TRANSITIONLABEL IN ('Approve', 'Quick Approve', 'Publish');
```

> **Important**: Back up your database before running SQL updates. Verify the
> `WORKFLOWAPPID` values match your environment by running the SELECT query first.

## How the Mechanism Works

```
percPerson approved
    │
    ▼
Workflow fires sys_DirectoryIndexTouchWorkflowAction
    │
    ▼
PSDirectoryIndexTouchWorkflowAction.performAction()
    │
    ▼
IPSPublisherService.touchContentTypeItems([percDirectory])
    │   (finds all percDirectory items)
    ├── touches each percDirectory item (LASTMODIFIEDDATE = NOW)
    │
    ├── touchActiveAssemblyParents([percDirectory item IDs])
    │       (follows AA relationship: Page → percDirectory widget)
    │
    └── touches Directory Index pages (LASTMODIFIEDDATE = NOW)
            │
            ▼
    Incremental publisher detects changed pages
            │
            ▼
    Directory Index pages re-published with fresh data
```

## Testing

1. In Percussion UI, create a `percPerson` asset assigned to an Organization
2. Create a Directory Index page with the `percDirectory` widget configured to display that
   Organization
3. Publish both items
4. Edit the `percPerson` and change their title or name
5. Approve the `percPerson` change
6. **Before fix**: The Directory Index page keeps the old data until manually re-published
7. **After fix**: The next publish run automatically re-publishes the Directory Index page
   with the updated person data

## Files Changed

|                                                        File                                                        |                                Change                                |
|--------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `modules/extensions-main/src/main/java/com/percussion/extensions/general/PSDirectoryIndexTouchWorkflowAction.java` | **NEW** — workflow action implementation                             |
| `modules/extensions-main/src/main/resources/Java/Extensions.xml`                                                   | Added `sys_DirectoryIndexTouchWorkflowAction` extension registration |
| `docs/ai-generated/tasks/829-directory-index-touch-on-asset-update/README.md`                                      | This documentation                                                   |

