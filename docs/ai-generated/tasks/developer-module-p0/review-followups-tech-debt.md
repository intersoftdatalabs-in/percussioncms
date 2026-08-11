# Developer module — review follow-ups & tech debt

|        Field        |                                                             Value                                                              |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------|
| **Purpose**         | Durable backlog for items **deferred from PR review** so they are not lost when threads close                                  |
| **Product roadmap** | Still use [README.md](./README.md) “Next slices” and [content-type-api-gaps.md](./content-type-api-gaps.md) for FR-driven work |
| **Sources**         | PR #1588 (Kilo), #1594 (Minimax/Kilo), #1596 (Minimax/Kilo), #1598 (Kilo) — thread replies only until this file                |

Capture rule: when a review finding is **intentionally not fixed in that PR**, add a row here (or update disposition) in the same change set if possible.

---

## Cross-cutting REST / API

|      ID      |                                                                      Item                                                                      |                                                   Why deferred                                                    |                            Suggested home                             |                                                             Status                                                              |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| REST-ERR-01  | **Typed JSON error body** for 4xx/5xx (`{ status, message, code? }` or project-standard DTO) instead of bare `WebApplicationException(e, 500)` | Sibling catalog resources use the same pattern post-#1588; fix is a **global exception mapper**, not per-resource | `rest` exception mapper + SPA `ApiError` alignment                    | Done — #2085 (`WebApplicationExceptionMapper` → `RestError` + SPA `extractRestErrorMessage` / `formatApiError` / `panelErrMsg`) |
| REST-ERR-02  | Consistent **cause-preserving** + client-visible message across all Developer catalog resources                                                | Partially done per-resource; should be centralized with REST-ERR-01                                               | Same as REST-ERR-01                                                   | Done — #2085 (cause-preserving client message in global WAE mapper)                                                             |
| REST-TEST-01 | Prefer **resource unit tests with mocked adaptors** for every new `GET/POST/...` surface (200/404/400/500)                                     | Some PRs added these (#1594, #1596 harden); make it the default checklist                                         | `rest/src/test/...`                                                   | Open (process)                                                                                                                  |
| REST-GAPS-01 | **`designGaps` as structured `{ code, message }`** (i18n / docs links / SPA grouping)                                                          | Free-text strings enough for P0 honesty; structured form needed for product UX                                    | `ContentTypeDetail`, `TemplateDetail`, `SlotDetail`, pipelines if any | Open                                                                                                                            |
| REST-GAPS-02 | Avoid **duplicating identical designGaps arrays** on every detail row if payload size becomes a concern                                        | Currently intentional so SPA needs one call; constant list is fine                                                | Detail DTOs or `X-Design-Gaps` / static SPA list                      | Open (low)                                                                                                                      |

---

## WebUI Developer module

|     ID      |                                             Item                                             |                               Why deferred                                |                Suggested home                |                                                                      Status                                                                      |
|-------------|----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| UI-ERR-01   | Finish migrating all panels to shared **`panelErrMsg`** (`developer/errors.ts`)              | Introduced on pipelines/template paths; older panels still inline ladders | All `WebUI/src/main/ts/developer/*Panel.tsx` | Done — [#2090](https://github.com/intersoftdatalabs-in/percussioncms/pull/2090) (ContentTypesPanel last ladder + Vitest)                         |
| UI-STYLE-01 | Shared **catalog/theme tokens** (`catalogStyles.ts` or CSS module) for mono/muted/padding    | Partial adoption; many panels still inline hex colors                     | `developer/catalogStyles.ts` + consumers     | Done — [#2102](https://github.com/intersoftdatalabs-in/percussioncms/pull/2102) (`catalogColors` + tableHeaderRow/tableRow; panels adopt tokens) |
| UI-TABLE-01 | Prefer **`SimpleCatalogTable`** (+ optional open button) over per-panel hand-rolled tables   | Templates panel was fixed on #1596; slots/others may still diverge        | `CatalogTable.tsx` + panels                  | Done — list catalogs adopt `SimpleCatalogTable` (#2087 / #2106); detail-panel nested tables residual                                             |
| UI-SRC-01   | Template **source viewer** enhancements: syntax highlight, line numbers, copy-to-clipboard   | P0.4b only needed a readable `<pre>`                                      | `TemplateDetailPanel`                        | Done — pure helpers + line gutter + copy + lightweight highlight (issue #2088 / #2114)                                                           |
| UI-SRC-02   | Long binding **expression** preview (“show more”) beyond maxWidth clamp                      | Clamp shipped; expand UX later                                            | `TemplateDetailPanel`                        | Open                                                                                                                                             |
| UI-A11Y-01  | Consistent a11y for catalog open actions (button-only, aria-labels on back/nav)              | Improved on CT/templates; audit remaining panels                          | Developer panels                             | Done — [#2902](https://github.com/intersoftdatalabs-in/percussioncms/issues/2902) (remaining detail back + Keywords open aria-label peers)         |
| UI-TEST-01  | Dedicated **panel-level** tests (empty/error/success) for each catalog, not only shell tests | Pipelines + template detail have some; make default                       | `WebUI/src/test/ts/developer/`               | Open (process)                                                                                                                                   |

---

## Process / hygiene (review bots)

|   ID    |                                                Item                                                 |                              Why deferred                              |                   Suggested home                   |     Status     |
|---------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|----------------------------------------------------|----------------|
| PROC-01 | **Erlang / pre-commit review** report when Maven modules change (`.kilo/rules` / team practice)     | Fixpacks sometimes skip with explicit ack on PR; should be intentional | `docs/ai-generated/code-reviews/` + PR body        | Open (process) |
| PROC-02 | **Standalone** `rest` then `sitemanage` clean install evidence in PR body (AGENTS Pre-PR HARD GATE) | Often run reactor compile in worktrees; CI remains source of truth     | PR template / AGENTS.md reminder                   | Open (process) |
| PROC-03 | Do not leave **deferred review items only on GitHub threads**                                       | This file exists to prevent that                                       | Update this doc in the same PR or a docs follow-up | Active         |

---

## Product slices (not deferred review)

Tracked on **[issue #1622](https://github.com/intersoftdatalabs-in/percussioncms/issues/1622)** (living todo list). Do not maintain a parallel “next slices” checklist in README.md on every PR.

API surface gaps for content types: [content-type-api-gaps.md](./content-type-api-gaps.md).

---

## Per-PR notes (optional pointers)

|  PR   |                                                                       Notes                                                                       |
|-------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| #1588 | Early Kilo pass; exception-cause and SPA a11y patterns                                                                                            |
| #1594 | Pipelines catalog; [code-reviews/2026-07-29-pr-1594-pipelines-kilo-followup.md](../../code-reviews/2026-07-29-pr-1594-pipelines-kilo-followup.md) |
| #1596 | Template detail; REST-ERR-01, REST-GAPS-01, UI-SRC-01 deferred on threads                                                                         |
| #1598 | CT field rule flags; unit tests for `mapOccurrence` / `hasTranslation`                                                                            |

---

## How to use

1. **New PR review deferral:** add/update a row, set Status `Open`, link PR in “Per-PR notes”.
2. **When fixed:** set Status `Done` and note the fixing PR.
3. Prefer **small dedicated PRs** for cross-cutting items (REST-ERR-01, UI-STYLE-01) rather than stacking onto feature slices.

