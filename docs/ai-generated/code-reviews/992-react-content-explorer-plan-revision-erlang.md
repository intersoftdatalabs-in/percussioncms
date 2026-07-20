# Erlang review — 992-react-content-explorer plan/spec/tasks revision

**Branch**: `992-react-content-explorer-us1`
**Date**: 2026-07-19
**Scope**: Revision commit factoring in the new docker dev runtime (commit `2d35a93c49`) and the new `modules/perc-qa-automation/` Playwright + TestNG module (commit `7a6dcb4221`). Updates `spec.md`, `plan.md`, `tasks.md`, `quickstart.md`, `contracts/capability-matrix.md`.

## Motivation

After the docker dev runtime came online (commit `2d35a93c49`) and the `perc-qa-automation` module was cherry-picked (commit `7a6dcb4221`), much of what was flagged as "manual UAT" in the spec / plan / tasks is now **automatable via Playwright against the live docker dev CMS**. Additionally, the tasks file had five `<!-- handoff: requires running CMS / Vitest run / reviewer -->` markers (T024, T024a, T025, T027, T024a-dup, T028) that no longer apply — the docker CMS is running, Vitest is set up, and the reviewer can run Playwright locally. This revision commit reflects the new reality.

Per user directive:
- [Issue #1388 (MySQL install + collation)](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388) is **not** a blocker; the UI doesn't care about backend DB type.
- [Issue #1387 (FolderAdaptor ClassCastException)](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) **may** block tasks that hit the affected REST endpoints. Mitigations codified: mock in Vitest; `test.skip` + `BUG:` note in Playwright; flip on fix. **If a Phase 3+ task cannot proceed past the bug, escalate the fix into this feature's scope.**

## Files reviewed

| File | Change | Net lines |
|------|--------|-----------|
| `spec.md` | Added `modules/perc-qa-automation/` to module scope; dev runtime paragraph; new clarifications session; updated Desktop CE stack description (pure JavaFX 21); each SC rewritten with Playwright spec reference; added Playwright/vitest testing framework note; new assumptions covering dev runtime, backend-DB-is-not-UI, automated-via-Playwright, and #1387 mitigation policy | +16 |
| `plan.md` | Added Node 22, `modules/perc-qa-automation/`, dev runtime section, `RX_USEBASICAUTH` header, Playwright in Technical Context; updated Testing approach (two-layer); updated Constitution Check III/IV/VI/VII; per-US Playwright spec mentioned in each phase; Complexity Tracking rows for #1387, #1388, perc-qa-automation module | +15 |
| `tasks.md` | Header updated with two-layer test strategy; T012f (docker runtime) + T012g (Playwright) added; T024/T024a handoff markers removed; T024b/T025b (US1 Playwright spec + run) added; T028b (US6 Playwright), T045*-pw per host, T046b, T056b, T057b, T064b, T070b, T081b, T082b (axe-core) added; task count summary updated (106 → 125); new "Test framework map" section | +22 |
| `quickstart.md` | Rewritten with Docker dev runtime section at top; Playwright commands; per-scenario "Automated via" reference; new SC-009 axe-core automation note; SC-010 explicitly called out as intentionally manual; new troubleshooting rows for Playwright | +52 |
| `contracts/capability-matrix.md` | Added "Test framework note" header line; added `Test coverage` column to P0-Core table; per-host P-Host rows updated to reference Playwright specs | +63 |

## Hard gates checked

| Gate | Status |
|------|--------|
| Missing-behavioral-test gate | **Pass** — every SC now has a Playwright + Vitest pair referenced; axe-core a11y gate added (T082b); SC-010 (usability survey) explicitly kept manual with rationale. |
| Non-portable filesystem path joins | **Pass (n/a)** — no filesystem code in this commit. `/opt/Percussion` is a literal constant agreed on by host + container (per docker dev runtime commit `2d35a93c49`). |
| Secrets on command line | **Pass (n/a)** — no env-var changes; same `.env.compose` model as commit `2d35a93c49`. |
| Path containment | **Pass (n/a)** — none in this commit. |
| Duplicate method declarations | **Pass** — no compile pass to confirm; manual review of task additions shows no duplicates (T045*-pw are per-host siblings, all distinct). |
| Constitution IX (PR Review Comment Resolution) | **Pass** — per-PR subtasks (T027, T029a, T045a, T045b-pw, T045b, T045c-pw, T045c, T045d-pw, T045d, T047, T057, T057b, T064, T064b, T070, T070b, T081, T081b) all carry the same inline-reply + `resolveReviewThread` discipline via the existing task descriptions and the new "Test framework map". |

## Cross-platform path checklist

- All path tokens in this commit are URL/string literals — no shell or filesystem path joins. Docker dev runtime path `/opt/Percussion` is documented as host + container MUST match (per docker commit `2d35a93c49`).
- Playwright specs use `node`-runnable JS (no OS-specific shell). `auth.js` uses portable `path.resolve`. `package.json` pins `@playwright/test ^1.58` (cross-platform).

## Recommendation

**Approve.**

## Memory patterns hit

- **Task count summary must reflect all additions**: bumped 106 → 125 with the full list of new task IDs in the format-validation footer.
- **Per-US Playwright spec pattern**: each US gets a `T0XXb` Playwright task named after its spec (`tests/usN-<surface>.spec.js` or `tests/host-<host>.spec.js`).
- **Per-US axe-core gate**: single `T082b` task injects axe-core into every Playwright spec via `@axe-core/playwright`.

## Outstanding (deferred, non-blocking)

- **T012f + T012g docker/Playwright bring-up tasks** are tracked as open. They're already done in this session's docker commit but not yet marked `[x]` in `tasks.md`; flagged as open for the human implementer to verify (matches the existing "this commit sets up the contract; the implementer verifies" pattern from earlier tasks).
- **T024/T024a handoff markers removed** — but actual shell mount + verification is still a runtime task that needs the human implementer to commit the JSP edit + visual check. The TASK is now in scope (docker CMS is up); only the runtime-execution step is human-implementer work.

## Gate

**May commit/push: yes.**