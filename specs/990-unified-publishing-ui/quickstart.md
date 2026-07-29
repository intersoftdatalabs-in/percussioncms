# Quickstart Validation: Unified Publishing UI

**Feature**: `990-unified-publishing-ui`  
**Purpose**: Runnable validation scenarios for implementers and UAT—not a full test suite.

## Prerequisites

- Branch `990-unified-publishing-ui` (or stacked PR branch) built with JDK 21: `./mvnw` / `mvnw.cmd`
- Running CMS with at least one site and one publish server (Local file is enough for smoke)
- User with publish rights; separate admin for design if roles differ
- Modern UI bundle built (WebUI frontend / Maven module that produces `/cm/modern/`)

## Build / unit checks (dev machine)

```bash
# From repo root — adjust module goals to project norms
./mvnw -pl WebUI -am test -Dtest=none   # or WebUI frontend test goal
# TypeScript unit tests (typical)
cd WebUI && npm test -- --run src/test/ts/publishing   # path when tests exist
```

Cross-platform: same flows on Windows with `mvnw.cmd` and `npm test`.

## Scenario A — Ops full publish (US1–US2)

1. Sign in → open **Publish** from main nav.
2. **Expect**: modern Publishing shell (after ops cutover), site list.
3. Filter sites; open a site; confirm servers list.
4. Run **Full publish** on Local (or configured) server.
5. Open **Status** → job appears with progress.
6. When complete, open **Logs** → find job → open details.
7. **Pass**: SC-001 path completable; no classic Minuet-only requirement after cutover.

## Scenario B — Incremental preview (US1)

1. Ensure incremental queue has items (edit/approve content per product norms).
2. Open site → Incremental preview.
3. **Expect**: queue and related items or empty state message.
4. Run incremental publish when items exist.
5. **Pass**: job starts; empty queue does not hard-crash UI.

## Scenario C — Server config (US3)

1. Add a new Local file server; save; refresh list.
2. Edit FTP/S3/DB fields on a non-prod test server if available.
3. Invalid save (blank required field) → blocked with message.
4. **Pass**: server persists across reload; secrets not shown in browser console errors.

## Scenario D — Design smoke (US4, after façade)

1. Open **Design** section.
2. Create or edit a content list; associate with an edition; save.
3. Open context → location scheme; save.
4. **Pass**: objects reload after navigation; classic Design JSF not required.

## Scenario E — Runtime edition (US5)

1. Open **Runtime / Editions**.
2. Start an edition; observe status; stop if long-running.
3. Demand publish if edition supports it.
4. **Pass**: outcomes match prior Runtime JSF for same edition.

## Scenario F — Item publish regression (US6)

1. From finder/editor, Publish Now on a page.
2. Takedown or stage if environment supports.
3. Open publishing history.
4. **Pass**: no regression vs pre-feature behavior.

## Scenario G — Deep links (US8)

1. Hit classic `view=publish` URL → modern shell.
2. Hit sample `/ui/publishing/` URL after design cutover → design section or moved message.
3. **Pass**: FR-014.

## UAT sign-off artifacts

- [ ] Capability matrix rows for milestone marked Done (`contracts/capability-matrix.md`)
- [ ] Removal inventory for retired surface (`checklists/removal-inventory.md`)
- [ ] i18n key checklist for new strings (`checklists/i18n-key-checklist.md`)
- [ ] Story PR tests green on CI

## References

- [spec.md](./spec.md)
- [plan.md](./plan.md)
- [research/inventory.md](./research/inventory.md)
- [contracts/ops-publish-api.md](./contracts/ops-publish-api.md)
- [contracts/design-runtime-api.md](./contracts/design-runtime-api.md)

