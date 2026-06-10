# Bug Fix #819 – jQuery Dependency Scripts Fail with "jQuery is not defined" Errors When Using jQuery Widget

## Problem Summary

- When a template or page is configured with the jQuery Widget, multiple dependent JavaScript files (like `jquery-ui.js`, `jquery.cookie.min.js`, `PercGlobalVariables.js`, `perc_common_ui_slim.js`) fail to execute in the browser.
- The browser console displays `ReferenceError: jQuery is not defined` errors because the jQuery library is not loaded/evaluated before the dependent scripts are run.

## Root Cause

1. **JQuery Loaded via Dependency Check Only:** In [sys_assembly.vm](file:///home/nate/projects/java8/percussioncms/system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm), `#print_jquery("additionalHeadContent")` was wrapped inside the `#foreach($js_link in $rx.pageutils.javascriptLinks(...))` loop.
   - If a page has the jQuery Widget but has no widgets on it that declare a direct JCR dependency on `/jquery.js` (e.g. only custom Javascript scripts referencing jQuery), the head loop never encounters `/jquery.js`, meaning the jQuery script is *never* printed in the head.
2. **Deferral Execution Mismatch:** If the jQuery Widget has `isDeferred` set to `"yes"`, it prints jQuery with the `defer` attribute.
   - However, other scripts (e.g., `jquery.cookie.min.js`, `PercGlobalVariables.js`, and `perc_common_ui_slim.js`) were printed without the `defer` attribute.
   - Browsers execute synchronous scripts immediately during HTML parsing, but execute deferred scripts only after parsing completes. As a result, the dependent scripts executed before jQuery loaded, leading to `jQuery is not defined` runtime errors.

## Solution

Modified the template assembly script loading logic in [sys_assembly.vm](file:///home/nate/projects/java8/percussioncms/system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm) to:
1. **Unconditional head JQuery Output:** If the jQuery Widget is present on the page/template, we load jQuery in the head based on the widget's config (or default), bypassing the head loop entirely. The loop in the head only processes `/jquery.js` if the jQuery Widget is *not* present.
2. **Synchronize Deferral:** Introduced a `$deferDependents` flag. If the jQuery Widget is present and configured with `isDeferred="yes"` (and the page is not in edit mode), we automatically propagate the `defer` attribute to all dependent scripts:
   - `jquery-ui.js` (including inside `print_jqueryUI` fallback and when loaded in preview mode)
   - All other Javascript scripts collected from `$rx.pageutils.javascriptLinks(...)` printed in the footer.
This ensures correct script execution order under all script location and deferral settings.

## Validation

- Ran spotless check (`./mvn-env.sh spotless:check`) and verified no formatting violations.
- Built the `system` module via `./mvn-env.sh clean install -pl system -DskipTests` to ensure changes integrate successfully.
