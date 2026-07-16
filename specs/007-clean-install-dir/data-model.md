# Data Model: Clean Obsolete Install Directories

**Feature**: `specs/007-clean-install-dir`  
**Date**: 2026-07-16

No application database schema. Install-time filesystem entities only.

## Entities

### 1. InstallRoot

| Field | Type | Description |
|-------|------|-------------|
| path | absolute path | CMS install directory being upgraded |

**Rules**: Cleanup only mutates children of this path; never parent or sibling trees.

### 2. ObsoleteCandidate

| Field | Type | Description |
|-------|------|-------------|
| relativePath | string | e.g. `PreInstall`, `JBossServerXML_BAK` |
| absolutePath | path | `installRoot/relativePath` (or casing variant) |
| exists | boolean | directory (or allowed node) present |
| sizeBytes | long | recursive size estimate before delete |
| eligible | boolean | version/safety gate (e.g. JBoss bak) |

**MVP relative names** (see research D3):

- `PreInstall`
- `_Percussion_Installation` / `_Percussion_installation` (whichever exists)
- `JBossServerXML_BAK` (conditional eligibility)

### 3. CleanupDecision

| Field | Type | Description |
|-------|------|-------------|
| source | enum | `flag` \| `interactive-yes` \| `interactive-no` \| `none` (no candidates / new install) |
| cleanInstallDirFlag | boolean | from `--clean-install-dir`, default false |
| interactive | boolean | TTY available |
| proceed | boolean | whether delete runs |

**Precedence**:

1. Not upgrade → no cleanup  
2. No eligible candidates → no cleanup  
3. Flag true → proceed without prompt  
4. Interactive + candidates → prompt → yes/no  
5. Non-interactive + flag false → no cleanup  

### 4. CleanupResult

| Field | Type | Description |
|-------|------|-------------|
| deleted | list of paths | successfully removed |
| failed | list of (path, message) | warn-and-continue |
| skipped | list of paths | not eligible or user declined |
| totalBytesAttempted | long | sum of sizes for candidates that proceeded |
| continueUpgrade | always true for MVP | FR-013 |

## State transitions

```text
[Start Main]
    → resolve install path + CLI options
    → detect upgrade? ──no──→ [skip cleanup]
              │ yes
              ▼
    → list eligible candidates + sizes
              │ empty
              ▼
         [skip cleanup]
              │ non-empty
              ▼
    → decide (flag / prompt / default no)
              │ proceed=false
              ▼
         [log retain; skip delete]
              │ proceed=true
              ▼
    → delete each candidate (best-effort)
              ▼
    → log CleanupResult
              ▼
    → continue extract / ANT upgrade (always for cleanup outcome)
```

## Validation rules

- Candidate absolute path must start with install root after normalization.
- Symlinks: do not delete if target resolves outside install root.
- Size estimate may be approximate; zero-size empty dirs still listable.

## Relationships

- One InstallRoot has many ObsoleteCandidates (0–N present).
- One CleanupDecision applies to the set of eligible candidates for a run.
- One CleanupResult records outcomes of that decision.
