# Contract: Content Browser host integration (US2)

**Component name (registry)**: `ContentBrowser` (proposed)  
**Mount**: `window.PercModernUI.mount(elementId, 'ContentBrowser', props)`  
**Also**: importable React component for pure React hosts (Home Library, future shells)

## Purpose

Embeddable navigate / search / locate UI that returns a **SelectionResult** to the host without miller-column Finder or Desktop CE.

## Props (host → browser)

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| mode | `'select' \| 'browse'` | `'select'` | select requires confirm; browse may omit confirm |
| multiSelect | boolean | `false` | Allow multiple items |
| allowFolderSelect | boolean | `true` | Folders selectable |
| allowItemSelect | boolean | `true` | Items selectable |
| allowedTypes | string[] \| null | `null` | If set, only these type/category values confirmable |
| allowedCategories | string[] \| null | `null` | Optional category filter |
| initialPath | string \| null | product root | Starting folder path |
| roots | `'sites' \| 'assets' \| 'all' \| string[]` | `'all'` | Root set (align with path services) |
| enableSearch | boolean | `true` | Show search when API available |
| title | string \| null | TMX default | Dialog title |
| onConfirm | `(selection: SelectionResult) => void` | required in select mode | Host receives selection |
| onCancel | `() => void` | optional | User cancelled |
| onError | `(message: string) => void` | optional | Load/action errors |

## SelectionResult (browser → host)

```ts
interface SelectionItem {
  id: string;
  path: string;
  name?: string;
  type?: string;
  category?: string;
}

interface SelectionResult {
  items: SelectionItem[]; // length 1 if !multiSelect
}
```

## Behavioral rules

1. **Same visibility rules** as Content Explorer for the same session (FR-009).
2. Confirm **disabled** when selection empty or fails filters (US2).
3. Multi-select returns full set; single-select returns one-element `items`.
4. Independent selection state from main explorer instance (edge case).
5. CSRF/session errors call `onError` or host-visible message; do not hang.
6. User-visible chrome strings via TMX keys.

## Hard-cut per host (FR-008a)

When a host is declared ready:

- Production MUST mount modern ContentBrowser (or React import)—**no** classic Finder miller picker and **no** Desktop CE for that host.
- Other hosts may remain on legacy until their phase.
- Inventory each host in [../checklists/cutover-inventory.md](../checklists/cutover-inventory.md).

## Pilot hosts (recommended order)

1. Low-risk internal dialog or modern Home Library (if 989 available).
2. Asset/page pickers in WebUI plugins that call Finder today.
3. AA / `ContentBrowserDialog.jsp` (coordinate with Track A Dojo work—prefer not to invest in Dojo browser if Track A/B can jump to React).

## Non-goals

- Full admin action menus inside the picker (optional later).
- Editing ACL inside the browser dialog.
- Replacing the full explorer workspace.
