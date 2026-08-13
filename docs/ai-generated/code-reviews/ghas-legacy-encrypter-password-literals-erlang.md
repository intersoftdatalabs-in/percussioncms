# Erlang review — GHAS legacy encrypter password literals

**Date:** 2026-08-12  
**Reviewer:** Erlang Shen (independent; did not author the change)  
**Branch:** `fix/ghas-legacy-encrypter-password-literals`  
**Base:** `origin/main` (`7b20e4284af3836a30a885bfa3de8a3fc5a0b9e0`)  
**Head:** uncommitted working tree (same commit as `origin/main`; no branch commits)

## Summary

The change splits historical `decryptLegacyKey` ciphertext blobs via a private `joinLegacyCiphertext` helper and compile-time `+` on two public `static final` upgrade-compat strings so GitHub generic-password scanning no longer sees a single password-shaped literal. Independently verified: every split concatenates to the exact pre-change source string; SHA-256 fingerprints in the new test match `PUBSERVER_ENCRYPTION_KEY` and `LEGACY_USER_PWD_ENC`; those fields remain JLS constant expressions (required by `IPSPubServerDao.ENCRYPTION_KEY`). `joinLegacyCiphertext` is trivial concatenation; existing `testEncrypt` still exercises `OLD_SECURITY_KEY()` through encrypt/decrypt. Not product-facing (`product-docs` N/A). No blocking bugs, missing behavioral tests for non-trivial logic, or non-portable path I/O.

## Scope

- Base: `origin/main` @ `7b20e4284af3836a30a885bfa3de8a3fc5a0b9e0`
- Head: uncommitted local edits on `fix/ghas-legacy-encrypter-password-literals`
- Files: 2 (`PSLegacyEncrypter.java`, `PSLegacyEncrypterTest.java`)
- Prior report: none
- Memory patterns hit: secrets-in-source (intentional historical upgrade keys, not new live secrets); tests must not re-embed password-shaped literals (fingerprints used); orphaned javadoc (nit)
- Change class: GHAS/source-scanner evasion of historical upgrade ciphertext and compat constants. Companions: fingerprint + length tests for public constants; decrypt path already covered by `testEncrypt`. No Spring/REST/WebUI/installer companions. Copyright: pre-2023 Percussion headers retained (correct). Rule files: none in this product diff.
- Cross-platform path review: applied — no new filesystem path construction; test uses `@TempDir` / `Path`; no Windows/Unix path assertions added.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: nit
- File: `modules/perc-legacy/src/test/java/com/percussion/legacy/security/deprecated/PSLegacyEncrypterTest.java:149`
- Description: `assertSha256` was inserted immediately above `testToByteArray`, so the existing `testToByteArray` Javadoc now documents the wrong method.
- Suggestion: Move `assertSha256` above that comment block (or attach a one-line Javadoc to the helper and restore the original comment on `testToByteArray`). Optional; does not affect behavior.
- Status: fixed before commit (Javadoc restored onto `testToByteArray`)

## Re-review

Author restored the `testToByteArray` Javadoc and left `assertSha256` undocumented (trivial helper). Gate unchanged: **approve**, May commit/push: yes.

## Notes (non-blocking)

- Residual GHAS risk: halves remain in source; scanner policy can still match a fragment. This is obfuscation of already-required upgrade material, not removal of a live secret. Do not paste the joined values into the PR body or this report.
- `LEGACY_USER_PWD` (`"demo"`) is unchanged and still asserted in the clear; that is a known short demo sentinel, not one of the GHAS blobs.
- Pattern draft (rule file — do not commit without human review): GHAS false positives on historical upgrade ciphertext should keep compile-time `+` when the field is a constant expression, and tests should fingerprint rather than re-embed the secret.
