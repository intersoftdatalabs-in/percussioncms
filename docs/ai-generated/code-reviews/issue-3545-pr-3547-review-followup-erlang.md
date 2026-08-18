# Erlang review — PR #3547 review follow-up (#3545)

**Date:** 2026-08-18  
**Branch:** `fix/issue-3545-translations-guid-content-id`  
**Scope:** uncommitted review-response (comment accuracy + Playwright detach retry)

## Summary

Kilo threads on #3547: (1) `variantsKey` comment claimed folders stay null while
code falls back to raw `itemId`; (2) item-row click lacked the detach retry used
for folder rows. Comment rewritten, unit test asserts GET uses the raw token on
parse-null, Playwright helper retries item (and folder fallback) clicks.

## Recommendation

**approve** — May commit/push: **yes**

## Gate

- Bugs: none
- Behavioral tests: new Vitest case for parse-null GET fallback
- Cross-platform path review: N/A (no filesystem I/O)

## Issues

None.

## Memory patterns hit

- Orphaned JSDoc above extracted helper — fixed before commit
- Public helper comment contradicting implementation — addressed

> Co-Authored by Grok Build 1.0.4 using grok-4.6 with agent night-issue-prs.
