# Erlang review — issue #1510 WebUI integrity seal / unused min prune

| Field | Value |
|-------|--------|
| **Scope** | Uncommitted changes on `development` (Option C + min prune) |
| **Date** | 2026-07-26 |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

The legacy WebUI builder no longer writes regenerable npm jquery mins into
`src/main/webapp/cm/` (integrity seal fix). Standalone copies go only to
`target/generated-webui/cm/`, which `maven-war-plugin` already overlays into the
WAR. Unused committed `*.min.js` / `*.min.css` (and junk `.tmp`/`.bak` files)
were pruned via reference analysis; only mins with direct JSP/HTML/bundle
references remain. Behavioral vitest coverage and a standalone
`WebUI` `clean install` (integrity: verified, 0 failed) support the change.

## Cross-platform path checklist

- [x] No new `".../" +` / `"...\\" +` filesystem joins — uses `path.join`
- [x] Tests normalize path segments with `path.sep` before string asserts
- [x] No Unix-only absolute roots or OS-specific temp hardcodes
- [x] Line-ending sensitive asserts not introduced
- [x] Required workflow remains Maven/`frontend-maven-plugin` (cross-platform)

**Outcome:** clean.

## Memory patterns hit

- Missing behavioral unit tests for new/changed non-trivial logic — **addressed**
  (sync destination + source-tree absence tests)
- Non-portable path joins / Unix-only path asserts — **not introduced**
- Multi-copy WebUI assets edited in only one of several lockstep paths —
  noted as residual (war vs webapp allowlist mirrors; installer still on `war/`
  non-min jquery) — **suggestion only**, not a gate for this fix

## Issues

### Bugs

None.

### Suggestions

1. **Residual dual trees** — `WebUI/war/` still mirrors allowlisted mins and
   non-min jquery used by `perc-distribution-tree`. Follow-up: re-point
   installer to `src/main/webapp` (or generated overlay) and delete the
   parallel tree.
2. **`cm/vendor/js/legacy/` non-min** — entire tree appears unreferenced by
   product paths; only mins were pruned. Larger deletion is a separate change.
3. **Stale `WebUI/scripts/build-legacy-bundles.js`** — documented and made safe
   for standalone mins; still not the Maven entry point. Prefer deletion or a
   one-line re-export of the frontend script in a later cleanup.

### Nits

- Vitest integration path soft-skips when `node_modules` jquery is absent
  (hermetic CI without frontend install). Structural “source must not contain
  regenerable mins” test always runs.

## Verification evidence (pre-PR)

- `cd WebUI/src/main/frontend && npm test -- src/test/js/buildLegacyBundles.test.js`
  → 8/8 passed
- `cd WebUI && ../mvnw clean install` → BUILD SUCCESS;
  `Hash verification complete: … verified, 0 failed`
- WAR contains overlay `cm/jslib/profiles/3x/jquery/jquery.min.js` (+ migrate)
  and allowlisted bootstrap/fancytree/api mins

## Recommendation

**approve** — no hard-gate bugs; behavioral tests and integrity gate green.
