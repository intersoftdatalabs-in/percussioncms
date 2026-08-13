# Erlang review — #3326 Explorer sample-site expand

**Scope:** uncommitted `fix/issue-3326-explorer-site-expand` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** empty list vs error envelope; site name vs folder root bind; installer seed companions

## Summary

Sample sites listed under Explorer `/Sites` but expand showed LIST_EMPTY because `installSampleSites` only created empty FOLDER_ROOT folders (350/351). Finder path uses `SITENAME` (`Corporate_Investments`) while repository folder is `//Sites/CorporateInvestments`.

Fix: seed Pages/Files children under 350/351; bind tree/list loads to `PathItem.folderPath`; cycle-only tree filter so name vs folder-root children are not dropped.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Cross-platform path checklist

- CMS finder paths use `/` (URL-style), not OS file separators.
- New helpers normalize `\` / drive letters only for caller paste; no filesystem I/O.
- Seed XML is installer data, not path concatenation.

## Tests

- Vitest: `resolveExplorerListPath`, `isSafeExplorerTreeChild`, ExplorerTree folderPath fetch
- InstallSampleSitesWiringTest: 350/351 child relationships
- Playwright surface: REST site children + UI expand (live C5 blocked — see PR)

## C5

`perc-devctl qa-up` used this worktree installer jar; sample sites echoed seeded, but Jetty context failed (`NoClassDefFoundError: PSVirtualSitePublishCopyResult` on `sitesAdaptor`). Unrelated SNAPSHOT mix; cell torn down.
