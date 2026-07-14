# Accepted Risks — intersoftdatalabs-in/percussioncms

**Branch**: `004-zero-code-scanning-alerts`
**Generated**: 2026-07-14 (initially empty; rows added per US3 same-PR rule
T061b or by explicit escalation per spec FR-008)

Schema follows `specs/004-zero-code-scanning-alerts/contracts/README.md` C4.
Every `Disposition` of `accepted-risk` in `triage.md` MUST have a row here
with non-empty `rationale`, `compensating_control`, `owner`,
`target_milestone`, and `expires_at`. Each accepted-risk MUST also be cited
by alert ID in the `8.2` release notes.

| alert_id | rule_id | file_path | rationale | compensating_control | owner | target_milestone | expires_at |
|----------|---------|-----------|-----------|----------------------|-------|------------------|------------|
| 759 | java/weak-cryptographic-algorithm | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java | Legacy deprecated AES/CBC kept for decrypt of historical ciphertext; AES/CBC is acceptable for at-rest blob storage but a stronger AEAD (AES/GCM with random IV) is the recommended primitive. Upgrading requires an API-breaking change to PSAesCBC + a migration utility that re-encrypts all stored credentials. Deferred to 9.0; class now annotated `@Deprecated(forRemoval=true, since="8.2")`. | (1) Class annotated @Deprecated(forRemoval=true) so callers get a compile-time warning. (2) Class lives in `modules/perc-legacy/security/deprecated/` — already segregated from production crypto paths. (3) New `encrypt(byte[])` overload already uses a random prepended IV, so new callers are not affected. (4) Audit trail in `docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md`. | security@percussion.com | 9.0 | 2027-12-31 |
| 758 | java/weak-cryptographic-algorithm | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java | Same root cause and mitigation as alert #759. | See alert #759. | security@percussion.com | 9.0 | 2027-12-31 |
| 757 | java/weak-cryptographic-algorithm | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java | Same root cause and mitigation as alert #759. | See alert #759. | security@percussion.com | 9.0 | 2027-12-31 |
| 650 | java/static-initialization-vector | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java | Legacy `INITIAL_VECTOR` (all-zero 16-byte IV) intentionally retained for backward compatibility with legacy ciphertext that was encrypted with the fixed IV. Randomising the IV requires a one-time re-encryption pass over all stored credentials. Deferred to 9.0 alongside the AES/CBC removal. | (1) Field documented with the ACCEPTED-RISK marker in javadoc. (2) New `encrypt(byte[])` overload prepends a random IV. (3) Class annotated @Deprecated(forRemoval=true). | security@percussion.com | 9.0 | 2027-12-31 |
| 649 | java/static-initialization-vector | modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java | Same root cause as #650. | See #650. | security@percussion.com | 9.0 | 2027-12-31 |