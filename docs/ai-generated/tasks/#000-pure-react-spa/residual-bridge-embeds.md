# Residual PercModernUI bridge embeds (PR-6)

Primary product navigation for modern features is the **SPA** (`spa.jsp?entry=…`).
`window.PercModernUI.mount` remains only for **legacy full-page JSP hosts** that still
embed React islands until those pages are rewritten or removed (PR-8).

## Product SPA (no bridge)

| Surface | Entry |
|---------|--------|
| Login | `rxlogin.jsp` → `#perc-login-root` |
| Home / Publish / Workflow / Admin / WB / Explorer | `spa.jsp?entry=…` |
| Retired `*Modern.jsp` product hosts | 302 into SPA / dispatcher |

## Still using the bridge (residual)

These hosts load `/cm/modern/assets/perc-modern-ui.js` and call `PercModernUI.mount`
(component names must stay registered in `registry.ts`):

| Host (cm/app, often mirrored under cm/pages/app) | Component(s) | Why residual |
|--------------------------------------------------|--------------|--------------|
| `admin.jsp` (Design legacy) | `ContentExplorerShell` | Full-page Design exit |
| `adminWorkflow.jsp` | `ContentExplorerShell` | Classic workflow page embeds |
| `editAsset.jsp` / `editTemplate.jsp` | `ContentExplorerShell` | Editor-adjacent trees |
| `users.jsp` | `ContentExplorerShell` | Legacy users UI finder |
| `assetPickerModern.jsp` / `pagePickerModern.jsp` / `folderPickerModern.jsp` | `ContentBrowser` | Dialog pickers |
| `folderSecurityModern.jsp` | `FolderSecurityPanel` | Dialog |
| `searchModern.jsp` | `SearchPanel` | Dialog / side panel |
| `actionMenuModern.jsp` | `ActionToolbar`, `ContextMenu` | Menu chrome |
| `us7AdvancedModern.jsp` | Clipboard / wizards / dependency / relationships | Advanced tools pack |

Do **not** reintroduce product feature navigation through these hosts. Prefer SPA
routes for anything users open from TopNav.

## CSS contract

Bridge embeds and SPA/login all depend on:

- Stable stylesheet: `/cm/modern/assets/perc-modern-ui.css` (`cssCodeSplit: false`)
- Entry JS injects the link via `ensureModernStyles()` if the host omitted it
