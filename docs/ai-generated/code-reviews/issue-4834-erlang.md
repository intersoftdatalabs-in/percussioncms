# Erlang review — Explorer non-Inbox custom URL view (#4834)

Reviewed the Playwright spec, `explorer-custom-url-view` helper, `node:test` unit file, and the `product-docs/8.2/admin/content-explorer.md` hunk. No other files were in scope.

## Prior BLOCK re-check

| Item | Result |
|---|---|
| `waitForResponse` exact URL | Met. Listener is armed before the leaf click, requires `POST`, and delegates to `isNamedViewExecute`. The regex is `/services/views/${escapedName}/execute(?:\?|$)` after `decodeURIComponent`. `Outbox` matches `.../views/Outbox/execute` and `.../execute?x=1`. It does not match `.../views/notoutbox/execute` or `.../views/OutboxExtra/execute` (the next character after the name must be `/execute`, then `?` or end). A malformed `%` throws and returns false. |
| STUB absent on results panel | Met. `STUB = /Custom URL views cannot be run/i` is asserted with `explorer-view-results` `not.toContainText` after that panel is visible. |
| Folder list replaced | Met. After results are visible and `explorer-view-results-loading` is gone, `detail-col-header-icon` and `detail-list-empty` must be count 0. The 2xx branch then requires exactly one of `explorer-view-results-list` or `explorer-view-results-empty`. The non-2xx branch requires `explorer-view-results-error` to show `HTTP ${status}` and both list and empty to be count 0. |
| Unit tests for unwrap and exact URL | Met. Unwrap covers `ViewDefList.ViewDef` and a bare array. Pick prefers `Outbox` and skips Inbox. URL cases cover exact hit, `notoutbox` rejection, query string, and `%` decode failure. |

`GET /services/views` is a live catalog read. The spec does not stub it. `pickNonInboxCustomView` prefers Outbox, then any other non-Inbox `customView === true` with a non-empty name.

## Other checks

- Copyright on the three new sources is `Copyright (c) 2026 Intersoft Data Labs, Inc.` with the Apache 2.0 block.
- URL matching uses `/` for the HTTP path. No filesystem path construction.
- `escapeRegExp` covers regex metacharacters in the view name. The regexp is built per call, so `lastIndex` does not leak.
- `unwrapViewDefs` treats a single nested `ViewDef` object as a one-element list and a top-level array as already unwrapped. Null, non-objects, and missing wrappers become `[]`.
- Product doc states the same contract: selecting Outbox, Recent, or another non-Inbox custom URL view on `spa.jsp?entry=explorer` replaces the folder list with rows, an empty state, or an HTTP status error, and the “cannot be run” message is gone. Frontmatter `id` is unchanged.

## Findings

None.

Residual (non-gating): the negative URL sample is `notoutbox`, which already fails a prefix pattern. A longer sibling (`OutboxExtra`) and a trailing `/execute/extra` are rejected by the implementation and are not separate assertions. Behavior under the current regex is correct.

Gate: PASS
May commit/push: yes
