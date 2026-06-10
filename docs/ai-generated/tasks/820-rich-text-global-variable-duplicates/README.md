# Bug Fix #820 – Duplicate Global Variable Displayed in Rich Text Widget "Insert Global Variable" Dialog

## Problem Summary

- When inserting a Global Variable from the Rich Text Widget using the "Insert Global Variable" dialog, duplicate entries are displayed in the selection dialog.
- Specifically, the first variable in the global variables map is listed twice in the UI table.

## Root Cause

- Inside the TinyMCE plugin file [plugin.js](file:///home/nate/projects/java8/percussioncms/modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/plugin.js):
  - The loop iterating over `PercGlobalVariablesData` unconditionally appended the current variable name and value to the `dialogHtml` table row variable.
  - Immediately following that, it checked if `counter2 == 0` (meaning it was the first item) and, if true, appended the exact same variable's details a second time (adding a `scope="row"` attribute).
  - This resulted in the first global variable being duplicated.

## Solution

- Modified the logic in [plugin.js](file:///home/nate/projects/java8/percussioncms/modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/percglobalvariables/plugin.js) to use an `if-else` block:
  - If `counter2 == 0`, it appends the first row (with the `scope="row"` attribute).
  - Otherwise, it appends the subsequent rows normally.
- This prevents the first item from being duplicated while preserving any intended markup differences.

## Validation

- Checked code style compliance and ran `./mvn-env.sh spotless:apply` to ensure there are no formatting violations.
- Verified that the JS files compile and package correctly.

