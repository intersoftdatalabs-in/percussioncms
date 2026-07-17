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
 * Thin wrapper over the product TMX JS catalog global {@code I18N.message}.
 *
 * <p>Shell JSPs must load {@code /Rhythmyx/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=…}
 * before the modern bundle so keys resolve for the session locale (FR-023).</p>
 */

declare global {
  interface Window {
    I18N?: {
      message: (key: string, args?: unknown[]) => string;
    };
  }
}

/**
 * Resolve a TMX message key. Falls back to the key itself when I18N is
 * unavailable (e.g. unit tests without tmx.jsp).
 *
 * @param key - catalog key such as {@code perc.ui.home@My Recent}
 * @param args - optional format arguments (legacy I18N.message second arg)
 */
export function message(key: string, args?: unknown[]): string {
  const i18n = typeof window !== "undefined" ? window.I18N : undefined;
  if (i18n?.message) {
    try {
      return args != null ? i18n.message(key, args) : i18n.message(key);
    } catch {
      return key;
    }
  }
  return key;
}

export const MSG = {
  HOME_TITLE: "perc.ui.home.modern@Home",
  SECTION_RECENT: "perc.ui.home@My Recent",
  SECTION_LIBRARY: "perc.ui.home.modern@Library",
  SECTION_SEARCH: "perc.ui.home.modern@Search",
  SECTION_CREATE: "perc.ui.home@Add New",
  RECENT_EMPTY: "perc.ui.home.modern@No Recent Items",
  LIBRARY_EMPTY: "perc.ui.home@No Site Exists",
  LIBRARY_HELP: "perc.ui.home@Click on Site",
  SEARCH_EMPTY: "perc.ui.home.modern@No Search Results",
  SEARCH_PLACEHOLDER: "perc.ui.home.modern@Search Placeholder",
  SEARCH_SUBMIT: "perc.ui.home.modern@Search",
  CREATE_PAGE: "perc.ui.home.modern@Create Page",
  CREATE_HINT: "perc.ui.home.modern@Create Hint",
  LOADING: "perc.ui.home.modern@Loading",
  ERROR_GENERIC: "perc.ui.home.modern@Error",
  OPEN_ITEM: "perc.ui.home.modern@Open",
  NO_SITES_ADMIN: "perc.ui.home@Click Create Site",
} as const;
