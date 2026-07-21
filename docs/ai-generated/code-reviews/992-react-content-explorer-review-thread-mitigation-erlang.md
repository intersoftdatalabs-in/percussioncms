# Erlang review — 992-react-content-explorer review-thread mitigation

**Branch**: `992-react-content-explorer-us1`
**Date**: 2026-07-19
**Scope**: Mitigation of 13 review threads on PR #1386 raised by `kilo-code-bot`.

## Files reviewed

| File | Change |
|------|--------|
| `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` | `onPreview: actionHandlers?.onPreview ?? (() => undefined)` — fallback no-op when host doesn't supply preview handler (PR #1386 review). |
| `WebUI/src/main/ts/contentExplorer/DetailList.tsx` | Use `pageRef` to track the page-to-request across renders; effect no longer depends on a stale captured `page`; `setPage(0)` on folder change no longer re-triggers the effect with a duplicate `startIndex` request. |
| `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` | ARIA tree keyboard navigation: ArrowRight expands, ArrowLeft collapses, ArrowUp/Down use native focus traversal between treeitems. |
| `docker-compose.yml` | `user: "${CMS_UID:-1000}:${CMS_GID:-1001}"` — overridable via `.env.compose` per host. |
| `scripts/install-cms-dev.sh` | Removed `--bootstrap` / `--no-bootstrap` flags. Bootstrap now happens automatically when `${INSTALL_ROOT}` is empty (the natural first-time setup). Auto-detect is the only behavior; no flag is reachable. |
| `docker/scripts/perc-devctl.sh` | Synced `install` subcommand flags to the new `install-cms-dev.sh` arg parser (removed `--no-bootstrap`, added `--skip-dts` / `--install-dts`). |
| `modules/perc-qa-automation/frontend/playwright.config.js` | `testDir: './tests'` (was `'./frontend/tests'` which resolved to `frontend/frontend/tests` from the wrong CWD). |
| `modules/perc-qa-automation/frontend/tests/helpers/auth.js` | (1) `updateEnvFile` uses a replacer function so `$&` / `$'` etc. in passwords are not interpreted as regex replacement specials. (2) First-run fallback: if `.env` doesn't exist, copy from `.env.example`; if no example, create empty. Avoids `ENOENT` crash. |
| `modules/perc-qa-automation/README.md` | Fixed the `mvn test` confusion (it doesn't run Playwright; the Maven build only installs Node + `npm ci`); updated running instructions to make clear Playwright must be run from `modules/perc-qa-automation/frontend` (CWD-sensitive due to `testDir: './tests'`). |
| `specs/992-react-content-explorer/tasks.md` | Replaced `npx --prefix frontend playwright test` (which runs Playwright from the wrong CWD) with `cd modules/perc-qa-automation/frontend && npm test` in T012g and T024b descriptions. |
| `specs/992-react-content-explorer/quickstart.md` | Same fix in the Build / unit tests section. |

## Hard gates checked

| Gate | Status |
|------|--------|
| Missing-behavioral-test gate | **Pass (n/a)** — review-fix commit is a code/docs delta, no new logic; existing Vitest + Playwright specs unchanged. |
| Non-portable filesystem path joins | **Pass (n/a)** — no filesystem path code. |
| Secrets on command line | **Pass** — `auth.js` `updateEnvFile` is now safe for passwords containing `$&`, `$'`, etc. (replacer function). Same `.env` model as before. |
| Path containment | **Pass (n/a)** — none. |
| Empty catch / swallowed exceptions | **Pass** — `updateEnvFile` no longer crashes on missing `.env`; the ENOENT path now auto-creates the file from the example. |
| Bootstrap unreachable code | **Pass** — bootstrap is now auto-detected (no flag), so the dead `if [[ "${BOOTSTRAP}" == "true" ]]` block is gone. The auto-detect function still has clear logs. |
| Auto-config from wrong CWD | **Pass** — `playwright.config.js` `testDir: './tests'` is now correct; docs and tasks explicitly say "run from `modules/perc-qa-automation/frontend`". |

## Cross-platform path checklist

- All env vars in `docker-compose.yml` are portable (`${CMS_UID:-1000}:${CMS_GID:-1001}`); works on Linux/macOS/Windows. The bind-mount path `/opt/Percussion` is a literal constant agreed by host + container (per docker dev runtime commit `2d35a93c49`).
- `auth.js` is portable Node.js; no shell.

## Recommendation

**Approve.**

## Gate

**May commit/push: yes.**