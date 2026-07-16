# Erlang review: 985-clean-install-dir / #1157

**Date**: 2026-07-16  
**Scope**: Uncommitted cleaner + Main + tests + docs (exclude `org/`)

## Recommendation (initial)

**`request-changes`**

## Issues

### BUG-1 — `Integer.parseInt(null)` on missing Version.properties keys

**Location**: `Main.java` version load (used immediately for cleanup eligibility)

When `Version.properties` is missing, `loadVersionProperties` returns an empty `Properties` (never null). The block always runs; `getProperty("majorVersion")` is null; `Integer.parseInt(null)` throws **NPE** (not `NumberFormatException`). That aborts `main` before install and before cleanup.

**Fix**: Only parse when non-blank; catch NPE/NumberFormatException; leave major/minor at 0.

### BUG-2 — Inconsistent symlink / `isDirectory` following

**Location**: `ObsoleteInstallDirCleaner.listEligibleCandidates` / `isJBossBakEligible`

Some checks use `Files.isDirectory(path)` (follows links); `addIfDirectory` uses `NOFOLLOW_LINKS`. AppServer eligibility follows links. Can mis-classify symlink trees.

**Fix**: Use `LinkOption.NOFOLLOW_LINKS` consistently for candidate and AppServer existence checks. Treat top-level candidate that is a **symlink** as a single deletable node (delete link only, never follow).

### BUG-3 — Weak path-prefix safety for confinement

**Location**: `isUnderInstallRoot`

Rely only on `Path.startsWith` after normalize. Harden by requiring `root.relativize(target)` has no `..` and is not absolute.

### BUG-4 — Partial delete success leaves no “still present” summary

When proceed=true and some deletes fail, `retained` is empty; failed list exists but operator may miss that data remains.

**Fix**: After delete loop, any candidate still existing goes to retained or stay in failed with clear wording; report always lists outcomes.

### SUGGESTION — warn-and-continue multi-candidate test

Add test where one path fails confinement and another deletes successfully.

## Gate

May commit: **no** until BUG-1–4 fixed and tests green.
