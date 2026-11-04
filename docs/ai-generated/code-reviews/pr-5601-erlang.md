<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5601

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5601
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5588
- Parent: #4532
- Base: origin/main
- Head: 970df2bb52
- Reviewed: published PR head (worktree `~/.grok/worktrees/intersoft-workspace-percussioncms/slice-5588`)
- Reviewer: independent Erlang pass (not the implementer persona)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1441 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=159 (max 15), cyclomatic=202 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

- The single finding is **preexisting**: `handleSave` already carried the date, datetime, number, decimal, yes-no, link, table, and keyword NUL gates before this slice. This PR adds one more gate in the same chain; it does not introduce the complexity. Splitting `handleSave` is a separate slice, not part of a file-field NUL refusal.
- The gate reads the **raw** field value and the picked `File.name`, not the rendered name. `displayBinaryFileName` sanitizes the displayed text, so a check against the rendered name would let the NUL through — this is the substantive correctness point of the slice.
- A file row is excluded from the item-field PUT by `fieldsForEditorSave` (`row.kind === "file"` is skipped), so the client refusal is what keeps a NUL-bearing file name from reaching the server through the binary upload path (`form.append("file", file, file.name)` in `itemBinaryApi.ts`).
- Scope check: the slice covers the file field only. The image field (`row.kind === "image"`) has the same binary path and is **not** covered here; image replace is explicitly out of scope for #5588, so that gap stays visible in the parent tracker rather than being silently widened.
- No missing behavioral tests: `fileField.test.ts` (5) covers the value check, the picked-file check, and the collector's kind filtering; the `EditorHost` block (4) covers the refusal, the reload, the ordinary save, and HTTP 400 / 403 / 409; the H2 Playwright spec covers the stored-name refusal and the ordinary replace end to end.
- No non-portable path/file I/O: the slice compares strings only.
- Gate: **PASS**.

## Independent verification (reviewer, not the implementer)

Standalone JDK 21 clean install on the branch tree, run from the review worktree:

- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS (Surefire + Vitest green).
- `cd modules/perc-qa-automation && ../../mvnw clean install` — BUILD SUCCESS.
- `npx vitest run ../../test/ts/editor/` — Test Files 59 passed, Tests **691 passed** (includes the new `fileField.test.ts` and the `EditorHost` `#5588` block).
- `npx tsc --noEmit -p tsconfig.json` — clean.

C5 live UI proof on a fresh H2 QA cell with this branch's WebUI SPA hot-deployed (`TEST_CMS_URL=http://127.0.0.1:9993`):

- `python3 docker/scripts/perc-devctl.py qa-up --skip-image-build --then-qa-deploy-webui` — RESULT:OK
- `qa-deploy-webui` RESULT:OK; `qa-deploy-war-jars --restart-jetty` RESULT:OK
- `qa-health` — RESULT:OK HTTP:200 HEALTH:healthy
- `npm run test:surface -- --path tests/editor-host-file-nul.spec.js` — **1 passed** (refuses a stored file name with a NUL, keeps the previous file, and still saves an ordinary replace)
- server.log-clean=yes (0 ERROR/FATAL in the QA cell `server.log` for the test window)

Verdict: **LGTM** — independent review, C1 and C5 re-verified.
