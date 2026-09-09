# Erlang review — issue #4402 graphql-sdl Build and Preview

**Branch:** `feat/issue-4402-graphql-sdl-build-preview`  
**Parent:** #2678  
**Peer:** openapi-yaml #4381/#4387, asyncapi-yaml #4391/#4398

Independent pre-commit review of the graphql-sdl Virtual Site Build + Preview vertical (REST + adaptor + WebUI + Playwright + product-docs). Does not re-implement persist PUT/GET (#4401/#4405). Does not implement Publish (#4403).

## Change class

One vertical user-visible increment: Admin can Build Virtual Site and Preview assembled site for `sourceKind=graphql-sdl` from a local GraphQL SDL fixture (`schema.graphql` / `_config.yaml` `graphql.file`). REST `POST …/virtual/build` returns HTTP 200 with `pagesWritten > 0`. Leftover `virtual.remoteUrl`, credentials, cloud `rootPath`, and `graphql.url` are 400 (no live GraphQL fetch). REST `GET …/virtual/preview` after Build is `available=true`; missing build is `available=false` HTTP 200 (do not invent HTML). Developer Sites shows Build + Preview; Publish chrome stays hidden.

## Companions

| Layer | Present |
|-------|---------|
| rest resource OpenAPI + resource tests | yes |
| sitemanage adaptor tests (build + preview + leftover 400) | yes |
| WebUI chrome predicates + Vitest | yes (`shouldShowVirtualPublishChrome("graphql-sdl")` remains false) |
| Playwright C5 H2 surface | yes (live Build + Preview; bind-mount fixture; no Jetty restart) |
| product-docs 8.2 REST / Virtual Sites / admin Sites / site-config | yes |
| Cross-platform paths | NIO `Path`/`Files` in Java; POSIX in-container fixture helper |

## Findings

None that block. Production Build already routes through `PSVirtualSiteBuildService.forSourceType` / factory; this slice flips allow-list chrome and adds REST/adaptor tests plus last-build Preview via existing `recordLastOutputRoot` / sole-HTML home fallback (`8.2/user-1.html`).
