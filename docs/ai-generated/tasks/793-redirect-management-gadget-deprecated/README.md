# Task: Move Redirect Management Gadget to Deprecated (issue #793)

**Branch**: `bugfix/793-redirect-management-to-deprecated`
**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/793
**Commit**: 4294738bb
**Date**: 2026-06-02
**Agent**: Grok (in percussioncms wing)

## Summary

The Redirect Management gadget (defined in `perc_website_config_gadget.xml` with title "Redirect Management" and category="integration") was appearing in the Custom type in the Dashboard Gadget Tray instead of Deprecated.

## Root Cause

- Gadget type is determined solely by `GadgetRepositoryListingServlet.getGadgetType(title)` which loads `GadgetRegistry.xml`.
- If no match, falls back to "Custom" (line 188).
- The Deprecated `<group>` did not contain an entry for "Redirect Management" (unlike Activity, Google Setup, Siteimprove, etc.).
- Client-side logic in `PercDashboard.js` (manageCategories, containsCategory) and server filter then classified it as Custom.

## Changes

- Added entry to `WebUI/src/main/resources/com/percussion/webui/gadget/servlets/GadgetRegistry.xml` under Deprecated group:
  `<gadget name="Redirect Management" baseuri="/cm/gadgets/repository/perc_website_config_gadget" file="perc_website_config_gadget.xml"/>`
- Added assertion in `WebUI/src/test/java/com/percussion/webui/gadget/servlets/PSGadgetRepositoryListingTests.java`:
  `assertTrue("Redirect Management should be registered as Deprecated", "Deprecated".equals(typeMap.get("Redirect Management")));`

## Verification

- Local test: `./mvn-env.sh test -Dtest=PSGadgetRepositoryListingTests -pl WebUI` → Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS
- Followed AGENTS.md: pulled base branch, created feature branch with issue # in name, tested before push, no direct commit to development-8.1.x

## mkd Logging

- Logged via mkd-mcp-http__mkd_log (wing=percussioncms, room=log)
- mkd task created and completed: task_1780398158565
- Receipts: 3889 (log), 3890 (push log)

## Next Steps

- Create PR from the branch: https://github.com/intersoftdatalabs-in/percussioncms/pull/new/bugfix/793-redirect-management-to-deprecated
- Reference issue #793
- Run full validations if needed: spotless, checkstyle, etc. before merge

## Related Files

- GadgetRegistry.xml
- PSGadgetRepositoryListingTests.java
- docs/ai-generated/tasks/793-redirect-management-gadget-deprecated/README.md (this)

