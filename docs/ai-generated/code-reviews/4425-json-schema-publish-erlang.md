# Erlang review: #4425 json-schema Publish to Site root

**Branch:** `feat/issue-4425-json-schema-publish`

Independent pre-commit review of the json-schema Virtual Site Publish slice (parent #2678). Peers: graphql-sdl Publish #4403 / PR #4414; asyncapi-yaml Publish #4399 / PR #4400.

## Scope

Enables Developer Sites **Publish Virtual Site** for `sourceKind=json-schema`. Production `POST /sites/{nameOrId}/virtual/publish` already builds then NIO-copies last-build HTML to `IPSSite.root` after `PSVirtualSiteHelper.validate()`. This slice adds adaptor/resource tests, Publish chrome, Playwright C5, and product-docs 8.2. Does not re-implement PUT/GET persist (#4416) or Build/Preview (#4417).

Uncommitted + branch vs `origin/main`. No `gh` PR number yet.

## Findings

**Bugs:** none.

**Behavioral tests:** rest resource delegates + 400 propagation for leftover remoteUrl/credentials/cloud/`jsonschema.url`; sitemanage adaptor copies `8.2/sku-1.html` (injected runner and real assemble), leftover remoteUrl/credentials/cloud/`jsonschema.url` 400, missing fixture 400 without inventing pages; Vitest `shouldShowVirtualPublishChrome("json-schema")` plus panel chrome/hint; Playwright intercept plus live H2 Publish with file assertion under Site root.

**Portable paths:** Java uses `Path`/`Files`. Playwright dest assert normalizes POSIX in-cell paths and rejects `..` / drive letters (Linux QA cell only — same as graphql-sdl peer). Cross-platform path review: clean for production I/O; QA helper POSIX `/` is in-container only.

**Change-class companions:** REST + sitemanage adaptor + WebUI + Playwright + product-docs present. No rule-file edits. System NIO publisher reused unchanged.

**C1:** rest, sitemanage, WebUI, perc-qa-automation standalone `mvnw clean install` recorded in the PR body.

**C5:** H2 QA Playwright json-schema Publish surface recorded in the PR body.

Memory patterns hit: change-class completeness (REST + SPA + Playwright + product-docs); leftover remote/credentials/`jsonschema.url` fail closed; missing assemble fail-closed.

Recommendation: **approve**.

Gate: **approve**. May commit/push: **yes**.
