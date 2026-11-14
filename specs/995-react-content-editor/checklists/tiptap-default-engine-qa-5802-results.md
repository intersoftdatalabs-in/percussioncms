# QA record — #5802: TipTap default rich-text engine + tinymce escape hatch

First-pass agent execution of the #5802 test plan (QA for merged #5785, slice
of #5758 / program #5622) on a fresh H2 QA cell. **PASS**: TipTap is the
default engine with working edit+save and zero `/sys_resources/tinymce`
fetches; the `?engine=tinymce` escape hatch is gone by design (#5902, shipped
in #6112 — the param is now ignored and TipTap renders); regression sweep
green; zero JS console/page errors in-spec; golden smoke green.

> Parent: #5758 (program #5622) / merged PR #5785. This PR supplies QA
> evidence only — it does not close #5802; human sign-off still required.

## Environment

- Cell: `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`,
  `TEST_CMS_URL=http://127.0.0.1:9993` (`TEST_DB_TYPE=h2`,
  `QA_CONTAINER=perc-matrix-cms-h2`).
- `qa-health` → `RESULT:OK HTTP:200 HEALTH:healthy` on
  `/Rhythmyx/rest/mimetypes` (before the runs).
- Creds: `ADMIN_USERNAME=Admin`, password from `qa-up` output (not committed).
- Base: `origin/main` at `76b70e954d` (post-#6112: escape hatch removed).
- Tear-down: `qa-down` → `RESULT:OK` (cell removed).

## Test-plan mapping (#5802)

| # | QA step (#5802 test plan) | Result |
|---|---------------------------|--------|
| 1 | Smoke default engine: HTML field with no `?engine` param → TipTap renders; edit + save works; `/sys_resources/tinymce` never fetched | PASS (`editor-host-default-tiptap` test 1: `data-editor-kind=html`, `data-editor-engine=tiptap`, `contenteditable=true`, PUT body `<p>Bye</p>`, `Saved` shown, `tinymceRequests=[]`, zero console/page errors, axe gate green) |
| 2 | Escape hatch: `?engine=tinymce` → legacy TinyMCE-backed editor | PASS-WITH-NOTE: behavior superseded by design — #5902 (shipped #6112, on this base) removed the hatch; `?engine=tinymce` is now ignored and renders TipTap with zero tinymce fetches (`editor-host-default-tiptap` test 2). No legacy surface remains to restore. |
| 3 | Other values: `?engine=<anything-else>` → TipTap | PASS (by construction: the engine param is no longer consumed anywhere — `HtmlFieldWidget` renders `TipTapFieldWidget` unconditionally; covered by the same ignored-param path as step 2) |
| 4 | Deep-link handoff: login-return / deep links preserve the `engine` param | N/A-OBSOLETE: no `engine` param is consumed by any route, so there is nothing to preserve; login + shell entry still proven by golden smoke (Admin login + Content Explorer shell, 2 passed) |
| 5 | Regression sweep: HTML validation (required-blank, NUL, unsafe markup), clear, rich controls behave as before under the TipTap default | PASS (html-fields 3, html-clear 1, html-nul 1, html-required-blank 1, html-unsafe 1, rich-controls 2, html-tiptap 6, html-tiptap-nul 1, html-tiptap-i18n 1 — all green, zero unexpected console/page errors in-spec) |

## Spec runs (this PR)

| Spec | Result |
|------|--------|
| `tests/editor-host-default-tiptap.spec.js` | 2 passed |
| `tests/editor-host-html-fields.spec.js` | 3 passed |
| `tests/editor-host-html-clear.spec.js` | 1 passed |
| `tests/editor-host-html-nul.spec.js` | 1 passed |
| `tests/editor-host-html-required-blank.spec.js` | 1 passed |
| `tests/editor-host-html-unsafe.spec.js` | 1 passed |
| `tests/editor-rich-controls.spec.js` | 2 passed |
| `tests/editor-host-html-tiptap.spec.js` | 6 passed (rerun; see flake note) |
| `tests/editor-host-html-tiptap-nul.spec.js` | 1 passed |
| `tests/editor-host-html-tiptap-i18n.spec.js` | 1 passed |
| `tests/golden-unattended-smoke.spec.js` | 2 passed |
| **Total** | **21 passed, 0 failed (final)** |

Flake note: the first `editor-host-html-tiptap` run showed 5 passed / 1 failed
(`tiptap toolbar parity`, line 290). An immediate rerun of the same spec on
the same cell passed 6/6 with no code or cell change — first-run timing flake,
not a product defect. Both runs are recorded here.

## server.log

Zero `ERROR`/`FATAL` attributable to the spec runs. The cell log shows exactly
3 `ERROR` lines, all timestamped in the first (unstubbed) proof-screenshot
attempt window: `EditorItemLockResource checkout failed: Not a valid content
id: 42` + the two mapped `PSComponentSummary id '42' does not exist` lines —
the known fake-id-42 fixture artifact class (same as #5802's own agent
evidence), engine-independent and outside every spec assertion path. The
second proof capture used the full spec route stubs and produced no
server-side errors.

## Product-docs verification

`product-docs/8.2/admin/content-explorer.md` already documents the shipped UI:
TipTap as the bundled default HTML editor **and** the `?engine=tinymce`
removal ("the editor always renders TipTap, and the URL param is ignored").
Matches the live surface — no doc edit in this PR.

## Live proof

![TipTap default engine with toolbar, Body=Hi, engine=tiptap](docs/ai-generated/proofs/issue-5802-tiptap-default-engine.png)

React Content Editor host (default path, no engine param): TipTap toolbar
(link, image, table, code, lists), `data-editor-engine=tiptap`, Save enabled,
no checkout errors. PNG: `docs/ai-generated/proofs/issue-5802-tiptap-default-engine.png`
(1280x900, ~52KB).

## Out of scope (per issue)

- `modules/perc-tinymce` deletion (#5734), CM6 migration (#5701),
  classic-surface migration (#5784, incl. template editing + `sys_tinymce`
  control) — all residual-filed, intentionally untouched.
