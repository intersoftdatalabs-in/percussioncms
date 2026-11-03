<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5511

Independent pre-merge review. Author of the feature is not this review. Persona: erlang 0.1.1. Persona source: /home/nate/.local/share/mkd/agents/erlang.

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml \
    WebUI/src/main/ts/publishing \
    WebUI/src/test/ts/publishing \
    modules/perc-qa-automation/frontend/tests/publishing/sitesDeliveryServerLocalFolder.spec.js \
    product-docs/8.2/admin/publishing.md \
    projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSLocalDeliveryFolderUpdate.java \
    projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java \
    projects/sitemanage/src/test/java/com/percussion/pubserver/impl/PSLocalDeliveryFolderUpdateTest.java
```

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=21 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
