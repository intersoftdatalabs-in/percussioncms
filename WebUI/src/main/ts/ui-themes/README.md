# WebUI Themes (`ui-themes`)

The `ui-themes` folder holds the **branded theming layer** for the modern
React UI of Percussion CMS.

The product is still **Percussion CMS**, but a particular distribution
(the Intersoft Data Labs distribution) carries Intersoft brand alignment
in the chrome, colors, and typography so the product feels native to
that publisher.

The default theme is `intersoft`. New themes can be registered without
touching the components that consume them.

## Layout

```
ui-themes/
├── index.ts                          # Public surface + ThemeRegistry
├── types.ts                          # Theme / Brand / Token TS interfaces
├── ThemeProvider.tsx                 # React context + CSS-var injection
├── components/
│   ├── index.ts
│   ├── Branding.tsx                  # <BrandBar />, <BrandFooter />
│   └── Branding.module.css
├── intersoft/                        # Default Intersoft Data Labs theme
│   ├── intersoftTheme.ts             # Tokens (colors, type, spacing, brand)
│   └── __tests__/intersoftTheme.test.ts
└── __tests__/                        # Registry + provider + branding tests
    ├── registry.test.ts
    └── ThemeProvider.test.tsx
```

Brand assets (logo PNG, mark PNG) are served from the legacy webapp
tree so any JSP that uses the same paths keeps working:

```
WebUI/src/main/webapp/cm/themes/intersoft/brand/
├── intersoft-logo-horizontal.png
└── intersoft-mark.png
```

## Usage

```tsx
import {
  ThemeProvider,
  BrandBar,
  BrandFooter,
} from "@/ui-themes/components";

export function HomeShell() {
  return (
    <ThemeProvider>
      <BrandBar />
      <main>...</main>
      <BrandFooter />
    </ThemeProvider>
  );
}
```

Inside a child component, read tokens via the hook:

```tsx
import { useTheme } from "@/ui-themes/components";

function Header() {
  const theme = useTheme();
  return (
    <h1 style={{ color: theme.colors.semantic.primary }}>
      {theme.brand.productName}
    </h1>
  );
}
```

Or, when you need a one-off inline style in a deeply nested component
without a provider, the active theme is resolved by the registry helper:

```tsx
import { getActiveTheme } from "@/ui-themes";
const accent = getActiveTheme().colors.semantic.accent;
```

## Overriding the theme

Set `window.PERC_THEME_ID = "<id>"` before the modern bundle mounts to
switch themes. Unknown ids fall back to the default theme (no error).

## Adding a new theme

1. Create a new folder `ui-themes/<id>/` with a `<id>Theme.ts` exporting
   a `Theme` object that satisfies the contract in `types.ts`.
2. Register it in `ui-themes/index.ts` (add it to the `REGISTRY` map).
3. Add tests under `ui-themes/__tests__/` or a sibling `__tests__/`.
4. Run `npm run test` from `WebUI/src/main/frontend`.

## Brand alignment policy

- The product wordmark is **Percussion CMS** (do not change the product
  name from any theme).
- Theme tokens may set the **publisher** (Intersoft Data Labs, the
  Rhythmyx team, a customer white-label, etc.) and the colors, fonts,
  and chrome style.
- The current palette is sampled from `https://intsof.com/` (sampled
  2026-07): navy `#0b224a`, steel blue `#4a6aa3`, amber accent
  `#fbb03b`. Update the `version` field in `intersoftTheme.ts` when
  bumping tokens.

## Cross-platform notes

- The brand assets are checked-in PNGs (no runtime path manipulation).
- All theme code is plain TypeScript with no Node-only dependencies, so
  it works in browser, Node tests, and Jetty the same way.
- CSS-module styles use `var(--token, fallback)` so a JSP host that
  has not yet loaded the React bundle degrades to sensible inline
  fallbacks rather than an unstyled flash.

