# Erlang review: #4362 robots-txt Publish to Site root

**Branch:** `feat/issue-4362-robots-txt-publish`

Independent pre-commit review of the robots-txt Virtual Site Publish slice (parent #2678).

## Scope

Enables `POST /sites/{nameOrId}/virtual/publish` for `sourceKind=robots-txt` (existing adaptor already builds then NIO-copies last-build HTML to `IPSSite.root`). Developer Sites Publish chrome, rest/sitemanage tests, Playwright C5, and product-docs 8.2.

## Findings

**Bugs:** none.

**Behavioral tests:** rest resource delegates + 400 propagation; sitemanage adaptor copies `8.2/star-1.html`, leftover remoteUrl/credentials/cloud 400, missing fixture 400 without inventing pages; Vitest chrome + dest path; Playwright intercept + live H2 file assertion.

**Portable paths:** Java uses `Path`/`Files`. Playwright dest assert normalizes POSIX in-cell paths and rejects `..` / drive letters (Linux QA cell only — correct).

**Change-class companions:** REST + sitemanage adaptor + WebUI + Playwright + product-docs present. No rule-file edits.

**C1:** rest, sitemanage, WebUI standalone `mvnw clean install` BUILD SUCCESS.

**C5:** `qa-up --skip-image-build --then-qa-deploy-webui` → `qa-health` HTTP 200 → Playwright `--grep robots-txt` 6 passed; console-clean; server.log-clean for the test window.

Recommendation: **approve**.
