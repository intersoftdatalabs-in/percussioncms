# Erlang review — fix/home-bookmarks-ui

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 |
| **Branch** | `fix/home-bookmarks-ui` |
| **Scope** | WebUI Home SPA bookmarks add/remove + list polish |
| **Base** | `origin/development` (post #1569) |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

Wires classic My Pages REST (`addtomypages` PUT, `removefrommypages` DELETE, `mycontent` GET, `ismypage` GET) into the React Home shell. Bookmarks section can remove favorites; Recent / Search / Library can toggle add/remove. Shared list row + styles polish empty states, meta (path/status), and action buttons. Unit tests cover API helpers, remove-from-bookmarks, and Recent toggle. Live smoke on native CMS confirmed add → mycontent populated → remove → empty; `ismypage` requires `Accept: text/plain` (handled in client).

## Cross-platform path checklist

N/A — no filesystem path construction. REST URLs use encodeURIComponent; CMS paths displayed as opaque strings only.

## Issues

None (bugs). No missing behavioral tests for new non-trivial logic.

### Suggestions (non-blocking)

1. **Retry on Bookmarks load error** uses a plain button without primary styling — minor polish only.
2. **act() warnings** in HomeShell tests from concurrent Recent + useBookmarks fetches — pre-existing pattern; not a product defect.
3. **Deploy note**: modern assets must be copied to `/opt/Percussion/.../cm/modern` for live UI (agent did this for smoke).

## Verification

- `cd WebUI && npm test -- --run` (homeApi, BookmarksSection, RecentSection.bookmarks, HomeShell, App) — pass
- `cd WebUI && ../mvn-env.sh clean install` — BUILD SUCCESS
- Live REST: add/remove My Pages for `HomeSmokeB66513` — pass

## Memory patterns hit

- Prefer real REST contracts from classic CUI (PercPageService) rather than inventing endpoints
- Partial `vi.mock` of homeApi must spread `importOriginal` so helpers (`isBookmarkableItem`) remain defined
