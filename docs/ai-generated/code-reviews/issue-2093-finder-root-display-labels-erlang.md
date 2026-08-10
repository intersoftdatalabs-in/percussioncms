# Erlang review: issue #2093 Finder root display labels

**Branch:** `fix/issue-2093-finder-root-display-labels`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (agent self-review before PR)

## Summary

Display-only I18N for classic Finder repository roots (Sites/Assets/Design/Search/Recycling) via pure helper `perc_finder_root_display.js` and `make_item` wiring. Path identity stays English.

## Scope

- `WebUI/src/main/webapp/cm/plugins/perc_finder_root_display.js` (new)
- `WebUI/src/main/webapp/cm/widgets/perc_finder.js` (+ war dual-ship)
- Peers: `perc_save_as.js`, `PercContentBrowserWidget.js` (+ war)
- `finder_js.jsp` includes (src app, pages, war)
- `WebUI/src/test/js/percFinderRootDisplay.test.js`

Cross-platform path review: no file I/O or path joining in this change (browser display strings only). Clean.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral unit tests: yes (pure map + source-contract wiring)
- Non-portable paths: N/A / clean
- **May commit/push: yes**

## Issues

None at `bug` severity.

### suggestion (non-blocking)

1. **HTML concatenation of `displayLabel`** in `make_item` inherits pre-existing pattern of injecting names into HTML strings. Translated roots from TMX are trusted product strings (no user HTML). Follow-up could use `.text()` for the name div if/when Finder listing construction is modernized.
2. **Playwright Spanish smoke** deferred to #2094 by design (issue triage / residual).

## Tests

- `npm test -- --run src/test/js/percFinderRootDisplay.test.js` (8 passed)
- WebUI `mvnw clean install` (see PR gates evidence)
- Spotless apply+check on WebUI module (in-scope only)

