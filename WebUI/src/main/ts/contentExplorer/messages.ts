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
  ASSEMBLER_PREVIEW_NEEDS_PAGE:
    "perc.ui.explorer@Select a page to open assembler preview",
  ASSEMBLER_PREVIEW_NO_TARGET:
    "perc.ui.explorer@This item has no assembler preview target",
  PREVIEW_OPEN_ERROR: "perc.ui.explorer@Could not open preview",
  ACTION_CREATE_FOLDER: "perc.ui.explorer@Create Folder",
  ACTION_CREATE_FOLDER_NOT_FOUND:
    "perc.ui.explorer@The destination folder was not found",
  ACTION_CREATE_FOLDER_CONFLICT:
    "perc.ui.explorer@Could not create the folder (name in use or destination is not a folder)",
  ACTION_CREATE_FOLDER_INVALID:
    "perc.ui.explorer@Enter a folder name without path separators",
  ACTION_CREATE_PAGE: "perc.ui.explorer@Create Page",
  ACTION_CREATE_PAGE_NAME: "perc.ui.explorer@Page name",
  ACTION_CREATE_PAGE_TYPE: "perc.ui.explorer@Content type",
  ACTION_CREATE_PAGE_NEEDS_NAME: "perc.ui.explorer@Enter a page name",
  ACTION_CREATE_PAGE_NEEDS_TYPE: "perc.ui.explorer@Choose a page content type",
  ACTION_CREATE_PAGE_NO_TYPE:
    "perc.ui.explorer@No page content type is available",
  ACTION_CREATE_PAGE_INVALID:
    "perc.ui.explorer@Enter a page name without path separators",
  ACTION_CREATE_PAGE_NOT_FOUND:
    "perc.ui.explorer@The destination folder was not found",
  ACTION_CREATE_PAGE_CONFLICT:
    "perc.ui.explorer@Could not create the page (name in use or folder is not writable)",
  ACTION_CREATE_PAGE_NO_TEMPLATE:
    "perc.ui.explorer@This page type has no template",
  ACTION_CREATE_ASSET: "perc.ui.explorer@Create Asset",
  ACTION_CREATE_ASSET_NAME: "perc.ui.explorer@Asset name",
  ACTION_CREATE_ASSET_TYPE: "perc.ui.explorer@Asset type",
  ACTION_CREATE_ASSET_NEEDS_NAME: "perc.ui.explorer@Enter an asset name",
  ACTION_CREATE_ASSET_NEEDS_TYPE: "perc.ui.explorer@Choose an asset type",
  ACTION_CREATE_ASSET_NO_TYPE:
    "perc.ui.explorer@No asset type is available",
  ACTION_CREATE_ASSET_INVALID:
    "perc.ui.explorer@Enter an asset name without path separators",
  ACTION_CREATE_ASSET_NOT_FOUND:
    "perc.ui.explorer@The destination folder was not found",
  ACTION_CREATE_ASSET_CONFLICT:
    "perc.ui.explorer@Could not create the asset (name in use or folder is not writable)",
  ACTION_RENAME: "perc.ui.explorer@Rename",
  ACTION_RENAME_NOT_FOUND:
    "perc.ui.explorer@The selected item was not found",
  ACTION_RENAME_CONFLICT:
    "perc.ui.explorer@Could not rename the item (name in use, locked, or not an item)",
  ACTION_RENAME_INVALID: "perc.ui.explorer@That name is not valid",
  FOLDER_RENAME_TITLE: "perc.ui.explorer@Rename folder",
  FOLDER_RENAME_NAME: "perc.ui.explorer@New folder name",
  FOLDER_RENAME_SUBMIT: "perc.ui.explorer@Rename",
  FOLDER_RENAME_CANCEL: "perc.ui.explorer@Cancel",
  FOLDER_RENAME_BLANK: "perc.ui.explorer@Enter a folder name",
  FOLDER_RENAME_INVALID:
    "perc.ui.explorer@Enter a folder name without path separators",
  FOLDER_RENAME_COLLISION:
    "perc.ui.explorer@A folder with that name is already in this folder",
  FOLDER_RENAME_USE_SITE:
    "perc.ui.explorer@Use Rename Site to rename a site. Folder Rename does not rename sites.",
  ACTION_MOVE: "perc.ui.explorer@Move",
  MOVE_DEST_TITLE: "perc.ui.explorer@Move to folder",
  MOVE_DEST_LABEL: "perc.ui.explorer@Destination folder path",
  ACTION_MOVE_NOT_FOUND:
    "perc.ui.explorer@The selected item or destination folder was not found",
  ACTION_MOVE_CONFLICT:
    "perc.ui.explorer@Could not move the item to that folder (destination conflict)",
  ACTION_COPY: "perc.ui.explorer@Copy",
  COPY_DEST_TITLE: "perc.ui.explorer@Copy to folder",
  COPY_DEST_LABEL: "perc.ui.explorer@Destination folder path",
  ACTION_COPY_NOT_FOUND:
    "perc.ui.explorer@The selected item or destination folder was not found",
  MULTI_COPY: "perc.ui.explorer@Copy selected to folder",
  MULTI_COPY_SUCCESS:
    "perc.ui.explorer@Copied {count} item(s) to {path}",
  MULTI_COPY_PARTIAL:
    "perc.ui.explorer@Copied {count} item(s) to {path}; some copies failed and were not rolled back",
  MULTI_COPY_NONE:
    "perc.ui.explorer@No pages or assets were copied",
  MULTI_COPY_SKIPPED: "perc.ui.explorer@Skipped folders: {names}",
  MULTI_COPY_FAILURES: "perc.ui.explorer@Copy failed for: {details}",
  MULTI_MOVE: "perc.ui.explorer@Move selected to folder",
  MULTI_MOVE_SUCCESS:
    "perc.ui.explorer@Moved {count} item(s) to {path}",
  MULTI_MOVE_PARTIAL:
    "perc.ui.explorer@Moved {count} item(s) to {path}; some moves failed and were not rolled back",
  MULTI_MOVE_NONE:
    "perc.ui.explorer@No pages or assets were moved",
  MULTI_MOVE_SKIPPED: "perc.ui.explorer@Skipped folders: {names}",
  MULTI_MOVE_FAILURES: "perc.ui.explorer@Move failed for: {details}",
  MULTI_RECYCLE: "perc.ui.explorer@Recycle selected",
  MULTI_RECYCLE_CONFIRM:
    "perc.ui.explorer@Recycle the checked pages and assets? Folders in the selection are skipped.",
  MULTI_RECYCLE_SUCCESS: "perc.ui.explorer@Recycled {count} item(s)",
  MULTI_RECYCLE_PARTIAL:
    "perc.ui.explorer@Recycled {count} item(s); some recycles failed and were not rolled back",
  MULTI_RECYCLE_NONE: "perc.ui.explorer@No pages or assets were recycled",
  MULTI_RECYCLE_SKIPPED: "perc.ui.explorer@Skipped folders: {names}",
  MULTI_RECYCLE_FAILURES: "perc.ui.explorer@Recycle failed for: {details}",
  MULTI_PURGE: "perc.ui.explorer@Purge selected",
  MULTI_PURGE_CONFIRM:
    "perc.ui.explorer@Permanently purge the selected recycled pages and assets? This cannot be undone.",
  MULTI_PURGE_SUCCESS: "perc.ui.explorer@Purged {count} item(s)",
  MULTI_PURGE_PARTIAL:
    "perc.ui.explorer@Purged {count} item(s); the rest of the selection was not purged",
  MULTI_PURGE_NONE: "perc.ui.explorer@No pages or assets were purged",
  MULTI_PURGE_SKIPPED: "perc.ui.explorer@Skipped folders: {names}",
  MULTI_PURGE_FAILURES: "perc.ui.explorer@Purge failed for: {details}",
  MULTI_RESTORE: "perc.ui.explorer@Restore selected",
  MULTI_RESTORE_CONFIRM:
    "perc.ui.explorer@Restore the checked pages, assets, and folders to their original folders?",
  MULTI_RESTORE_SUCCESS: "perc.ui.explorer@Restored {count} item(s)",
  MULTI_RESTORE_PARTIAL:
    "perc.ui.explorer@Restored {count} item(s); the rest of the selection was not restored",
  MULTI_RESTORE_NONE: "perc.ui.explorer@No items were restored",
  MULTI_RESTORE_FAILURES: "perc.ui.explorer@Restore failed for: {details}",
  ACTION_DELETE: "perc.ui.explorer@Delete",
  ACTION_DELETE_NOT_FOUND:
    "perc.ui.explorer@The selected item was not found",
  ACTION_DELETE_CONFLICT:
    "perc.ui.explorer@Could not delete the item (in use, locked, or not a recyclable item)",
  ACTION_RESTORE: "perc.ui.explorer@Restore",
  ACTION_RESTORE_NOT_FOUND:
    "perc.ui.explorer@The recycled item was not found",
  ACTION_RESTORE_CONFLICT:
    "perc.ui.explorer@Could not restore the item (destination already has that name)",
  ACTION_EMPTY_RECYCLE: "perc.ui.explorer@Empty recycle bin",
  ACTION_EMPTY_RECYCLE_NOT_FOUND:
    "perc.ui.explorer@The recycle bin was not found",
  ACTION_EMPTY_RECYCLE_CONFLICT:
    "perc.ui.explorer@Could not empty the recycle bin (one or more items could not be purged)",
  CONFIRM_EMPTY_RECYCLE:
    "perc.ui.explorer@Permanently empty the recycle bin? This cannot be undone.",
  ACTION_PURGE: "perc.ui.explorer@Purge",
  ACTION_PURGE_NOT_FOUND:
    "perc.ui.explorer@The recycled item was not found",
  ACTION_PURGE_CONFLICT:
    "perc.ui.explorer@Could not purge the item (in use or locked)",
  CONFIRM_PURGE:
    "perc.ui.explorer@Permanently purge this recycled item? This cannot be undone.",
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

  SITE_RENAME_TITLE: "perc.ui.explorer@Rename Site",
  TOGGLE_SITE_RENAME_ARIA: "perc.ui.explorer@Show or hide rename site",
  SITE_RENAME_PANEL_REGION: "perc.ui.explorer@Rename site panel",
  SITE_RENAME_SELECT_SITE:
    "perc.ui.explorer@Open a site under Sites to rename it.",
  SITE_RENAME_NAME_LABEL: "perc.ui.explorer@New site name",
  SITE_RENAME_SUBMIT: "perc.ui.explorer@Rename",
  SITE_RENAME_CANCEL: "perc.ui.explorer@Cancel",
  SITE_RENAME_ERROR_400:
    "perc.ui.explorer@That site name is not valid. Use letters, digits, spaces, hyphens, or underscores.",
  SITE_RENAME_ERROR_403:
    "perc.ui.explorer@You do not have permission to rename this site.",
  SITE_RENAME_ERROR_409:
    "perc.ui.explorer@A site or site folder already uses that name.",
  SITE_RENAME_ERROR_GENERIC:
    "perc.ui.explorer@The site could not be renamed.",
  SITE_COPY_COMPLETED: "perc.ui.explorer@Site copy completed",
  SITE_COPY_HTTP_400:
    "perc.ui.explorer@Site copy was rejected (HTTP 400). Check the source site and target name.",
  SITE_COPY_HTTP_403:
    "perc.ui.explorer@You do not have permission to copy this site (HTTP 403).",
  SITE_COPY_HTTP_409:
    "perc.ui.explorer@Site copy conflicts with an existing site or a copy already in progress (HTTP 409).",

  // Create Site (#3002 / parent #2989 / type picker #3512 / Virtual #3521)
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
  SITE_CREATE_PAGE_NOTE:
    "perc.ui.explorer@Creates a page-based site. Managed navigation is required. Choose a page template on the next step.",
  SITE_CREATE_VIRTUAL_NOTE:
    "perc.ui.explorer@Creates a Virtual site. Managed navigation and a page template are not used. Git source path can be set here or later on Developer Sites.",
  SITE_CREATE_VIRTUAL_SOURCE_NOTE:
    "perc.ui.explorer@Git source (root path, config file) is configured on Developer → Sites after create if you skip the optional path below.",
  SITE_CREATE_VIRTUAL_ROOT_LABEL:
    "perc.ui.explorer@Git root path (optional)",
  SITE_CREATE_VIRTUAL_ROOT_HELP:
    "perc.ui.explorer@Optional filesystem path for virtual.sourceKind git-filesystem. Leave blank to finish source settings on Developer → Sites.",
  SITE_CREATE_VIRTUAL_ROOT_UNSAFE:
    "perc.ui.explorer@Root path must not contain .. segments.",
  SITE_CREATE_REPOSITORY_KIND: "perc.ui.explorer@Repository",
  SITE_CREATE_TYPE_LABEL: "perc.ui.explorer@Site type",
  SITE_CREATE_TRADITIONAL: "perc.ui.explorer@Traditional",
  SITE_CREATE_TYPE_PAGE: "perc.ui.explorer@Page",
  SITE_CREATE_TYPE_VIRTUAL: "perc.ui.explorer@Virtual",
  SITE_CREATE_MANAGED_NAV_LABEL: "perc.ui.explorer@Include managed navigation",
  SITE_CREATE_MANAGED_NAV_HELP:
    "perc.ui.explorer@When unchecked, the site folder is created without a NavTree or homepage. You can add navigation later in Explorer. Virtual Sites do not use this option.",
  SITE_CREATE_MANAGED_NAV_REQUIRED:
    "perc.ui.explorer@Page sites always include managed navigation.",
  SITE_CREATE_MANAGED_NAV_YES: "perc.ui.explorer@Yes",
  SITE_CREATE_MANAGED_NAV_NO: "perc.ui.explorer@No",
  SITE_CREATE_TEMPLATES_LOADING: "perc.ui.explorer@Loading base templates…",
  SITE_CREATE_TEMPLATES_ERROR: "perc.ui.explorer@Could not load base templates",
  SITE_CREATE_VALIDATION: "perc.ui.explorer@Enter a valid site name",
  SITE_CREATE_VALIDATION_PAGE:
    "perc.ui.explorer@Enter a valid site name, template name, and base template",
  SITE_CREATE_FORBIDDEN:
    "perc.ui.explorer@You do not have permission to create a site",
  SITE_CREATE_INVALID:
    "perc.ui.explorer@Could not create the site (invalid name or existing NavTree)",
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
  SUBFOLDER_COPY_BAD_REQUEST:
    "perc.ui.explorer@Folder copy needs a source folder and a destination folder (HTTP 400)",
  SUBFOLDER_COPY_FORBIDDEN:
    "perc.ui.explorer@You do not have permission to copy this folder (HTTP 403)",
  SUBFOLDER_COPY_NOT_FOUND:
    "perc.ui.explorer@The source or destination folder was not found (HTTP 404). The source folder was not deleted.",
  SUBFOLDER_COPY_CONFLICT:
    "perc.ui.explorer@Could not copy the folder (destination conflict, HTTP 409). The source folder was not deleted.",

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
  DEPENDENCY_NOT_FOUND: "perc.ui.explorer@This item was not found.",
  DEPENDENCY_EMPTY: "perc.ui.explorer@No known dependencies for this item.",
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
  RELATIONSHIPS_REMOVE: "perc.ui.explorer@Remove",
  RELATIONSHIPS_REMOVE_CONFIRM:
    "perc.ui.explorer@Remove this relationship from the selected item?",
  RELATIONSHIPS_REMOVE_CANCEL: "perc.ui.explorer@Cancel",
  RELATIONSHIPS_REMOVE_DO: "perc.ui.explorer@Remove relationship",
  RELATIONSHIPS_REMOVED: "perc.ui.explorer@Relationship removed.",
  RELATIONSHIPS_REMOVE_EMPTY:
    "perc.ui.explorer@No removable relationships for this item.",
  RELATIONSHIPS_REMOVE_FAILED_400:
    "perc.ui.explorer@The relationship could not be removed.",
  RELATIONSHIPS_REMOVE_FAILED_403:
    "perc.ui.explorer@You do not have permission to remove this relationship.",
  RELATIONSHIPS_REMOVE_FAILED_409:
    "perc.ui.explorer@This relationship cannot be removed.",
  RELATIONSHIPS_REMOVE_ALL: "perc.ui.explorer@Remove all relationships",
  RELATIONSHIPS_REMOVE_ALL_CONFIRM:
    "perc.ui.explorer@Remove every owned relationship on the selected item? Folder relationships stay.",
  RELATIONSHIPS_REMOVED_ALL: "perc.ui.explorer@Relationships removed.",
  RELATIONSHIPS_ADD: "perc.ui.explorer@Add relationship",
  RELATIONSHIPS_ADD_TARGET: "perc.ui.explorer@Target item id",
  RELATIONSHIPS_ADD_TYPE: "perc.ui.explorer@Relationship type",
  RELATIONSHIPS_ADD_DO: "perc.ui.explorer@Add relationship",
  RELATIONSHIPS_ADDED: "perc.ui.explorer@Relationship added.",
  RELATIONSHIPS_ADD_FAILED_400:
    "perc.ui.explorer@The relationship could not be added.",
  RELATIONSHIPS_ADD_FAILED_403:
    "perc.ui.explorer@You do not have permission to add this relationship.",
  RELATIONSHIPS_ADD_FAILED_409:
    "perc.ui.explorer@This relationship cannot be added.",
  RELATIONSHIPS_MOVE_UP: "perc.ui.explorer@Move up",
  RELATIONSHIPS_MOVE_DOWN: "perc.ui.explorer@Move down",
  RELATIONSHIPS_MOVE_UP_CONFIRM:
    "perc.ui.explorer@Move this Active Assembly relationship up one position?",
  RELATIONSHIPS_MOVE_DOWN_CONFIRM:
    "perc.ui.explorer@Move this Active Assembly relationship down one position?",
  RELATIONSHIPS_MOVE_DO: "perc.ui.explorer@Move relationship",
  RELATIONSHIPS_MOVED: "perc.ui.explorer@Relationship moved.",
  RELATIONSHIPS_MOVE_FAILED_400:
    "perc.ui.explorer@The relationship could not be moved.",
  RELATIONSHIPS_MOVE_FAILED_403:
    "perc.ui.explorer@You do not have permission to move this relationship.",
  RELATIONSHIPS_MOVE_FAILED_409:
    "perc.ui.explorer@This relationship cannot be moved.",
  RELATIONSHIPS_OPEN: "perc.ui.explorer@Open",
  RELATIONSHIPS_OPEN_CONFIRM:
    "perc.ui.explorer@Open this related item in the editor?",
  RELATIONSHIPS_OPEN_DO: "perc.ui.explorer@Open related item",
  RELATIONSHIPS_OPENED:
    "perc.ui.explorer@Opened the related item in the editor.",
  RELATIONSHIPS_OPEN_FOLDER:
    "perc.ui.explorer@Folders are not opened in the editor",
  RELATIONSHIPS_OPEN_NO_CONTENT:
    "perc.ui.explorer@This relationship has no content id",
  RELATIONSHIPS_OPEN_FORBIDDEN:
    "perc.ui.explorer@You do not have permission to open this related item (HTTP 403)",
  RELATIONSHIPS_OPEN_NOT_FOUND:
    "perc.ui.explorer@This related item was not found (HTTP 404)",
  RELATIONSHIPS_OPEN_FAILED:
    "perc.ui.explorer@Could not open this related item",
  RELATIONSHIPS_CHANGE_TEMPLATE: "perc.ui.explorer@Change template",
  RELATIONSHIPS_CHANGE_TEMPLATE_TITLE: "perc.ui.explorer@Change snippet template",
  RELATIONSHIPS_CHANGE_TEMPLATE_LABEL: "perc.ui.explorer@Snippet template",
  RELATIONSHIPS_CHANGE_TEMPLATE_PLACEHOLDER:
    "perc.ui.explorer@Choose a snippet template",
  RELATIONSHIPS_CHANGE_TEMPLATE_DO: "perc.ui.explorer@Change snippet template",
  RELATIONSHIPS_TEMPLATE_CHANGED: "perc.ui.explorer@Snippet template changed.",
  RELATIONSHIPS_TEMPLATE_FAILED_400:
    "perc.ui.explorer@The snippet template could not be changed (HTTP 400)",
  RELATIONSHIPS_TEMPLATE_FAILED_403:
    "perc.ui.explorer@You are not allowed to change this snippet template (HTTP 403)",
  RELATIONSHIPS_TEMPLATE_FAILED_409:
    "perc.ui.explorer@This snippet template could not be saved (HTTP 409)",
  RELATIONSHIPS_TEMPLATE_NEEDS:
    "perc.ui.explorer@Choose a snippet template before applying.",
  RELATIONSHIPS_TEMPLATE_SAME:
    "perc.ui.explorer@Choose a different snippet template. The current template is not saved again",
  RELATIONSHIPS_TEMPLATE_FOLDER:
    "perc.ui.explorer@Folder relationships do not have a snippet template",
  RELATIONSHIPS_TEMPLATE_NOT_ASSEMBLY:
    "perc.ui.explorer@Only an Active Assembly relationship can change its snippet template",
  RELATIONSHIPS_TEMPLATE_NO_SLOT:
    "perc.ui.explorer@This relationship has no slot for a snippet template",
  RELATIONSHIPS_TEMPLATE_NOT_ALLOWED:
    "perc.ui.explorer@That snippet template is not allowed for this slot",
  RELATIONSHIPS_TEMPLATE_FAILED:
    "perc.ui.explorer@Could not change the snippet template",
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
    "perc.ui.explorer@Custom URL views cannot be run from the saved-search list",
  DISPLAY_FORMAT_LABEL: "perc.ui.explorer@Display format",
  DISPLAY_FORMAT_DEFAULT: "perc.ui.explorer@Default columns",
  /** Non-fatal catalog load failure — selector stays mounted (#3208). */
  DISPLAY_FORMAT_LOAD_ERROR:
    "perc.ui.explorer@Could not load display formats",
  LIST_COLUMNS_OPEN: "perc.ui.explorer@Columns",
  LIST_COLUMNS_APPLY: "perc.ui.explorer@Apply columns",
  LIST_COLUMNS_NEED_FOLDER: "perc.ui.explorer@Select a folder before changing columns (HTTP 400)",
  LIST_COLUMNS_SAVE_ERROR: "perc.ui.explorer@Could not save list columns",
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
  ITEM_PROPS_TITLE: "perc.ui.explorer@Item properties",
  TOGGLE_ITEM_PROPS_ARIA: "perc.ui.explorer@Show or hide item properties",
  ITEM_PROPS_PANEL_REGION: "perc.ui.explorer@Item properties panel",
  ITEM_PROPS_SELECT_ITEM:
    "perc.ui.explorer@Select a page, file, or asset to view or save properties.",
  ITEM_PROPS_LOADING: "perc.ui.explorer@Loading item properties",
  ITEM_PROPS_NAME: "perc.ui.explorer@Name",
  ITEM_PROPS_DISPLAY_TITLE: "perc.ui.explorer@Display title",
  ITEM_PROPS_SAVE: "perc.ui.explorer@Save properties",
  ITEM_PROPS_SAVE_SUCCESS: "perc.ui.explorer@Properties saved",
  ITEM_PROPS_READ_ONLY:
    "perc.ui.explorer@View-only (you do not have write access)",
  ITEM_PROPS_BAD_REQUEST:
    "perc.ui.explorer@Name is required to save item properties.",
  ITEM_PROPS_NOT_FOUND: "perc.ui.explorer@Item not found.",
  ITEM_PROPS_CONFLICT:
    "perc.ui.explorer@Could not save properties (folder selected, name in use, or item locked).",
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
  TRANSLATIONS_NOT_FOUND:
    "perc.ui.explorer@Item not found",
  TRANSLATIONS_CONFLICT:
    "perc.ui.explorer@A translation already exists for that locale",
  TRANSLATIONS_OPEN_VARIANT: "perc.ui.explorer@Open locale copy",
  TRANSLATIONS_OPEN_CONFIRM:
    "perc.ui.explorer@Open this translation variant in the editor?",
  TRANSLATIONS_OPEN_CANCEL: "perc.ui.explorer@Cancel",
  TRANSLATIONS_OPENED:
    "perc.ui.explorer@Opened the translation variant in the editor",
  TRANSLATIONS_OPEN_NO_VARIANT:
    "perc.ui.explorer@No translation variant to open",
  TRANSLATIONS_OPEN_FOLDER:
    "perc.ui.explorer@Folders are not opened as translation variants",
  TRANSLATIONS_OPEN_FORBIDDEN:
    "perc.ui.explorer@You do not have permission to open this translation variant (HTTP 403)",
  TRANSLATIONS_OPEN_NOT_FOUND:
    "perc.ui.explorer@This translation variant was not found (HTTP 404)",
  TRANSLATIONS_OPEN_FAILED:
    "perc.ui.explorer@Could not open this translation variant",
  TRANSLATIONS_COL_OPEN: "perc.ui.explorer@Open",
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
  WORKFLOW_TRANSITION_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to run this workflow transition (HTTP 403)",
  WORKFLOW_TRANSITION_REJECTED:
    "perc.ui.explorer@This workflow transition was rejected (HTTP 400)",
  WORKFLOW_TRANSITION_CONFLICT:
    "perc.ui.explorer@This workflow transition conflicts with the item state (HTTP 409)",
  WORKFLOW_COMMENT_REQUIRED:
    "perc.ui.explorer@A comment is required for this workflow transition",
  WORKFLOW_COMMENT_PROMPT:
    "perc.ui.explorer@Enter a comment for this workflow transition",
  CONFIRM_WORKFLOW_MULTI:
    "perc.ui.explorer@Apply {trigger} to {count} selected items? Folders in the selection are skipped.",
  WORKFLOW_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not transitioned: {names}",
  WORKFLOW_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not workflow items and were not transitioned: {names}",
  WORKFLOW_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was transitioned. {detail}",
  WORKFLOW_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can take this workflow transition. Folders are skipped.",
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
  VIEWS_PAGE_REGION: "perc.ui.explorer@View results pages",
  VIEWS_PAGE_NEXT: "perc.ui.explorer@Next",
  VIEWS_PAGE_PREVIOUS: "perc.ui.explorer@Previous",

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
  TYPE_PICKER_TITLE: "perc.ui.explorer@Choose a content type",
  TYPE_PICKER_LABEL: "perc.ui.explorer@Content type",
  ACTION_NEEDS_TEMPLATE:
    "perc.ui.explorer@This page needs a template. Choose a site folder or use Home → Create.",
  ACTION_NEEDS_SLOT:
    "perc.ui.explorer@Select a slot in Active Assembly first",
  ACTION_NEEDS_RELATIONSHIP:
    "perc.ui.explorer@Select an item in the slot first",
  ACTION_SLOT_FAILED:
    "perc.ui.explorer@Could not update the slot",
  CONFIRM_SLOT_REMOVE:
    "perc.ui.explorer@Remove this item from the slot?",
  TEMPLATE_PICKER_TITLE: "perc.ui.explorer@Choose a page template",
  TEMPLATE_PICKER_LABEL: "perc.ui.explorer@Template",
  CONFIRM_PURGE_BODY:
    "perc.ui.explorer@Permanently delete this item from the system?",
  CONFIRM_PUBLISH_NOW:
    "perc.ui.explorer@Publish this item now?",
  CONFIRM_PUBLISH_FOLDER:
    "perc.ui.explorer@Publish the pages and assets in this folder now? Nested folders are not included.",
  PUBLISH_FOLDER_EMPTY:
    "perc.ui.explorer@This folder has no pages or assets to publish.",
  CONFIRM_PUBLISH_NOW_MULTI:
    "perc.ui.explorer@Publish {count} selected items now? Folders in the selection are not published.",
  PUBLISH_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not published: {names}",
  PUBLISH_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not published: {names}",
  PUBLISH_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was published. {detail}",
  PUBLISH_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be published. Folders are not published.",
  CONFIRM_FORCE_CHECKIN:
    "perc.ui.explorer@Force check-in this item? Other users will lose their checkout.",
  CONFIRM_FORCE_CHECKIN_MULTI:
    "perc.ui.explorer@Force check-in {count} selected items? Other users will lose their checkout. Folders in the selection are not force checked in.",
  FORCE_CHECKIN_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not force checked in: {names}",
  FORCE_CHECKIN_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not force checked in: {names}",
  FORCE_CHECKIN_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was force checked in. {detail}",
  FORCE_CHECKIN_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be force checked in. Folders are not force checked in.",
  FORCE_CHECKIN_FORBIDDEN:
    "perc.ui.explorer@Admin assignment is required to force check-in",
  FORCE_CHECKIN_NOT_FOUND:
    "perc.ui.explorer@Item not found",
  FORCE_CHECKIN_NOT_CHECKED_OUT:
    "perc.ui.explorer@Item is not checked out",
  CHECKOUT_OWNER_LABEL: "perc.ui.explorer@Checked out by",
  CHECKOUT_OWNER_NONE: "perc.ui.explorer@Not checked out",
  CHECKOUT_OWNER_LOADING: "perc.ui.explorer@Loading checkout owner",
  CHECKOUT_OWNER_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to see who has this item checked out",
  CHECKOUT_OWNER_FAILED:
    "perc.ui.explorer@Could not load who has this item checked out",
  CHECKOUT_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to check out this item",
  CHECKOUT_CONFLICT:
    "perc.ui.explorer@Item is checked out to another user",
  CONFIRM_CHECKOUT_MULTI:
    "perc.ui.explorer@Check out {count} selected items? Folders in the selection are not checked out.",
  CHECKOUT_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not checked out: {names}",
  CHECKOUT_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not checked out: {names}",
  CHECKOUT_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was checked out. {detail}",
  CHECKOUT_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be checked out. Folders are not checked out.",
  CHECKIN_COMMENT_TITLE: "perc.ui.explorer@Revision comment",
  CHECKIN_COMMENT_HINT:
    "perc.ui.explorer@Optional comment stored with this check-in. Leave blank to check in without a comment.",
  CHECKIN_COMMENT_LABEL: "perc.ui.explorer@Revision comment",
  CHECKIN_COMMENT_PROMPT: "perc.ui.explorer@Revision comment (optional)",
  CHECKIN_CONFIRM: "perc.ui.explorer@Check in",
  CHECKIN_CANCEL: "perc.ui.explorer@Cancel",
  CHECKIN_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to check in this item",
  CHECKIN_CONFLICT:
    "perc.ui.explorer@Item is not checked out to you",
  CHECKIN_REJECTED: "perc.ui.explorer@Check-in was rejected",
  CONFIRM_CHECKIN_MULTI:
    "perc.ui.explorer@Check in {count} selected items? Folders in the selection are not checked in.",
  CHECKIN_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not checked in: {names}",
  CHECKIN_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not checked in: {names}",
  CHECKIN_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was checked in. {detail}",
  CHECKIN_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be checked in. Folders are not checked in.",
  CONFIRM_TAKEDOWN:
    "perc.ui.explorer@Take down (unpublish) this item from its site?",
  CONFIRM_TAKEDOWN_LINKED:
    "perc.ui.explorer@These pages link to this item:",
  CONFIRM_TAKEDOWN_MULTI:
    "perc.ui.explorer@Take down {count} selected items from their sites? Folders in the selection are not taken down.",
  TAKEDOWN_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not taken down: {names}",
  TAKEDOWN_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not taken down: {names}",
  TAKEDOWN_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was taken down. {detail}",
  TAKEDOWN_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be taken down. Folders are not taken down.",
  CONFIRM_STAGE:
    "perc.ui.explorer@Stage this item to the staging server?",
  CONFIRM_STAGE_MULTI:
    "perc.ui.explorer@Stage {count} selected items to the staging server? Folders in the selection are not staged.",
  STAGE_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not staged: {names}",
  STAGE_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not staged: {names}",
  STAGE_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was staged. {detail}",
  STAGE_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be staged. Folders are not staged.",
  CONFIRM_REMOVE_FROM_STAGING:
    "perc.ui.explorer@Remove this item from staging?",
  CONFIRM_REMOVE_FROM_STAGING_MULTI:
    "perc.ui.explorer@Remove {count} selected items from staging? Folders in the selection are not removed.",
  UNSTAGE_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not removed from staging: {names}",
  UNSTAGE_SKIPPED_OTHER:
    "perc.ui.explorer@These selected items are not pages or assets and were not removed from staging: {names}",
  UNSTAGE_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item was removed from staging. {detail}",
  UNSTAGE_NOTHING_ELIGIBLE:
    "perc.ui.explorer@Nothing in the selection can be removed from staging. Folders are not removed.",
  CONFIRM_SCHEDULE:
    "perc.ui.explorer@Save schedule publish dates for this item?",
  CONFIRM_SCHEDULE_MULTI:
    "perc.ui.explorer@Save the same schedule publish dates for every selected page and asset? Folders are skipped.",
  SCHEDULE_MULTI_HINT:
    "perc.ui.explorer@These dates apply to every selected page and asset. Folders are skipped.",
  SCHEDULE_PARTIAL:
    "perc.ui.explorer@Schedule was not saved for every selected item.",
  PUBLISHING_HISTORY_TITLE: "perc.ui.explorer@Publishing History",
  SCHEDULE_TITLE: "perc.ui.explorer@Schedule",
  SCHEDULE_PUBLISH_DATE: "perc.ui.explorer@Publish date",
  SCHEDULE_REMOVAL_DATE: "perc.ui.explorer@Removal date",
  SCHEDULE_COMMENTS: "perc.ui.explorer@Comments",
  SCHEDULE_CLEAR: "perc.ui.explorer@Clear dates",
  SCHEDULE_DATES_SAME:
    "perc.ui.explorer@Publish and removal dates cannot be the same",
  SCHEDULE_DATE_RANGE:
    "perc.ui.explorer@Enter a valid date range. Removal must be after publish.",
  CLEAR_SCHEDULE_TITLE: "perc.ui.explorer@Clear scheduled dates",
  CLEAR_SCHEDULE_BODY:
    "perc.ui.explorer@Clear publish and removal dates for this item?",
  CLEAR_SCHEDULE_CONFIRM: "perc.ui.explorer@Clear dates",
  CLEAR_SCHEDULE_CURRENT: "perc.ui.explorer@Current dates",
  CLEAR_SCHEDULE_NONE: "perc.ui.explorer@This item has no scheduled dates.",
  CLEAR_SCHEDULE_SINGLE:
    "perc.ui.explorer@Clear scheduled dates applies to one selected page or asset.",
  CONFIRM_CLEAR_SCHEDULE_MULTI:
    "perc.ui.explorer@Clear start and end publish dates on {count} selected pages and assets? Folders in the selection are not cleared.",
  CLEAR_SCHEDULE_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not cleared: {names}",
  CLEAR_SCHEDULE_PARTIAL:
    "perc.ui.explorer@Publish dates were not cleared for every selected item. {detail}",
  CLEAR_SCHEDULE_NOTHING:
    "perc.ui.explorer@Nothing in the selection can have scheduled dates cleared. Folders are not cleared.",
  CLEAR_SCHEDULE_FAILED: "perc.ui.explorer@Could not clear scheduled dates.",
  ACTION_COPY_URL_SUCCESS: "perc.ui.explorer@Item URL copied to the clipboard",
  ACTION_COPY_URL_FAILED: "perc.ui.explorer@Could not copy the item URL",
  ACTION_COPY_URL_EMPTY: "perc.ui.explorer@No URL is available for this item",
  ACTION_COPY_URL_SKIPPED_FOLDERS:
    "perc.ui.explorer@Folders are not copied: {names}",
  ACTION_COPY_URL_EMPTY_ITEMS:
    "perc.ui.explorer@No URL is available for: {names}",
  ACTION_COPY_URL_BATCH_INCOMPLETE:
    "perc.ui.explorer@Not every selected item URL was copied. {detail}",
  ACTION_COPY_URL_NOTHING:
    "perc.ui.explorer@Nothing in the selection has a URL to copy.",
  COPY_FOLDER_PATH: "perc.ui.explorer@Copy folder path",
  COPY_FOLDER_PATH_ARIA:
    "perc.ui.explorer@Copy the selected folder path to the clipboard",
  CONFIRM_COPY_FOLDER_PATH:
    "perc.ui.explorer@Copy this folder path to the clipboard?",
  COPY_FOLDER_PATH_SUCCESS:
    "perc.ui.explorer@Folder path copied to the clipboard",
  COPY_FOLDER_PATH_EMPTY:
    "perc.ui.explorer@No folder path is available to copy",
  COPY_FOLDER_PATH_FAILED:
    "perc.ui.explorer@Could not copy the folder path",
  SET_WORKFLOW: "perc.ui.explorer@Set workflow",
  SET_WORKFLOW_ARIA:
    "perc.ui.explorer@Set the workflow on the selected pages and assets",
  SET_WORKFLOW_MULTI_NOTE:
    "perc.ui.explorer@This workflow is saved on each selected page and asset. Folders are not changed",
  SET_WORKFLOW_PARTIAL:
    "perc.ui.explorer@Not every selected item had its workflow set: {detail}",
  SET_WORKFLOW_TITLE: "perc.ui.explorer@Set workflow",
  SET_WORKFLOW_LABEL: "perc.ui.explorer@Workflow",
  SET_WORKFLOW_CURRENT: "perc.ui.explorer@current",
  SET_WORKFLOW_SAVE: "perc.ui.explorer@Save workflow",
  SET_WORKFLOW_CANCEL: "perc.ui.explorer@Cancel",
  SET_WORKFLOW_SAVED: "perc.ui.explorer@Workflow saved",
  SET_WORKFLOW_EMPTY:
    "perc.ui.explorer@Select a page or asset before setting its workflow",
  SET_WORKFLOW_FOLDER: "perc.ui.explorer@Folders are not assigned a workflow",
  SET_WORKFLOW_MULTI:
    "perc.ui.explorer@Set workflow applies to one selected page or asset",
  SET_WORKFLOW_NOT_ITEM:
    "perc.ui.explorer@Only a page or asset can have its workflow set",
  SET_WORKFLOW_NO_ID:
    "perc.ui.explorer@No content id is available to set a workflow",
  SET_WORKFLOW_NONE:
    "perc.ui.explorer@This item has no workflows that can be assigned",
  SET_WORKFLOW_UNCHANGED:
    "perc.ui.explorer@Choose a different workflow. The current workflow is not saved again",
  SET_WORKFLOW_FORBIDDEN:
    "perc.ui.explorer@That workflow is not allowed for this item",
  SET_WORKFLOW_BLANK: "perc.ui.explorer@Choose a workflow before saving",
  SET_WORKFLOW_HTTP_400:
    "perc.ui.explorer@Could not set the workflow (HTTP 400)",
  SET_WORKFLOW_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this workflow (HTTP 403)",
  SET_WORKFLOW_HTTP_409:
    "perc.ui.explorer@This item could not change workflow (HTTP 409)",
  SET_WORKFLOW_FAILED: "perc.ui.explorer@Could not set the workflow",
  SET_FOLDER_WORKFLOW: "perc.ui.explorer@Set folder workflow",
  SET_FOLDER_WORKFLOW_ARIA:
    "perc.ui.explorer@Set the workflow on the selected folder",
  SET_FOLDER_WORKFLOW_TITLE: "perc.ui.explorer@Set folder workflow",
  SET_FOLDER_WORKFLOW_LABEL: "perc.ui.explorer@Folder workflow",
  SET_FOLDER_WORKFLOW_CURRENT: "perc.ui.explorer@current",
  SET_FOLDER_WORKFLOW_SAVE: "perc.ui.explorer@Save folder workflow",
  SET_FOLDER_WORKFLOW_CANCEL: "perc.ui.explorer@Cancel",
  SET_FOLDER_WORKFLOW_SAVED: "perc.ui.explorer@Folder workflow saved",
  SET_FOLDER_WORKFLOW_EMPTY:
    "perc.ui.explorer@Select a folder before setting its workflow",
  SET_FOLDER_WORKFLOW_PAGE:
    "perc.ui.explorer@Pages are not given a folder workflow",
  SET_FOLDER_WORKFLOW_ASSET:
    "perc.ui.explorer@Assets are not given a folder workflow",
  SET_FOLDER_WORKFLOW_NOT_FOLDER:
    "perc.ui.explorer@Only a folder can have its folder workflow set",
  SET_FOLDER_WORKFLOW_MULTI:
    "perc.ui.explorer@Set folder workflow applies to one selected folder",
  SET_FOLDER_WORKFLOW_NO_ID:
    "perc.ui.explorer@No folder id is available to set a workflow",
  SET_FOLDER_WORKFLOW_NONE:
    "perc.ui.explorer@No workflows are available to assign to this folder",
  SET_FOLDER_WORKFLOW_UNCHANGED:
    "perc.ui.explorer@Choose a different workflow. The current folder workflow is not saved again",
  SET_FOLDER_WORKFLOW_FORBIDDEN:
    "perc.ui.explorer@That workflow is not in the folder workflow catalog",
  SET_FOLDER_WORKFLOW_BLANK: "perc.ui.explorer@Choose a workflow before saving",
  SET_FOLDER_WORKFLOW_HTTP_400:
    "perc.ui.explorer@Could not set the folder workflow (HTTP 400)",
  SET_FOLDER_WORKFLOW_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this folder workflow (HTTP 403)",
  SET_FOLDER_WORKFLOW_HTTP_409:
    "perc.ui.explorer@This folder workflow could not be saved (HTTP 409)",
  SET_FOLDER_WORKFLOW_FAILED: "perc.ui.explorer@Could not set the folder workflow",
  SET_FOLDER_WORKFLOW_MISMATCH:
    "perc.ui.explorer@The folder workflow did not change after refresh",
  SET_FOLDER_WORKFLOW_MULTI_NOTE:
    "perc.ui.explorer@This workflow is saved on each selected folder. Pages and assets are not changed",
  SET_FOLDER_WORKFLOW_PARTIAL:
    "perc.ui.explorer@Not every selected folder had its workflow set: {detail}",
  SET_FOLDER_COMMUNITY: "perc.ui.explorer@Set folder community",
  SET_FOLDER_COMMUNITY_ARIA:
    "perc.ui.explorer@Set the community on the selected folder",
  SET_FOLDER_COMMUNITY_TITLE: "perc.ui.explorer@Set folder community",
  SET_FOLDER_COMMUNITY_LABEL: "perc.ui.explorer@Folder community",
  SET_FOLDER_COMMUNITY_CURRENT: "perc.ui.explorer@current",
  SET_FOLDER_COMMUNITY_SAVE: "perc.ui.explorer@Save folder community",
  SET_FOLDER_COMMUNITY_CANCEL: "perc.ui.explorer@Cancel",
  SET_FOLDER_COMMUNITY_SAVED: "perc.ui.explorer@Folder community saved",
  SET_FOLDER_COMMUNITY_EMPTY:
    "perc.ui.explorer@Select a folder before setting its community",
  SET_FOLDER_COMMUNITY_PAGE:
    "perc.ui.explorer@Pages are not given a folder community",
  SET_FOLDER_COMMUNITY_ASSET:
    "perc.ui.explorer@Assets are not given a folder community",
  SET_FOLDER_COMMUNITY_NOT_FOLDER:
    "perc.ui.explorer@Only a folder can have its folder community set",
  SET_FOLDER_COMMUNITY_MULTI:
    "perc.ui.explorer@Set folder community applies to one selected folder",
  SET_FOLDER_COMMUNITY_NO_ID:
    "perc.ui.explorer@No folder id is available to set a community",
  SET_FOLDER_COMMUNITY_NONE:
    "perc.ui.explorer@No communities are available to assign to this folder",
  SET_FOLDER_COMMUNITY_UNCHANGED:
    "perc.ui.explorer@Choose a different community. The current folder community is not saved again",
  SET_FOLDER_COMMUNITY_FORBIDDEN:
    "perc.ui.explorer@That community is not in the folder community catalog",
  SET_FOLDER_COMMUNITY_BLANK: "perc.ui.explorer@Choose a community before saving",
  SET_FOLDER_COMMUNITY_HTTP_400:
    "perc.ui.explorer@Could not set the folder community (HTTP 400)",
  SET_FOLDER_COMMUNITY_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this folder community (HTTP 403)",
  SET_FOLDER_COMMUNITY_HTTP_409:
    "perc.ui.explorer@This folder community could not be saved (HTTP 409)",
  SET_FOLDER_COMMUNITY_FAILED: "perc.ui.explorer@Could not set the folder community",
  SET_FOLDER_COMMUNITY_MISMATCH:
    "perc.ui.explorer@The folder community did not change after refresh",
  SET_FOLDER_COMMUNITY_MULTI_NOTE:
    "perc.ui.explorer@This community is saved on each selected folder. Pages and assets are not changed",
  SET_FOLDER_COMMUNITY_PARTIAL:
    "perc.ui.explorer@Not every selected folder had its community set: {detail}",
  SET_FOLDER_LOCALE: "perc.ui.explorer@Set folder locale",
  SET_FOLDER_LOCALE_ARIA:
    "perc.ui.explorer@Set the locale on the selected folder",
  SET_FOLDER_LOCALE_TITLE: "perc.ui.explorer@Set folder locale",
  SET_FOLDER_LOCALE_LABEL: "perc.ui.explorer@Folder locale",
  SET_FOLDER_LOCALE_CURRENT: "perc.ui.explorer@current",
  SET_FOLDER_LOCALE_SAVE: "perc.ui.explorer@Save folder locale",
  SET_FOLDER_LOCALE_CANCEL: "perc.ui.explorer@Cancel",
  SET_FOLDER_LOCALE_SAVED: "perc.ui.explorer@Folder locale saved",
  SET_FOLDER_LOCALE_EMPTY:
    "perc.ui.explorer@Select a folder before setting its locale",
  SET_FOLDER_LOCALE_PAGE:
    "perc.ui.explorer@Pages are not given a folder locale",
  SET_FOLDER_LOCALE_ASSET:
    "perc.ui.explorer@Assets are not given a folder locale",
  SET_FOLDER_LOCALE_NOT_FOLDER:
    "perc.ui.explorer@Only a folder can have its folder locale set",
  SET_FOLDER_LOCALE_MULTI:
    "perc.ui.explorer@Set folder locale applies to one selected folder",
  SET_FOLDER_LOCALE_NO_ID:
    "perc.ui.explorer@No folder id is available to set a locale",
  SET_FOLDER_LOCALE_NONE:
    "perc.ui.explorer@No locales are available to assign to this folder",
  SET_FOLDER_LOCALE_UNCHANGED:
    "perc.ui.explorer@Choose a different locale. The current folder locale is not saved again",
  SET_FOLDER_LOCALE_FORBIDDEN:
    "perc.ui.explorer@That locale is not in the folder locale catalog",
  SET_FOLDER_LOCALE_BLANK: "perc.ui.explorer@Choose a locale before saving",
  SET_FOLDER_LOCALE_HTTP_400:
    "perc.ui.explorer@Could not set the folder locale (HTTP 400)",
  SET_FOLDER_LOCALE_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this folder locale (HTTP 403)",
  SET_FOLDER_LOCALE_HTTP_409:
    "perc.ui.explorer@This folder locale could not be saved (HTTP 409)",
  SET_FOLDER_LOCALE_FAILED: "perc.ui.explorer@Could not set the folder locale",
  SET_FOLDER_LOCALE_MISMATCH:
    "perc.ui.explorer@The folder locale did not change after refresh",
  SET_FOLDER_LOCALE_MULTI_NOTE:
    "perc.ui.explorer@This locale is saved on each selected folder. Pages and assets are not changed",
  SET_FOLDER_LOCALE_PARTIAL:
    "perc.ui.explorer@Not every selected folder had its locale set: {detail}",
  SET_FOLDER_DISPLAY_FORMAT: "perc.ui.explorer@Set folder display format",
  SET_FOLDER_DISPLAY_FORMAT_ARIA:
    "perc.ui.explorer@Set the display format on the selected folder",
  SET_FOLDER_DISPLAY_FORMAT_TITLE: "perc.ui.explorer@Set folder display format",
  SET_FOLDER_DISPLAY_FORMAT_LABEL: "perc.ui.explorer@Folder display format",
  SET_FOLDER_DISPLAY_FORMAT_CURRENT: "perc.ui.explorer@current",
  SET_FOLDER_DISPLAY_FORMAT_SAVE: "perc.ui.explorer@Save folder display format",
  SET_FOLDER_DISPLAY_FORMAT_CANCEL: "perc.ui.explorer@Cancel",
  SET_FOLDER_DISPLAY_FORMAT_SAVED: "perc.ui.explorer@Folder display format saved",
  SET_FOLDER_DISPLAY_FORMAT_EMPTY:
    "perc.ui.explorer@Select a folder before setting its display format",
  SET_FOLDER_DISPLAY_FORMAT_PAGE:
    "perc.ui.explorer@Pages are not given a folder display format",
  SET_FOLDER_DISPLAY_FORMAT_ASSET:
    "perc.ui.explorer@Assets are not given a folder display format",
  SET_FOLDER_DISPLAY_FORMAT_NOT_FOLDER:
    "perc.ui.explorer@Only a folder can have its folder display format set",
  SET_FOLDER_DISPLAY_FORMAT_MULTI:
    "perc.ui.explorer@Set folder display format applies to one selected folder",
  SET_FOLDER_DISPLAY_FORMAT_NO_ID:
    "perc.ui.explorer@No folder id is available to set a display format",
  SET_FOLDER_DISPLAY_FORMAT_NONE:
    "perc.ui.explorer@No display formats are available to assign to this folder",
  SET_FOLDER_DISPLAY_FORMAT_UNCHANGED:
    "perc.ui.explorer@Choose a different display format. The current folder display format is not saved again",
  SET_FOLDER_DISPLAY_FORMAT_FORBIDDEN:
    "perc.ui.explorer@That display format is not in the folder display format catalog",
  SET_FOLDER_DISPLAY_FORMAT_BLANK:
    "perc.ui.explorer@Choose a display format before saving",
  SET_FOLDER_DISPLAY_FORMAT_HTTP_400:
    "perc.ui.explorer@Could not set the folder display format (HTTP 400)",
  SET_FOLDER_DISPLAY_FORMAT_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this folder display format (HTTP 403)",
  SET_FOLDER_DISPLAY_FORMAT_HTTP_409:
    "perc.ui.explorer@This folder display format could not be saved (HTTP 409)",
  SET_FOLDER_DISPLAY_FORMAT_FAILED:
    "perc.ui.explorer@Could not set the folder display format",
  SET_FOLDER_DISPLAY_FORMAT_MISMATCH:
    "perc.ui.explorer@The folder display format did not change after refresh",
  SET_FOLDER_DISPLAY_FORMAT_MULTI_NOTE:
    "perc.ui.explorer@This display format is saved on each selected folder. Pages and assets are not changed",
  SET_FOLDER_DISPLAY_FORMAT_PARTIAL:
    "perc.ui.explorer@Not every selected folder had its display format set: {detail}",
  SET_FOLDER_ALLOWED_SITES: "perc.ui.explorer@Set allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_ARIA:
    "perc.ui.explorer@Set allowed publish sites on the selected folder",
  SET_FOLDER_ALLOWED_SITES_TITLE: "perc.ui.explorer@Set allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_LABEL: "perc.ui.explorer@Allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_HINT:
    "perc.ui.explorer@Leave every site unchecked and save to allow publishing to all sites",
  SET_FOLDER_ALLOWED_SITES_CURRENT: "perc.ui.explorer@current",
  SET_FOLDER_ALLOWED_SITES_SAVE: "perc.ui.explorer@Save allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_CANCEL: "perc.ui.explorer@Cancel",
  SET_FOLDER_ALLOWED_SITES_SAVED: "perc.ui.explorer@Allowed publish sites saved",
  SET_FOLDER_ALLOWED_SITES_CLEARED:
    "perc.ui.explorer@Allowed publish sites cleared. Assets may publish to all sites",
  SET_FOLDER_ALLOWED_SITES_EMPTY:
    "perc.ui.explorer@Select a folder before setting its allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_PAGE:
    "perc.ui.explorer@Pages are not given allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_ASSET:
    "perc.ui.explorer@Assets are not given allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_NOT_FOLDER:
    "perc.ui.explorer@Only a folder can have its allowed publish sites set",
  SET_FOLDER_ALLOWED_SITES_MULTI:
    "perc.ui.explorer@Set allowed publish sites applies to one selected folder",
  SET_FOLDER_ALLOWED_SITES_MULTI_NOTE:
    "perc.ui.explorer@These allowed publish sites are saved on each selected folder. Pages and assets are not changed. Leave every site unchecked to allow publishing to all sites",
  SET_FOLDER_ALLOWED_SITES_PARTIAL:
    "perc.ui.explorer@Not every selected folder had its allowed publish sites set: {detail}",
  SET_FOLDER_ALLOWED_SITES_ALL: "perc.ui.explorer@All sites",
  SET_FOLDER_ALLOWED_SITES_NO_ID:
    "perc.ui.explorer@No folder id is available to set allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_NONE:
    "perc.ui.explorer@No publish sites are available to assign to this folder",
  SET_FOLDER_ALLOWED_SITES_UNCHANGED:
    "perc.ui.explorer@That allowed publish site list is already stored. It is not saved again",
  SET_FOLDER_ALLOWED_SITES_FORBIDDEN:
    "perc.ui.explorer@That site is not in the allowed publish sites catalog",
  SET_FOLDER_ALLOWED_SITES_INVALID:
    "perc.ui.explorer@Allowed publish sites must be catalog site ids",
  SET_FOLDER_ALLOWED_SITES_HTTP_400:
    "perc.ui.explorer@Could not set allowed publish sites (HTTP 400)",
  SET_FOLDER_ALLOWED_SITES_HTTP_403:
    "perc.ui.explorer@You are not allowed to set allowed publish sites (HTTP 403)",
  SET_FOLDER_ALLOWED_SITES_HTTP_409:
    "perc.ui.explorer@Allowed publish sites could not be saved (HTTP 409)",
  SET_FOLDER_ALLOWED_SITES_FAILED: "perc.ui.explorer@Could not set allowed publish sites",
  SET_FOLDER_ALLOWED_SITES_MISMATCH:
    "perc.ui.explorer@The allowed publish sites did not change after refresh",
  SET_COMMUNITY: "perc.ui.explorer@Set community",
  SET_COMMUNITY_ARIA:
    "perc.ui.explorer@Set the community on the selected pages and assets",
  SET_COMMUNITY_MULTI_NOTE:
    "perc.ui.explorer@This community is saved on each selected page and asset. Folders are not changed",
  SET_COMMUNITY_PARTIAL:
    "perc.ui.explorer@Not every selected item had its community set: {detail}",
  SET_COMMUNITY_TITLE: "perc.ui.explorer@Set community",
  SET_COMMUNITY_LABEL: "perc.ui.explorer@Community",
  SET_COMMUNITY_CURRENT: "perc.ui.explorer@current",
  SET_COMMUNITY_SAVE: "perc.ui.explorer@Save community",
  SET_COMMUNITY_CANCEL: "perc.ui.explorer@Cancel",
  SET_COMMUNITY_SAVED: "perc.ui.explorer@Community saved",
  SET_COMMUNITY_EMPTY:
    "perc.ui.explorer@Select a page or asset before setting its community",
  SET_COMMUNITY_FOLDER: "perc.ui.explorer@Folders are not assigned a community",
  SET_COMMUNITY_MULTI:
    "perc.ui.explorer@Set community applies to one selected page or asset",
  SET_COMMUNITY_NOT_ITEM:
    "perc.ui.explorer@Only a page or asset can have its community set",
  SET_COMMUNITY_NO_ID:
    "perc.ui.explorer@No content id is available to set a community",
  SET_COMMUNITY_NONE:
    "perc.ui.explorer@This item has no communities that can be assigned",
  SET_COMMUNITY_UNCHANGED:
    "perc.ui.explorer@Choose a different community. The current community is not saved again",
  SET_COMMUNITY_FORBIDDEN:
    "perc.ui.explorer@That community is not allowed for this item",
  SET_COMMUNITY_BLANK: "perc.ui.explorer@Choose a community before saving",
  SET_COMMUNITY_HTTP_400:
    "perc.ui.explorer@Could not set the community (HTTP 400)",
  SET_COMMUNITY_HTTP_403:
    "perc.ui.explorer@You are not allowed to set this community (HTTP 403)",
  SET_COMMUNITY_HTTP_409:
    "perc.ui.explorer@This item could not change community (HTTP 409)",
  SET_COMMUNITY_FAILED: "perc.ui.explorer@Could not set the community",
  CHANGE_PAGE_TEMPLATE: "perc.ui.explorer@Change page template",
  CHANGE_PAGE_TEMPLATE_ARIA:
    "perc.ui.explorer@Change the page template of the selected page",
  CHANGE_PAGE_TEMPLATE_TITLE: "perc.ui.explorer@Change page template",
  CHANGE_PAGE_TEMPLATE_LABEL: "perc.ui.explorer@Page template",
  CHANGE_PAGE_TEMPLATE_CURRENT: "perc.ui.explorer@current",
  CHANGE_PAGE_TEMPLATE_SAVE: "perc.ui.explorer@Save page template",
  CHANGE_PAGE_TEMPLATE_CANCEL: "perc.ui.explorer@Cancel",
  CHANGE_PAGE_TEMPLATE_SAVED: "perc.ui.explorer@Page template saved",
  CHANGE_PAGE_TEMPLATE_EMPTY:
    "perc.ui.explorer@Select a page before changing its page template",
  CHANGE_PAGE_TEMPLATE_FOLDER:
    "perc.ui.explorer@Folders do not have a page template",
  CHANGE_PAGE_TEMPLATE_ASSET:
    "perc.ui.explorer@Assets do not have a page template",
  CHANGE_PAGE_TEMPLATE_NOT_PAGE:
    "perc.ui.explorer@Only a page can change its page template",
  CHANGE_PAGE_TEMPLATE_MULTI:
    "perc.ui.explorer@Change page template applies to one selected page",
  CHANGE_PAGE_TEMPLATE_NO_ID:
    "perc.ui.explorer@No page id is available to change the page template",
  CHANGE_PAGE_TEMPLATE_NONE:
    "perc.ui.explorer@This page has no page template to choose",
  CHANGE_PAGE_TEMPLATE_UNCHANGED:
    "perc.ui.explorer@Choose a different page template. The current template is not saved again",
  CHANGE_PAGE_TEMPLATE_FORBIDDEN:
    "perc.ui.explorer@That page template is not allowed for this page",
  CHANGE_PAGE_TEMPLATE_BLANK:
    "perc.ui.explorer@Choose a page template before saving",
  CHANGE_PAGE_TEMPLATE_HTTP_400:
    "perc.ui.explorer@Could not change the page template (HTTP 400)",
  CHANGE_PAGE_TEMPLATE_HTTP_403:
    "perc.ui.explorer@You are not allowed to change this page template (HTTP 403)",
  CHANGE_PAGE_TEMPLATE_HTTP_409:
    "perc.ui.explorer@This page template could not be saved (HTTP 409)",
  CHANGE_PAGE_TEMPLATE_FAILED: "perc.ui.explorer@Could not change the page template",
  MOBILE_PREVIEW: "perc.ui.explorer@Mobile preview",
  MOBILE_PREVIEW_ARIA:
    "perc.ui.explorer@Open a mobile preview of the selected page",
  MOBILE_PREVIEW_OPENED: "perc.ui.explorer@Mobile preview opened",
  MOBILE_PREVIEW_NONE:
    "perc.ui.explorer@Select a page to open mobile preview",
  MOBILE_PREVIEW_FOLDER:
    "perc.ui.explorer@Folders do not open a mobile preview: {name}",
  MOBILE_PREVIEW_NOT_PAGE:
    "perc.ui.explorer@Mobile preview is only available for a page: {name}",
  MOBILE_PREVIEW_NO_TARGET:
    "perc.ui.explorer@This page has no mobile preview target",
  MOBILE_PREVIEW_BLOCKED:
    "perc.ui.explorer@Could not open the mobile preview window",
  COPY_ITEM_GUID: "perc.ui.explorer@Copy item id",
  COPY_ITEM_GUID_ARIA:
    "perc.ui.explorer@Copy the selected item content id to the clipboard",
  COPY_ITEM_GUID_SUCCESS:
    "perc.ui.explorer@Item id copied to the clipboard",
  COPY_ITEM_GUID_NONE:
    "perc.ui.explorer@Select a page or asset to copy its content id",
  COPY_ITEM_GUID_FOLDER:
    "perc.ui.explorer@Folders are not copied: {name}",
  COPY_ITEM_GUID_NO_ID:
    "perc.ui.explorer@No content id is available for {name}",
  COPY_ITEM_GUID_FAILED: "perc.ui.explorer@Could not copy the item id",

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
  REVISIONS_COMPARE: "perc.ui.explorer@Compare",
  REVISIONS_COMPARE_FROM: "perc.ui.explorer@Compare from",
  REVISIONS_COMPARE_TO: "perc.ui.explorer@Compare to",
  REVISIONS_COMPARE_NEED_TWO:
    "perc.ui.explorer@Select two revisions to compare",
  REVISIONS_COMPARE_LOADING: "perc.ui.explorer@Comparing revisions…",
  REVISIONS_COMPARE_ERROR: "perc.ui.explorer@Could not compare those revisions",
  REVISIONS_COMPARE_NOT_FOUND:
    "perc.ui.explorer@That revision was not found (HTTP 404)",
  REVISIONS_COMPARE_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to compare this item (HTTP 403)",
  REVISIONS_COMPARE_EMPTY:
    "perc.ui.explorer@No field differences are recorded for these revisions",
  REVISIONS_COMPARE_COL_FIELD: "perc.ui.explorer@Field",
  REVISIONS_COMPARE_COL_LEFT: "perc.ui.explorer@Revision A",
  REVISIONS_COMPARE_COL_RIGHT: "perc.ui.explorer@Revision B",
  REVISIONS_COMPARE_CHANGED: "perc.ui.explorer@Changed",
  REVISIONS_COMPARE_SAME: "perc.ui.explorer@Same",
  CONFIRM_RESTORE_REVISION:
    "perc.ui.explorer@Restore this prior revision as the current revision?",
  CONFIRM_FLUSH_CACHE:
    "perc.ui.explorer@Flush the assembler cache for all items?",
  CONFIRM_NAV_RESET:
    "perc.ui.explorer@Reload managed navigation configuration?",
  CONFIRM_APPROVE_INCREMENTAL:
    "perc.ui.explorer@Approve this page or asset onto the incremental publish queue?",
  ACTION_APPROVE_INCREMENTAL_OK:
    "perc.ui.explorer@Approved onto the incremental queue",
  ACTION_APPROVE_INCREMENTAL_EMPTY:
    "perc.ui.explorer@Select a page or asset before approving it onto the incremental queue",
  ACTION_APPROVE_INCREMENTAL_FOLDER:
    "perc.ui.explorer@Folders are not approved onto the incremental queue",
  ACTION_APPROVE_INCREMENTAL_NOT_ITEM:
    "perc.ui.explorer@Only a page or asset can be approved onto the incremental queue",
  ACTION_APPROVE_INCREMENTAL_REJECTED:
    "perc.ui.explorer@Could not approve this item onto the incremental queue (HTTP 400)",
  ACTION_APPROVE_INCREMENTAL_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to approve this item (HTTP 403)",
  ACTION_APPROVE_INCREMENTAL_CONFLICT:
    "perc.ui.explorer@This item could not be approved onto the incremental queue (HTTP 409)",
  CONFIRM_UNAPPROVE_INCREMENTAL:
    "perc.ui.explorer@Unapprove this page or asset on the incremental publish queue?",
  ACTION_UNAPPROVE_INCREMENTAL_OK:
    "perc.ui.explorer@Unapproved on the incremental queue",
  ACTION_UNAPPROVE_INCREMENTAL_EMPTY:
    "perc.ui.explorer@Select a page or asset before unapproving it on the incremental queue",
  ACTION_UNAPPROVE_INCREMENTAL_FOLDER:
    "perc.ui.explorer@Folders are not unapproved on the incremental queue",
  ACTION_UNAPPROVE_INCREMENTAL_NOT_ITEM:
    "perc.ui.explorer@Only a page or asset on the incremental queue can be unapproved",
  ACTION_UNAPPROVE_INCREMENTAL_REJECTED:
    "perc.ui.explorer@Could not unapprove this item on the incremental queue (HTTP 400)",
  ACTION_UNAPPROVE_INCREMENTAL_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to unapprove this item (HTTP 403)",
  ACTION_UNAPPROVE_INCREMENTAL_CONFLICT:
    "perc.ui.explorer@This item could not be unapproved on the incremental queue (HTTP 409)",
  CONFIRM_REMOVE_INCREMENTAL:
    "perc.ui.explorer@Remove this page or asset from the incremental publish queue?",
  ACTION_REMOVE_INCREMENTAL_OK:
    "perc.ui.explorer@Removed from the incremental queue",
  ACTION_REMOVE_INCREMENTAL_EMPTY:
    "perc.ui.explorer@Select a page or asset before removing it from the incremental queue",
  ACTION_REMOVE_INCREMENTAL_FOLDER:
    "perc.ui.explorer@Folders are not removed from the incremental queue",
  ACTION_REMOVE_INCREMENTAL_NOT_ITEM:
    "perc.ui.explorer@Only a page or asset on the incremental queue can be removed",
  ACTION_REMOVE_INCREMENTAL_MULTI:
    "perc.ui.explorer@Remove from the incremental queue applies to one selected page or asset",
  ACTION_REMOVE_INCREMENTAL_REJECTED:
    "perc.ui.explorer@Could not remove this item from the incremental queue (HTTP 400)",
  ACTION_REMOVE_INCREMENTAL_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to remove this item from the incremental queue (HTTP 403)",
  ACTION_REMOVE_INCREMENTAL_CONFLICT:
    "perc.ui.explorer@This item could not be removed from the incremental queue (HTTP 409)",
  CONFIRM_NEW_COPY:
    "perc.ui.explorer@Create a new copy of this item in the same folder?",
  CONFIRM_PROMOTABLE:
    "perc.ui.explorer@Create a promotable version of this item in the same folder?",
  ACTION_FLUSH_OK: "perc.ui.explorer@Assembler cache flushed",
  ACTION_FLUSH_FAILED: "perc.ui.explorer@Could not flush the assembler cache",
  ACTION_NAV_RESET_OK: "perc.ui.explorer@Managed navigation reset",
  ACTION_NEW_COPY_OK: "perc.ui.explorer@New copy created",
  ACTION_NEW_COPY_REJECTED:
    "perc.ui.explorer@Could not create a new copy (HTTP 400)",
  ACTION_NEW_COPY_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to create a new copy (HTTP 403)",
  ACTION_NEW_COPY_CONFLICT:
    "perc.ui.explorer@A new copy conflicts with the item state (HTTP 409)",
  ACTION_PROMOTABLE_OK: "perc.ui.explorer@Promotable version created",
  ACTION_PROMOTABLE_REJECTED:
    "perc.ui.explorer@Could not create a promotable version (HTTP 400)",
  ACTION_PROMOTABLE_FORBIDDEN:
    "perc.ui.explorer@You are not allowed to create a promotable version (HTTP 403)",
  ACTION_PROMOTABLE_CONFLICT:
    "perc.ui.explorer@A promotable version conflicts with the item state (HTTP 409)",
} as const;
