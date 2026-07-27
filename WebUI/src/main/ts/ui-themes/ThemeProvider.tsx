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
 * React provider that injects the active theme as CSS custom properties on
 * a wrapper element, and exposes the {@link Theme} object to descendants
 * via {@link useTheme}.
 *
 * <p>The CSS custom properties follow the {@code --color-}, {@code --font-},
 * and {@code --space-} prefixes (see {@link Theme.toCssVariables}).</p>
 *
 * <p>Reactivity: when no {@link ThemeProviderProps.theme theme} prop is
 * supplied, the provider re-resolves the active theme on every render so
 * runtime changes to {@code window.PERC_THEME_ID} take effect on the next
 * render, matching the behavior of {@link useTheme} used outside a
 * provider. (Resolution is an O(1) registry lookup; the small CSS-var
 * rebuild is intentional and far cheaper than maintaining a stale context.)</p>
 */

import React, { createContext, useContext, type ReactNode } from "react";
import type { CSSProperties, HTMLAttributes } from "react";
import { getActiveTheme } from "./index";
import type { Theme } from "./types";

const ThemeContext = createContext<Theme | null>(null);

export interface ThemeProviderProps extends Omit<HTMLAttributes<HTMLElement>, "children"> {
  /** Override the auto-detected theme. Useful in tests and previews. */
  theme?: Theme;
  /** Element used to render the CSS-var scope. Defaults to {@code <div>}. */
  as?: keyof React.JSX.IntrinsicElements;
  /** Extra class names appended to the scope element. */
  className?: string;
  children?: ReactNode;
}

export function ThemeProvider({
  theme,
  as = "div",
  className,
  children,
  style: extraStyle,
  ...rest
}: ThemeProviderProps): React.ReactElement {
  // Re-resolve each render so a runtime change to window.PERC_THEME_ID
  // is picked up immediately, the same way useTheme() does outside a
  // provider. Do NOT wrap in useMemo([theme]) — the missing dep on the
  // window override is the inconsistency the fall-back path already
  // handles. See Erlang review comment 2026-07.
  const resolved: Theme = theme ?? getActiveTheme();
  const cssVars: CSSProperties = resolved.toCssVariables() as CSSProperties;
  const mergedStyle: CSSProperties = extraStyle
    ? ({ ...(cssVars as object), ...(extraStyle as object) } as CSSProperties)
    : cssVars;
  const Component = as as React.ElementType;
  return React.createElement(
    ThemeContext.Provider,
    { value: resolved },
    React.createElement(Component, {
      ...rest,
      className,
      style: mergedStyle,
      "data-perc-theme": resolved.meta.id,
    }, children),
  );
}

/** Returns the theme provided by the nearest {@link ThemeProvider}. */
export function useTheme(): Theme {
  const t = useContext(ThemeContext);
  if (!t) {
    // Fall back to the active theme when no provider is in the tree.
    // This lets individual components (e.g. Dashboard widget used outside
    // the modern shell) still pick up branding without an explicit wrap.
    return getActiveTheme();
  }
  return t;
}
