/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Intersoft Data Labs distribution of the Percussion CMS modern UI theme.
 *
 * <p>This is the default theme for the modern React UI. The visual language
 * is the <a href="https://intsof.com/">Intersoft Data Labs</a> brand
 * (intersoft.com): deep navy, steel blue, and the amber accent used on
 * the marketing site, paired with the Percussion CMS product wordmark.</p>
 *
 * <p>Token values are derived from the live public site (sampled 2026-07):
 * primary navy {@code #0b224a} / steel blue {@code #4a6aa3} / amber accent
 * {@code #fbb03b}. The legacy teal palette is preserved as a fallback
 * within the brand scale so existing components that referenced it can
 * keep working during the migration.</p>
 */

import type {
  BrandAssets,
  ColorScale,
  SemanticColors,
  SpacingTokens,
  Theme,
  ThemeMetadata,
  TypographyTokens,
} from "../types";

const META: ThemeMetadata = {
  id: "intersoft",
  label: "Intersoft Data Labs",
  version: "1.0.0",
  isDefault: true,
  description:
    "Intersoft Data Labs distribution theme for Percussion CMS. " +
    "Sampled from the intsof.com marketing site (deep navy + steel blue " +
    "with an amber accent).",
};

const BRAND: BrandAssets = {
  publisher: "Intersoft Data Labs",
  productName: "Percussion CMS",
  tagline: "Intelligent \u2022 Innovative \u2022 Imaginative",
  publisherUrl: "https://intsof.com/",
  // Served from the webapp at /cm/themes/intersoft/brand/ (see WebUI/AGENTS.md
  // "Build Outputs & WAR Packaging" — paths inside the WAR are root-relative).
  logoHorizontal: "/cm/themes/intersoft/brand/intersoft-logo-horizontal.png",
  logoMark: "/cm/themes/intersoft/brand/intersoft-mark.png",
};

/**
 * Steel-blue / navy scale derived from the Intersoft brand. The 500 stop
 * matches the wordmark color used on the public site.
 */
const BRAND_COLORS: ColorScale = {
  50: "#eef1f7",
  100: "#d5dce9",
  200: "#aab9d2",
  300: "#7e95bb",
  400: "#5a78a8",
  500: "#4a6aa3",
  600: "#3c5789",
  700: "#2f456e",
  800: "#223454",
  900: "#0b224a",
};

const SEMANTIC: SemanticColors = {
  primary: BRAND_COLORS[500],
  primaryHover: BRAND_COLORS[700],
  primaryTint: BRAND_COLORS[50],
  accent: "#fbb03b",
  accentHover: "#e09a1f",
  text: "#181c2c",
  textMuted: "#5b6478",
  textInverse: "#ffffff",
  surface: "#ffffff",
  surfaceAlt: "#f5f7fb",
  border: "#d8dee9",
  danger: "#d63637",
  success: "#1f8a4c",
  warning: "#e09a1f",
  info: "#1dc2ef",
};

const TYPOGRAPHY: TypographyTokens = {
  // Inter is bundled with most CMS shells; fall back to system-ui.
  fontFamily:
    "'Inter', 'Rubik', 'Open Sans', -apple-system, BlinkMacSystemFont, " +
    "'Segoe UI', Roboto, system-ui, sans-serif",
  fontFamilyHeading:
    "'Rubik', 'Inter', 'Open Sans', -apple-system, BlinkMacSystemFont, " +
    "'Segoe UI', Roboto, system-ui, sans-serif",
  fontSizeBase: 14,
  fontSizeSm: 12,
  fontSizeLg: 16,
  fontSizeXl: 18,
  fontSize2xl: 22,
  fontSize3xl: 28,
  fontWeightNormal: 400,
  fontWeightMedium: 500,
  fontWeightSemibold: 600,
  fontWeightBold: 700,
  lineHeightBase: 1.5,
  lineHeightTight: 1.25,
};

const SPACING: SpacingTokens = {
  scale: [0, 4, 8, 12, 16, 20, 24, 32, 40, 56, 72],
  radii: { none: 0, sm: 2, md: 4, lg: 8, pill: 999 },
  shadows: {
    none: "none",
    sm: "0 1px 2px rgba(11, 34, 74, 0.08)",
    md: "0 2px 6px rgba(11, 34, 74, 0.12)",
    lg: "0 6px 20px rgba(11, 34, 74, 0.18)",
  },
};

/** Convert a dotted token path to a `--kebab-case` CSS variable name. */
function toVarName(path: string): string {
  return (
    "--" +
    path
      .replace(/\./g, "-")
      .replace(/[A-Z]/g, (m) => "-" + m.toLowerCase())
  );
}

function flatten(obj: unknown, prefix: string, out: Record<string, string>): void {
  if (obj === null || typeof obj !== "object") return;
  if (Array.isArray(obj)) {
    // Recurse into arrays so consumers can flatten indexed spacing/radius
    // scales (e.g. spacing.scale -> --space-scale-0 .. --space-scale-N).
    for (let i = 0; i < obj.length; i++) {
      const next = `${prefix}.${i}`;
      const v = obj[i];
      if (v !== null && typeof v === "object" && !Array.isArray(v)) {
        flatten(v, next, out);
      } else if (typeof v === "number" || typeof v === "string") {
        out[toVarName(next)] = String(v);
      }
    }
    return;
  }
  for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
    const next = prefix ? `${prefix}.${k}` : k;
    if (v !== null && typeof v === "object") {
      flatten(v, next, out);
    } else if (typeof v === "number" || typeof v === "string") {
      out[toVarName(next)] = String(v);
    }
  }
}

export const intersoftTheme: Theme = {
  meta: META,
  brand: BRAND,
  colors: { brand: BRAND_COLORS, semantic: SEMANTIC },
  typography: TYPOGRAPHY,
  spacing: SPACING,
  toCssVariables(): Readonly<Record<string, string>> {
    const out: Record<string, string> = {};
    flatten(this.colors.brand, "color.brand", out);
    flatten(this.colors.semantic, "color", out);
    flatten(this.typography, "font", out);
    flatten(this.spacing, "space", out);
    // Brand metadata for use in branded chrome
    out["--brand-publisher"] = this.brand.publisher;
    out["--brand-product"] = this.brand.productName;
    out["--brand-tagline"] = this.brand.tagline ?? "";
    out["--brand-logo-horizontal"] = `url('${this.brand.logoHorizontal}')`;
    out["--brand-logo-mark"] = `url('${this.brand.logoMark}')`;
    return out;
  },
};

// Deep-freeze the theme and every nested object/array so accidental mutation
// in a component throws a TypeError instead of silently corrupting tokens
// that are shared across the entire React tree. (See Theme contract: tokens
// are immutable.)
function deepFreeze<T>(value: T): T {
  if (value === null || typeof value !== "object") return value;
  if (Object.isFrozen(value)) return value;
  Object.freeze(value);
  for (const v of Object.values(value as Record<string, unknown>)) {
    deepFreeze(v);
  }
  return value;
}

deepFreeze(intersoftTheme);
