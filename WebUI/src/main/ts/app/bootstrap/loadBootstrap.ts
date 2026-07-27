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

import { isSpaEntry } from "../deepLinks/allowlists";
import { DEFAULT_SPA_BOOTSTRAP, type SpaBootstrap } from "./types";

const SPA_BOOTSTRAP_ID = "perc-bootstrap";

/**
 * Load SPA bootstrap from the host page JSON script tag.
 */
export function loadSpaBootstrap(
  elementId: string = SPA_BOOTSTRAP_ID,
): SpaBootstrap {
  const el = document.getElementById(elementId);
  if (!el || !el.textContent) {
    return { ...DEFAULT_SPA_BOOTSTRAP };
  }
  try {
    const raw = JSON.parse(el.textContent) as Partial<SpaBootstrap>;
    const entryRaw =
      typeof raw.entry === "string" ? raw.entry.trim().toLowerCase() : "home";
    const entry = isSpaEntry(entryRaw)
      ? entryRaw
      : entryRaw === "widgetbuilder"
        ? "widget-builder"
        : "home";
    return {
      userName: typeof raw.userName === "string" ? raw.userName : "",
      locale: typeof raw.locale === "string" ? raw.locale : "en-us",
      entry,
      isAdmin: raw.isAdmin === true,
      isDesigner: raw.isDesigner === true,
      isWidgetBuilderActive: raw.isWidgetBuilderActive === true,
    };
  } catch {
    console.error(`[PercModernUI] Failed to parse SPA bootstrap (#${elementId})`);
    return { ...DEFAULT_SPA_BOOTSTRAP };
  }
}
