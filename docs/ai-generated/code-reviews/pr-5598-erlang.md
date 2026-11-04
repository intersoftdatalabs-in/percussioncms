<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5598

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5598
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5585
- Parent: #4531
- Base: origin/main
- Head: 4694877a1237c8e4f39f5f499b7c03382656000a
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/review-5598`)
- Reviewer: independent Erlang pass (not the implementer persona)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=32 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

- The single finding is **preexisting**: `updatePubServer` was already over the
  complexity limit before this slice. The region branch adds 6 lines to a method
  that was already at cognitive 31 (the peer bucket PR #5594 reported the same
  method at 31). Refactoring `updatePubServer` is a separate slice, not this
  PR's scope — same row as sibling PR #5594.
- `PSAmazonS3DeliveryRegionUpdate` mirrors the merged peer
  `PSAmazonS3DeliveryBucketUpdate` exactly: same `isXOnly` body shape, same
  `reject()` helper, same 255-character `PROPERTYVALUE` bound. The property
  constant is `IPSPubServerDao.PUBLISH_EC2_REGION` ("region"), which is the
  key the S3 delivery handler already reads.
- Secrets are not read, rendered, or echoed: `apply()` never touches
  `PUBLISH_AS3_ACCESSKEY_PROPERTY` / `PUBLISH_AS3_SECURITYKEY_PROPERTY`, and the
  validation messages are asserted not to contain them in
  `PSAmazonS3DeliveryRegionUpdateTest`.
- No missing behavioral tests: `PSAmazonS3DeliveryRegionUpdateTest` (6 tests),
  `deliveryServerS3Region.test.ts` + `S3DeliveryRegionPanel.test.tsx`
  (11 Vitest tests), and the H2 Playwright spec cover the region-only body,
  blank/overlong refusal, non-S3 drivers, and the HTTP 400/403/409 paths.
- No non-portable path/file I/O: the slice only sends property strings.
- Gate: **PASS**.

## Independent verification (reviewer, not the implementer)

Standalone JDK 21 clean install on the branch tree, run from the review worktree:

- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 3969, Failures: 0, Errors: 0, Skipped: 125 (507 test classes).
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS (Surefire + Vitest green).
- `PSAmazonS3DeliveryRegionUpdateTest` — Tests run: 6, Failures: 0, Errors: 0.
- `npx vitest run ../../test/ts/publishing/S3DeliveryRegionPanel.test.tsx ../../test/ts/publishing/deliveryServerS3Region.test.ts` — Test Files 2 passed, Tests 11 passed.

C5 live UI proof on a fresh H2 QA cell with this branch's `sitemanage` jar and
WebUI SPA hot-deployed (`TEST_CMS_URL=http://127.0.0.1:9993`):

- `python3 docker/scripts/perc-devctl.py qa-up --skip-image-build --then-qa-deploy-webui` — RESULT:OK
- `qa-deploy-war-jars --restart-jetty` — RESULT:OK
- `qa-health` — RESULT:OK HTTP:200 HEALTH:healthy
- `npm run test:surface -- --path tests/publishing/sitesDeliveryServerS3Region.spec.js` — **2 passed** (sets an S3 region only and does not write a Local folder; HTTP 400/403/409 keep the previous S3 region)

Note on the PR body's `Tests run: 4041`: that figure matches a main-based tree
(origin/main adds `LocalesAdaptorDesignWsTest` + `LocalesAdaptorTest` = 76 tests
after this branch's base). The branch tree itself runs 3969. Both are green.

Verdict: **LGTM** — independent review, C1 and C5 re-verified.
