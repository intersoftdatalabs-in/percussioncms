# Erlang review — issue #5663 (jQuery BBQ 1.1 retirement)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

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

---

## Reviewer notes (work agent, pre-push)

Change: retire the dead-upstream jQuery BBQ v1.1 vendored copy
(`jquery-perc-retiredjs/jquery.bbq.js`, 3 trees) and serve its only live
surface (`$.deparam.querystring` reads, `$.param.querystring` merges) from
the first-party `jquery.deparam.js` shim. The unreferenced Mathias Bank
getUrlParam 2.1 copy (2006-2007, dead upstream, zero references — not even
loaded) was removed outright in the same slice.

Verification beyond the machine gate:

- Node + jsdom side-by-side parity harness (real BBQ 1.1 vs shim, 18
  cases incl. coercion, bracket keys, proto-pollution guard, merge modes
  0/1/2, hash preservation): PARITY PASS. Harness was throwaway
  (`/tmp/opencode/parity-bbq.cjs`, not committed); expectations are pinned
  in `WebUI/src/main/frontend/src/test/js/jqueryDeparam.test.js`
  (12 tests, green).
- Full frontend Vitest suite: 934 files / 8935 tests green.
- `buildLegacyBundles.test.js`: 67/67 green, incl. 3 new BBQ-retirement
  pins (3-tree removal, bundle/JSP reference swap, no BBQ-only callers).
- WebUI `mvnw clean install`: BUILD SUCCESS, 77 Java tests pass.
- C5 live proof (QA H2 cell): rebuilt legacy bundles deployed, Jetty
  restarted, `classic-deparam-shim.spec.js` Playwright surface green,
  `server.log` zero ERROR/FATAL.
