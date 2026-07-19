# WebUI

This module contains the support for the User Interface for CMS.

## Building

mvn clean install

## Layout

Most ui elements are located under the war folder. When deployed they are placed into the main application war file under the cm folder.

war/


## Modern Publishing UI (feature 990)

Primary nav `view=publish` loads `cm/app/publishModern.jsp` (mirrored under `cm/pages/app`), mounting React `PublishingShell` via `PercModernUI`.

### Query parameters (allowlisted)

| Param | Purpose |
|-------|---------|
| `section` | `sites` (default), `status`, `logs`, `design`, `runtime` |
| `siteId` | Preselect site (safe charset) |
| `serverId` | Preselect server |

Classic Minuet `publish.jsp` and JSF `/ui/publishing`, `/ui/pubruntime` entries redirect to this shell.

Spec/plan/tasks: `specs/990-unified-publishing-ui/`.
