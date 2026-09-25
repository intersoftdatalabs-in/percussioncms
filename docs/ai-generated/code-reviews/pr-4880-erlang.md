<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4880

Status: mkd-code-review 0.1.18, pack percussion, gate advisory, git-base origin/main.
Interpreter: 0 in-diff bugs. Three suggestions are non-blocking. Gate PASS. May commit/push: yes.
CI snapshot: python-build-scripts failed on SlotRelationshipAdaptor.java (not in this diff; present on base). Not a required status check (branch protection contexts null).

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/test/java/com/percussion/delivery/distribution/DtsLinuxServiceNamespaceSoakTest.java:43
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: Both happy-path tests skip only when `unshare --help` is missing or non-zero. `ProcessBuilder("unshare", "--help")` still throws `IOException` if the binary is absent (error, not skip), and `--help` succeeding does not mean `--user --map-root-user --mount` is allowed. On a Linux CI image without user namespaces both module suites fail.
- Suggestion: Share one skip helper: missing `unshare` (catch `IOException`) and EPERM on `--user --map-root-user --mount` → `assumeTrue` false.
- Status: open

### Issue 2 -- Severity: suggestion

- File: deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/README-systemd.md:100
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: CMS `README-systemd.md` documents the user-namespace soak. The DTS sibling still stops at “offline review of the template + scripts” and does not mention `linux-service-namespace-soak.sh`.
- Suggestion: Add the same “namespace soak, not journalctl sign-off” paragraph used on the CMS README.
- Status: open

### Issue 3 -- Severity: suggestion

- File: scripts/linux-service-namespace-soak.sh:29
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: `uid_map="$(awk … || true)"` treats a missing `awk` or unreadable `uid_map` as empty, which is not equal to `0 0 4294967295`, so the identity-map refuse does not fire. Mapped-root then still requires `id -u` = 0. Host root plus `PERCUSSION_SOAK_NS=1` plus no `awk` would reach `mount`. Unlikely on a real Linux image.
- Suggestion: If `uid_map` is empty, refuse (fail closed) the same as the identity map.
- Status: open

