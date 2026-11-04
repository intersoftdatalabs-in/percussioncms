<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5604

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5604
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5519
- Parent: #4530
- Base: origin/main
- Head: 05bae95dd0
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/slice-5519`)
- Reviewer: independent Erlang pass (not the implementer persona)

## CLI stdout (`mkd-code-review analyze --format markdown`)

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

## Agent interpretation

- **0 findings, 0 bugs.** The slice is a one-line behavior change plus tests
  and docs, so the analyzer had nothing to flag. The reviewer's own reading
  confirms the change is in the shape of the merged sibling slices:
  - `readOverlayEdits` tags a keyword edit with `dataType: "keyword"`, exactly
    where the merged link slice tags `link`, the number slice tags
    `integer`/`float`, the date slice tags `date`, the datetime slice tags
    `datetime`, and the yes-no slice (#5576 / PR #5596) tags `boolean`.
  - The server treats `dataType` as a hint, not an enum:
    `PSItemEditorNumeric.rejectionMessage` returns `null` for a
    non-numeric type, `PSItemEditorLink.isLinkDataType` is false for
    `keyword`, and `PSItemEditorFieldsMapper.applyUpdates` never reads
    `dataType` — it stores the value. So `keyword` is inert on the server
    and cannot turn a valid save into a 400.
  - `PSItemEditorField.dataType` javadoc now lists the types the editor may
    send, so the wire contract is documented rather than implicit.
- Correctness points checked by the reviewer:
  - The client sends `format` rows through the same item field PUT the
    Content Editor uses, so the language string, label, description, status,
    and the other format fields on the page stay.
  - The other fields on the assembled page stay: the PUT body carries their
    stored values unchanged, and the spec asserts each one.
  - Leaving the field unsaved does not write; **Cancel** does not write; a
    **read-only** keyword field is not written (the schema `readOnly` flag
    keeps it out of the overlay, so it never reaches the PUT at all).
  - HTTP 400, 403, and 409 are not success: the host restores the previous
    choice and shows **Could not save**, not **Fields saved**.
  - The stored choice shows again after a reload, because the assembly host
    re-reads the fields on mount.
- No missing behavioral tests: `overlayFields.test.ts` (a keyword edit
  carries `dataType: "keyword"`), `AssemblyHost.test.tsx` (the PUT body row
  for the keyword carries `dataType: "keyword"` and the title row carries
  none), and the new H2 Playwright spec (6 cases: the save with the keyword
  data type and the reload, Cancel, a read-only keyword field, and HTTP
  400 / 403 / 409).
- No non-portable path/file I/O.
- Gate: **PASS**.

## Independent verification (reviewer, not the implementer)

Standalone JDK 21 clean install on the branch tree, run from the review worktree:

- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS.
- `cd rest && ../mvnw clean install` — BUILD SUCCESS.
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS (Surefire + Vitest green).
- `cd modules/perc-qa-automation && ../../mvnw clean install` — BUILD SUCCESS.
- `npx vitest run` (full WebUI suite) — Test Files **814 passed**, Tests **7863 passed**.
- `npx vitest run ../../test/ts/assembly/overlayFields.test.ts ../../test/ts/assembly/AssemblyHost.test.tsx` — Test Files 2 passed, Tests **250 passed**.

Regression proof (the reviewer reverted the one-line fix and rebuilt):

- Removing the `dataType: "keyword"` spread makes the WebUI Maven build
  **fail**, and the three assertions that catch it are
  `overlayFields.test.ts` *"sends the keyword data type with a keyword edit"*,
  `overlayFields.test.ts` *"reads a keyword NUL from the assembled select"*, and
  `AssemblyHost.test.tsx` *"still saves a listed catalog choice"*.
- The fix was restored and the tree re-verified clean against the commit.

C5 live UI proof on a fresh H2 QA cell with this branch's WebUI SPA and
`sitemanage` / `rest` jars hot-deployed (`TEST_CMS_URL=http://127.0.0.1:9993`):

- `python3 docker/scripts/perc-devctl.py qa-up --skip-image-build --then-qa-deploy-webui` — RESULT:OK
- `qa-deploy-webui` RESULT:OK; `qa-deploy-war-jars --restart-jetty` RESULT:OK
- `qa-health` — RESULT:OK HTTP:200 HEALTH:healthy
- `npm run test:surface -- --path tests/assembly-keyword-save.spec.js` — **6 passed** (saves the choice with the keyword data type and shows it again after reload; Cancel does not write; a read-only keyword field is not written; HTTP 400 / 403 / 409 leave the previous choice in place)
- `npm run test:surface -- --path tests/assembly-required-keyword-field.spec.js` — **4 passed** (sibling slice #5520 unchanged)
- `npm run test:surface -- --path tests/assembly-keyword-nul-field.spec.js` — **4 passed** (sibling slice #5521 unchanged)
- server.log-clean=yes (0 ERROR/FATAL in the QA cell `server.log`)

Verdict: **LGTM** — independent review, C1 and C5 re-verified.
