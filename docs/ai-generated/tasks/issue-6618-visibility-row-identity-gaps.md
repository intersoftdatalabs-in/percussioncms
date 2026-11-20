# Explorer visibility selection-row identity gaps (issue #6618)

Parent: #4530. Follow-on of #6517 (client-side Workbench visibility
evaluation, PR #6620). Deliverable is this documented audit — the gap
rules stay permissive (visible), exactly as before #6517, and no
rendered output changes in this slice.

## Source

- Server semantics: `system/src/main/java/com/percussion/cms/PSActionVisibilityChecker.java`
  (`checkObjectState` arms for contexts `1`, `3`, `4`, `6`, `9`, `10`,
  `11`; `checkGlobalState` arms for `2`, `5`, `7`, `8`). Every entry
  lists values that *hide* the action on match.
- Client evaluator: `WebUI/src/main/ts/contentExplorer/menuVisibility.ts`
  (`isMenuVisibleByVisibilityContexts` / `filterMenuActionsByVisibility`),
  audited in its own docblock. Constitution II (Evidence Over Invention):
  no invented fields — unknown state never hides.
- Shell state: `ContentExplorerShell#menuVisibilityState` supplies only
  `multiSelected`, `userName`, `userRoles` (bootstrap identities),
  `locale` (bootstrap locale), and `accessLevel` (selected-row band).
- Row DTO: `WebUI/src/main/ts/api/contentExplorer/types.ts#PSPathItem`
  carries `id`, `name`, `title`, `path`, display `type` (e.g. `percPage`),
  `category`, and `accessLevel` — no numeric ids, no checkout state, no
  community id.

The contract is pinned by
`WebUI/src/test/ts/contentExplorer/menuVisibility.test.ts`
(`selection-row identity gaps (#6618)`: all eight gap contexts stay
visible under the exact shell state; evaluated contexts still hide;
evaluator readiness for string-form ids).

## Verdicts

### Evaluated — the shell supplies the facts

| Server context | Client fact | Notes |
|---|---|---|
| Multi-select rule (`SupportsMultiSelect`) | Checked-row count | `isMenuVisibleByVisibilityContexts` hides directly, mirroring `isVisible` |
| `7` roles | Bootstrap role identities | Server hides when the listed values contain ALL user roles; client mirrors |
| `8` locales | Bootstrap locale | Case-insensitive match, mirroring `equalsIgnoreCase` |
| `11` folder security | Selected-row `accessLevel` band | Coarse rank approximation (`ADMIN > WRITE > READ > VIEW`); hide fires only strictly below the listed band |

### Permissive — facts not on `PSPathItem` rows

| Server context | Why unresolvable client-side | What would close it |
|---|---|---|
| `6` checkout status | Rows carry no checkout state (`CheckoutOwnerPanel` loads it per item via a separate editor-lock call, returning owner names — not the `Checked Out` / `Checked Out By Me` status tokens) | Expose the status token on path/paginated rows, a lightweight batch endpoint, or filter visibility server-side for the SPA entry |
| `3` content-type id | Rows carry display strings (`type: percPage`), not numeric ids | Same as above (evaluator already compares numerically and accepts string-form ids) |
| `4` object type | Same — display strings only | Same as above |
| `9` workflow id | Same — no workflow id on rows (dialog-local save results are not row identity) | Same as above |
| `2` community id | The SPA session exposes no community id | Publish the session community (or per-row community) to the shell, or server-side filtering |
| `1` assignment type | Not on rows | Same row/batch/server options |
| `5` client context | Never populated by the shell | Shell claims its context token, or server-side filtering |
| `10` publishable type | Never populated by the shell | Same row/batch/server options |

Per-mode `uiContexts` entries are a separate gap (#6619): the modern
shell cannot resolve Workbench mode ids, so they never hide a menu
client-side (already permissive by design and separately tracked).

## Operator-visible summary

Workbench-configured hide rules on multi-select support, roles, locale,
and folder-access band shape the Explorer toolbar and right-click menu
exactly as they shaped DCE. Rules on checkout status, content/object
type, workflow, community, assignment type, client context, and
publishable type stay permissive: the action remains visible, exactly as
before #6517. Nothing is silently dropped — the shell evaluates what it
knows and documents the rest. See
`product-docs/8.2/admin/content-explorer.md`
("DCE chrome model parity") for the operator-facing pointer.

## Gaps / residuals

No new issues filed (host sweep files them deduped; the options below
are logged in the PR body and the `work-6618.json` structured output):

1. Row-identity exposure: add checkout-status token + numeric
   content/object/workflow/community/assignment ids to path/paginated
   rows (or a lightweight batch endpoint), then populate
   `ExplorerMenuVisibilityState` from the selection. Evaluator needs no
   change (readiness is tested).
2. Alternative: filter visibility server-side for the SPA entry and let
   the client keep its permissive fallback.
3. Out of scope here by triage: `uiContexts` mode ids (#6619) and the
   deferred siblings named in the run brief.
