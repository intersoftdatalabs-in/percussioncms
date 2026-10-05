## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Reviewer note

Name-only `updateContext` omits description and default scheme. `PSPublishingDesignRestService.updateContext` writes those only when non-null, so a name-only body leaves both stored. Location schemes are loaded by context id, not by name, and the rename path does not create or move them. Blank and over-long names return before any PUT. Cancel, and HTTP 400, 403, and 409, do not replace the list name. Companions present: `contextRename.ts`, ContextsPanel Vitest, `designContextRename.spec.js`, `updateContext_nameOnly_keepsDescriptionDefaultSchemeAndSchemes`, and `product-docs/8.2/admin/publishing.md`. No new filesystem path joins. No agent rule files. The machine report has no findings. Recommendation: approve. May commit/push: yes.
