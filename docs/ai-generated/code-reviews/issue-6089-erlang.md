# Erlang review — issue #6089 (docs-only RET-06 reconciliation)

Source: `mkd-code-review analyze --pack percussion --format markdown --gate advisory
--git-base origin/main --models .../models.ollama-dev-coder.toml`
Branch: `fix/issue-6089-reconcile-1372-ret-06` (2 spec-markdown files, no production code)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Reviewer note (human-readable)

Docs-only reconciliation slice for #6089: `capability-matrix.md` RET-06
Todo → Done plus two stale rows in `removal-inventory.md` (parent #1372
status, UAT #1371 status). All factual claims re-verified on the branch tip
before commit:

- `WebUI/src/main/webapp/ui/publishing/` and `ui/pubruntime/` each ship only
  `index.jsp` (entry redirect); deep faces JSPs deleted via #1819/#1818.
- `publishing-faces-config.xml` referenced only inside the intentionally
  retained commented-out `jakarta.faces.CONFIG_FILES` block in
  `cm/WEB-INF/web.xml` (shared JSF-bootstrap cleanup out of scope per #1820).
- T124 checked; inventory RET-06 Done; #1371 CLOSED by maintainer
  2026-11-08T03:21:08Z; #1372 left OPEN for host close decision (no manual
  close per hard gate).
