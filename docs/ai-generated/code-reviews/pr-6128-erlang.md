# Erlang review — PR #6128 (post-hoc; MERGED)

- Date: 2026-11-11
- PR: #6128 `fix(webui): route followed container login redirects to React login (#6085)`
- State at review: MERGED (human APPROVED by natechadwick, all checks SUCCESS/NEUTRAL/SKIPPED as applicable)
- Tool: `mkd-code-review analyze --pack percussion` (branch report: 0 findings, 0 bugs) + independent diff read
- Persona: erlang 0.1.1

## Scope

- `WebUI/src/main/ts/api/client.ts` — new module-private `isLoginRedirect` (followed-302-to-login detection via `response.redirected` + URL pathname) and `throwIfSessionLost` (401 or login-redirect → React Login + `SessionRedirectError`); `handleResponse` and `getBinary` both route through it
- `WebUI/src/test/ts/api/clientSessionRedirect.test.ts` — 3 new behavioral cases (302-login-redirect, getBinary parity, non-login-redirect negative) + `stubLocation` helper extraction
- `specs/989-react-cui-widget-builder/tasks.md` — T057 tick per acceptance
- `docs/ai-generated/code-reviews/issue-6085-erlang.md` — machine report (0/0)

## Gate checks

- In-diff bugs: **0**.
  - `new URL(response.url, "http://localhost").pathname` — safe; `response.url` is absolute in browsers, base is fallback only.
  - Regex `/(^|\/)login$/` + `endsWith("/rxlogin.jsp")` on lowercased pathname: matches container `/login` and product `/rxlogin.jsp`; negative test proves non-login 200 redirects still parse as data (no false logout).
  - `throwIfSessionLost` runs before the `!response.ok` branch: normal 200s are untouched (both guards return early); error bodies still parsed as before. `getBinary` now matches `handleResponse` — consistency fix, not drift.
  - No invented APIs (`SessionRedirectError`, `redirectToLoginOnUnauthorized`, `resetLoginRedirectLatch` all pre-exist).
- Tests: behavioral Vitest (1104 passed incl. 3 new) + live H2 cell proof (shipped-bundle expiry → `/rxlogin.jsp?return=…`, 0 pageerrors, 0 server ERROR/FATAL). Temp Playwright spec deleted after run — recorded.
- Change-class closure: complete (shared-client fix at the single choke point, both call paths, unit + live proof).
- Paths/I-O: none. Headers: no new source files. Cross-platform: N/A (browser URL semantics, not OS paths).
- C2 downstream_checked: none — justified (no public signature change; two new module-private helpers).
- Product docs: N/A — justified (documented SPA login-redirect contract, no operator-facing change).

## Process note (non-blocking, post-hoc)

- C1 shows `npm run build:modern` (tsc + vite) + vitest + live-cell deploy, but no standalone WebUI `mvnw clean install`. For a TS-only change with no pom edit this is low-risk (the Maven frontend build runs the same tsc/vite steps, and the bundle was proven live), and the human approved — but future WebUI-source PRs should record one standalone `cd WebUI && ../mvnw clean install` line so the gate is literal.

## Residuals

- #6127 (product-wide CSRFGuard posture) already filed and OPEN. Nothing new to file.

## Verdict

**lgtm_merged** — 0 in-diff bugs, evidence complete, checks green, human-approved and merged. No action for erlang-fix.
