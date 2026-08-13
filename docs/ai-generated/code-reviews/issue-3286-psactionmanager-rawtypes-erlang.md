# Erlang review: issue #3286 PSActionManager remaining rawtypes

Date: 2026-08-13
Branch: fix/issue-3286-psactionmanager-rawtypes
Reviewer persona: Erlang (pre-commit gate)

## Scope
- Uncommitted + branch vs `origin/main`
- `modules/DesktopContentExplorer/.../PSActionManager.java`
- New `PSActionManagerTypingTest`

## Summary
Finish #2371 / #2326 leftovers inside `PSActionManager` only: drop dead `asNodes` / `asDisplayFormats` wrappers (sources already `Iterator<PSNode>` / `Iterator<PSDisplayFormat>`), type `prepareSearchFilterMap` to `Map<String, PSSearchFieldFilter>`, collapse four `getParamKeys()` suppressions into one inherent adapter, and take `asMenuActions(PSMenuAction)` so call sites no longer pass raw iterators. Three method-level `rawtypes`/`unchecked` suppressions remain and are documented as inherent (`PSMenuAction.getChildren` / `setChildren`, `PSParameters.getParamKeys` still raw in `system`). No product behavior change.

## Recommendation
approve

## Gate
PASS — May commit/push: yes

## Memory patterns hit
- Missing behavioral unit tests for new/changed non-trivial logic — adapters + empty/null param-key and menu-child round-trip covered
- Incomplete change-class closure — DCE-only slice; no public/protected signature change; no Spring scan / WebUI / product-docs companion required
- Non-portable filesystem path joins — none (existing `"../" + url` is URL-relative, not OS path)

## Findings
### Bugs
None.

### Behavioral tests
- `PSActionManagerTypingTest`: param keys (including null), menu child set/replace/round-trip, null action iterator
- Module suite: `PSActionManagerTypingTest` Tests run: 5, Failures: 0; full DCE `mvnw clean install` BUILD SUCCESS

### Cross-platform paths
No new filesystem I/O. `getXMLDocument("../" + url)` remains a CMS URL path (always `/`).

### Change-class companions
- Package-private adapters only (same module tests). No `final`/`sealed`, no public signature change → C2 reverse-deps N/A.
- Product-docs N/A (internal generics / tech-debt).
- Playwright / C5 N/A (no UI surface).

### Suggestions (non-blocking)
- Later system-module slice can parameterize `PSMenuAction.getChildren` / `setChildren` and `PSParameters.getParamKeys` and delete the three inherent adapters.

> Co-Authored by Grok Build using grok-4.5 with agent main.
