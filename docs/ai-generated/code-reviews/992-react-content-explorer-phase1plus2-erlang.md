# Erlang review — 992-react-content-explorer — Phase 1+2 commit

**Branch**: `992-react-content-explorer`
**Date**: 2026-07-19
**Scope**: uncommitted + unstaged code in `WebUI/src/main/ts/`, `WebUI/src/test/ts/`, `scripts/` (planning artifacts under `specs/992-react-content-explorer/` are out of Erlang scope — they are spec/plan docs, not code).

## Files reviewed

- `WebUI/src/main/ts/api/paths.ts` (extended; 12 new URL constants)
- `WebUI/src/main/ts/registry.ts` (extended; 2 new registry entries)
- `WebUI/src/main/ts/api/contentExplorer/pathApi.ts` (new; typed REST client)
- `WebUI/src/main/ts/api/contentExplorer/types.ts` (new; DTO mirrors)
- `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` (new; placeholder)
- `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx` (new; placeholder)
- `WebUI/src/main/ts/contentBrowser/types.ts` (new; host contract types)
- `WebUI/src/test/ts/contentExplorer/index.ts` (new; scaffold)
- `WebUI/src/test/ts/contentBrowser/index.ts` (new; scaffold)
- `scripts/create-large-folder-fixture.sh` (new; perf fixture seed)
- `scripts/README.md` (extended; fixture script doc)

## Summary

Phase 1+2 setup and foundational scaffolding for the 992-react-content-explorer feature. The new TS/TSX code is type-only (placeholder components + typed REST client + URL constants + Vitest directory scaffolds). No runtime behavior changes for existing production paths. **Two HARD gates found** in the shell script (`scripts/create-large-folder-fixture.sh`):

- **H1 — False green on child-creation failures** (Erlang hard gate "process or API reports success when child work failed"). `set -euo pipefail` is set, but every `curl` invocation has `|| true`, so the script always exits 0 even when every child creation 4xx's. A 500-iteration run could report success with zero folders created.
- **H2 — Secret on command line** (Erlang hard gate "Passwords or secrets on process command lines"). `curl -u "${CMS_USER}:${CMS_PASS}"` puts the password on the process command line, visible to `ps` on Linux/macOS during the run.

Both fixed in this commit (see Diff below).

LOW / style findings (not blocking, intentional placeholders will be rewritten in US1/US2/T017/T040):

- **`ContentExplorerShell.tsx` / `ContentBrowser.tsx`** use `import * as React` + `React.createElement(...)` instead of the project's `import React, { ... } from "react"` + JSX pattern. Intentional: these are placeholder components scheduled for full JSX rewrite in US1 (T017) and US2 (T040). Adding style churn now would be thrown away. Worth a follow-up note in US1 to use the project's `import React, { useState, ... } from "react"` pattern.
- **`encodePath` in `pathApi.ts`** correctly splits on `/` before `encodeURIComponent` so the multi-segment URL pattern is preserved for the JAX-RS `{path:.*}` route. Verified: `encodeURIComponent("/")` would be `%2F`, but `["a","b"].map(encodeURIComponent).join("/")` returns `"a/b"`. No bug.
- **No behavioral tests in this commit.** The Vitest scaffolds (`contentExplorer/index.ts`, `contentBrowser/index.ts`) are directory + JSDoc placeholders only. **Acceptable** because (a) the new TS code in this commit is itself placeholder/scaffold code (no runtime logic), (b) behavioral tests are explicitly scheduled per US in `tasks.md` (T013–T016, T037–T039, T048–T050, T058–T059, T065–T066, T071–T073), (c) `pathApi.ts` is a thin transport wrapper around `apiFetch` whose behavior is covered by the existing `apiFetch` tests in `WebUI/src/test/ts/`. The behavioral test gate applies to non-trivial logic in each US PR, not to scaffolding.

## Cross-platform path checklist

Erlang requires applying this checklist whenever the diff touches file I/O, paths, installers, packaging, or path assertions in tests.

- **TS code (pathApi.ts, paths.ts, registry.ts)**: uses `/` for URL paths only. URL/URI/ZIP entry paths correctly use `/` (patterns.md false-positive guard applies). ✓
- **Shell script (`create-large-folder-fixture.sh`)**: uses `/Rhythmyx/services/...` for HTTP URLs (URL paths, correctly `/`). No filesystem path joins. The script targets Linux/macOS; README documents a Windows `.cmd` counterpart as future work. **Note**: per root `AGENTS.md` **Cross-Platform File I/O & Paths**, repo automation that must run on Windows needs a `.bat`/`.cmd` counterpart. The script is **explicitly opt-in** for UAT perf runs by an implementer, not a CI-required workflow — a Windows `.cmd` is a follow-up. **No cross-platform violation in this commit.**
- **No new Java I/O or path code.** Server-side work explicitly evaluated to "none added for 8.2" in `cutover-inventory.md` §C (system/ decision table).
- **No tests added in this commit** that assert path strings.

## Recommendation

**Approve after fixes below are committed.**

## Diff (fixes in this commit)

### `scripts/create-large-folder-fixture.sh`

Replace `|| true` swallowing with explicit failure tracking, and use a netrc-style credential file to keep `${CMS_PASS}` off the process command line:

```diff
-  curl -sS -k -u "${CMS_USER}:${CMS_PASS}" \
-    -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}?name=PerfFixtureRoot" \
-    -o /dev/null -w "createRoot=%{http_code}\n" || true
+  curl -sS -k \
+    -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}?name=PerfFixtureRoot" \
+    -o /dev/null -w "createRoot=%{http_code}\n"
```

```diff
-  for i in $(seq 1 "${FIXTURE_COUNT}"); do
-    child="child_$(printf '%04d' "$i")"
-    curl -sS -k -u "${CMS_USER}:${CMS_PASS}" \
-      -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}/PerfFixtureRoot?name=${child}" \
-      -o /dev/null -w "${child}=%{http_code}\n" || true
-  done
+  netrc_file="$(mktemp)"
+  printf 'machine %s login %s password %s\n' "${CMS_HOST}" "${CMS_USER}" "${CMS_PASS}" > "${netrc_file}"
+  chmod 600 "${netrc_file}"
+  trap 'rm -f "${netrc_file}"' EXIT
+
+  failures=0
+  for i in $(seq 1 "${FIXTURE_COUNT}"); do
+    child="child_$(printf '%04d' "$i")"
+    code="$(curl -sS -k --netrc-file "${netrc_file}" \
+      -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}/PerfFixtureRoot?name=${child}" \
+      -o /dev/null -w '%{http_code}')"
+    printf '%s=%s\n' "${child}" "${code}"
+    case "${code}" in
+      2*) ;;  # 2xx — created
+      409) ;; # 409 — already exists (idempotent re-run)
+      *)   failures=$((failures + 1)) ;;
+    esac
+  done
+
+  if [ "${failures}" -gt 0 ]; then
+    echo "[$ts] FAILED: ${failures}/${FIXTURE_COUNT} child creations did not return 2xx or 409. See above." >&2
+    exit 1
+  fi
```

Both fixes are applied in the committed version of the file.

## Gate

**May commit/push: yes** (after fixes above).

**Memory patterns hit**: false-green-on-ignored-exit-codes, secrets-on-process-command-line.
