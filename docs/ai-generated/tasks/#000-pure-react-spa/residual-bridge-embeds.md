# Residual PercModernUI bridge embeds (delete list)

**Policy (unified-ui-plan rev 4.0):** Primary product navigation is the **SPA**.  
`window.PercModernUI.mount` is **legacy debt**. **Do not add new product mounts.**  
Prefer SPA routes/dialogs; when a surface is accepted in React, **delete** its residual host.

## Product SPA (no bridge)

| Surface | Entry |
|---------|--------|
| Login | `rxlogin.jsp` → `#perc-login-root` |
| Home / Publish / Workflow / Admin / WB / Explorer | path or `spa.jsp?entry=…` |

## Still using the bridge (eliminate)

| Host (cm/app, often mirrored under cm/pages/app) | Component(s) | Elimination path |
|--------------------------------------------------|--------------|------------------|
| `admin.jsp` (Design legacy) | `ContentExplorerShell` | Design SPA or retire exit |
| `adminWorkflow.jsp` | `ContentExplorerShell` | SPA workflow already; remove classic page |
| `editAsset.jsp` / `editTemplate.jsp` | `ContentExplorerShell` | Editor SPA wave |
| `users.jsp` | `ContentExplorerShell` | SPA users / admin |
| `assetPickerModern.jsp` / `pagePickerModern.jsp` / `folderPickerModern.jsp` | `ContentBrowser` | SPA dialogs from openers |
| `folderSecurityModern.jsp` | `FolderSecurityPanel` | Explorer SPA actions |
| `searchModern.jsp` | `SearchPanel` | SPA search |
| `actionMenuModern.jsp` | `ActionToolbar`, `ContextMenu` | SPA explorer chrome |
| `us7AdvancedModern.jsp` | Clipboard / wizards / dependency / relationships | SPA advanced tools |

## CSS contract

- Stable stylesheet: `/cm/modern/assets/perc-modern-ui.css`
- Entry JS may inject via `ensureModernStyles()` if a residual host omitted the link
