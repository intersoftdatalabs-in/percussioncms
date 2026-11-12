# Closeout — Assembly verification battery + Compare decision (996 / #6154)

**Issue**: #6154 · **Date**: 2026-11-12
**Inventory**: `checklists/coverage.md` (T003/T004) · **Tasks**: `../tasks.md`

## T011 — FR-007 related-content menu parity: stays with #6104

Not built here. `updaterelateditems.html` has no in-tree React/REST/sitemanage
implementation (see `coverage.md` T004), and #6104 (OPEN, p3) owns the
implement-or-recorded-reduced-scope decision. T011 box in `tasks.md` is
intentionally left unticked; #6104 was not closed by this work.

## T012 — FR-008 Compare decision: DEFER (not built)

- `sys_compare` appears only in the legacy-Data-Flow blocklist
  (`DATA_FLOW_PATH_MARKERS`, `actionDispatch.ts`); no React Compare surface exists.
- The only backend "compare" (`PSItemService.compareRevisions`) serves revision
  compare, a separate shipped surface — not assembly Compare.
- Rationale: the plan's rule is *verify before building*. WP1 verifies FR-001–006
  as shipped; Compare is an explicitly "later / small add-on" spec item with no
  acceptance criteria beyond "build or defer". Building a new Compare host without
  a requesting slice would violate the vertical-increment rule, so: **defer**.
- Revisit trigger: file a new issue referencing this note when a Compare consumer
  slice is scheduled. No residual filed — deferral *is* the recorded decision.

## T013 — perc-i18n TMX keys: N/A (fallback-only, by repo convention)

No new user-visible strings were added by this PR (one Playwright test + docs),
so there is nothing new to key. Standing state, verified 2026-11-12: all 56
`perc.ui.assembly@*` keys in `WebUI/src/main/ts/assembly/messages.ts` resolve via
the English fallback (`fallbackLabelFromKey`) because none are in any TMX catalog
— the same posture as 863 `perc.ui.explorer@*`, ~1700 `perc.ui.developer@*`, and
419 `perc.ui.editor@*` keys. TMX population is the locale-translation workflow,
not this verify-first slice.

## T010 — Gap fixes: none required beyond the battery addition

WP1 confirmed the FR-001–006 implementation already matches the spec; the single
battery gap found was T009 live coverage (denied 403 messaging had Vitest but no
Playwright proof). Fix = the new `@aa-denied` test itself. No production code changed.

## R15 — #6106 rich-toolbar ownership decision: OWNED BY 996, DEFERRED (not built)

**Issue**: #6106 (self-parented; no tracking epic — work rode in
#2400/#3471/#3475/#4530 slices) · **Date**: 2026-11-12

- **Shipped baseline**: scalar contenteditable overlay persists via
  itemmanagement (#3475 CLOSED); single-HTML-field save on the assembled page
  (#5296 CLOSED via PR #5307 MERGED).
- **Gap as filed**: rich-toolbar editing in AA has no owner; plan.md/tasks.md
  were missing at filing time (R15). **Plan/tasks pointer**: both now exist in
  this directory — `plan.md` + `tasks.md` (added via #6151), battery + Compare
  defer recorded via #6154/#6105 (see T012 above). That half of #6106 is
  satisfied by pointer, no new planning files needed.
- **Ownership decision**: rich-toolbar editing in AA is **owned by spec 996**,
  not #5622. #5622 (TipTap epic, OPEN) explicitly scopes out the AA overlay
  ("Active Assembly contenteditable overlay (#3475 / spec 996) except noting
  candidate fitness"). Verified 2026-11-12: no toolbar/TipTap/rich surface
  exists in `WebUI/src/main/ts/assembly/` and no open issue owns an AA
  toolbar/i18n/a11y slice (search: assembly/rich-toolbar — only #6106 itself).
- **Rationale for defer**: the plan's rule is *verify before building*. The
  shipped scalar + HTML-save baseline is verified; a rich toolbar has no
  requesting slice and no acceptance criteria beyond "decide and record", so
  building it now would violate the vertical-increment rule. **Defer to a
  future 996 slice** (toolbar + i18n + a11y for the AA overlay, reusing #5622's
  TipTap candidate fitness when scheduled).
- **Revisit trigger**: file a new 996 slice referencing this note when rich AA
  editing is scheduled. No residual filed — deferral *is* the recorded
  decision; umbrella #4530 (OPEN) remains the residual tracker per repo
  convention (umbrellas stay open).
- **Spec pointer**: `../spec.md` "In scope (later)" rich-overlay bullet now
  records this ownership + deferral.

## T014/T016 — Battery evidence

_Note on T016's "#6110 disposition": #6110 ([2400] Phase 3 disposition) is a
different epic's tracker, unrelated to 996 — no disposition needed here._

- Vitest (WebUI): `npx vitest run src/test/ts/assembly` — see PR body.
- Playwright QA cell (H2): `npm run test:surface -- --path tests/explorer-active-assembly.spec.js`
  (covers T006–T009 incl. the new `@aa-denied` test) — see PR body.
- Slot + field companions: `explorer-create-item-in-slot`, `explorer-move-relationship-slot`,
  `explorer-link-existing-slot`, `assembly-text-field` specs — unchanged, listed as
  regression context; surface run covers the assembly entry spec per the surface-filter gate.
