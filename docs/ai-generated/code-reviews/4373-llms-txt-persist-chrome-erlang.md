# Erlang review — #4373 llms-txt persist + Developer Sites source chrome

**Verdict:** pass (self-review; no hard-gate bugs found)

## Scope

REST GET/PUT `/sites/{nameOrId}/virtual` round-trips `sourceKind=llms-txt` with a portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credentials, and cloud URL `rootPath` stay 400 via existing `PSVirtualSiteHelper` (LLMS_TXT already on main). Developer Sites adds an **llms.txt** option; Build/Preview/Publish chrome stay hidden (#4374/#4375).

## Checks

- **Bugs:** Persist uses the same adaptor `validate` path as robots-txt. UI PUT omits remotes (`remoteUrl: ""`). Unknown kinds still 400.
- **Tests:** rest resource + serial; sitemanage adaptor round-trip and 400s; Vitest form/panel/build-chrome-hidden; Playwright intercept + live save/reload.
- **Portable paths:** Tests use `C:/llms-docs` string fixtures (peer robots-txt); no `"/" +` filesystem join in production.
- **Companions:** REST + sitemanage tests + WebUI + Playwright + product-docs 8.2 in this change set.
- **Hard bans:** Did not implement REST/UI Build, Preview, or Publish for llms-txt.

> Co-Authored by Grok Build 1.0.13 using grok-4.6 with agent night-issue-prs.
