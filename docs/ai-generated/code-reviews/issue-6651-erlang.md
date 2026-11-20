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

## Combined with #6847

This PR folds in the overlapping #6651 work from #6847 (mayank-intsof):

- Product docs: #6847's wording for the **Checked out by**, **Check Out**,
  **Check In**, and **Force Check-in** rows (the Check Out row no longer
  says "same kind rules as Publish Now").
- Vitest: #6847's dispatch cases (single Check In via the editor
  endpoint, single Force Check-in, multi-select with folders skipped),
  the hidden-predicate checks, and the `CheckoutOwnerPanel` a11y case,
  merged into `genericCheckoutActions.test.tsx` (15 tests, one file).
- Playwright: one spec, `explorer-generic-checkout.spec.js`, using
  #6847's real custom-type seed (`rffFile`/`percFile`), owner-panel
  check, live-proof screenshot, and separate force check-in case, plus
  the kind-"none" shell assertion from #6840.
- Live proof: `docs/ai-generated/proofs/issue-6651-checkout-generic.png`.

Left out on purpose: the `qa_preflight.py` provenance fix (tracked as its
own PR under #6829) and the `IPSDashboardDataRepository` package line
(already on main via #6836).
