# Erlang review: #4859 PublishingShell stop a running edition

**Branch:** `fix/issue-4859-runtime-stop-edition` (`1ea811c73b`)  
**Base:** `origin/main`  
**Issue:** [#4859](https://github.com/intersoftdatalabs-in/percussioncms/issues/4859) (slice of #4531)  
**Date:** 2026-09-25  
**Persona:** erlang 0.1.1 (`~/.agents/skills/erlang/`)  
**Reviewer:** Erlang (independent of implementer; read-only on product code)  
**Recommendation:** approve  
**Prior report:** same path, HEAD `1762c75974`, **BLOCK** (Playwright stop-failure `jsErrors`)  
**This pass:** re-review after `1ea811c73b` (`test(publish): ignore expected 409/500 resource logs on Runtime stop`)  
**Memory patterns hit:** WebUI Playwright companion; change-class closure (Vitest + Playwright + product-docs); CMS URL paths use `/`; HTTP-error specs must filter Chromium resource-status console noise

## Summary

Runtime Stop already existed. The old `stopRuntimeJob` catch called ops `stopPublishing` and, on dual failure, threw a plain `ApiError` object. `onStop` then used `e instanceof Error ? e.message : PUBLISH_ERROR`, so operators saw generic chrome instead of the server message, and a prior **Last result** could remain.

This change rethrows the design-stop error when the ops fallback also fails, formats it with `formatApiError`, and clears `lastResult`. Idle editions still omit Stop (`canStopEdition`). Companions: new `runtimeApi.stop.test.ts`, RuntimeSection Vitest, Playwright stop-failure spec, product-docs.

The previous Erlang pass **BLOCK**ed because the new Playwright error-path test asserted `jsErrors` empty while stubbing HTTP **409** and **500**. Follow-up `1ea811c73b` filters Chromium `Failed to load resource` lines for those statuses, matching `runtimeDemandContentIds.spec.js`. That bug is **fixed**. No remaining blocking issues.

`mkd-code-review` analyzed all 6 diff files and reported 0 machine findings. Hand review of `git diff origin/main...HEAD` (including the Playwright filter) found no in-diff bugs.

## CLI (`mkd-code-review analyze`)

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

`--git-base` included all 6 committed files (none omitted). Uncommitted `WebUI/.vitest/` / `WebUI/src/main/frontend/.vitest/` are local cache, not in the review scope. The review file itself is untracked and is the only write in this turn.

## CLI report (verbatim)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Hand review (intent / companions the CLI cannot see)

### Diff

| Path | Role |
|------|------|
| `WebUI/src/main/ts/api/publishing/runtimeApi.ts` | Dual-fail rethrows design-stop `primary` |
| `WebUI/src/main/ts/publishing/sections/RuntimeSection.tsx` | `formatApiError` + clear last result on stop failure |
| `WebUI/src/test/ts/api/publishing/runtimeApi.stop.test.ts` | **new** — success / fallback / dual-fail identity |
| `WebUI/src/test/ts/publishing/runtimeEditions.test.tsx` | Idle Stop hidden; stop-failure alert, no last result |
| `modules/perc-qa-automation/frontend/tests/publishing/runtimeEditions.spec.js` | Idle Stop count 0; stop-failure alert; **409/500 resource-log filter** |
| `product-docs/8.2/admin/publishing.md` | Idle Stop; dual-fail error region |

Commits vs `origin/main`: `1762c75974` (product + tests + docs), `1ea811c73b` (Playwright console filter). Author/committer: `Nate Chadwick <263952448+natechadwick-intsof@users.noreply.github.com>`. No agent rule files in the diff. Copyright on the new test file is Intersoft 2026 + Apache 2.0.

### Production behavior (looks correct)

`stopRuntimeJob` (`runtimeApi.ts:71-88`): design POST success returns as-is; design failure tries `stopPublishing`; ops success still returns `{ jobId: Number(jobId), status: "cancelled" }`; both fail → `throw primary`. Empty inner `catch` is justified (rethrows). `post()` throws a plain `ApiError` (not `Error`); `formatApiError` (`client.ts:182-198`) reads `body.message`. `onStop` (`RuntimeSection.tsx:172-185`) matches the demand-error path in the same file.

`canStopEdition` is `(runningJobId ?? 0) > 0`. Vitest and Playwright both assert idle edition `10` has no `runtime-stop-10`.

`stopPublishing` posts `PATHS.PUB_SERVERS` + `stopPublishing/{jobId}` (`…/publishmanagement/servers/stopPublishing/99`). The Playwright route glob matches that URL. The broader `**/services/publishmanagement/servers/**` handler continues non-GET, so the later stop-fulfill is not swallowed.

### Prior BLOCK — resolved

**Was:** `runtimeEditions.spec.js:176-249` (then) fulfilled design stop **409** and ops **500**, then `expect(jsErrors).toEqual([])`. Chromium logs `Failed to load resource: the server responded with a status of 409|500` for `route.fulfill` of those statuses. Same Runtime surface already filters **400** in `runtimeDemandContentIds.spec.js:160-166`.

**Now:** `runtimeEditions.spec.js:249-258` filters

```text
Failed to load resource: the server responded with a status of (409|500)
```

before asserting. `pageerror` listener remains (`:178`). Alert text (`edition job 99 is not running`) and `runtime-job-status` count 0 remain. Filter is assertion-time, same shape as the demand-publish peer (status list is 409|500 instead of 400). `isKnownPublishConsoleNoise` covers 403/404/409 only (no 500), so a local 409|500 filter is the right match for this stub.

## Issues

_No blocking issues._

## Suggestions (non-blocking)

- `RuntimeSection.onStop` uses `formatApiError(..., MSG.PUBLISH_ERROR)` rather than `mapJobStopError` (`publishing/jobStop.ts`), which maps 403/404/409 to catalog fallbacks when the body has no message. Fine when the body includes `message` (the tests’ shape). Catalog fallbacks would help empty 409 bodies.
- `runtimeEditions.test.tsx` stop-failure case leaves `stopRuntimeJob` rejected; it is the last test in the file. Restore the mock in `afterEach` if more cases are added.

## Change-class closure

Change class: **WebUI Publishing Runtime Stop (operator screen)**.

| Companion | Status |
|-----------|--------|
| `stopRuntimeJob` dual-fail rethrow | present |
| Runtime `onStop` `formatApiError` + clear last result | present |
| Vitest API: success / fallback / dual-fail | present (`runtimeApi.stop.test.ts`) |
| Vitest UI: idle no Stop; stop-failure alert | present |
| Playwright Runtime spec | present; error-path filters 409/500 resource logs |
| `product-docs/8.2/admin/publishing.md` | present |
| REST / sitemanage | N/A — existing stop APIs |

Issue AC: Stop only when running; stop API + failure text; H2 surface-filtered Playwright; product-docs. Parent #4531 Agent-progress comment is process, not this diff.

## Tests

Behavioral coverage of the new logic is present in Vitest:

1. Design stop 200 → no `stopPublishing`.
2. Design fail → ops success → `{ jobId, status: "cancelled" }`.
3. Both fail → `rejects.toBe(primary)` (same object).
4. RuntimeSection: idle row has no Stop; stop reject shows `body.message` and no `runtime-job-status`.

Playwright: happy path (start → refresh → stop) plus idle Stop hidden; stop-failure alert + no last result; `jsErrors` ignore expected 409/500 resource lines only. Unrelated console/page errors still fail the spec.

## Cross-platform path checklist

- [x] No new filesystem `"/" +` / `"\\" +` construction
- [x] REST/URL paths correctly use `/` (`…/runtime/jobs/{id}/stop`, `…/servers/stopPublishing/{id}`)
- [x] Tests assert URL fragments and testids, not OS path strings
- [x] No Unix-only temp/root assumptions
- [x] Line-ending assertions: none

**Outcome: clean.**

## Notes

- `onStart` / clear-site / purge still use `e instanceof Error` for API failures. Preexisting; not in this stop catch. Does not block.
- Dual-fail prefers the **design-stop** message over the ops 500 body. Documented in product-docs. Intentional.
- Erlang did not run Vitest or Playwright in this review turn.

---

Gate: PASS  
May commit/push: yes
