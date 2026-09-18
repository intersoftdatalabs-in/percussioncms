# Erlang review — #4570 EditorHost new copy / promotable

- New copy / promotable reuse existing itemmanagement POSTs; 403/404 mapped to errors.
- Landing uses `parseExplorerContentId` so GUID `itemId` values become numeric `contentId`.
- Confirm cancel does not POST. View/Promote hide actions.
- Playwright + product-docs companions included. No REST signature changes.
- Cross-platform: URL paths use `/` only (REST).

Verdict: pass for commit.
