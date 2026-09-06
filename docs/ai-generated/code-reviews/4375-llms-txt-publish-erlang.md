# Erlang review: #4375 llms-txt Publish to Site root

**Branch:** `feat/issue-4375-llms-txt-publish`

Independent pre-commit review of the llms-txt Virtual Site Publish slice (parent #2678). Peer: robots-txt Publish #4362 / PR #4376.

## Scope

Enables `POST /sites/{nameOrId}/virtual/publish` for `sourceKind=llms-txt` (existing adaptor already builds then NIO-copies last-build HTML to `IPSSite.root` after `PSVirtualSiteHelper.validate`). Developer Sites Publish chrome, rest/sitemanage tests, Playwright C5, and product-docs 8.2.

## Findings

**Bugs:** none.

**Behavioral tests:** rest resource delegates + 400 propagation for leftover remoteUrl/credentials/cloud; sitemanage adaptor copies `8.2/Quickstart-1.html`, leftover remoteUrl/credentials/cloud 400, missing fixture 400 without inventing pages; Vitest chrome + dest path; Playwright intercept + live H2 file assertion.

**Portable paths:** Java uses `Path`/`Files`. Playwright dest assert normalizes POSIX in-cell paths and rejects `..` / drive letters (Linux QA cell only — same as robots-txt peer).

**Change-class companions:** REST + sitemanage adaptor + WebUI + Playwright + product-docs present. No rule-file edits. Did not implement openapi-yaml or #4366.

**C1:** rest, sitemanage, WebUI standalone `mvnw clean install` recorded in the PR body.

**C5:** H2 QA Playwright `--grep llms-txt` recorded in the PR body.

Memory patterns hit: change-class completeness (REST + SPA + Playwright + product-docs); leftover remote/credentials fail closed; missing assemble fail-closed.

Recommendation: **approve**.
