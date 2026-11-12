# Erlang review — PR #6118 (issue #6074)

- Status: **LGTM — approved, squash-merged**
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 11 files (sitemanage + WebUI +
  qa-automation + product-docs)
- Verdict: `lgtm_merged`, merged: true, bugs_found: [] (in-diff)

## Gate

- In-diff bugs: **0** (machine: 0 findings). Recommendation: approve.

## Machine findings

- PR-embedded + independent re-run agree: **0 findings, 0 blocking bugs**.

## Manual verification (secret-handling core)

- Triple shape check requires exactly `{server, userid, password}` in any order;
  single-field bodies (host/port/user/name/password-only) match earlier
  dispatch branches first — dispatch unambiguous, no full-replace fallback leak.
- `updateDatabaseConnectionOnly`: base64-decodes the single secret pre-apply
  (product norm, mirrors password-only path); blank/masked-placeholder/overlong
  values and non-database drivers throw **before** `savePubServer` (no write);
  error messages never carry the secret (verified in shape class: reject paths
  use constant messages).
- Response masks stored secret as `passwordEntry` via `toPSPublishServerInfo`;
  SPA secret input always opens empty, never written into loaded state; cancel
  confirms when dirty; 400/403/409 keep form + previous values.
- New `PSDatabaseDeliveryConnectionUpdate.java` + test carry 2026 Intersoft
  headers; no new public/protected signatures (new class + private methods) —
  downstream_checked: none is correct.
- 8 new unit tests (shape + apply + validation) + 7/8 new Vitest; H2 Playwright
  asserts the one-save triple on the wire (3 props, base64 secret, no other
  props), never-rendered secret, Local-driver no-op, blank/cancel/4xx keep
  state; 0 pageerrors; 0 new server.log ERROR.
- C1 evidence present (sitemanage 4237/0, WebUI build + 253-file/1505-test
  publishing vitest, tsc clean). product-docs checkbox: done (new "Set a
  database delivery-server connection" section).
- Checks: all green. No unresolved review threads.

## Residuals

- None.
