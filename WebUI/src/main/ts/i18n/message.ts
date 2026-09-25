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
 * Thin wrapper over the product TMX JS catalog global {@code I18N.message}.
 *
 * <p>Shell JSPs must load {@code /Rhythmyx/tmx/tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=...}
 * before the modern bundle so keys resolve for the session locale (FR-023).</p>
 *
 * <p>Resolutions are tracked for third-party {@code @mkd/language} so correction
 * popovers can associate catalog keys without per-element {@code data-i18n-key}
 * attributes. See {@link getTrackedMessageId}.</p>
 */

import { createTrackedMessage } from "@mkd/language";

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
 * Resolve a TMX message key (no tracking). Prefer {@link message} at call sites.
 */
export function resolveMessage(key: string, args?: unknown[]): string {
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

const tracked = createTrackedMessage((key, args) => resolveMessage(key, args));

/**
 * Resolve a TMX message key. Falls back to the English segment after {@code @}
 * (or the raw key) when I18N is unavailable (tests, or spa.jsp missing tmx load).
 *
 * <p>Also registers the key↔display mapping for {@code @mkd/language}
 * ({@link getTrackedMessageId}).</p>
 *
 * @param key - catalog key such as {@code perc.ui.home@My Recent}
 * @param args - optional format arguments (legacy I18N.message second arg)
 */
export function message(key: string, args?: unknown[]): string {
  return tracked.message(key, args);
}

/**
 * Catalog key for a DOM host from tracked {@link message} resolutions.
 * Passed to {@code @mkd/language} {@code init({ getMessageId })}.
 */
export function getTrackedMessageId(el: Element): string | undefined {
  return tracked.getMessageId(el);
}

/** Test helper — clear tracked key↔text map. */
export function __resetMessageTrackingForTests(): void {
  tracked.clear();
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
  LIBRARY_UP: "perc.ui.home.modern@Up",
  LIBRARY_BREADCRUMB: "perc.ui.home.modern@Library path",
  LIBRARY_FOLDER_EMPTY: "perc.ui.home.modern@This folder is empty",
  SEARCH_EMPTY: "perc.ui.home.modern@No Search Results",
  SEARCH_PLACEHOLDER: "perc.ui.home.modern@Search pages and assets",
  SEARCH_SUBMIT: "perc.ui.home.modern@Search",
  SEARCH_HINT:
    "perc.ui.home.modern@Search by full words from the title or body. Open or bookmark results.",
  SEARCH_RESULT_COUNT: "perc.ui.home.modern@Search result count",
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
  CREATE_OPEN_EDITOR_FAILED:
    "perc.ui.home.modern@The page was created but the editor could not open.",
  CREATE_NO_BLOGS:
    "perc.ui.home.modern@No blogs are configured. Create a blog section on a site first.",
  CREATE_NO_ASSET_TYPES: "perc.ui.home.modern@No asset types available",
  CREATE_ASSET_HINT:
    "perc.ui.home.modern@Choose a type and folder. The asset editor opens so you can finish and save.",
  CREATE_BLOG_HINT:
    "perc.ui.home.modern@Choose a blog, then enter a title for the new post.",
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
  PUBLISH_CONFIRM_STOP: "perc.ui.publish.modern@Confirm Stop Publish Job",
  PUBLISH_INCREMENTAL: "perc.ui.publish.modern@Incremental",
  PUBLISH_CONFIRM_INCREMENTAL:
    "perc.ui.publish.modern@Confirm Incremental Publish",
  PUBLISH_JOB_STARTED: "perc.ui.publish.modern@Publish Job Started",
  PUBLISH_BACK: "perc.ui.publish.modern@Back",
  PUBLISH_EMPTY_SITES: "perc.ui.publish.modern@No Sites",
  PUBLISH_EMPTY_SERVERS: "perc.ui.publish.modern@No Servers",
  PUBLISH_EMPTY_JOBS: "perc.ui.publish.modern@No Active Jobs",
  PUBLISH_EMPTY_LOGS: "perc.ui.publish.modern@No Logs",
  PUBLISH_EMPTY_QUEUE: "perc.ui.publish.incrementalPreview@No items queued for incremental",
  PUBLISH_REMOVE_QUEUE_ITEM: "perc.ui.publish.modern@Remove from queue",
  PUBLISH_CONFIRM_REMOVE_QUEUE_ITEM:
    "perc.ui.publish.modern@Remove this item from the incremental queue?",
  PUBLISH_QUEUE_ITEM_REMOVED: "perc.ui.publish.modern@Removed from incremental queue",
  PUBLISH_QUEUE_REMOVE_FORBIDDEN:
    "perc.ui.publish.modern@You are not allowed to remove this queued item",
  PUBLISH_QUEUE_REMOVE_NOT_FOUND:
    "perc.ui.publish.modern@This item is not on the incremental queue",
  PUBLISH_QUEUE_REMOVE_FAILED:
    "perc.ui.publish.modern@Could not remove the queued item",
  PUBLISH_CLEAR_QUEUE: "perc.ui.publish.modern@Clear queue",
  PUBLISH_CONFIRM_CLEAR_QUEUE:
    "perc.ui.publish.modern@Clear the incremental queue for this site and server?",
  PUBLISH_QUEUE_CLEARED: "perc.ui.publish.modern@Incremental queue cleared",
  PUBLISH_QUEUE_CLEAR_PARTIAL:
    "perc.ui.publish.modern@Some items are still on the incremental queue",
  PUBLISH_QUEUE_CLEAR_FORBIDDEN:
    "perc.ui.publish.modern@You are not allowed to clear the incremental queue",
  PUBLISH_QUEUE_CLEAR_NOT_FOUND:
    "perc.ui.publish.modern@The site or server was not found",
  PUBLISH_QUEUE_CLEAR_FAILED:
    "perc.ui.publish.modern@Could not clear the incremental queue",
  PUBLISH_LOADING: "perc.ui.home.modern@Loading",
  PUBLISH_ERROR: "perc.ui.home.modern@Error",
  PUBLISH_FORBIDDEN: "perc.ui.publish.modern@Publish Forbidden",
  PUBLISH_JOB_NOT_FOUND: "perc.ui.publish.modern@Publish job not found",
  PUBLISH_LOG_OPEN_EDITOR: "perc.ui.publish.modern@Open in editor",
  PUBLISH_LOG_ITEM_FORBIDDEN:
    "perc.ui.publish.modern@You are not allowed to open this item",
  PUBLISH_LOG_ITEM_NOT_FOUND: "perc.ui.publish.modern@This item was not found",
  PUBLISH_LOG_ITEM_NO_ID:
    "perc.ui.publish.modern@This log row has no content id",
  PUBLISH_LOG_ITEM_OPEN_FAILED:
    "perc.ui.publish.modern@Could not open this item in the editor",
  PUBLISH_JOB_STOP_CONFLICT:
    "perc.ui.publish.modern@Publish job cannot be stopped",
  PUBLISH_EDITION_NAME_CONFLICT:
    "perc.ui.publish.modern@Edition name already exists",
  PUBLISH_CONTENT_LIST_NAME_CONFLICT:
    "perc.ui.publish.modern@Content list name already exists",
  PUBLISH_DELIVERY_TYPE_NAME_CONFLICT:
    "perc.ui.publish.modern@Delivery type name already exists",
  PUBLISH_LOCATION_SCHEME_NAME_CONFLICT:
    "perc.ui.publish.modern@Location scheme name already exists",
  PUBLISH_SERVER_NAME_CONFLICT:
    "perc.ui.publish.modern@Publish server name already exists",
  PUBLISH_CONTEXT_NAME_CONFLICT:
    "perc.ui.publish.modern@Publishing context name already exists",
  PUBLISH_BADCONFIG: "perc.ui.publish.modern@Bad Server Configuration",
  PUBLISH_SUCCESS: "perc.ui.publish.title@Publish Request",
  PUBLISH_SELECT_SERVER: "perc.ui.publish.title@Server",
  PUBLISH_PLACEHOLDER_SECTION: "perc.ui.publish.modern@Section Coming Soon",
  PUBLISH_FOLDER: "perc.ui.publish.modern@Folder",
  PUBLISH_ADD_SERVER: "perc.ui.publish.modern@Add Server",
  PUBLISH_EDIT_SERVER: "perc.ui.publish.modern@Edit Server",
  PUBLISH_SERVER_NAME: "perc.ui.publish.modern@Server Name",
  PUBLISH_SERVER_TYPE: "perc.ui.publish.view@Production", // @deprecated re-keyed — use MSG.PUBLISH.SERVER.EDITOR.TYPE (Phase 1 audit-publishing.md).
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
  PUBLISH_ITEM_HISTORY: "perc.ui.publishing.history@Publishing History",
  PUBLISH_ITEM_HISTORY_HINT:
    "perc.ui.publish.modern@Look up a page or asset by item id. Deep-link Status or Logs with itemId.",
  PUBLISH_ITEM_HISTORY_ITEM_ID: "perc.ui.publishing.history@Content Id",
  PUBLISH_ITEM_HISTORY_VIEW: "perc.ui.publishing.history@View History",
  PUBLISH_ITEM_HISTORY_EMPTY: "perc.ui.publishing.history@No Publishing History",
  PUBLISH_ITEM_HISTORY_EMPTY_HINT:
    "perc.ui.publishing.history@No Publishing History For this item",
  PUBLISH_ITEM_HISTORY_SERVER: "perc.ui.publishing.history@Server",
  PUBLISH_ITEM_HISTORY_LOCATION: "perc.ui.publishing.history@Location",
  PUBLISH_ITEM_HISTORY_REVISION: "perc.ui.publishing.history@Revision",
  PUBLISH_ITEM_HISTORY_DATE: "perc.ui.publishing.history@Date",
  PUBLISH_ITEM_HISTORY_OPERATION: "perc.ui.publishing.history@Operation",
  PUBLISH_ITEM_HISTORY_STATUS: "perc.ui.publishing.history@Status",
  PUBLISH_ITEM_HISTORY_ERROR: "perc.ui.publishing.history@Error Fetching History",
  PUBLISH_ITEM_HISTORY_NEED_ID: "perc.ui.publish.modern@Enter an item id",
  PUBLISH_ITEM_HISTORY_INVALID_ID: "perc.ui.publish.modern@Invalid item id",
  PUBLISH_ITEM_HISTORY_OPEN_STATUS: "perc.ui.publish.modern@Open Status",
  PUBLISH_ITEM_HISTORY_OPEN_LOGS: "perc.ui.publish.modern@Open Logs",
  PUBLISH_SCHEDULE: "perc.ui.schedule.dialog@Schedule",
  PUBLISH_SCHEDULE_HINT:
    "perc.ui.publish.modern@Get or set publish and removal dates for a page or asset from this site workspace.",
  PUBLISH_SCHEDULE_PUBLISH_DATE: "perc.ui.publish.modern@Publish date",
  PUBLISH_SCHEDULE_REMOVAL_DATE: "perc.ui.publish.modern@Removal date",
  PUBLISH_SCHEDULE_COMMENTS: "perc.ui.publish.modern@Comments",
  PUBLISH_SCHEDULE_CLEAR: "perc.ui.publish.modern@Clear dates",
  PUBLISH_SCHEDULE_SAVE: "perc.ui.publish.modern@Save dates",
  PUBLISH_SCHEDULE_LOAD: "perc.ui.publish.modern@Load dates",
  PUBLISH_SCHEDULE_DATES_SAME: "perc.ui.schedule.dialog@Dates Cannot Be Same",
  PUBLISH_SCHEDULE_DATE_RANGE: "perc.ui.schedule.dialog@Enter Valid Date Range",
  PUBLISH_SCHEDULE_INVALID: "perc.ui.publish.modern@Invalid schedule dates",
  PUBLISH_SCHEDULE_FORBIDDEN: "perc.ui.publish.modern@Publish Forbidden",
  PUBLISH_SCHEDULE_CONFLICT:
    "perc.ui.publish.modern@Someone else is editing this item",
  PUBLISH_SCHEDULE_SAVED: "perc.ui.publish.modern@Schedule dates saved",
  PUBLISH_NOW: "perc.ui.publish.title@Publish Now",
  PUBLISH_NOW_HINT:
    "perc.ui.publish.modern@Publish a selected page or asset now from this site workspace.",
  PUBLISH_NOW_REVIEW: "perc.ui.publish.modern@Review publish now",
  PUBLISH_NOW_CONFIRM: "perc.ui.editor@Publish this item now?",
  PUBLISH_NOW_SUBMIT: "perc.ui.publish.title@Publish Now",
  PUBLISH_NOW_SUCCESS: "perc.ui.publish.modern@Publish now started.",
  PUBLISH_NOW_NOT_FOUND: "perc.ui.publish.modern@Item not found",
  PUBLISH_TAKEDOWN: "perc.ui.publish.title@Remove From Site",
  PUBLISH_TAKEDOWN_HINT:
    "perc.ui.publish.modern@Take down (unpublish) a page or asset from this site workspace.",
  PUBLISH_TAKEDOWN_KIND: "perc.ui.publish.modern@Item type",
  PUBLISH_TAKEDOWN_KIND_PAGE: "perc.ui.publish.modern@Page",
  PUBLISH_TAKEDOWN_KIND_RESOURCE: "perc.ui.publish.modern@Asset",
  PUBLISH_TAKEDOWN_LOAD: "perc.ui.publish.modern@Review take down",
  PUBLISH_TAKEDOWN_SUBMIT: "perc.ui.publish.modern@Take down",
  PUBLISH_TAKEDOWN_SUCCESS: "perc.ui.publish.modern@Take down started.",
  PUBLISH_TAKEDOWN_NONE: "perc.ui.publish.modern@No linked pages.",
  PUBLISH_STAGE: "perc.ui.publish.title@Stage / Remove From Staging",
  PUBLISH_STAGE_HINT:
    "perc.ui.publish.modern@Stage or remove from staging a page or asset from this site workspace.",
  PUBLISH_STAGE_ACTION: "perc.ui.publish.modern@Action",
  PUBLISH_STAGE_ACTION_STAGE: "perc.ui.publish.modern@Stage",
  PUBLISH_STAGE_ACTION_UNSTAGE: "perc.ui.publish.modern@Remove from staging",
  PUBLISH_STAGE_REVIEW: "perc.ui.publish.modern@Review stage",
  PUBLISH_STAGE_SUBMIT_STAGE: "perc.ui.publish.modern@Stage item",
  PUBLISH_STAGE_SUBMIT_UNSTAGE: "perc.ui.publish.modern@Remove from staging",
  PUBLISH_STAGE_CONFIRM_STAGE: "perc.ui.publish.modern@Stage this item for publish?",
  PUBLISH_STAGE_CONFIRM_UNSTAGE: "perc.ui.publish.modern@Remove this item from staging?",
  PUBLISH_STAGE_SUCCESS_STAGE: "perc.ui.publish.modern@Item staged.",
  PUBLISH_STAGE_SUCCESS_UNSTAGE: "perc.ui.publish.modern@Item removed from staging.",
  PUBLISH_ACTIONS: "perc.ui.publish.modern@Available publishing actions",
  PUBLISH_ACTIONS_HINT:
    "perc.ui.publish.modern@Server-driven actions for the selected page or asset. Unavailable actions are disabled.",
  PUBLISH_ACTIONS_LOAD: "perc.ui.publish.modern@Load actions",
  PUBLISH_ACTIONS_EMPTY: "perc.ui.publish.modern@No publishing actions for this item",
  PUBLISH_ACTIONS_UNAVAILABLE: "perc.ui.publish.modern@Action not available",
  // SPA top navigation (shell chrome)
  NAV_HOME: "perc.ui.navMenu.home@Home",
  NAV_DASHBOARD: "perc.ui.navMenu.dashboard@Dashboard",
  NAV_EDITOR: "perc.ui.navMenu.webmgt@Editor",
  NAV_ARCHITECTURE: "perc.ui.navMenu.architecture@Navigation",
  /** Navigation SPA top-nav tooltip (#3094 / #3217). */
  NAV_ARCHITECTURE_TITLE: "perc.ui.architecture.modern@Site navigation",
  /** Design SPA top-nav (#2808) — classic key already en-us "Design". */
  NAV_DESIGN: "perc.ui.navMenu.design@Design",
  NAV_DESIGN_TITLE:
    "perc.ui.design.modern@Template library and design tools",
  NAV_DEVELOPER: "perc.ui.dashboard.modern@Developer",
  NAV_PUBLISH: "perc.ui.navMenu.publish@Publish",
  /**
   * Consolidated top-nav Admin label (#2702 / #3201). TMX en-us is "Admin".
   * TopNav also normalizes the English {@code @Administration} fallback so
   * chrome never shows the old dual-entry word.
   */
  NAV_ADMIN: "perc.ui.navMenu.admin@Administration",
  /** @deprecated Prefer NAV_ADMIN for top chrome; retained for residual page titles. */
  NAV_ADMINISTRATION: "perc.ui.dashboard.modern@Administration",
  /** @deprecated Prefer NAV_ADMIN for top chrome; Admin tools remain at /admin deep link. */
  NAV_ADMIN_TOOLS: "perc.ui.dashboard.modern@Admin tools",
  NAV_WIDGET_BUILDER: "perc.ui.navMenu.admin@Widget Builder",
  NAV_EXPLORER: "perc.ui.dashboard.modern@Explorer",
  NAV_ARIA_MAIN: "perc.ui.dashboard.modern@Main",
  /** Page/title for dashboard gadgets (not top-nav; deep link /home/gadgets). */
  NAV_DASHBOARD_TITLE: "perc.ui.dashboard.modern@Dashboard gadgets on Home",
  NAV_DEVELOPER_TITLE: "perc.ui.dashboard.modern@CMS design tools (content types, templates, ...)",
  USER_SIGNED_IN_AS: "perc.ui.dashboard.modern@Signed in as",
  USER_DEFAULT_NAME: "perc.ui.dashboard.modern@user",
  USER_LOGOUT: "perc.ui.common.label@Log Out",
  USER_COMMUNITY: "perc.ui.dashboard.modern@Community",
  USER_COMMUNITY_NONE: "perc.ui.dashboard.modern@None",
  USER_COMMUNITY_SWITCH: "perc.ui.dashboard.modern@Switch",
  USER_COMMUNITY_SWITCH_ARIA: "perc.ui.dashboard.modern@Switch community",
  USER_COMMUNITY_LIST_ARIA: "perc.ui.dashboard.modern@Available communities",
  USER_COMMUNITY_SWITCH_ERROR:
    "perc.ui.dashboard.modern@Could not switch community.",
  USER_COMMUNITY_SWITCHING: "perc.ui.dashboard.modern@Switching community…",
  /** Profile hub menu entry (#2393) — prefer PROFILE_MSG in profile/messages.ts for new code */
  USER_MY_PROFILE: "perc.ui.profile.modern@My profile",
  // Dashboard / Gadgets chrome
  DASHBOARD_TITLE: "perc.ui.dashboard.title@Dashboard",
  DASHBOARD_EMBEDDED_TITLE: "perc.ui.dashboard.modern@Gadgets",
  DASHBOARD_ADD_GADGET: "perc.ui.dashboard.modern@Add Gadget",
  DASHBOARD_LOADING: "perc.ui.dashboard.modern@Loading gadgets",
  DASHBOARD_LAYOUT_WARNING_PREFIX:
    "perc.ui.dashboard.modern@Could not load saved gadget layout",
  DASHBOARD_LAYOUT_WARNING_TITLE: "perc.ui.dashboard.modern@Layout load warning",
  DASHBOARD_LEGACY_LINK: "perc.ui.dashboard.modern@Legacy dashboard",
  GADGET_REMOVE_TITLE: "perc.ui.dashboard.modern@Remove gadget",
  // Add Gadget modal
  MODAL_ADD_GADGET_TITLE: "perc.ui.dashboard.modern@Add Gadget",
  MODAL_SEARCH_PLACEHOLDER: "perc.ui.dashboard.modern@Search gadgets...",
  MODAL_ADD_BUTTON: "perc.ui.dashboard.modern@Add",
  MODAL_ADDED_BUTTON: "perc.ui.dashboard.modern@Added",
  MODAL_NO_RESULTS: "perc.ui.dashboard.modern@No gadgets found",
  MODAL_DEFAULT_CATEGORY: "perc.ui.dashboard.modern@Other",
  // Widget: Widget Configuration
  WIDGET_CONFIG_HINT:
    "perc.ui.dashboard.modern@Choose which gadgets appear on Home for this session. You can also use Add Gadget on the dashboard toolbar.",
  WIDGET_CONFIG_APPLY: "perc.ui.dashboard.modern@Apply layout",
  WIDGET_CONFIG_EMPTY: "perc.ui.dashboard.modern@Select at least one gadget.",
  WIDGET_CONFIG_APPLIED:
    "perc.ui.dashboard.modern@Layout applied for this browser session. Refresh if tiles did not update.",
  // Widget: Welcome
  WELCOME_GREETING_MORNING: "perc.ui.dashboard.welcome@Good morning",
  WELCOME_GREETING_AFTERNOON: "perc.ui.dashboard.welcome@Good afternoon",
  WELCOME_GREETING_EVENING: "perc.ui.dashboard.welcome@Good evening",
  WELCOME_BLURB: "perc.ui.dashboard.welcome@Using Percussion CMS",
  WELCOME_LINK_SITEMANAGE: "perc.ui.dashboard.welcome@Site Management",
  WELCOME_LINK_WEBMGT: "perc.ui.dashboard.welcome@Web Management",
  WELCOME_LINK_ADMINCONSOLE: "perc.ui.dashboard.welcome@Admin Console",
  // Widget: Activity
  ACTIVITY_LOADING: "perc.ui.dashboard.activity@Loading activity",
  ACTIVITY_EMPTY: "perc.ui.dashboard.activity@No activity for path",
  ACTIVITY_PATH: "perc.ui.dashboard.activity@Path",
  ACTIVITY_SITE: "perc.ui.dashboard.activity@Site",
  ACTIVITY_PUBLISHED: "perc.ui.dashboard.activity@Published",
  ACTIVITY_PENDING: "perc.ui.dashboard.activity@Pending",
  ACTIVITY_NEW: "perc.ui.dashboard.activity@New",
  ACTIVITY_UPDATED: "perc.ui.dashboard.activity@Updated",
  ACTIVITY_ARCHIVED: "perc.ui.dashboard.activity@Archived",
  // Gadget catalog (Title Case, modern UI)
  GADGET_WELCOME: "perc.ui.gadgets.welcome@WELCOME",
  GADGET_PAGES_BY_STATUS: "perc.ui.gadgets.workflowStatus@PAGES BY STATUS",
  GADGET_PROCESS_MONITOR: "perc.ui.gadgets.processmonitor@Process Monitor",
  GADGET_COMMENTS: "perc.ui.gadgets.comments@COMMENTS",
  GADGET_COOKIE_CONSENT: "perc.ui.gadgets.cookieConsent@COOKIE CONSENT",
  GADGET_SITEWIDE_FRAMEWORK: "perc.ui.gadgets.sitewideFramework@Sitewide Framework",
  GADGET_MEMBERSHIP: "perc.ui.gadgets.membership@MEMBERSHIP",
  GADGET_SEO_AUDIT: "perc.ui.gadgets.seo@SEO AUDIT",
  GADGET_ASSETS_BY_STATUS: "perc.ui.asset.status.gadget@ASSETS BY STATUS",
  GADGET_BULK_UPLOAD: "perc.ui.gadget.bulkUpload@Bulk Upload",
  GADGET_GLOBAL_VARIABLES: "perc.ui.global.variables.gadget@Global Variables",
  GADGET_SITEIMPROVE: "perc.ui.site.improve.gadget@SITEIMPROVE",
  GADGET_TRAFFIC: "perc.ui.traffic.gadget@TRAFFIC",
  GADGET_ACTIVITY: "perc.ui.dashboard.modern@Activity",
  GADGET_WHATS_WORKING: "perc.ui.dashboard.modern@What's Working",
  GADGET_REPORTS: "perc.ui.dashboard.modern@Reports",
  GADGET_FORM_TRACKER: "perc.ui.dashboard.modern@Form Tracker",
  GADGET_EXTERNAL_CONTENT: "perc.ui.dashboard.modern@External Content",
  GADGET_GOOGLE_SETUP: "perc.ui.dashboard.modern@Google Setup",
  GADGET_BLOGS: "perc.ui.dashboard.modern@Blogs",
  GADGET_DASHBOARD_CONFIG: "perc.ui.dashboard.modern@Dashboard Configuration",
  // Gadget catalog descriptions
  GADGET_DESC_WELCOME:
    "perc.ui.dashboard.modern@Welcome message and dashboard introduction",
  GADGET_DESC_PAGES_BY_STATUS: "perc.ui.dashboard.modern@Pages grouped by workflow state",
  GADGET_DESC_ACTIVITY: "perc.ui.dashboard.modern@Content activity metrics by path and duration",
  GADGET_DESC_PROCESS_MONITOR: "perc.ui.dashboard.modern@System process and monitoring status",
  GADGET_DESC_WHATS_WORKING:
    "perc.ui.dashboard.modern@Effectiveness scores (requires Google Analytics)",
  GADGET_DESC_ASSETS_BY_STATUS:
    "perc.ui.dashboard.modern@Asset workflow status distribution",
  GADGET_DESC_BULK_UPLOAD: "perc.ui.dashboard.modern@Upload files into Assets/uploads",
  GADGET_DESC_REPORTS: "perc.ui.dashboard.modern@Quick CMS reports hub",
  GADGET_DESC_TRAFFIC: "perc.ui.dashboard.modern@Content traffic series",
  GADGET_DESC_BLOGS: "perc.ui.dashboard.modern@Blog listings and section create",
  GADGET_DESC_COMMENTS: "perc.ui.dashboard.modern@Pages with visitor comments",
  GADGET_DESC_FORM_TRACKER: "perc.ui.dashboard.modern@Form submission tracking",
  GADGET_DESC_COOKIE_CONSENT: "perc.ui.dashboard.modern@Cookie consent log totals",
  GADGET_DESC_SEO_AUDIT: "perc.ui.dashboard.modern@Non-SEO pages by severity",
  GADGET_DESC_GOOGLE_SETUP: "perc.ui.dashboard.modern@Google Analytics provider and site profiles",
  GADGET_DESC_MEMBERSHIP: "perc.ui.dashboard.modern@Site membership users (DTS)",
  GADGET_DESC_SITEIMPROVE: "perc.ui.dashboard.modern@Siteimprove token and publish config",
  GADGET_DESC_EXTERNAL_CONTENT: "perc.ui.dashboard.modern@Embed an external URL",
  GADGET_DESC_GLOBAL_VARIABLES: "perc.ui.dashboard.modern@System global variables metadata",
  GADGET_DESC_SITEWIDE_FRAMEWORK: "perc.ui.dashboard.modern@Theme / framework summaries",
GADGET_DESC_DASHBOARD_CONFIG:
    "perc.ui.dashboard.modern@Choose which gadgets appear this session",

  // ========================================================================
  // Phase 1 nested groups (added by i18n extraction plan, see
  // docs/ai-generated/tasks/webui-i18n-string-extraction/plan.md Phase 1).
  //
  // Existing flat constants above (HOME_TITLE, SECTION_*, NAV_*, USER_*,
  // PUBLISH_*, DASHBOARD_*, GADGET_*, MODAL_*, WIDGET_CONFIG_*, WELCOME_*,
  // ACTIVITY_*) are preserved for backward compatibility. New code in
  // Phase 3 PRs should consume the nested groups below, e.g.
  //   message(MSG.DASHBOARD.WIDGETS.ASSETS_STATUS.EMPTY)
  //   message(MSG.PUBLISH.SERVER.EDITOR.TYPE)
  //   message(MSG.WIDGETBUILDER.EDITOR.FIELD.LABEL)
  //
  // Re-keying: MSG.PUBLISH_SERVER_TYPE above is @deprecated. The new
  // canonical key is MSG.PUBLISH.SERVER.EDITOR.TYPE. ServerEditor.tsx
  // migrates in Phase 3 PR-B3.
  // ========================================================================

  DASHBOARD: {
    WIDGETS: {
      ASSETS_STATUS: {
        LOADING: "perc.ui.dashboard.modern@Loading asset status",
        EMPTY:
          "perc.ui.dashboard.modern@No assets found for this path and workflow.",
      },
      BLOGS: {
        NO_TEMPLATES:
          "perc.ui.dashboard.modern@A blog needs two existing templates: one with a Blog List widget and one with a Blog Post widget. Create those in Design / Templates first (or copy base blog templates onto this site).",
      },
      BULK_UPLOAD: {
        TYPE_FILE: "perc.ui.dashboard.modern@Bulk upload asset type File",
        TYPE_IMAGE: "perc.ui.dashboard.modern@Bulk upload asset type Image",
      },
      COMMENTS: {
        LOADING: "perc.ui.dashboard.modern@Loading comments",
        NO_SITES: "perc.ui.dashboard.modern@No sites available.",
      },
      COOKIE_CONSENT: {
        LOADING: "perc.ui.dashboard.modern@Loading cookie consent",
      },
      EFFECTIVENESS: {
        LOADING: "perc.ui.dashboard.modern@Loading effectiveness",
        NO_ANALYTICS_TITLE:
          "perc.ui.dashboard.modern@Google Analytics is not configured",
        NO_ANALYTICS_BODY:
          "perc.ui.dashboard.modern@What's Working needs a Google Analytics provider and site profile. Use the Google Setup gadget, then refresh this widget.",
        EMPTY:
          "perc.ui.dashboard.modern@No effectiveness data for this path and duration.",
      },
      FORMS: {
        LOADING: "perc.ui.dashboard.modern@Loading forms",
        NO_SITES:
          "perc.ui.dashboard.modern@No sites available to load forms.",
      },
      MEMBERSHIP: {
        LOADING: "perc.ui.dashboard.modern@Loading membership",
        NO_SITES: "perc.ui.dashboard.modern@No sites available.",
        EMPTY: "perc.ui.dashboard.modern@No members for this site.",
      },
      PROCESS_MONITOR: {
        LOADING: "perc.ui.dashboard.modern@Loading process monitor",
        EMPTY: "perc.ui.dashboard.modern@No monitors available",
      },
      REPORTS: {
        EMPTY: "perc.ui.dashboard.modern@No report data.",
      },
      SEO: {
        LOADING: "perc.ui.dashboard.modern@Loading SEO audit",
      },
      SITEIMPROVE: {
        LOADING: "perc.ui.dashboard.modern@Loading Siteimprove",
        TOKEN_LABEL: "perc.ui.dashboard.modern@Token on server",
        NOT_CONFIGURED: "perc.ui.dashboard.modern@Not configured",
        PRESENT: "perc.ui.dashboard.modern@Present",
        NONE: "perc.ui.dashboard.modern@None",
      },
      SITEWIDE_FRAMEWORK: {
        EMPTY: "perc.ui.dashboard.modern@No themes found.",
      },
      TRAFFIC: {
        LOADING: "perc.ui.dashboard.modern@Loading traffic data",
        EMPTY:
          "perc.ui.dashboard.modern@No traffic data for this path and date range.",
        LEGEND_VISITS: "perc.ui.traffic.gadget@Visits",
      },
      UNAVAILABLE_GADGET: {
        HEADER: "perc.ui.dashboard.modern@Not available in React Home",
      },
      WORKFLOW: {
        LOADING: "perc.ui.dashboard.modern@Loading workflow status",
        EMPTY:
          "perc.ui.dashboard.modern@No pages found for this path and workflow.",
      },
    },
  },

  PUBLISH: {
    SECTIONS: {
      LOGS: {
        SHOW: "perc.ui.publish.title@Show",
        FILTER_ALL: "perc.ui.publish.title@All",
        SITE_ID: "perc.ui.publish.sections.logs@Site id",
        SERVER_ID: "perc.ui.publish.sections.logs@Server id",
        DAYS: "perc.ui.publish.sections.logs@Days",
      },
      RUNTIME: {
        SITE: "perc.ui.publish.title@Site",
        SITE_PICKER_ARIA: "perc.ui.publish.sections.runtime@Runtime site",
        PUBLISH_SERVER: "perc.ui.publish.sections.runtime@Publish server",
        SERVER_PICKER_ARIA:
          "perc.ui.publish.sections.runtime@Runtime publish server",
        REFRESH: "perc.ui.publish.sections.runtime@Refresh",
        EDITIONS_EMPTY:
          "perc.ui.publish.sections.runtime@No editions for this site",
        IDLE: "perc.ui.publish.sections.runtime@Idle",
        START: "perc.ui.publish.sections.runtime@Start",
        JOB_RUNNING: "perc.ui.publish.sections.runtime@Job {0}",
        OPEN_JOB: "perc.ui.publish.sections.runtime@Open job",
        DEMAND_HEADING: "perc.ui.publish.sections.runtime@Demand publish",
        DEMAND_HELP:
          "perc.ui.publish.sections.runtime@Selected edition: {0}. Enter content ids (comma-separated). Folder parent is resolved on the server when possible.",
        DEMAND_NONE: "perc.ui.publish.sections.runtime@none",
        CONTENT_IDS: "perc.ui.publish.sections.runtime@Content ids",
        CONTENT_IDS_PLACEHOLDER:
          "perc.ui.publish.sections.runtime@e.g. 101, 102",
        QUEUE_DEMAND: "perc.ui.publish.sections.runtime@Queue demand",
        DEMAND_NEED_IDS:
          "perc.ui.publish.sections.runtime@Select an edition and enter at least one content id",
        ADVANCED_CLEANUP_HEADING:
          "perc.ui.publish.sections.runtime@Advanced cleanup",
        CLEAR_SITE: "perc.ui.publish.sections.runtime@Clear site record",
        CONFIRM_CLEAR:
          "perc.ui.publish.sections.runtime@Clear published site record for this site? This cannot be undone.",
        PURGE_JOB_LOG: "perc.ui.publish.sections.runtime@Purge job log by id",
        NEED_JOB_ID: "perc.ui.publish.sections.runtime@Enter a job id to purge",
        CONFIRM_PURGE: "perc.ui.publish.sections.runtime@Purge log for job {0}?",
        PURGE_LOG: "perc.ui.publish.sections.runtime@Purge log",
        LAST_RESULT: "perc.ui.publish.sections.runtime@Last result: {0}",
        LAST_JOB: "perc.ui.publish.sections.runtime@job {0}",
        LAST_REQUEST: "perc.ui.publish.sections.runtime@request {0}",
        LAST_DELIVERED: "perc.ui.publish.sections.runtime@delivered {0}",
      },
      DESIGN: {
        SITE_PICKER_ARIA: "perc.ui.publish.sections.design@Design site",
        EDITIONS_EMPTY:
          "perc.ui.publish.sections.design@No editions for this site",
        CONTENT_LISTS_EMPTY: "perc.ui.publish.sections.design@No content lists",
      },
      SITE: {
        SELECT_ALL_ARIA: "perc.ui.publish.sections.site@Select all related items",
        ITEM_HEADING: "perc.ui.publish.sections.site@Item",
      },
    },
    DESIGN: {
      EDITIONS: {
        COMMENT: "perc.ui.publish.design.editions@Comment",
        ASSOCIATED_LISTS:
          "perc.ui.publish.design.editions@Associated content lists",
        ASSOCIATED_LISTS_NONE: "perc.ui.publish.design.editions@None",
        ASSOCIATE_LIST_ARIA:
          "perc.ui.publish.design.editions@Content list to associate",
        SELECT_LIST: "perc.ui.publish.design.editions@Select content list",
        DELIVERY_CONTEXT_ARIA:
          "perc.ui.publish.design.editions@Delivery context",
        COPY_TO_SITE_HEADING: "perc.ui.publish.design.editions@Copy to site",
        TARGET_SITE: "perc.ui.publish.design.editions@Target site",
      },
      CONTENT_LISTS: {
        DESCRIPTION: "perc.ui.publish.design.contentLists@Description",
        TYPE: "perc.ui.publish.design.contentLists@Type",
        TYPE_MODERN: "perc.ui.publish.design.contentLists@Modern",
        TYPE_LEGACY: "perc.ui.publish.design.contentLists@Legacy",
        GENERATOR: "perc.ui.publish.design.contentLists@Generator",
      },
      CONTEXTS: {
        DESCRIPTION: "perc.ui.publish.design.contexts@Description",
        SCHEME_DESCRIPTION:
          "perc.ui.publish.design.contexts@Scheme Description",
        SCHEME_CONTENT_TYPE: "perc.ui.publish.design.contexts@Content type id",
        SCHEME_TEMPLATE: "perc.ui.publish.design.contexts@Template id",
        PARAM_NAME_PLACEHOLDER: "perc.ui.publish.design.contexts@Parameter Name",
        PARAM_TYPE_STRING: "perc.ui.publish.title@Type",
        PARAM_TYPE_BACKEND_COLUMN:
          "perc.ui.publish.design.contexts@BackendColumn",
        PARAM_VALUE_PLACEHOLDER:
          "perc.ui.publish.design.contexts@Parameter Value",
        LIST_ARIA: "perc.ui.publish.design.contexts@Publishing context",
        EMPTY: "perc.ui.publish.design.contexts@No publishing contexts",
        SCHEMES_HEADING: "perc.ui.publish.design.contexts@Location schemes",
        SCHEMES_EMPTY:
          "perc.ui.publish.design.contexts@No schemes for this context",
      },
      DELIVERY_TYPES: {
        DESCRIPTION: "perc.ui.publish.design.deliveryTypes@Description",
        EMPTY: "perc.ui.publish.design.deliveryTypes@No delivery types",
      },
      SITE: {
        PICKER_ARIA: "perc.ui.publish.design.site@Design site",
        PROPERTY_CONTEXT_ARIA: "perc.ui.publish.design.site@Property context",
        CONTEXT_VARIABLES_HEADING:
          "perc.ui.publish.design.site@Context variables",
        PROPERTY_NAME: "perc.ui.publish.design.site@Property Name",
        PROPERTY_VALUE: "perc.ui.publish.design.site@Property Value",
      },
      SITE_ROOT: {
        EMPTY: "perc.ui.publish.design.siteRoot@Empty folder or path not found",
      },
    },
    SERVER: {
      EDITOR: {
        TYPE: "perc.ui.publish.server.editor@Server Type",
        SERVER_TYPE_PRODUCTION: "perc.ui.publish.server.editor@Production",
        SERVER_TYPE_STAGING: "perc.ui.publish.server.editor@Staging",
        DELIVERY_TYPE_FILE: "perc.ui.publish.server.editor@File",
        DELIVERY_TYPE_DATABASE: "perc.ui.publish.server.editor@Database",
      },
      DRIVERS: {
        FILE: {
          SELECT_REGION: "perc.ui.publish.drivers.file@Select",
        },
      },
    },
    STATUS_DETAIL: {
      HEADING: "perc.ui.publish.modern@Job details",
      JOB_ID: "perc.ui.publish.title@Job ID",
      EDITION: "perc.ui.publish.modern@Edition",
      ERROR: "perc.ui.publish.title@Error",
    },
    LOGS_DETAILS: {
      JOB_ID: "perc.ui.publish.title@Job ID",
      FILTER_ITEMS: "perc.ui.publish.title@Filter Items",
      OPERATION: "perc.ui.publish.title@Operation",
      LOCATION: "perc.ui.publish.title@Location",
      CONTENT_ID: "perc.ui.publish.title@Content ID",
      FILENAME: "perc.ui.publish.title@Filename",
      ELAPSED: "perc.ui.publish.logs.details@Elapsed",
      REVISION: "perc.ui.publish.logs.details@Revision",
      TEMPLATE: "perc.ui.publish.logs.details@Template",
    },
  },

  WIDGETBUILDER: {
    LABEL: "perc.ui.widgetbuilder@Label",
    PREFIX: "perc.ui.widgetbuilder@Prefix",
    VERSION: "perc.ui.widgetbuilder@Version",
    LIST: {
      ACTIONS: "perc.ui.widgetbuilder.list@Actions",
    },
    EDITOR: {
      FIELD: {
        AUTHOR: "perc.ui.widgetbuilder.editor.field.author@Author",
        PUBLISHER_URL:
          "perc.ui.widgetbuilder.editor.field.publisherUrl@Publisher URL",
        DESCRIPTION:
          "perc.ui.widgetbuilder.editor.field.description@Description",
        WIDGET_HTML:
          "perc.ui.widgetbuilder.editor.field.widgetHtml@Widget HTML",
      },
      LEGEND: "perc.ui.widgetbuilder.editor.legend@Fields",
      FIELD_NAME: "perc.ui.widgetbuilder.editor.field.name@Field Name",
    },
  },

  WORKFLOWADMIN: {
    CATEGORIES: {
      SYSTEM_LOCK_TITLE:
        "perc.ui.workflowadmin.categories.system_lock_title@System Category (Read-Only)",
      HIERARCHY_TREE: "perc.ui.workflowadmin.categories.hierarchy_tree@Hierarchy Tree",
      EMPTY: "perc.ui.workflowadmin.categories.empty@No categories available.",
    },
    ROLE: {
      DESCRIPTION: "perc.ui.workflowadmin.role.description@Description",
      NO_USERS_ASSIGNED:
        "perc.ui.workflowadmin.role.no_users_assigned@No users assigned",
      NO_AVAILABLE_USERS:
        "perc.ui.workflowadmin.role.no_available_users@No available users",
    },
    STEPS: {
      MOVE_UP: "perc.ui.workflowadmin.steps.move_up@Move Up",
      MOVE_DOWN: "perc.ui.workflowadmin.steps.move_down@Move Down",
    },
    WORKFLOW: {
      SITE_ASSIGN: {
        SITE_LABEL:
          "perc.ui.workflowadmin.workflow.siteassign.site_label@Site",
      },
    },
  },

  WORKFLOWACTIONS: {
    PANEL: {
      LOADING: "perc.ui.workflowactions.panel.loading@Loading workflow status...",
      CURRENT_STATE_LABEL:
        "perc.ui.workflowactions.panel.current_state_label@CURRENT STATE",
      AVAILABLE_ACTIONS_LABEL:
        "perc.ui.workflowactions.panel.available_actions_label@AVAILABLE ACTIONS",
      NO_TRANSITIONS:
        "perc.ui.workflowactions.panel.no_transitions@No transitions available.",
    },
    ADHOC_SEARCH: {
      PLACEHOLDER:
        "perc.ui.workflowactions.adhocsearch.placeholder@Search users to add...",
    },
  },

  ADMIN: {
    SHELL: {
      TASK: {
        LABEL_CRON: "perc.ui.admin.shell.task@Cron:",
        LABEL_CLASS: "perc.ui.admin.shell.task@Class:",
        LABEL_SUBJECT: "perc.ui.admin.shell.task@Subject:",
        PLACEHOLDER_CLASS_NAME:
          "perc.ui.admin.shell.task@Enter fully-qualified class name",
        BUTTON_EDIT: "perc.ui.admin.shell.task@Edit",
        BUTTON_DELETE: "perc.ui.admin.shell.task@Delete",
        OPTIONS: {
          PURGE_SCHEDULED_TASK_LOG:
            "perc.ui.admin.shell.task.option@Purge Scheduled Task Log",
          RUN_EDITION: "perc.ui.admin.shell.task.option@Run Edition",
          PURGE_REVISIONS: "perc.ui.admin.shell.task.option@Purge Revisions",
          RUN_COMMAND: "perc.ui.admin.shell.task.option@Run Command",
          PURGE_PUBLISHING_LOG:
            "perc.ui.admin.shell.task.option@Purge Publishing Log",
          PURGE_EXPIRED_LOG:
            "perc.ui.admin.shell.task.option@Purge Expired Log",
          CUSTOM: "perc.ui.admin.shell.task.option@Custom...",
        },
      },
    },
    TOOLS: {
      CONSISTENCY_CHECKER: {
        TITLE: "perc.ui.admin.tools.consistencychecker@System Consistency Checker",
        BUTTON_RUN:
          "perc.ui.admin.tools.consistencychecker@Run Consistency Check",
        LABEL_STATUS: "perc.ui.admin.tools.consistencychecker@Status:",
        SECTION_REPORTED_ISSUES:
          "perc.ui.admin.tools.consistencychecker@Reported Issues",
        EMPTY_ISSUES:
          "perc.ui.admin.tools.consistencychecker@No consistency issues found. System is fully aligned.",
      },
    },
  },

  CONTENTEXPLORER: {
    COPY_CONFIRM: {
      SOURCE: "perc.ui.contentexplorer.copyconfirm@Source",
      TARGET: "perc.ui.contentexplorer.copyconfirm@Target",
    },
    SITE_COPY: {
      WORKFLOWS: "perc.ui.contentexplorer.sitecopy@Workflows",
      TEMPLATES: "perc.ui.contentexplorer.sitecopy@Templates",
    },
    RELATIONSHIPS: {
      SUPPLEMENTARY_LINKS:
        "perc.ui.contentexplorer.relationships@Supplementary links",
    },
  },

  CONTENTBROWSER: {
    SEARCH_PLACEHOLDER: "perc.ui.contentbrowser@Search...",
  },

  APP: {
    SHELL: {
      TEMPORARY_NAV: "perc.ui.app.shell@Temporary navigation",
      UNAVAILABLE_TITLE: "perc.ui.home.modern@Unavailable",
    },
  },

  DEVELOPER: {
    COMM_BACK_ARIA: "perc.ui.developer@Back to communities list",
    COMM_ROLES_SAVE_ARIA: "perc.ui.developer@Save community roles",
    SHELL_NAV_ARIA: "perc.ui.developer@Developer sections",
    KW_BACK_ARIA: "perc.ui.developer@Back to keywords list",
    KW_SAVE_ARIA: "perc.ui.developer@Save keyword",
    KW_DELETE_ARIA: "perc.ui.developer@Delete keyword",
    ACL_SAVE_ARIA: "perc.ui.developer@Save object ACL",
    SLOT_BACK_ARIA: "perc.ui.developer@Back to slots list",
    SLOT_ASSOC_ADD_ARIA: "perc.ui.developer@Add slot association",
    TPL_BACK_ARIA: "perc.ui.developer@Back to templates list",
  },
} as const;