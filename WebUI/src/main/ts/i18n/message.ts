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
 * Human-readable fallback when TMX is not loaded.
 * Percussion keys often look like {@code perc.ui.home.modern@Home} — use text after {@code @}.
 */
export function fallbackLabelFromKey(key: string): string {
  if (!key) {
    return "";
  }
  const at = key.lastIndexOf("@");
  if (at >= 0 && at < key.length - 1) {
    return key.slice(at + 1);
  }
  return key;
}

/**
 * Resolve a TMX message key. Falls back to the English segment after {@code @}
 * (or the raw key) when I18N is unavailable (tests, or spa.jsp missing tmx load).
 *
 * @param key - catalog key such as {@code perc.ui.home@My Recent}
 * @param args - optional format arguments (legacy I18N.message second arg)
 */
export function message(key: string, args?: unknown[]): string {
  const i18n = typeof window !== "undefined" ? window.I18N : undefined;
  if (i18n?.message) {
    try {
      const resolved = args != null ? i18n.message(key, args) : i18n.message(key);
      // Real translation: non-empty and not an echo of the catalog key
      if (typeof resolved === "string" && resolved.trim() && resolved !== key) {
        return resolved;
      }
      // Missing key / empty stub / key echo → human text after @
      return fallbackLabelFromKey(key);
    } catch {
      return fallbackLabelFromKey(key);
    }
  }
  return fallbackLabelFromKey(key);
}

export const MSG = {
  HOME_TITLE: "perc.ui.home.modern@Home",
  SECTION_RECENT: "perc.ui.home@My Recent",
  SECTION_BOOKMARKS: "perc.ui.home.modern@My Bookmarks",
  SECTION_LIBRARY: "perc.ui.home.modern@Library",
  SECTION_SEARCH: "perc.ui.home.modern@Search",
  SECTION_CREATE: "perc.ui.home@Add New",
  /** Dashboard gadgets composed on Home (PR-7) */
  SECTION_GADGETS: "perc.ui.home.modern@Gadgets",
  RECENT_EMPTY: "perc.ui.home.modern@No Recent Items",
  RECENT_HINT:
    "perc.ui.home.modern@Recently opened pages. Bookmark pages you want to keep handy.",
  BOOKMARKS_EMPTY: "perc.ui.home.modern@No Bookmarks",
  BOOKMARKS_HINT:
    "perc.ui.home.modern@Pages you saved as favorites. Remove a bookmark anytime, or add from Recent, Search, or Library.",
  BOOKMARK_ADD: "perc.ui.page.mypages@Add to My Pages",
  BOOKMARK_REMOVE: "perc.ui.page.mypages@Remove from My Pages",
  BOOKMARK_NEEDS_ID: "perc.ui.home.modern@Cannot bookmark item without id",
  LIBRARY_EMPTY: "perc.ui.home@No Site Exists",
  LIBRARY_HELP: "perc.ui.home@Click on Site",
  SEARCH_EMPTY: "perc.ui.home.modern@No Search Results",
  SEARCH_PLACEHOLDER: "perc.ui.home.modern@Search pages and assets",
  SEARCH_SUBMIT: "perc.ui.home.modern@Search",
  SEARCH_HINT: "perc.ui.home.modern@Search across sites. Open or bookmark results.",
  CREATE_PAGE: "perc.ui.home.modern@Create Page",
  CREATE_HINT: "perc.ui.home.modern@Create Hint",
  CREATE_CHOOSE_TYPE: "perc.ui.home.modern@Choose Content Type",
  CREATE_TYPE_PAGE: "perc.ui.home.modern@Create Page",
  CREATE_TYPE_ASSET: "perc.ui.home.modern@Create Asset",
  CREATE_TYPE_BLOG: "perc.ui.home.modern@Create Blog Post",
  CREATE_BACK: "perc.ui.home.modern@Back",
  CREATE_SITE: "perc.ui.home.modern@Site",
  CREATE_TEMPLATE: "perc.ui.home.modern@Template",
  CREATE_FOLDER: "perc.ui.home.modern@Folder",
  CREATE_TITLE: "perc.ui.home.modern@Title",
  CREATE_FILENAME: "perc.ui.home.modern@File Name",
  CREATE_ASSET_TYPE: "perc.ui.home.modern@Asset Type",
  CREATE_BLOG: "perc.ui.home.modern@Blog",
  CREATE_SELECT: "perc.ui.home.modern@Select",
  CREATE_SUBMIT: "perc.ui.home.modern@Create",
  CREATE_VALIDATION: "perc.ui.home.modern@Create Validation",
  CREATE_FILE_TOO_LONG: "perc.ui.home.modern@File Name Too Long",
  CREATE_NOT_AUTHORIZED: "perc.ui.home.modern@Create Not Authorized",
  CREATE_NO_BLOGS: "perc.ui.home.modern@No Blogs",
  LOADING: "perc.ui.home.modern@Loading",
  RETRY: "perc.ui.home.modern@Retry",
  ERROR_GENERIC: "perc.ui.home.modern@Error",
  OPEN_ITEM: "perc.ui.home.modern@Open",
  NO_SITES_ADMIN: "perc.ui.home@Click Create Site",
  // Publishing shell (reuse perc.ui.publish.* where present; net-new under modern)
  PUBLISH_TITLE: "perc.ui.navMenu.publish@Publish",
  PUBLISH_SECTION_SITES: "perc.ui.publish.modern@Sites & Servers",
  PUBLISH_SECTION_STATUS: "perc.ui.publish.title@Status",
  PUBLISH_SECTION_LOGS: "perc.ui.publish.modern@Logs",
  PUBLISH_SECTION_DESIGN: "perc.ui.publish.modern@Design",
  PUBLISH_SECTION_RUNTIME: "perc.ui.publish.modern@Runtime",
  PUBLISH_FILTER_SITES: "perc.ui.publish.title@Filter Sites",
  PUBLISH_CARD: "perc.ui.publish.title@Card",
  PUBLISH_LIST: "perc.ui.publish.title@List",
  PUBLISH_FULL: "perc.ui.publish.title@Full",
  PUBLISH_STOP: "perc.ui.publish.title@Stop",
  PUBLISH_INCREMENTAL: "perc.ui.publish.modern@Incremental",
  PUBLISH_BACK: "perc.ui.publish.modern@Back",
  PUBLISH_EMPTY_SITES: "perc.ui.publish.modern@No Sites",
  PUBLISH_EMPTY_SERVERS: "perc.ui.publish.modern@No Servers",
  PUBLISH_EMPTY_JOBS: "perc.ui.publish.modern@No Active Jobs",
  PUBLISH_EMPTY_LOGS: "perc.ui.publish.modern@No Logs",
  PUBLISH_EMPTY_QUEUE: "perc.ui.publish.incrementalPreview@No items queued for incremental",
  PUBLISH_LOADING: "perc.ui.home.modern@Loading",
  PUBLISH_ERROR: "perc.ui.home.modern@Error",
  PUBLISH_FORBIDDEN: "perc.ui.publish.modern@Publish Forbidden",
  PUBLISH_BADCONFIG: "perc.ui.publish.modern@Bad Server Configuration",
  PUBLISH_SUCCESS: "perc.ui.publish.title@Publish Request",
  PUBLISH_SELECT_SERVER: "perc.ui.publish.title@Server",
  PUBLISH_PLACEHOLDER_SECTION: "perc.ui.publish.modern@Section Coming Soon",
  PUBLISH_FOLDER: "perc.ui.publish.modern@Folder",
  PUBLISH_ADD_SERVER: "perc.ui.publish.modern@Add Server",
  PUBLISH_EDIT_SERVER: "perc.ui.publish.modern@Edit Server",
  PUBLISH_SERVER_NAME: "perc.ui.publish.modern@Server Name",
  PUBLISH_SERVER_TYPE: "perc.ui.publish.view@Production",
  PUBLISH_DELIVERY_TYPE: "perc.ui.publish.modern@Delivery Type",
  PUBLISH_DRIVER: "perc.ui.publish.modern@Driver",
  PUBLISH_SET_DEFAULT: "perc.ui.publish.modern@Set as Publish Now Server",
  PUBLISH_IGNORE_UNMODIFIED: "perc.ui.publish.modern@Ignore Unmodified Assets",
  PUBLISH_RELATED_ITEMS: "perc.ui.publish.modern@Publish Related Items",
  PUBLISH_SAVE: "perc.ui.publish.modern@Save",
  PUBLISH_DELETE_SERVER: "perc.ui.publish.title@Delete Server",
  PUBLISH_CONFIRM_DELETE_SERVER: "perc.ui.publish.modern@Confirm Delete Server",
  PUBLISH_DISCARD_CHANGES: "perc.ui.publish.modern@Discard Changes",
  PUBLISH_CONFIRM_DELETE_DESIGN: "perc.ui.publish.modern@Confirm Delete Design Object",
} as const;
