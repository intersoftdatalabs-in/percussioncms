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
 * Public type contract for UI themes used by the modern React layer.
 *
 * <p>A theme is a static, immutable collection of design tokens (colors,
 * typography, spacing, radii, shadows, breakpoints) plus a small amount of
 * metadata (id, label, distributor, product, version, optional logo
 * assets). Themes are intentionally framework-agnostic so they can be
 * projected onto CSS custom properties, inline {@code CSSProperties} objects,
 * Tailwind config, or SCSS variables.</p>
 *
 * <p>This module deliberately exposes plain TypeScript interfaces and not
 * a class hierarchy. The {@link ThemeRegistry} (see {@link ./index.ts})
 * handles the singleton lifecycle.</p>
 */

export type ColorScale = Readonly<{
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string;
  600: string;
  700: string;
  800: string;
  900: string;
}>;

export type SemanticColors = Readonly<{
  /** Primary brand color (used for primary buttons, active nav, links). */
  primary: string;
  /** Slightly darker hover/active variant of {@link SemanticColors.primary}. */
  primaryHover: string;
  /** Lighter surface tint of the primary (badges, selected rows). */
  primaryTint: string;
  /** Accent / CTA color (used for the brightest call-to-action, e.g. "Get started"). */
  accent: string;
  /** Hover variant of the accent color. */
  accentHover: string;
  /** Primary surface text color. */
  text: string;
  /** Muted text (placeholders, captions, helper text). */
  textMuted: string;
  /** Inverted text (e.g. text drawn on top of {@link SemanticColors.primary}). */
  textInverse: string;
  /** Default page / panel background. */
  surface: string;
  /** Subtle alternate surface (zebra rows, hover backgrounds). */
  surfaceAlt: string;
  /** Border / divider color. */
  border: string;
  /** Danger / destructive actions (iconic red; may be low-contrast on white for body text). */
  danger: string;
  /**
   * Darker danger text for body copy on light surfaces (axe AA contrast on white).
   * Prefer this over {@link SemanticColors.danger} for field/form error messages.
   */
  dangerText: string;
  /** Strong danger text for error cards / emphasis on light surfaces. */
  dangerStrong: string;
  /** Soft danger border for error cards. */
  dangerBorder: string;
  /** Soft danger surface fill for error cards. */
  dangerSurface: string;
  /** Success / positive states. */
  success: string;
  /** Warning / attention. */
  warning: string;
  /** Informational highlights. */
  info: string;
}>;

export type TypographyTokens = Readonly<{
  /** CSS font-family stack for body text. */
  fontFamily: string;
  /** CSS font-family stack for headings. */
  fontFamilyHeading: string;
  /** Base font size in pixels. */
  fontSizeBase: number;
  fontSizeSm: number;
  fontSizeLg: number;
  fontSizeXl: number;
  fontSize2xl: number;
  fontSize3xl: number;
  fontWeightNormal: number;
  fontWeightMedium: number;
  fontWeightSemibold: number;
  fontWeightBold: number;
  lineHeightBase: number;
  lineHeightTight: number;
}>;

export type SpacingTokens = Readonly<{
  /** 4-pt spacing scale (px). Index 0 = 0, index 1 = 4px, etc. */
  scale: readonly number[];
  /** Border radius scale (px). */
  radii: Readonly<{ none: number; sm: number; md: number; lg: number; pill: number }>;
  /** Shadow elevation tokens. */
  shadows: Readonly<{
    none: string;
    sm: string;
    md: string;
    lg: string;
  }>;
}>;

export type BrandAssets = Readonly<{
  /** Absolute or root-relative URL to the horizontal logo (wordmark + tagline). */
  logoHorizontal: string;
  /** Absolute or root-relative URL to a square mark / favicon. */
  logoMark: string;
  /** Display name of the distributor / publisher. */
  publisher: string;
  /** Optional tagline shown under the logo on the marketing-style header. */
  tagline?: string;
  /** Optional website URL the publisher logo links to. */
  publisherUrl?: string;
  /** Product name displayed alongside the publisher (e.g. "Percussion CMS"). */
  productName: string;
}>;

export type ThemeMetadata = Readonly<{
  /** Stable id used for lookup and persistence ("intersoft", "classic", ...). */
  id: string;
  /** Human-readable label shown in the theme switcher. */
  label: string;
  /** Theme version, semver-ish. Bump when tokens change. */
  version: string;
  /** True when this is the default theme at first paint. */
  isDefault: boolean;
  /** Optional description / rationale. */
  description?: string;
}>;

export type Theme = Readonly<{
  meta: ThemeMetadata;
  brand: BrandAssets;
  colors: Readonly<{
    brand: ColorScale;
    semantic: SemanticColors;
  }>;
  typography: TypographyTokens;
  spacing: SpacingTokens;
  /**
   * Returns a flat map of CSS custom property name -> value ready to be
   * applied to a DOM element's {@code style} attribute (e.g. by the
   * {@link ThemeProvider}). Property names are prefixed with {@code --}.
   */
  toCssVariables: () => Readonly<Record<string, string>>;
}>;
