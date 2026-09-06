# Erlang review — #4380 openapi-yaml persist + Developer Sites source chrome

**Verdict:** pass (self-review; no hard-gate bugs found)

## Scope

REST GET/PUT `/sites/{nameOrId}/virtual` round-trips `sourceKind=openapi-yaml` with a portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` stay 400 via existing `PSVirtualSiteHelper` (`OPENAPI_YAML` already on main). Developer Sites adds an **OpenAPI YAML** option; Build/Preview/Publish chrome stay hidden (#4381/#4382).

## Checks

- **Bugs:** Persist uses the same adaptor `validate` path as llms-txt. UI PUT omits remotes (`remoteUrl: ""`). Unknown kinds still 400.
- **Tests:** rest resource + serial; sitemanage adaptor round-trip and 400s; Vitest form/panel/build-chrome-hidden; Playwright intercept + live save/reload.
- **Portable paths:** Tests use `C:/openapi-docs` string fixtures (peer llms-txt); no `"/" +` filesystem join in production.
- **Companions:** REST + sitemanage tests + WebUI + Playwright + product-docs 8.2 in this change set.
- **Hard bans:** Did not implement REST/UI Build, Preview, or Publish for openapi-yaml.

> Co-Authored by Grok Build 1.0.13 using grok-4.6 with agent night-issue-prs.
