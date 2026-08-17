# Erlang review — #3528 type-picker helper export + typeUnavailable testid

**Branch:** `fix/issue-3528-type-picker-helper` (stacks on `cluster/night-issue-20260817-site-create-wizard` @ `95ac375abf`)  
**Scope:** uncommitted helper/spec/unit vs cluster tip  
**Date:** 2026-08-17  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Cycle-verify residual for parent #3512 / cluster PR #3526. The cluster union left `explorer-site-create-type-picker.spec.js` importing `advanceTraditionalTypeStep` and `TEST_IDS.typeUnavailable` (and `confirmType`) that the helper no longer exported, so locators became `[data-testid=undefined]` and the Traditional confirm test threw `TypeError`. This change restores the helper IDs and export, adds a mock-page unit test for the helper, and updates the type-picker spec to assert the **shipped union** (Page/Virtual enabled, no force-disable).

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (`advanceTraditionalTypeStep` mock-page unit test; TEST_IDS assertions; Playwright spec aligned to product)
- Cross-platform path I/O: N/A (no filesystem path construction; URLs use `/`)
- Change-class companions: Playwright helper + unit + surface spec (product-docs N/A — test helper only)
- **May commit/push:** yes

## Issues

None (gate).

### Notes (non-blocking)

- Wizard was **not** rewritten. `site-create-type-unavailable` is restored as a TEST_IDS string for specs; the union wizard does not render that node while all kinds are enabled. Spec asserts `toHaveCount(0)` rather than inventing a blocking banner.
- Extra IDs (`confirmType`, `confirmTemplateName`, `confirmBaseTemplate`, `pageNote`) match existing wizard `data-testid`s so sibling #3527 can stack without `undefined` interpolation.
- Page spec (#3527) still asserts Virtual blocked; that is out of scope here.

## Change-class companions

| Kind | Status |
|------|--------|
| Helper | `TEST_IDS` + `advanceTraditionalTypeStep` export |
| Unit | `explorer-sites-list-create.test.js` (IDs + mock-page helper) |
| Playwright | type-picker spec asserts enabled Page/Virtual |
| product-docs | N/A (test helper) |

## Memory patterns hit

- Missing behavioral tests for new/changed non-trivial logic — addressed with mock-page unit test
- Incomplete change-class closure — helper IDs the specs interpolate
