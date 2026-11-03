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

# Erlang review — issue #5521 assembly keyword NUL

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml \
    WebUI modules/perc-qa-automation product-docs
```

Cwd: worktree `fix/issue-5521-assembly-keyword-nul`.
CLI: mkd-code-review 0.1.18. Reviewer did not author this PR.

## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- CLI: mkd-code-review 0.1.18
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- May merge: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: @babel/eslint-parser
- Description: [moderate] @babel/eslint-parser@*: eslint
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

### Issue 2 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: @eslint-community/eslint-utils
- Description: [moderate] @eslint-community/eslint-utils@*: eslint
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

### Issue 3 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: @eslint/config-array
- Description: [moderate] @eslint/config-array@*: minimatch
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

### Issue 4 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: @eslint/js
- Description: [moderate] @eslint/js@>=10.0.1: eslint
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

### Issue 5 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: eslint
- Description: [moderate] eslint@0.7.1 - 2.0.0-rc.1 || >=4.1.0: @eslint-community/eslint-utils
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

### Issue 6 -- Severity: suggestion

- File: package-lock.json:1 (preexisting)
- Rule: `js.audit`
- Tool: `npm-audit`
- Pattern-id: minimatch
- Description: [moderate] minimatch@<11.0.1: minimatch has potential denial of service via globparts amplification
- Suggestion: Run package manager audit fix / upgrade the dependency.
- Status: open

Disposition: preexisting npm-audit rows on `package-lock.json`. This slice does not change lockfiles. Not blocking.

## Gate notes

- In-diff bugs: none.
- Behavioral tests: `nulKeywordFieldNames` names a keyword NUL and ignores other kinds; AssemblyHost refuse-before-PUT, overlay strip, Cancel, catalog save, HTTP 400/403/409; Playwright `assembly-keyword-nul-field.spec.js` (4 passed on H2 QA).
- Change-class companions: overlay helper + AssemblyHost gate, Vitest, Playwright, `product-docs/8.2/admin/content-explorer.md` Active Assembly keyword cell (not an index).
- Non-portable filesystem paths: none.
- Peer of `nulLinkFieldNames` / required-keyword gate. Required-blank keyword stays on `blankRequiredKeywordFieldNames`. EditorHost keyword NUL stays on parent #4532.

Gate: **PASS**. Recommendation: approve.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
