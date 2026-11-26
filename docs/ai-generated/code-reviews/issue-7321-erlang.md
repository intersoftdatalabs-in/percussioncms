# Erlang pre-push review — issue #7321

Slice: adjudicate `paths.hardcoded_sep` in `getParentPath` (`WebUI/.../perc_site_map.js`) as CMS logical section-path assembly.

## Summary

Machine analysis found **1** finding(s) (1 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (1)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `WebUI/src/main/webapp/cm/widgets/perc_site_map.js:2335` -- *Justification*: : adjudicated intentional (issue #7321, same as #5897/#5898/#5910/#5924/#5927/#6701/#7270/#7271) — slash joins CMS logical section-path titles, not an OS path. (via in_source_mkd)

</details>

### Codex review

The diff adds suppression comments only, preserving executable behavior in both copies. The logical-path justification matches the code, and both JavaScript files pass Node syntax checks.
