# Erlang review — #3586 IPS*Errors freeze gate

**Branch:** `fix/issue-3586-ipserrors-freeze-gate`  
**Scope:** uncommitted `scripts/verify-no-bare-ipserrors.py`, pytest peer,
`scripts/ipserrors-residual-allowlist.txt`, `scripts/README.md`  
**Date:** 2026-08-19

## Summary

Adds a CI freeze gate (peer of #3143 `verify-no-bare-ipsobjectstoreerrors.py`)
so **new** production Java files cannot grow bare `IPS*Errors` call-sites
without an exact-path allow-list entry. `IPSObjectStoreErrors` stays on the
sibling gate. Residual SiteManage / webservices paths are exact-listed until
#3584 / #3585 land.

## Recommendation

**approve** — May commit/push: **yes**

## Gate

- Bugs: none
- Behavioral tests: 17 pytest cases (`test_verify_no_bare_ipserrors.py`) — fail
  on new file, allow-list pass, comment/test ignored, prefix freeze, empty
  allow-list fails on real residuals
- Cross-platform path checklist: **pass**
  - `Path` for repo-root / allow-list I/O; git paths normalized to posix
  - `git grep` invoked with `shell=False`
  - allow-list entries asserted to use `/` and no trailing-slash prefixes
  - Windows `python scripts\…` documented; no Unix-only runner

## Issues

None.

## Memory patterns hit

- Exact allow-list over directory prefixes (peer #3143 residual shrinkage)
- `tmp_path` negative probes so interrupted runs do not dirty the monorepo
- Tests and comment-only mentions ignored
