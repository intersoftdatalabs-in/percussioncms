# Erlang review: #4382 openapi-yaml Publish to Site root

**Branch:** `feat/issue-4382-openapi-yaml-publish`

Independent pre-commit review of the openapi-yaml Virtual Site Publish slice (parent #2678). Peer: llms-txt Publish #4375 / PR #4379 and robots-txt #4362 / PR #4376.

## Scope

Enables Developer Sites **Publish Virtual Site** for `sourceKind=openapi-yaml`. Production `POST /sites/{nameOrId}/virtual/publish` already builds then NIO-copies last-build HTML to `IPSSite.root` after `PSVirtualSiteHelper.validate()`. This slice adds adaptor/resource tests, Publish chrome, Playwright C5, and product-docs 8.2.

## Findings

**Bugs:** none.

**Behavioral tests:** rest resource delegates + 400 propagation for leftover remoteUrl/credentials/cloud; sitemanage adaptor copies `8.2/listPets-1.html` (injected runner and real assemble), leftover remoteUrl/credentials/cloud 400, missing fixture 400 without inventing pages; Vitest `shouldShowVirtualPublishChrome("openapi-yaml")` plus panel chrome/hint; Playwright intercept-equivalent live H2 Publish plus file assertion under Site root.

**Portable paths:** Java uses `Path`/`Files`. Playwright dest assert normalizes POSIX in-cell paths and rejects `..` / drive letters (Linux QA cell only — same as llms-txt peer).

**Change-class companions:** REST + sitemanage adaptor + WebUI + Playwright + product-docs present. No rule-file edits. Did not implement #4366 or #4367.

**C1:** rest, sitemanage, WebUI standalone `mvnw clean install` recorded in the PR body.

**C5:** H2 QA Playwright `--grep openapi-yaml` plus chrome test recorded in the PR body.

Memory patterns hit: change-class completeness (REST + SPA + Playwright + product-docs); leftover remote/credentials fail closed; missing assemble fail-closed.

Recommendation: **approve**.
