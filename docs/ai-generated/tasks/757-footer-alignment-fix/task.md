# Bug Fix #757 – Footer Text/Value Not Properly Aligned on Published Page

## Problem

When adding the following widgets to an index page template (e.g., Box or "L Left"):

1. **Title widget** in the title/header section
2. **Archive List**, **Categories**, and **Tags List** widgets in the Left sidebar section
3. **Rich Text** widget in the Footer section (e.g., "Copy rights(c) XXX")

…the published page shows the footer content appearing in the **wrong location** — visually mixed with or displaced below the sidebar widget content.

## Root Cause

The `vspan_X` CSS classes in `theme.css` (the published-page stylesheet) used **fixed `height`** values:

```css
.vspan_2 { height : 120px }
.vspan_4 { height : 240px }
.vspan_6 { height : 360px }
.vspan_8 { height : 480px }
```

These classes are applied to page region `<div>` elements in the template HTML (e.g., `<div id="leftsidebar" class="perc-region perc-vertical vspan_4 hspan_2">`).

In the page editor, fixed heights are desirable so empty placeholder regions are visibly sized. However, on a **published page** the actual widget content can exceed the fixed height. Because `overflow` defaults to `visible`, the content **bleeds beyond the region boundary** downward. Meanwhile, the footer `<div>` is positioned in the DOM immediately after the sidebar section's layout height — not after the visual overflow extent.

**Net effect:** Three widgets (Archive List + Categories + Tags) in a 240 px (vspan_4) or 360 px (vspan_6) sidebar produce far more than that in rendered content. The sidebar content overflows visually into the area occupied by the footer, making the footer text appear misaligned or displaced.

## Fix

Changed all `vspan_X` rules in `theme.css` from fixed `height` to `min-height`:

```css
.vspan_2 { min-height : 120px }
.vspan_4 { min-height : 240px }
.vspan_6 { min-height : 360px }
.vspan_8 { min-height : 480px }
```

With `min-height`, a sidebar region will:
- Be *at least* the specified height (preserving layout aesthetics when content is sparse), and
- **Expand** to accommodate actual widget output when content exceeds that height.

The `clear-float` div inside the `perc-horizontal` wrapper then correctly computes the expanded height as the parent's height, which in turn pushes the footer `<div>` to appear **below** the sidebar content — its intended location.

## CSS Cascade Impact on the Editor

In CSS, `min-height` overrides `height` regardless of declaration order. With `theme.css` specifying `min-height: Xpx` for `vspan_X` classes, those regions would expand past their editor-defined fixed heights when extra editor decorators/puffs were added, breaking the editor's visual alignment.

To fix this:
- We updated `WebUI/war/css/perc_decoration.css` to add `min-height: 0 !important` alongside the existing fixed `height: Xpx !important`.
- This correctly overrides the published-page `min-height` rules, forcing the editor regions to preserve their exact design heights.
- On published pages, where `perc_decoration.css` is not loaded, the regions can still grow dynamically to accommodate long content and keep the footer properly positioned.

## Files Changed

| File | Change |
|---|---|
| `system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css` | Changed all 4 occurrences of `vspan_X { height: Xpx }` to `vspan_X { min-height: Xpx }` |
| `WebUI/war/css/perc_decoration.css` | Added `min-height: 0 !important` and `!important` to the fixed `height` in `vspan_X` classes to restore editor styling |

## Testing

1. Create/use an index page with the Box or L-Left template.
2. Add Title widget to the header/title section.
3. Add Archive List, Categories, and Tags List widgets to the Left section.
4. Add a Rich Text widget to the Footer section with text such as "Copyright © 2024".
5. Save, approve, and publish the page.
6. **Expected:** Footer text appears at the bottom of the page, below all sidebar widget content.
7. **Previously:** Footer text appeared overlapping with sidebar widget content.
