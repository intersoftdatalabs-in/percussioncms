# Erlang self-review: #2354 rotateKey failure logging

**Branch:** `fix/issue-2354-rotatekey-failure-logging`  
**Scope:** system delivery client + PSDeliveryInfoService rotateKey WARN  
**Date:** 2026-08-07

## Change class

Operator-facing structured logging for DTS admin HTTP failures (rotateKey push).

### Companions delivered

| Artifact | Status |
|----------|--------|
| `PSDeliveryHttpErrorSupport` helper | new |
| `PSDeliveryClient` ERROR truncation + structured exception | done |
| `PSDeliveryClientException` status/method/url/snippet fields | done |
| `PSDeliveryInfoService` actionable WARN | done |
| Unit tests for helper | 8 tests green |
| Module `mvnw clean install` | BUILD SUCCESS |

## Checklist

- [x] No multi-KB HTML in ERROR/WARN (first-line snippet, max 200 chars)
- [x] Full URL + PUT + HTTP status in rotateKey failure WARN
- [x] Actionable operator hint (feeds WAR, deliverymanager, availableServices, key drift)
- [x] Success path INFO unchanged on 204
- [x] Behavioral unit tests for new logic
- [x] No non-portable path/file I/O
- [x] Intersoft copyright on new files (2026)
- [x] No unrelated reformat churn
- [x] Existing constructors of `PSDeliveryClientException` preserved (status -1)

## Verdict

**PASS** — ready to commit/PR.
