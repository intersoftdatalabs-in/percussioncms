# QA results — keywords readback includeChoices + saved-choices (#6203)

QA verification slice for merged PR #6183 (fix(qa): keywords readback
`includeChoices` claim + saved-choices assertion, parent #6174). Executes the
three numbered test-plan steps from issue #6203 against the H2 QA cell and
records the results here.

**Result: PASS** — 2/2 surface tests green, zero JS console/page errors,
zero new `server.log` errors.

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`,
  `TEST_CMS_URL=http://127.0.0.1:9993` (H2, `perc-matrix-cms-h2`)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK`,
  `HTTP:200 HEALTH:healthy`

## Test plan

### 1. Packaged keyword saved-choices readback — PASS

Opened the Developer → Keywords catalog and the packaged `Body_Markup` row.
The detail editor's `developer-kw-saved-choices` region lists exactly **2**
choices with stored labels **Text** and **HTML**
(`Text (1) — Text Markup [1]`, `HTML (2) — HTML Markup [2]`), matching the
stored detail (`GET /services/keywords/Body_Markup` returns both choices).

Live proof: `docs/ai-generated/proofs/issue-6203-keywords-readback.png`
(saved-choices region, 2 rows).

### 2. Choice-less created keyword empty state — PASS

Created a keyword through the shell (unique `QAKw6045Rb*` label), opened its
detail: no `developer-kw-saved-choices` list, `developer-kw-choices-empty`
state visible. REST cleanup (`DELETE /services/keywords/{guid}` → 200/204)
succeeded and the catalog no longer lists the row (catalog omission proven).

### 3. Choice counts match `includeChoices=true` — PASS

The catalog table renders through `GET /services/keywords?includeChoices=true`
(`KeywordsPanel.tsx` calls `listKeywords(true)`); the count cell reads
`kw.choices?.length ?? 0`, nonzero for `Body_Markup` (2). The thin-read parity
probe (`includeChoices=false`) lists the same row without per-keyword choice
payload — framed in-spec as an API-parity probe, not the shell request.

## Quality signals

- `npm run test:surface -- --path tests/developer-keywords-readback.spec.js`
  → **2 passed**, `assertConsoleClean` in-spec (zero pageerror / console errors)
- `server.log`: zero `ERROR` / `FATAL` lines after the run (no keyword-related
  noise; no delta from this run)
- `cd modules/perc-qa-automation && ../../mvnw clean install` → BUILD SUCCESS

## Out of scope (per #6203)

Keyword CRUD itself; editor 409 handling (defensive fallback retained).
