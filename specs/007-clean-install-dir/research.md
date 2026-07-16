# Research: Clean Obsolete Install Directories on Upgrade

**Feature**: `specs/007-clean-install-dir`  
**Branch**: `985-clean-install-dir`  
**Date**: 2026-07-16  
**Source**: [#1157](https://github.com/intersoftdatalabs-in/percussioncms/issues/1157)

## Current-state findings

| Topic | Evidence |
|-------|----------|
| Upgrade vs new install | `install.xml` sets `do.install` / `do.upgrade` from presence of `ObjectStore` and related conditions; preinstall `Main` loads `Version.properties` into `majorVersion` / `minorVersion` before ANT. |
| PreInstall still referenced | `install.xml` `deleteOldLog4jJars` deletes under `${install.dir}/PreInstall/Backups/` with `failonerror="false"` — optional residue, not required for upgrade success. |
| `_Percussion_Installation` | `remove_PercussionInstallation.xml` exists with **TODO: Implement Me**; comment uses `_Percussion_installation`, echo uses `_Percussion_Installation`. Never fully implemented. |
| `JBossServerXML_BAK` | Created during upgrade from JBoss `AppServer/.../server.xml` (`install.xml` ~1689). `Main.updateJettyServerPortAndSSLToPreUpgradeSettings` reads it **after** ANT when `majorVersion == 5 && minorVersion < 4`. Early deletion of that stub is unsafe for that narrow upgrade class if `AppServer` is already gone. |
| CLI parsing | `DbInstallConfigResolver.parseArgs` already supports `--key=value` and `--key value` (same style as `--dbprops`). |
| Interactive console | Java `System.console()` is the portable TTY check; if null, treat as non-interactive. |

## Decisions

### D1 — Implement cleanup in preinstall Java (Main), not only ANT

**Decision**: Run detect → prompt/flag → delete in `com.percussion.preinstall` **after** install path and upgrade detection (`Version.properties` / equivalent), **before** jar extract / ANT `execJar`.

**Rationale**: Spec requires early timing and interactive prompt; console I/O and size estimation fit Java unit tests better than ANT. Avoids waiting for full distribution unpack before freeing tens of GB.

**Alternatives**: ANT-only target at start of `upgrade.chain` — harder to unit-test prompts; later in chain — violates FR-014.

### D2 — Upgrade detection for cleanup gate

**Decision**: Treat as **upgrade** when `installPath` contains a product marker consistent with existing install (prefer `Version.properties` present and readable, aligned with Main’s existing load). Skip cleanup entirely on new-install path (no version file / do.install semantics).

**Rationale**: Matches “upgrade only”; avoids touching empty new roots.

### D3 — MVP candidate path names (exact)

**Decision**:

| Relative path | Always offer if exists? | Notes |
|---------------|-------------------------|--------|
| `PreInstall` | Yes | Issue primary target; entire directory tree. |
| `_Percussion_Installation` | Yes if either casing exists | Also check `_Percussion_installation` on case-sensitive FS; delete the path that exists. |
| `JBossServerXML_BAK` | **Conditional** | Offer only when **not** in the 5.3-era migration window: i.e. when `majorVersion > 5` OR `majorVersion == 5 && minorVersion >= 4` OR version unparsable but `AppServer` still present (can recreate bak). If `majorVersion == 5 && minorVersion < 4` and `AppServer` is missing, **exclude** from candidates and log why. |

**Rationale**: Spec clarification B plus safety for Main’s post-ANT SSL/port migration.

### D4 — Flag and prompt semantics

**Decision**:

- CLI: `--clean-install-dir` with values true/false; bare `--clean-install-dir` ⇒ true; default absent ⇒ false.
- Also accept `-Dclean.install.dir=true` for symmetry with other install system properties if cheap (optional).
- Interactive prompt only when: upgrade + TTY (`System.console() != null`) + candidates non-empty + flag not true.
- Flag true ⇒ no prompt, delete candidates (clarification).

### D5 — Delete and size estimation

**Decision**:

- Size: recursive walk of each candidate directory; sum file sizes; report human-readable (e.g. GB/MB) and raw bytes in logs.
- Delete: `Files.walk` reverse order delete; no follow of symlinks outside install root (`LinkOption.NOFOLLOW_LINKS` where applicable); refuse to delete if resolved path is not under install root.
- Per-path try/catch; collect failures; continue upgrade (warn-and-continue).

### D6 — Extraction for testability

**Decision**: New class e.g. `ObsoleteInstallDirCleaner` (or `InstallDirCleanup`) with pure methods:

- `listCandidates(installRoot, versionMajor, versionMinor)`
- `estimateSizeBytes(path)`
- `shouldPrompt(interactive, flag, candidates)`
- `deleteCandidates(candidates)` → result report

Unit-test without full installer.

### D7 — Documentation

**Decision**: Document flag, prompt behavior, and MVP path list in `modules/perc-distribution-tree/README.md`. Optional one-line in Main usage when install path missing.

### D8 — DTS

**Decision**: Out of CMS MVP. Same relative names may appear under DTS roots later; do not block on `MainDTSPreInstall`.

## Alternatives considered

| Alternative | Why rejected |
|-------------|----------------|
| Always delete without flag/prompt | Violates #1157 and safe automation default |
| Delete `AppServer` leftovers by default | Live upgrades may still zip/migrate AppServer; out of scope |
| Only implement `remove_PercussionInstallation.xml` TODO | Does not cover PreInstall or interactive/space UX |

## Open items for tasks (not blockers)

- Confirm production spelling of `_Percussion_*` folder on Windows case-insensitive vs Linux.
- Optional: implement or retire stub `remove_PercussionInstallation.xml` once Java cleaner owns the path (avoid double-delete; prefer single owner in Java).

## NEEDS CLARIFICATION

None remaining for design; all resolved via D1–D8 and clarified spec session 2026-07-16.
