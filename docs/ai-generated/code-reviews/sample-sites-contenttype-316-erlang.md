# Erlang review — `fix/sample-sites-contenttype-316`

**Reviewer:** Erlang Shen (independent; did not author this change)  
**Date:** 2026-08-14  
**Scope:** uncommitted vs `HEAD` on `fix/sample-sites-contenttype-316` (from `main`).  
**Memory patterns hit:** installer seed lockstep; unique PK/name collision aborting the rest of a tablefactory table; change-class companions (CONTENTTYPES + PSX_CONTENTTYPE_TEMPLATE).

## Summary

`RxffTableData` CONTENTTYPES tried to rename ids 313–315 to `percNavImage` / `percNavon` / `percNavTree`. `perc.nav` already owns those names at 1015/1024/1025. H2 `UNI_KEY_CTYPES` aborted the rest of CONTENTTYPES, so `rffPressRelease` (316) never inserted. Live install: Press Release items exist; editor failed to register.

Fix: remove the percNav* rename rows from both seed copies (installer data + FastForward Core). Keep 316. Wiring test asserts 316 stays `rffPressRelease` and 313–315 are not renamed to percNav*. Product-docs note the id split.

Live H2 (not in git): INSERT CONTENTTYPES 316 + seven `PSX_CONTENTTYPE_TEMPLATE` rows that also FK-failed.

## Recommendation

approve

## Gate

- **May commit/push: yes** (feature branch / PR — not `main`)
- Bugs: none remaining in seed
- Behavioral test: present (parses both XML copies)
- Change-class: seed XML both lockstep paths + test + product-docs
- Agent rule files: none
- Cross-platform path review: **clean** (`Path.of` module-relative)

## Issues

None that block. Type 1025 `percNavTree` items remain a dual-id leftover (CM1 nav trees vs historic 315); out of this change class.

## Tests

`mvnw -Dtest=InstallSampleSitesWiringTest test`: Tests run: 7, Failures: 0.
