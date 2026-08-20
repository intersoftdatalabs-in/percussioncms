# Erlang review — PR #3621 review-thread follow-up (#3618)

**Verdict:** PASS (no hard-gate bugs)

**Scope:** `displayFormatMap.ts` GUID/id resolution; perc-qa paginatedFolder matcher; skip-helper removal.

## Hard gates

- Behavioral tests: `uuidFromPercussionGuidString` typed/untyped/reject; `isPaginatedFolderDisplayFormatRequest` exact query (`8` vs `80`/`18`); displayId `0` never becomes the option key.
- Cross-platform: query parse via `URL` / `URLSearchParams` (HTTP URLs, not filesystem paths).
- Change-class: Playwright helper + unit tests updated with production mapper; no new REST/public Java API.

## Notes (non-blocking)

- `displayId === 0` rejection is intentional (pathmanagement `Integer` rejects `0`); selector falls through to Guid uuid / name.
- `isNumericDisplayFormatId` is a re-export of `pathApi` (single implementation).

## Builds

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS
- `npm run test:unit` (perc-qa frontend) — 337 passed, 0 failed
