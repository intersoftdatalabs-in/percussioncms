# Erlang review: fix/nightly-i18n-resume-url-identity

**Date:** 2026-08-11  
**Scope:** uncommitted tooling for nightly i18n resume + URL-only identity  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for non-trivial logic; false-green / ignored exit codes (resume checkout return codes checked)

## Summary

Small developer-tooling change only (Python scripts + unit tests + READMEs). No product runtime Java, no Maven modules, no product-docs surface.

1. **URL-only identity** in `i18n_translate.py` / `i18n_translate_direct.py` — bare `http(s)://…` segments pass through like placeholders; mixed prose still translates. Prevents hang/rewrite of help-doc href keys via translate-shell.
2. **`--resume`** on `nightly_i18n_refresh.py` — keeps dirty worktree/locale branch, skips clean-tree and main preflight, requires `--locale`. Proven in production resume of `tr` (#3120).

## Issues

None (no bugs, no missing behavioral tests for new logic).

## Cross-platform path checklist

N/A for product path I/O. Changes use `pathlib.Path` for worktree registration only (existing pattern). Nightly wrapper remains Linux/macOS + `fcntl` (documented).

## Tests run

- `python3 modules/perc-i18n/scripts/test_i18n_translate_direct.py` — 22 OK  
- `python3 modules/perc-i18n/scripts/test_i18n_translate.py` — 13 OK  
- `python3 scripts/test_nightly_i18n_refresh.py` — 37 OK  

## Product documentation

N/A — developer/cron tooling only.
