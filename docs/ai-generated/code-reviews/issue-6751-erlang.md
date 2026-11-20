# Erlang pre-push review — issue #6751 (jquery-dropdown MIT notice)

Slice: WebUI jquery.dropdown vendored copy (parent #6734, grandparent audit #5627).
Branch: `fix/issue-6751-dropdown-mit-notice` vs `origin/main`.
Gate: advisory. Date: 2026-11-20.

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Notes (author)

- License-attribution archaeology (not inventable, verified 2026-11-20):
  upstream master file headers at claviska/jquery-dropdown (now
  soundasleep/jquery-dropdown) ship the same holder-less banner — the
  copyright lives in the upstream README ("Copyright A Beautiful Site, LLC",
  MIT licensed) and surviving fork READMEs ("Copyright 2013 Cory LaViska for
  A Beautiful Site, LLC."). Same-author MiniColors precedent in-tree:
  "Developed by Cory LaViska for A Beautiful Site, LLC".
- Change is comment-only (one banner line × 6 files across the three
  committed trees: `cm/jslib`, `cm/vendor/js/legacy`, `war`) plus one new
  pinning test in `buildLegacyBundles.test.js` (fails without the banner
  fix, passes with it). No runtime behavior change.
- Full frontend vitest suite: 977 files / 9706 tests green; WebUI
  `mvnw clean install`: BUILD SUCCESS, 83 tests, 0 failures/errors/skips.
