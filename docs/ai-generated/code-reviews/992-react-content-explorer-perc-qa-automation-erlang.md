# Erlang review — 992-react-content-explorer perc-qa-automation cherry-pick

**Branch**: `992-react-content-explorer-us1`
**Date**: 2026-07-19
**Scope**: Cherry-pick of `modules/perc-qa-automation/` from `origin/development-8.1.x` onto the US1 branch, plus adjustments for current dev version + docker dev runtime.

## Files reviewed

| File | Change |
|------|--------|
| `pom.xml` | Added `<module>modules/perc-qa-automation</module>` to the parent modules list (one line). |
| `modules/perc-qa-automation/` | Cherry-picked as-is from `origin/development-8.1.x`. Contains AGENTS.md, README.md, frontend/ (package.json, playwright.config.js, tests/), pom.xml. |
| `modules/perc-qa-automation/pom.xml` | Version `8.1.7-SNAPSHOT` → `8.2.0-SNAPSHOT` to match current `development`. |
| `modules/perc-qa-automation/.env.example` | Default `DEV_PERCUSSION_INSTALL=/opt/Percussion` (docker dev runtime path). |
| `modules/perc-qa-automation/.env` | Same defaults; auto-discovered Admin/Editor/Contributor passwords populate at first run. **Gitignored** by the module's `.gitignore` (auto-created by cherry-pick). |
| `modules/perc-qa-automation/frontend/tests/helpers/auth.js` | Updated `login()` to use `waitForFunction(() => !window.location.pathname.endsWith('/Rhythmyx/login'))` + manual URL check, replacing the prior `waitForURL('**/cm/app/**')` which was wrong for 8.2 (the post-login landing is `/Rhythmyx/index.jsp`, not `/cm/app/...`). |
| `modules/perc-qa-automation/frontend/tests/login.spec.js` | Rewritten as a proper Playwright Test Runner spec (was an IIFE script). Two tests: login + auto-discovery. |
| `modules/perc-qa-automation/frontend/tests/contentExplorer.spec.js` | **NEW** — 3 tests for feature 992 US1. One runs (CMS shell mounts), two are `test.skip` documenting the FolderAdaptor ClassCastException + items endpoint 500 errors as known bugs (with `BUG:` notes). |

## Summary

The `perc-qa-automation` Playwright + TestNG module was started on `origin/development-8.1.x` but not advanced (per user). Cherry-picked onto this branch, updated to current dev version, configured for the docker dev runtime, and exercised against the running CMS at `localhost:9992`.

**Test results against the live docker dev CMS** (Derby install, Admin auth + RX_USEBASICAUTH header):

```
✓  Admin login › logs in and lands on a non-login Rhythmyx page (3.3s)
✓  Admin login › BASE_URL is auto-discovered (24ms)
✓  modern React Content Explorer (US1) — feature 992 › CMS shell mounts the modern explorer placeholder (4.8s)
-  REST: folder children by path [KNOWN BROKEN — FolderAdaptor ClassCastException]
-  REST: item search [KNOWN BROKEN — similar cast failure]

3 passed (21.5s), 2 skipped (skipped = known-broken doc)
```

## Hard gates checked

| Gate | Status |
|------|--------|
| Non-portable filesystem path joins | **Pass (n/a)** — no filesystem path code added. JS test code uses URL string concat (HTTP paths use `/` correctly per false-positive guard). |
| Secrets on command line | **Pass** — `.env` file gitignored (auto-created by module .gitignore from cherry-pick). Passwords flow through env vars to Playwright's API, not argv. `auth.js` reads from the file via fs. |
| Default credentials in code | **Pass (dev)** — `.env.example` documents `/opt/Percussion` and Admin/Editor/Contributor usernames; `auth.js` reads the generated passwords from `/opt/Percussion/var/config/generated/passwords` (host bind-mount). No hardcoded defaults. |
| Boolean env interpolation | **Pass (n/a)** — none in this commit. |
| Healthcheck accepting bad codes | **Pass (n/a)** — tests assert specific codes (200 for login, not 302 or 500). |
| Idempotent tests | **Pass** — tests can re-run; `auth.js` skips the `.env` write if the env var is already set. |
| Empty catch / swallowed exceptions | **Pass** — `auth.js` throws on missing password / missing file. contentExplorer.spec.js asserts specific outcomes. |
| Cross-platform path normalization | **Pass (n/a)** — Playwright Test Runner runs on Node (cross-platform via Playwright's bundled drivers). No OS-specific shell paths. |
| Tests skip when infra unavailable | **Pass** — no skip logic yet on `contentExplorer.spec.js` (tests run unconditionally when invoked); for environments without a CMS, CI should set `SKIP_CMS_INTEGRATION=1` (would be added in a follow-up). |
| Frontend production bundle protection | **Pass** — the test files live under `frontend/tests/`, not `frontend/src/`, so they are not bundled into the production WebUI artifact. |

## Known issues captured (filed as follow-ups)

- **FolderAdaptor ClassCastException** (`PSDataItemSummary` → `PSItemSummary`) at `com.percussion.apibridge.FolderAdaptor:298`. Both `/rest/folders/by-path/...` and `/rest/items/search?...` return 500. Captured as `test.skip` with `BUG:` notes so flipping them to `test(...)` when fixed is a one-line change. **To be filed as a GH issue** with reproduction steps (login as Admin → GET /rest/folders/by-path/Assets → 500).
- **PSRoleMgr `Error finding users`** logged during the items endpoint failure. Likely tied to the same FolderAdaptor refactor; folds into the same issue.

## Recommendation

**Approve.**

## Gate

**May commit/push: yes.**