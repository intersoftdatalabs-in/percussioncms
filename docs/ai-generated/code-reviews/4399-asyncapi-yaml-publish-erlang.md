# Erlang review: #4399 asyncapi-yaml Publish to Site root

**Branch:** `feat/issue-4399-asyncapi-yaml-publish`

Independent pre-commit review of the asyncapi-yaml Virtual Site Publish slice (parent #2678). Peer: openapi-yaml Publish #4382 / PR #4388.

## Scope

Enables Developer Sites **Publish Virtual Site** for `sourceKind=asyncapi-yaml`. Production `POST /sites/{nameOrId}/virtual/publish` already builds then NIO-copies last-build HTML to `IPSSite.root` after `PSVirtualSiteHelper.validate()`. This slice adds adaptor/resource tests, Publish chrome, Playwright C5, and product-docs 8.2.

Uncommitted + branch vs `origin/main`. No `gh` PR number yet.

## Findings

**Bugs:** none.

**Behavioral tests:** rest resource delegates + 400 propagation for leftover remoteUrl/credentials/cloud; sitemanage adaptor copies `8.2/onLightMeasured-1.html` (injected runner and real assemble), leftover remoteUrl/credentials/cloud 400, missing fixture 400 without inventing pages; Vitest `shouldShowVirtualPublishChrome("asyncapi-yaml")` plus panel chrome/hint; Playwright intercept plus live H2 Publish with file assertion under Site root.

**Portable paths:** Java uses `Path`/`Files`. Playwright dest assert normalizes POSIX in-cell paths and rejects `..` / drive letters (Linux QA cell only — same as openapi-yaml peer). Cross-platform path review: clean for production I/O; QA helper POSIX `/` is in-container only.

**Change-class companions:** REST + sitemanage adaptor + WebUI + Playwright + product-docs present. No rule-file edits. System NIO publisher reused unchanged.

**C1:** rest, sitemanage, WebUI standalone `mvnw clean install` recorded in the PR body.

**C5:** H2 QA Playwright `--grep asyncapi-yaml` plus chrome/live Publish recorded in the PR body.

Memory patterns hit: change-class completeness (REST + SPA + Playwright + product-docs); leftover remote/credentials fail closed; missing assemble fail-closed.

Recommendation: **approve**.

Gate: **approve**. May commit/push: **yes**.
