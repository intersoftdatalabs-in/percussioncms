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
 * Product CSS for the modern UI bundle.
 *
 * Vite extracts CSS modules to a separate file. Host JSPs load only the ES
 * module entry ({@code perc-modern-ui.js}); without an HTML entry Vite does not
 * auto-inject a {@code <link>} for entry CSS. Login therefore shipped unstyled
 * (and the Intersoft logo rendered at native 1477×720). This helper + stable
 * asset name + optional JSP {@code <link>} keep styles on every boot path.
 */
export const MODERN_UI_CSS_HREF = "/cm/modern/assets/perc-modern-ui.css";
export const MODERN_UI_CSS_ID = "perc-modern-ui-stylesheet";

/**
 * Ensure the modern UI stylesheet is present in {@code document.head}.
 * Safe to call multiple times (idempotent). No-ops when {@code document} is absent.
 */
export function ensureModernStyles(): void {
  if (typeof document === "undefined") {
    return;
  }
  if (document.getElementById(MODERN_UI_CSS_ID)) {
    return;
  }
  if (
    document.querySelector(
      `link[rel="stylesheet"][href="${MODERN_UI_CSS_HREF}"]`,
    )
  ) {
    return;
  }
  const link = document.createElement("link");
  link.id = MODERN_UI_CSS_ID;
  link.rel = "stylesheet";
  link.href = MODERN_UI_CSS_HREF;
  document.head.appendChild(link);
}
