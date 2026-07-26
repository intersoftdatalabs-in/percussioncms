# Erlang review — 004/us3-remaining-codeql-residuals

**Date:** 2026-07-18  
**Branch:** `004/us3-remaining-codeql-residuals`  
**Scope:** remaining open alerts after incomplete-sanitization + java/xss batches (sql ×3, crypto ×5, js/xss ×2)

## Summary

|                      Cluster                      |                                               Disposition                                               |
|---------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| java/sql-injection #658–#660                      | Runtime already had `requireSqlObjectName`; same-line annotations (metadata API / fixed COUNT template) |
| java/weak-crypto + static-IV #757–#759, #649–#650 | ACCEPTED-RISK legacy AES/CBC; same-line `justification:` (prior ACCEPTED-RISK prefix ignored)           |
| js/xss #945, #946                                 | **Runtime fix** — HTML escape + same-origin URL; webimagefx origin+pathname allow-list                  |

## Recommendation

**approve**

## Gate

|      Check       |                                        Result                                        |
|------------------|--------------------------------------------------------------------------------------|
| Bugs             | none                                                                                 |
| Behavioral tests | node verify-sys-resources-js-xss.js; SecureStringUtilsSqlInjectionTest; PSAes* tests |
| Cross-platform   | Path.of not used in product JS; node script portable                                 |
| May commit/push  | **yes**                                                                              |

## Tests run

```text
node scripts/verify-sys-resources-js-xss.js  # all passed
./mvn-env.sh -pl modules/perc-legacy -Dtest=PSAesTest,PSAesCBCDeprecationTest -Dai.integrity.skip=true test
./mvn-env.sh -pl modules/perc-security-utils -Dtest=SecureStringUtilsSqlInjectionTest -Dai.integrity.skip=true test
```

