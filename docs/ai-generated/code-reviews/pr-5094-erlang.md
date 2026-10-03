<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5094

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5094
- Base: origin/main
- Head reviewed: 923b1389dcdfc8960afe9fa1307bbed1dda4923e
- Reviewer: independent Erlang (did not author the change)
- Recommendation: approve

## Independent notes

Explorer Content → Set community writes `CONTENTSTATUS.COMMUNITYID` for one page or asset. Cancel, folders, empty selection, multi-select, unchanged/forbidden/blank ids, and HTTP 400/403/409 do not report success. Checkout held by another user is HTTP 409 and does not save. Behavioral coverage is in `ItemCommunityAssignmentRulesTest`, `PSItemCommunityServiceTest`, Vitest, and the H2 surface spec. Product docs on the Content Explorer page describe the action. No rule-file change.

The `secrets.heuristic` hit on `sitemanage-beans.xml:1442` is preexisting (this diff only adds the `itemCommunityRestService` jaxrs ref). Cognitive-complexity notes on `selectFirstContentItem` and `PSItemCommunityService.change` are suggestions, not defects; `change` follows the existing workflow-assignment shape. They do not block.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 22 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml:1442 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1442)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/explorer-set-community.spec.js:156 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `selectFirstContentItem` cognitive=16 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemCommunityService.java:121 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `change` cognitive=22 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
