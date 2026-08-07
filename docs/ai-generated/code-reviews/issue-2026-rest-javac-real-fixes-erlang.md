# Erlang review: issue #2026 rest javac real fixes

**Branch:** `fix/issue-2026-rest-javac-real-fixes`  
**Date:** 2026-08-07  
**Reviewer:** Erlang (self-review, Grok Build / night-issue-prs)

## Summary

Replaces residual `@SuppressWarnings` in `rest` (this-escape on `Guid` / `FolderNotFoundException`; serial on `RestExceptionBase` and summary DTOs) with real constructor / field-type fixes aligned with sibling night PRs (#2285–#2287). Adds behavioral unit tests. No residual `@SuppressWarnings` in `rest` main sources.

## Scope

- `rest/src/main/java/com/percussion/rest/Guid.java`
- `rest/src/main/java/com/percussion/rest/errors/FolderNotFoundException.java`
- `rest/src/main/java/com/percussion/rest/errors/RestExceptionBase.java`
- `rest/src/main/java/com/percussion/share/relationship/data/PSLocalDependencySummary.java`
- `rest/src/main/java/com/percussion/share/relationship/data/PSRelationshipSummary.java`
- `rest/src/main/java/com/percussion/share/relationship/data/PSTaxonomySummary.java`
- New tests: `GuidTest`, `FolderNotFoundExceptionTest`, `PSSummaryDtoSerialFieldsTest`
- Base: `origin/main`
- Memory patterns: this-escape via overridable setters; serial non-Serializable field types; missing behavioral tests

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral tests: present for Guid ctor, FolderNotFound cause, summary defensive copies
- Cross-platform path review: N/A (no path/file I/O in diff)
- **May commit/push: yes**

## Issues

None.

## Verification

- `mvnw -pl rest clean install` — BUILD SUCCESS, 229 tests, 0 failures
- Main compile: no `[WARNING]` on `rest/src/**` for this-escape or serial after fixes
- Zero `@SuppressWarnings` remaining under `rest/`
