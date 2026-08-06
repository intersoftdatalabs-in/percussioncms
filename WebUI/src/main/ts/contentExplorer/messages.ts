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

  SUBFOLDER_COPY_TITLE: "perc.ui.explorer@Subfolder Copy",
  SUBFOLDER_COPY_STEP_SOURCE: "perc.ui.explorer@Source folder",
  SUBFOLDER_COPY_STEP_TARGET: "perc.ui.explorer@Target folder",
  SUBFOLDER_COPY_STEP_CONFIRM: "perc.ui.explorer@Confirm",

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

  RELATIONSHIPS_TITLE: "perc.ui.explorer@IA Relationships",
  RELATIONSHIPS_CLIENT_SIDE_PREVIEW:
    "perc.ui.explorer@Client-side preview (full graph pending rest enhancement)",
  RELATIONSHIPS_LOADING: "perc.ui.explorer@Loading IA relationships…",
  RELATIONSHIPS_ERROR: "perc.ui.explorer@Could not load IA relationships",
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
  // US4 P-ACL / folder security (FR-014–FR-016, SC-004)
  SECURITY_TITLE: "perc.ui.explorer@Folder Security",
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
} as const;
