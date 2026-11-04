<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.
-->

# Erlang review — PR #5564 Explorer move destination NUL (#5552)

Status: independent overnight review (this agent did not author the PR)

Persona: erlang 0.1.1

Persona source: ~/.local/share/mkd/agents/erlang

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models …/models.ollama-dev-coder.toml`

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Gate: **PASS** (in-diff bugs: 0; preexisting `paths.hardcoded_sep` is a CMS/URL path, not OS filesystem join)

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/folders/FoldersResource.java:835 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 835)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

## Intent / change-class closure

Move destination U+0000 stays in the picker and is not POSTed; REST `move/item` and `move/folder` return HTTP 400 without calling the adaptor. Companions: `moveDest.ts`, dialog, Vitest (picker, reduced actions, shell), `FoldersTest`, Playwright, product-docs. Copy-dest NUL is a sibling slice. No in-diff hard-gate bugs.

Recommendation: **approve**.
