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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TMX message keys for the modern Content Explorer (FR-026).
 *
 * <p>Keys follow the product prefix {@code perc.ui.explorer.*}; actual
 * catalog entries are added in {@code modules/perc-i18n/.../CmsUi.tmx}
 * via T023 when the messages stabilize. Until then, the keys fall back
 * to themselves via the thin {@link message} wrapper.</p>
 */

export const EXPLORER_MSG = {
  TITLE: "perc.ui.explorer@Content Explorer",
  TREE_LOADING: "perc.ui.explorer@Loading folders",
  TREE_EMPTY: "perc.ui.explorer@No folders available",
  TREE_LOAD_ERROR: "perc.ui.explorer@Failed to load folders",
  LIST_LOADING: "perc.ui.explorer@Loading items",
  LIST_EMPTY: "perc.ui.explorer@No items in this folder",
  LIST_LOAD_ERROR: "perc.ui.explorer@Failed to load items",
  COL_NAME: "perc.ui.explorer@Name",
  COL_TYPE: "perc.ui.explorer@Type",
  COL_PATH: "perc.ui.explorer@Path",
  COL_MODIFIED: "perc.ui.explorer@Modified",
  COL_TITLE: "perc.ui.explorer@Title",
  COL_CATEGORY: "perc.ui.explorer@Category",
  COL_WORKFLOW: "perc.ui.explorer@Workflow",
  ACTION_OPEN: "perc.ui.explorer@Open",
  ACTION_PREVIEW: "perc.ui.explorer@Preview",
  /** View residual: reload detail list for the current folder (#2733). */
  ACTION_REFRESH: "perc.ui.explorer@Refresh",
  ACTION_REFRESH_ARIA: "perc.ui.explorer@Refresh the current folder list",
  PREVIEW_UNAVAILABLE:
    "perc.ui.explorer@Preview is not available for this item",
  PREVIEW_OPEN_ERROR: "perc.ui.explorer@Could not open preview",
  ACTION_CREATE_FOLDER: "perc.ui.explorer@Create Folder",
  ACTION_RENAME: "perc.ui.explorer@Rename",
  ACTION_MOVE: "perc.ui.explorer@Move",
  ACTION_COPY: "perc.ui.explorer@Copy",
  ACTION_DELETE: "perc.ui.explorer@Delete",
  CONFIRM_DELETE_TITLE: "perc.ui.explorer@Delete Confirmation",
  CONFIRM_DELETE_BODY:
    "perc.ui.explorer@Are you sure you want to delete this item?",
  CONFIRM_OK: "perc.ui.explorer@OK",
  CONFIRM_CANCEL: "perc.ui.explorer@Cancel",
  PERMISSION_DENIED:
    "perc.ui.explorer@You do not have permission to perform this action",
  SESSION_EXPIRED:
    "perc.ui.explorer@Your session has expired. Please log in again.",
  RETRY: "perc.ui.explorer@Retry",
  PROMPT_NEW_FOLDER_NAME: "perc.ui.explorer@Enter new folder name",
  PROMPT_NEW_NAME: "perc.ui.explorer@Enter new name",
  ERROR_GENERIC: "perc.ui.explorer@Something went wrong",
  BOOTSTRAP_UNAVAILABLE:
    "perc.ui.explorer@Content Explorer could not start because the application session is not available. Reload the page or sign in again.",

  // US7 P-Adv / clipboard / wizards / dependency / relationships (FR-021–FR-029, SC-011)
  CLIPBOARD_TITLE: "perc.ui.explorer@Clipboard",
  CLIPBOARD_MODE_LABEL: "perc.ui.explorer@Clipboard mode",
  CLIPBOARD_MODE_COPY: "perc.ui.explorer@Copy",
  CLIPBOARD_MODE_CUT: "perc.ui.explorer@Cut",
  CLIPBOARD_ADD: "perc.ui.explorer@Add to clipboard",
  CLIPBOARD_CLEAR: "perc.ui.explorer@Clear clipboard",
  CLIPBOARD_PASTE: "perc.ui.explorer@Paste",
  CLIPBOARD_EMPTY: "perc.ui.explorer@Clipboard is empty",
  CLIPBOARD_PASTE_TARGET_REQUIRED:
    "perc.ui.explorer@Select a destination folder before pasting",

  WIZARD_NEXT: "perc.ui.explorer@Next",
  WIZARD_BACK: "perc.ui.explorer@Back",
  WIZARD_CANCEL: "perc.ui.explorer@Cancel",
  WIZARD_SUBMIT: "perc.ui.explorer@Run",
  WIZARD_FINISH: "perc.ui.explorer@Finish",
  WIZARD_STEP: "perc.ui.explorer@Step",
  WIZARD_OF: "perc.ui.explorer@of",
  WIZARD_ERROR: "perc.ui.explorer@The wizard could not be completed",

  SITE_COPY_TITLE: "perc.ui.explorer@Site Copy",
  SITE_COPY_STEP_SOURCE: "perc.ui.explorer@Source site",
  SITE_COPY_STEP_TARGET: "perc.ui.explorer@Target site",
  SITE_COPY_STEP_OPTIONS: "perc.ui.explorer@Options",
  SITE_COPY_STEP_CONFIRM: "perc.ui.explorer@Confirm",
  SITE_COPY_STEP_PROGRESS: "perc.ui.explorer@Progress",
  /** Product shell: Content → Site Copy panel region (#2767). */
  SITE_COPY_PANEL_REGION: "perc.ui.explorer@Site copy panel",
  TOGGLE_SITE_COPY_ARIA: "perc.ui.explorer@Show or hide site copy wizard",
  SITE_COPY_SELECT_SITE:
    "perc.ui.explorer@Open a site under Sites to copy it.",

  // Create Site (#3002 / parent #2989 / type picker #3522)
  SITE_CREATE_TITLE: "perc.ui.explorer@Create Site",
  SITE_CREATE_STEP_TYPE: "perc.ui.explorer@Site type",
  SITE_CREATE_STEP_DETAILS: "perc.ui.explorer@Site details",
  SITE_CREATE_STEP_TEMPLATE: "perc.ui.explorer@Base template",
  SITE_CREATE_STEP_CONFIRM: "perc.ui.explorer@Confirm",
  SITE_CREATE_STEP_PROGRESS: "perc.ui.explorer@Progress",
  SITE_CREATE_PANEL_REGION: "perc.ui.explorer@Create site panel",
  TOGGLE_SITE_CREATE_ARIA: "perc.ui.explorer@Show or hide create site wizard",
  SITE_CREATE_NAME_LABEL: "perc.ui.explorer@Site name",
  SITE_CREATE_DESCRIPTION_LABEL: "perc.ui.explorer@Description",
  SITE_CREATE_TEMPLATE_NAME_LABEL: "perc.ui.explorer@Template name",
  SITE_CREATE_BASE_TEMPLATE_LABEL: "perc.ui.explorer@Base template",
  SITE_CREATE_TRADITIONAL_NOTE:
    "perc.ui.explorer@Creates a traditional repository site. Managed navigation is optional. A page template is not required.",
  SITE_CREATE_REPOSITORY_KIND: "perc.ui.explorer@Repository",
  SITE_CREATE_TYPE_LABEL: "perc.ui.explorer@Site type",
  SITE_CREATE_TRADITIONAL: "perc.ui.explorer@Traditional",
  SITE_CREATE_TYPE_PAGE: "perc.ui.explorer@Page",
  SITE_CREATE_TYPE_VIRTUAL: "perc.ui.explorer@Virtual",
  SITE_CREATE_TYPE_UNAVAILABLE:
    "perc.ui.explorer@This site type is not available yet. Choose Traditional to continue.",
  SITE_CREATE_MANAGED_NAV_LABEL: "perc.ui.explorer@Include managed navigation",
  SITE_CREATE_MANAGED_NAV_HELP:
    "perc.ui.explorer@When unchecked, the site folder is created without a NavTree or homepage. You can add navigation later in Explorer. Virtual Sites do not use this option.",
  SITE_CREATE_MANAGED_NAV_YES: "perc.ui.explorer@Yes",
  SITE_CREATE_MANAGED_NAV_NO: "perc.ui.explorer@No",
  SITE_CREATE_TEMPLATES_LOADING: "perc.ui.explorer@Loading base templates…",
  SITE_CREATE_TEMPLATES_ERROR: "perc.ui.explorer@Could not load base templates",
  SITE_CREATE_VALIDATION: "perc.ui.explorer@Enter a valid site name",
  SITE_CREATE_SUBMIT: "perc.ui.explorer@Create site",
  SITE_CREATE_SUBMITTING: "perc.ui.explorer@Creating site…",
  SITE_CREATE_SUCCESS: "perc.ui.explorer@Site {name} created",

  SUBFOLDER_COPY_TITLE: "perc.ui.explorer@Subfolder Copy",
  SUBFOLDER_COPY_STEP_SOURCE: "perc.ui.explorer@Source folder",
  SUBFOLDER_COPY_STEP_TARGET: "perc.ui.explorer@Target folder",
  SUBFOLDER_COPY_STEP_CONFIRM: "perc.ui.explorer@Confirm",
  /** Product shell: Content → Subfolder Copy panel region (#2792). */
  SUBFOLDER_COPY_PANEL_REGION: "perc.ui.explorer@Subfolder copy panel",
  TOGGLE_SUBFOLDER_COPY_ARIA:
    "perc.ui.explorer@Show or hide subfolder copy wizard",
  SUBFOLDER_COPY_SELECT_FOLDER:
    "perc.ui.explorer@Open a folder to copy it to another location.",

  DEPENDENCY_TITLE: "perc.ui.explorer@Dependencies",
  DEPENDENCY_OUTGOING: "perc.ui.explorer@Outgoing relationships",
  DEPENDENCY_INCOMING: "perc.ui.explorer@Incoming relationships",
  DEPENDENCY_AA: "perc.ui.explorer@Active Assembly links",
  DEPENDENCY_TAXONOMY: "perc.ui.explorer@Site / taxonomy edges",
  DEPENDENCY_LOCAL: "perc.ui.explorer@Local dependencies",
  DEPENDENCY_REVERSE: "perc.ui.explorer@Reverse dependencies",
  DEPENDENCY_CLIENT_SIDE_PREVIEW: "perc.ui.explorer@Client-side preview",
  DEPENDENCY_LOADING: "perc.ui.explorer@Loading relationship summary…",
  DEPENDENCY_ERROR: "perc.ui.explorer@Could not load relationship summary",
  /** Shell chrome: View → Dependencies toggle (#2768 / parent #2400). */
  TOGGLE_DEPENDENCIES_ARIA: "perc.ui.explorer@Show or hide dependency viewer",
  DEPENDENCY_PANEL_REGION: "perc.ui.explorer@Dependency viewer panel",
  DEPENDENCY_SELECT_ITEM:
    "perc.ui.explorer@Select a content item to view its dependencies.",

  RELATIONSHIPS_TITLE: "perc.ui.explorer@IA Relationships",
  RELATIONSHIPS_CLIENT_SIDE_PREVIEW:
    "perc.ui.explorer@Client-side preview (full graph pending rest enhancement)",
  RELATIONSHIPS_LOADING: "perc.ui.explorer@Loading IA relationships…",
  RELATIONSHIPS_ERROR: "perc.ui.explorer@Could not load IA relationships",
  /** Product shell: View → IA Relationships panel (#2769 / #2400). */
  TOGGLE_RELATIONSHIPS_ARIA: "perc.ui.explorer@Show or hide IA relationships",
  RELATIONSHIPS_PANEL_REGION: "perc.ui.explorer@IA relationships panel",
  RELATIONSHIPS_SELECT_ITEM:
    "perc.ui.explorer@Select a content item to view IA relationships.",
  // US5 P-Search / search panel (FR-017, FR-018, SC-005)
  SEARCH_TITLE: "perc.ui.explorer@Search",
  SEARCH_PLACEHOLDER: "perc.ui.explorer@Type to search…",
  SEARCH_SUBMIT: "perc.ui.explorer@Search",
  SEARCH_LOADING: "perc.ui.explorer@Searching…",
  SEARCH_EMPTY: "perc.ui.explorer@No results",
  SEARCH_ERROR: "perc.ui.explorer@Search failed",
  SEARCH_OPEN: "perc.ui.explorer@Open",
  SEARCH_REVEAL: "perc.ui.explorer@Reveal in folder",
  SEARCH_PERMISSION_DENIED:
    "perc.ui.explorer@You do not have permission to open this item",
  // Saved / design-search picker (#2506 / #2409 slice C)
  SEARCH_SAVED_LABEL: "perc.ui.explorer@Saved search",
  SEARCH_SAVED_PLACEHOLDER: "perc.ui.explorer@Select a saved search…",
  SEARCH_SAVED_RUN: "perc.ui.explorer@Run saved search",
  SEARCH_SAVED_LOADING: "perc.ui.explorer@Loading saved searches…",
  SEARCH_SAVED_EMPTY: "perc.ui.explorer@No saved searches available",
  SEARCH_SAVED_ERROR: "perc.ui.explorer@Could not load saved searches",
  SEARCH_SAVED_RETRY: "perc.ui.explorer@Retry loading saved searches",
  SEARCH_SAVED_CUSTOM_UNSUPPORTED:
    "perc.ui.explorer@Custom URL searches cannot be run from Explorer",
  DISPLAY_FORMAT_LABEL: "perc.ui.explorer@Display format",
  DISPLAY_FORMAT_DEFAULT: "perc.ui.explorer@Default columns",
  /** Non-fatal catalog load failure — selector stays mounted (#3208). */
  DISPLAY_FORMAT_LOAD_ERROR:
    "perc.ui.explorer@Could not load display formats",
  /** Product shell: server-driven action toolbar (US3 / #2400 / #2972). */
  SERVER_ACTIONS_ARIA: "perc.ui.explorer@Server actions",
  /** Visible chrome label so QA/operators can identify the toolbar region. */
  SERVER_ACTIONS_LABEL: "perc.ui.explorer@Server actions",
  /** Non-fatal load failure for the server action catalog. */
  SERVER_ACTIONS_LOAD_ERROR:
    "perc.ui.explorer@Could not load server actions",
  /** Product shell: view tools row (search / security / display format). */
  VIEW_TOOLS_ARIA: "perc.ui.explorer@Explorer view tools",
  /** DCE-style top menu bar (#2731 / ContentExplorerMenu.xml groups). */
  MENU_BAR_ARIA: "perc.ui.explorer@Explorer menu bar",
  MENU_CONTENT: "perc.ui.explorer@Content",
  MENU_VIEW: "perc.ui.explorer@View",
  MENU_HELP: "perc.ui.explorer@Help",
  MENU_VIEW_REFRESH: "perc.ui.explorer@Refresh",
  MENU_HELP_EXPLORER: "perc.ui.explorer@Content Explorer help",
  MENU_HELP_ABOUT: "perc.ui.explorer@About Content Explorer",
  MENU_HELP_ABOUT_BODY:
    "perc.ui.explorer@Percussion CMS Content Explorer — modern SPA shell (DCE parity program).",
  TOGGLE_SEARCH_ARIA: "perc.ui.explorer@Show or hide search",
  TOGGLE_SECURITY_ARIA: "perc.ui.explorer@Show or hide folder security",
  SEARCH_PANEL_REGION: "perc.ui.explorer@Search panel",
  SECURITY_PANEL_REGION: "perc.ui.explorer@Folder security panel",
  // US4 P-ACL / folder security (FR-014–FR-016, SC-004)
  SECURITY_TITLE: "perc.ui.explorer@Folder Security",
  SECURITY_SELECT_FOLDER:
    "perc.ui.explorer@Open or select a folder to edit security and properties.",
  /** Residual JSP host when folderSecurityModern.jsp has no folderId. */
  SECURITY_HOST_NO_FOLDER:
    "perc.ui.explorer@No folderId supplied. Append ?folderId=<id> to this URL.",
  SECURITY_LOADING: "perc.ui.explorer@Loading permissions",
  SECURITY_LOAD_ERROR: "perc.ui.explorer@Failed to load folder permissions",
  SECURITY_SAVE_SUCCESS: "perc.ui.explorer@Permissions saved",
  SECURITY_SAVE_ERROR: "perc.ui.explorer@Failed to save permissions",
  SECURITY_READ_ONLY:
    "perc.ui.explorer@View-only (you do not have ADMIN access)",
  SECURITY_LOCKOUT_WARNING_TITLE: "perc.ui.explorer@Confirm self-lockout",
  SECURITY_LOCKOUT_WARNING_BODY:
    "perc.ui.explorer@Saving these changes will remove your access to this folder. Continue?",
  SECURITY_LOCKOUT_WARNING_CONFIRM: "perc.ui.explorer@Save anyway",
  SECURITY_LOCKOUT_WARNING_CANCEL: "perc.ui.explorer@Cancel",
  SECURITY_LEVEL_ADMIN: "perc.ui.explorer@Admin",
  SECURITY_LEVEL_WRITE: "perc.ui.explorer@Write",
  SECURITY_LEVEL_READ: "perc.ui.explorer@Read",
  SECURITY_LEVEL_VIEW: "perc.ui.explorer@View",
  SECURITY_PRINCIPAL_REMOVE: "perc.ui.explorer@Remove",
  SECURITY_PRINCIPAL_ADD: "perc.ui.explorer@Add principal",
  SECURITY_PRINCIPAL_NAME_LABEL: "perc.ui.explorer@Principal name",
  /** Folder properties block (community / locale / display format / workflow). */
  FOLDER_PROPS_TITLE: "perc.ui.explorer@Folder properties",
  FOLDER_PROPS_COMMUNITY: "perc.ui.explorer@Community",
  FOLDER_PROPS_COMMUNITY_ID: "perc.ui.explorer@Community id",
  FOLDER_PROPS_LOCALE: "perc.ui.explorer@Locale",
  FOLDER_PROPS_DISPLAY_FORMAT: "perc.ui.explorer@Display format",
  FOLDER_PROPS_WORKFLOW_ID: "perc.ui.explorer@Workflow id",
  // US7 P-Adv / multi-select + clipboard panel (FR-026, #2400 #2408).
  SELECT_COLUMN_HEADER: "perc.ui.explorer@Select",
  SELECT_ROW_LABEL: "perc.ui.explorer@Select item",
  /** Type-icon column (#3328) — folder/open affordance, not a checkbox. */
  ICON_COLUMN_LABEL: "perc.ui.explorer@Item type",
  OPEN_FOLDER_LABEL: "perc.ui.explorer@Open folder",
  FOLDER_ICON_CLOSED: "perc.ui.explorer@Folder",
  FOLDER_ICON_OPEN: "perc.ui.explorer@Open folder",
  ITEM_ICON_LABEL: "perc.ui.explorer@Item",
  SELECT_ALL_LABEL: "perc.ui.explorer@Select all items on this page",
  SELECT_ALL_CLEAR_LABEL: "perc.ui.explorer@Clear all items on this page",
  SELECTED_COUNT_SINGULAR: "perc.ui.explorer@1 item selected",
  SELECTED_COUNT_PLURAL: "perc.ui.explorer@{count} items selected",
  TOGGLE_CLIPBOARD_ARIA: "perc.ui.explorer@Show or hide clipboard",
  CLIPBOARD_REGION: "perc.ui.explorer@Clipboard",
  CLIPBOARD_SUMMARY_ADDED_SINGULAR: "perc.ui.explorer@1 item added to clipboard",
  CLIPBOARD_SUMMARY_ADDED_PLURAL:
    "perc.ui.explorer@{count} items added to clipboard",
  // P-Trans / #2430 — item locales + create-variant (consumes public REST)
  TRANSLATIONS_TITLE: "perc.ui.explorer@Translations",
  TOGGLE_TRANSLATIONS_ARIA: "perc.ui.explorer@Show or hide translations",
  TRANSLATIONS_PANEL_REGION: "perc.ui.explorer@Translations panel",
  TRANSLATIONS_SELECT_ITEM:
    "perc.ui.explorer@Select a content item to view locales and create translation variants.",
  TRANSLATIONS_LOADING: "perc.ui.explorer@Loading translation locales…",
  TRANSLATIONS_ERROR: "perc.ui.explorer@Could not load translation locales",
  TRANSLATIONS_CURRENT_LOCALE: "perc.ui.explorer@Current locale",
  TRANSLATIONS_LOCALE_UNKNOWN: "perc.ui.explorer@Unknown",
  TRANSLATIONS_VARIANTS_HEADING: "perc.ui.explorer@Locale variants",
  TRANSLATIONS_VARIANTS_EMPTY:
    "perc.ui.explorer@No related translation variants",
  TRANSLATIONS_COL_LOCALE: "perc.ui.explorer@Locale",
  TRANSLATIONS_COL_ROLE: "perc.ui.explorer@Role",
  TRANSLATIONS_COL_CONTENT_ID: "perc.ui.explorer@Content id",
  TRANSLATIONS_ROLE_SOURCE: "perc.ui.explorer@Source",
  TRANSLATIONS_ROLE_TRANSLATION: "perc.ui.explorer@Translation",
  TRANSLATIONS_CREATE_HEADING: "perc.ui.explorer@Create translation variant",
  TRANSLATIONS_TARGET_LOCALES: "perc.ui.explorer@Target locales",
  TRANSLATIONS_NO_TARGET_LOCALES:
    "perc.ui.explorer@No additional target locales available",
  TRANSLATIONS_CREATE_ACTION: "perc.ui.explorer@Create variants",
  TRANSLATIONS_CREATING: "perc.ui.explorer@Creating…",
  TRANSLATIONS_SELECT_LOCALE:
    "perc.ui.explorer@Select at least one target locale",
  TRANSLATIONS_INVALID_ITEM:
    "perc.ui.explorer@Selected item does not have a numeric content id",
  TRANSLATIONS_CREATE_ERROR:
    "perc.ui.explorer@Could not create translation variants",
  TRANSLATIONS_CREATE_SUCCESS_SINGULAR:
    "perc.ui.explorer@Created 1 translation variant",
  TRANSLATIONS_CREATE_SUCCESS_PLURAL:
    "perc.ui.explorer@Created {count} translation variants",
  TRANSLATIONS_INFLIGHT_OUT:
    "perc.ui.explorer@In-flight translation queue status is not available (product disposition).",
  // Workflow transitions in Explorer menus (#2732 / parent #2400)
  WORKFLOW_MENU_LABEL: "perc.ui.explorer@Workflow",
  WORKFLOW_TRANSITION_FAILED:
    "perc.ui.explorer@Workflow transition failed",
  // Success path refreshes the list silently (error banner is fail-only).

  // Views catalog tree + run results (#3116 / parent #3110) — not the View menu
  VIEWS_CATEGORY: "perc.ui.explorer@Views",
  VIEWS_GROUP_MY: "perc.ui.explorer@My Content",
  VIEWS_GROUP_COMMUNITY: "perc.ui.explorer@Community Content",
  VIEWS_GROUP_ALL: "perc.ui.explorer@All Content",
  VIEWS_GROUP_OTHER: "perc.ui.explorer@Other Content",
  VIEWS_TREE_REGION: "perc.ui.explorer@Views catalog",
  VIEWS_LOADING: "perc.ui.explorer@Loading views",
  VIEWS_LOAD_ERROR: "perc.ui.explorer@Failed to load views",
  VIEWS_GROUP_EMPTY: "perc.ui.explorer@No views in this group",
  VIEWS_RUN_LOADING: "perc.ui.explorer@Running view…",
  VIEWS_RUN_EMPTY: "perc.ui.explorer@No items in this view",
  VIEWS_RUN_ERROR: "perc.ui.explorer@Failed to run view",
  VIEWS_CUSTOM_UNSUPPORTED:
    "perc.ui.explorer@Custom URL views cannot be run from Explorer",
  VIEWS_INBOX: "perc.ui.explorer@Inbox",
  VIEWS_INBOX_ICON: "perc.ui.explorer@Inbox view",
  VIEWS_RESULTS_REGION: "perc.ui.explorer@View results",

  // Server-action dispatcher (action-execution / stop Data Flow 404s)
  ACTION_EDITOR_UNAVAILABLE:
    "perc.ui.explorer@The content editor is not available in this Explorer release",
  ACTION_UNAVAILABLE:
    "perc.ui.explorer@This action is not available in Content Explorer yet",
  ACTION_NEEDS_ITEM:
    "perc.ui.explorer@Select a content item first",
  ACTION_NEEDS_FOLDER:
    "perc.ui.explorer@Select a folder first",
  ACTION_NEEDS_TYPE:
    "perc.ui.explorer@Choose a content type from New Item",
  ACTION_NEEDS_TEMPLATE:
    "perc.ui.explorer@This page needs a template. Choose a site folder or use Home → Create.",
  ACTION_NEEDS_SLOT:
    "perc.ui.explorer@Select a slot in Active Assembly first",
  ACTION_NEEDS_RELATIONSHIP:
    "perc.ui.explorer@Select an item in the slot first",
  ACTION_SLOT_FAILED:
    "perc.ui.explorer@Could not update the slot",
  TEMPLATE_PICKER_TITLE: "perc.ui.explorer@Choose a page template",
  TEMPLATE_PICKER_LABEL: "perc.ui.explorer@Template",
  CONFIRM_PURGE_BODY:
    "perc.ui.explorer@Permanently delete this item from the system?",
  CONFIRM_PUBLISH_NOW:
    "perc.ui.explorer@Publish this item now?",
  ACTION_COPY_URL_SUCCESS: "perc.ui.explorer@Item URL copied to the clipboard",
  ACTION_COPY_URL_FAILED: "perc.ui.explorer@Could not copy the item URL",
  ACTION_COPY_URL_EMPTY: "perc.ui.explorer@No URL is available for this item",

  REVISIONS_TITLE: "perc.ui.explorer@Revisions",
  REVISIONS_PANEL_REGION: "perc.ui.explorer@Revisions panel",
  REVISIONS_SELECT_ITEM:
    "perc.ui.explorer@Select a content item to view revisions and the audit trail.",
  REVISIONS_LOADING: "perc.ui.explorer@Loading revisions…",
  REVISIONS_ERROR: "perc.ui.explorer@Could not load revisions",
  REVISIONS_EMPTY: "perc.ui.explorer@No revisions are recorded for this item",
  REVISIONS_AUDIT_EMPTY:
    "perc.ui.explorer@No workflow comments are recorded for this item",
  REVISIONS_TABS: "perc.ui.explorer@Revision views",
  REVISIONS_TAB_REVISIONS: "perc.ui.explorer@Revisions",
  REVISIONS_TAB_AUDIT: "perc.ui.explorer@Audit trail",
  REVISIONS_COL_REV: "perc.ui.explorer@Revision",
  REVISIONS_COL_DATE: "perc.ui.explorer@Date",
  REVISIONS_COL_USER: "perc.ui.explorer@User",
  REVISIONS_COL_STATUS: "perc.ui.explorer@Status",
  REVISIONS_COL_ACTIONS: "perc.ui.explorer@Actions",
  REVISIONS_COL_TYPE: "perc.ui.explorer@Transition",
  REVISIONS_COL_COMMENT: "perc.ui.explorer@Comment",
  REVISIONS_RESTORE: "perc.ui.explorer@Restore",
  REVISIONS_RESTORE_ERROR: "perc.ui.explorer@Could not restore that revision",
  CONFIRM_RESTORE_REVISION:
    "perc.ui.explorer@Restore this prior revision as the current revision?",
  CONFIRM_FLUSH_CACHE:
    "perc.ui.explorer@Flush the assembler cache for all items?",
  CONFIRM_NAV_RESET:
    "perc.ui.explorer@Reload managed navigation configuration?",
  CONFIRM_NEW_COPY:
    "perc.ui.explorer@Create a new copy of this item in the same folder?",
  CONFIRM_PROMOTABLE:
    "perc.ui.explorer@Create a promotable version of this item in the same folder?",
  ACTION_FLUSH_OK: "perc.ui.explorer@Assembler cache flushed",
  ACTION_NAV_RESET_OK: "perc.ui.explorer@Managed navigation reset",
  ACTION_NEW_COPY_OK: "perc.ui.explorer@New copy created",
  ACTION_PROMOTABLE_OK: "perc.ui.explorer@Promotable version created",
} as const;
