# Erlang review — #4579 PublishingShell publish now

## Verdict

Pass for this slice. Reuses `publishSelectedItem` / `itemPublishPaths` (no second REST contract). Confirm then GET `publish/page|{resource}/{id}`. HTTP 400/403/404 and application-level FORBIDDEN/BADCONFIG map to panel errors. Workspace jobs refresh via `onPublished` → `refreshJobs`. Playwright + product-docs companions included. No public Java API shape change. CMS/URL paths use `/`.
