/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Public surface of the modern UI theme system.
 *
 * <p>Current themes:</p>
 * <ul>
 *   <li>{@link intersoftTheme} &mdash; Intersoft Data Labs distribution
 *       of the Percussion CMS UI. The default theme.</li>
 * </ul>
 *
 * <p>Consumers should import the registry helper or the concrete theme:</p>
 * <pre>
 *   import { getActiveTheme, intersoftTheme } from "@/ui-themes";
 *   const t = getActiveTheme();
 *   const accent = t.colors.semantic.accent;
 * </pre>
 */

import { intersoftTheme } from "./intersoft/intersoftTheme";
import type { Theme } from "./types";

export type { Theme, ThemeMetadata, BrandAssets, ColorScale, SemanticColors, TypographyTokens, SpacingTokens } from "./types";
export { intersoftTheme };

/** Built-in themes keyed by id. New themes register here. */
const REGISTRY: ReadonlyMap<string, Theme> = new Map<string, Theme>([
  [intersoftTheme.meta.id, intersoftTheme],
]);

/** Returns the theme registered under {@link id}, or `undefined`. */
export function getTheme(id: string): Theme | undefined {
  return REGISTRY.get(id);
}

/**
 * Returns the active theme. Resolution order:
 *   1. {@code window.PERC_THEME_ID} (set by ops / distribution override)
 *   2. The first theme flagged {@link ThemeMetadata.isDefault}
 *   3. The first registered theme
 */
export function getActiveTheme(): Theme {
  if (typeof window !== "undefined") {
    const override = (window as unknown as { PERC_THEME_ID?: string })
      .PERC_THEME_ID;
    if (override) {
      const t = REGISTRY.get(override);
      if (t) return t;
    }
  }
  for (const t of REGISTRY.values()) {
    if (t.meta.isDefault) return t;
  }
  return REGISTRY.values().next().value as Theme;
}

/** All registered theme ids in registration order. */
export function listThemeIds(): readonly string[] {
  return Array.from(REGISTRY.keys());
}
