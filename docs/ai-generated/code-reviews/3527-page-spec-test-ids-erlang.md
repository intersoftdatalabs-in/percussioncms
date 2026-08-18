# Erlang review — #3527 page-spec TEST_IDS + Virtual-enabled assertions

**Branch:** `fix/issue-3527-page-spec-test-ids` (stacks on `fix/issue-3528-type-picker-helper` / PR #3530)  
**Scope:** uncommitted page spec + helper unit vs stacked #3528 tip  
**Date:** 2026-08-17  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Cycle-verify residual for parent #3512 / cluster PR #3526. After #3530 restored `TEST_IDS.typeUnavailable`, `confirmType`, `confirmTemplateName`, and `pageNote`, `explorer-site-create-page.spec.js` still asserted **Virtual blocked** (`typeUnavailable` visible, Next disabled). The union wizard enables Page and Virtual and does not render `site-create-type-unavailable`. This change aligns the page spec with the shipped union (same IDs as `SiteCreateWizard` / type-picker spec) and adds a unit list of every key the page spec interpolates so another cluster absorb cannot go green with `undefined` locators.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (`TEST_IDS` key-closure unit; Playwright spec asserts enabled Virtual/Page + Traditional skip-template + Page happy path)
- Cross-platform path I/O: N/A (no filesystem path construction; URL helpers already use `/`)
- Change-class companions: Playwright page spec + helper unit (product-docs N/A — test-only)
- **May commit/push:** yes

## Issues

None (gate).

### Notes (non-blocking)

- Helper IDs were **not** rewritten; #3530 already aligned them to wizard `data-testid`s. This slice only consumes those IDs.
- `site-create-type-unavailable` remains a TEST_IDS string; union wizard does not render it while all kinds are enabled. Spec asserts `toHaveCount(0)`.
- Happy-path Page create is unchanged except radio `.check()` (same control as type-picker).

## Change-class companions

| Kind | Status |
|------|--------|
| Helper | unchanged (stacked #3530 IDs) |
| Unit | `explorer-sites-list-create.test.js` page-spec key closure |
| Playwright | `explorer-site-create-page.spec.js` union-enabled assertions |
| product-docs | N/A (Playwright/test-only) |

## Memory patterns hit

- Incomplete change-class closure — specs interpolating helper IDs the union dropped
- Tests that only grep source strings — avoided; unit asserts exported string values
