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

import { message } from "../i18n/message";

/**
 * Architecture / Navigation SPA chrome keys
 * (#3094 / #3095 / #3096 / #3097 / parent #3092).
 * English after {@code @} is the source fallback when TMX is not loaded.
 * Small key set only — no multi-locale mass TMX backfill in this slice.
 */
const KEYS = {
  TITLE: "perc.ui.architecture.modern@Navigation",
  INTRO:
    "perc.ui.architecture.modern@Browse and edit site navigation trees (navons / sections), including landing pages, section links, and external links.",
  SHELL_LOADING: "perc.ui.architecture.modern@Loading Navigation…",
  SITE_LABEL: "perc.ui.architecture.modern@Site",
  SITE_PLACEHOLDER: "perc.ui.architecture.modern@Select a site…",
  SITE_HINT: "perc.ui.architecture.modern@Site context: {0}",
  SITE_NONE:
    "perc.ui.architecture.modern@Select a site to load its navigation tree.",
  SITES_LOADING: "perc.ui.architecture.modern@Loading sites…",
  SITES_ERROR: "perc.ui.architecture.modern@Could not load the site list.",
  SITES_EMPTY:
    "perc.ui.architecture.modern@No sites are available. Create a site to browse its navigation tree.",
  TREE_LOADING: "perc.ui.architecture.modern@Loading navigation tree…",
  TREE_ERROR: "perc.ui.architecture.modern@Could not load the navigation tree.",
  TREE_EMPTY:
    "perc.ui.architecture.modern@This site has no navigation sections yet.",
  TREE_EMPTY_TITLE: "perc.ui.architecture.modern@No navigation tree",
  TREE_EMPTY_HINT:
    "perc.ui.architecture.modern@A site can exist without a NavTree. Add a navigation tree at the site root in Explorer, then refresh, or choose another site.",
  TREE_PANEL_TITLE: "perc.ui.architecture.modern@Navigation tree",
  TREE_STRUCTURE_NOTE:
    "perc.ui.architecture.modern@Select a section, then use structure actions: create, rename, move, delete, landing page, or link editors.",
  REFRESH: "perc.ui.architecture.modern@Refresh",
  // Kept for older tests / deep links that still assert empty-shell keys
  EMPTY_TITLE: "perc.ui.architecture.modern@No site selected",
  EMPTY_BODY:
    "perc.ui.architecture.modern@Choose a site from the list above to browse its navigation tree (navons / sections).",
  // Structure actions (#3096 / #3097)
  ACTIONS_LABEL: "perc.ui.architecture.modern@Structure actions",
  ACTION_CREATE: "perc.ui.architecture.modern@Create section",
  ACTION_CREATE_SECTION_LINK: "perc.ui.architecture.modern@Create section link",
  ACTION_CREATE_EXTERNAL_LINK:
    "perc.ui.architecture.modern@Create external link",
  ACTION_LANDING: "perc.ui.architecture.modern@Landing page",
  ACTION_EDIT_LINK: "perc.ui.architecture.modern@Edit link",
  ACTION_RENAME: "perc.ui.architecture.modern@Rename",
  ACTION_MOVE_UP: "perc.ui.architecture.modern@Move up",
  ACTION_MOVE_DOWN: "perc.ui.architecture.modern@Move down",
  ACTION_DELETE: "perc.ui.architecture.modern@Delete",
  ACTION_BUSY: "perc.ui.architecture.modern@Working…",
  SELECT_HINT:
    "perc.ui.architecture.modern@Select a section in the tree to enable structure actions.",
  MUTATION_ERROR: "perc.ui.architecture.modern@Could not update navigation structure.",
  CREATE_DIALOG_TITLE: "perc.ui.architecture.modern@Create section",
  CREATE_PARENT_LABEL: "perc.ui.architecture.modern@Parent section",
  CREATE_TITLE_LABEL: "perc.ui.architecture.modern@Title",
  CREATE_URL_LABEL: "perc.ui.architecture.modern@URL name",
  CREATE_TEMPLATE_LABEL: "perc.ui.architecture.modern@Template",
  CREATE_TEMPLATE_LOADING: "perc.ui.architecture.modern@Loading templates…",
  CREATE_TEMPLATE_EMPTY:
    "perc.ui.architecture.modern@No templates are available for this site.",
  CREATE_SUBMIT: "perc.ui.architecture.modern@Create",
  CREATE_CANCEL: "perc.ui.architecture.modern@Cancel",
  RENAME_DIALOG_TITLE: "perc.ui.architecture.modern@Rename section",
  RENAME_TITLE_LABEL: "perc.ui.architecture.modern@Title",
  RENAME_SUBMIT: "perc.ui.architecture.modern@Save",
  RENAME_CANCEL: "perc.ui.architecture.modern@Cancel",
  DELETE_CONFIRM:
    "perc.ui.architecture.modern@Delete section \"{0}\"? This cannot be undone.",
  DELETE_ROOT_BLOCKED:
    "perc.ui.architecture.modern@The site root section cannot be deleted.",
  CREATE_PARENT_BLOCKED:
    "perc.ui.architecture.modern@Select a regular section (not a section or external link) as the parent.",
  // Landing page (#3097)
  LANDING_DIALOG_TITLE: "perc.ui.architecture.modern@Replace landing page",
  LANDING_SECTION_LABEL: "perc.ui.architecture.modern@Section",
  LANDING_HINT:
    "perc.ui.architecture.modern@Choose a page under this site to use as the section landing page.",
  LANDING_SUBMIT: "perc.ui.architecture.modern@Replace landing page",
  LANDING_CANCEL: "perc.ui.architecture.modern@Cancel",
  LANDING_PICKER_TITLE: "perc.ui.architecture.modern@Select landing page",
  LANDING_NO_PAGE:
    "perc.ui.architecture.modern@Select a page before replacing the landing page.",
  LANDING_BLOCKED:
    "perc.ui.architecture.modern@Landing page can only be set on a regular section.",
  // Section link (#3097)
  SECTION_LINK_DIALOG_TITLE: "perc.ui.architecture.modern@Create section link",
  SECTION_LINK_EDIT_TITLE: "perc.ui.architecture.modern@Edit section link",
  SECTION_LINK_TARGET_LABEL: "perc.ui.architecture.modern@Target section",
  SECTION_LINK_TARGET_HINT:
    "perc.ui.architecture.modern@Browse the navigation tree and select the section this link should point to.",
  SECTION_LINK_BROWSE: "perc.ui.architecture.modern@Browse sections…",
  SECTION_LINK_NO_TARGET:
    "perc.ui.architecture.modern@Select a target section for the link.",
  SECTION_LINK_INVALID_TARGET:
    "perc.ui.architecture.modern@That target is not valid for a section link under the selected parent.",
  SECTION_LINK_SUBMIT: "perc.ui.architecture.modern@Create link",
  SECTION_LINK_SAVE: "perc.ui.architecture.modern@Save link",
  SECTION_LINK_CANCEL: "perc.ui.architecture.modern@Cancel",
  // External link (#3097)
  EXTERNAL_LINK_DIALOG_TITLE:
    "perc.ui.architecture.modern@Create external link",
  EXTERNAL_LINK_EDIT_TITLE: "perc.ui.architecture.modern@Edit external link",
  EXTERNAL_LINK_TEXT_LABEL: "perc.ui.architecture.modern@Link text",
  EXTERNAL_LINK_URL_LABEL: "perc.ui.architecture.modern@URL",
  EXTERNAL_LINK_TARGET_LABEL: "perc.ui.architecture.modern@Target window",
  EXTERNAL_LINK_TARGET_SELF: "perc.ui.architecture.modern@Same window",
  EXTERNAL_LINK_TARGET_BLANK: "perc.ui.architecture.modern@New window",
  EXTERNAL_LINK_TARGET_TOP: "perc.ui.architecture.modern@Top window",
  EXTERNAL_LINK_TARGET_PARENT: "perc.ui.architecture.modern@Parent window",
  EXTERNAL_LINK_SUBMIT: "perc.ui.architecture.modern@Create link",
  EXTERNAL_LINK_SAVE: "perc.ui.architecture.modern@Save",
  EXTERNAL_LINK_CANCEL: "perc.ui.architecture.modern@Cancel",
  // Tree picker (#3097)
  TREE_PICKER_TITLE: "perc.ui.architecture.modern@Select section",
  TREE_PICKER_CONFIRM: "perc.ui.architecture.modern@Select",
  TREE_PICKER_CANCEL: "perc.ui.architecture.modern@Cancel",
  TREE_PICKER_HINT:
    "perc.ui.architecture.modern@Select a section in the tree, then confirm.",
  // Blog support note
  BLOG_NOTE:
    "perc.ui.architecture.modern@Blog sections appear in the tree; full blog authoring remains outside this editor.",
  // a11y / type badges / validation (#3098)
  SECURE_BADGE: "perc.ui.architecture.modern@Secure",
  SECURE_TITLE: "perc.ui.architecture.modern@Requires login",
  TYPE_SECTION_LINK: "perc.ui.architecture.modern@Section link",
  TYPE_EXTERNAL_LINK: "perc.ui.architecture.modern@External link",
  TYPE_BLOG: "perc.ui.architecture.modern@Blog",
  LANDING_PAGE_ID_LABEL: "perc.ui.architecture.modern@Page id",
  VALIDATION_URL_NAME_REQUIRED:
    "perc.ui.architecture.modern@URL name is required",
  VALIDATION_URL_NAME_TOO_LONG:
    "perc.ui.architecture.modern@URL name is too long (max 100 characters)",
  VALIDATION_URL_NAME_CHARS:
    "perc.ui.architecture.modern@URL name may only contain letters, numbers, dash, underscore, and period",
  VALIDATION_TITLE_REQUIRED: "perc.ui.architecture.modern@Title is required",
  VALIDATION_TITLE_TOO_LONG:
    "perc.ui.architecture.modern@Title is too long (max 512 characters)",
  VALIDATION_URL_REQUIRED: "perc.ui.architecture.modern@URL is required",
  VALIDATION_URL_TOO_LONG:
    "perc.ui.architecture.modern@URL is too long (max 2048 characters)",
  VALIDATION_URL_SCHEME_BLOCKED:
    "perc.ui.architecture.modern@URL scheme is not allowed (use http(s) or a site path)",
  VALIDATION_URL_INVALID:
    "perc.ui.architecture.modern@Enter a valid URL (for example https://example.com or /path)",
} as const;

export type ArchitectureMsgKey = keyof typeof KEYS;

/** Stable message-key map (tests / i18n key attributes). */
export const ARCH_MSG_KEYS = KEYS;

/** Resolved English-fallback strings for Architecture shell chrome. */
export const ARCH_MSG: { readonly [K in ArchitectureMsgKey]: string } = {
  TITLE: message(KEYS.TITLE),
  INTRO: message(KEYS.INTRO),
  SHELL_LOADING: message(KEYS.SHELL_LOADING),
  SITE_LABEL: message(KEYS.SITE_LABEL),
  SITE_PLACEHOLDER: message(KEYS.SITE_PLACEHOLDER),
  SITE_HINT: message(KEYS.SITE_HINT),
  SITE_NONE: message(KEYS.SITE_NONE),
  SITES_LOADING: message(KEYS.SITES_LOADING),
  SITES_ERROR: message(KEYS.SITES_ERROR),
  SITES_EMPTY: message(KEYS.SITES_EMPTY),
  TREE_LOADING: message(KEYS.TREE_LOADING),
  TREE_ERROR: message(KEYS.TREE_ERROR),
  TREE_EMPTY: message(KEYS.TREE_EMPTY),
  TREE_EMPTY_TITLE: message(KEYS.TREE_EMPTY_TITLE),
  TREE_EMPTY_HINT: message(KEYS.TREE_EMPTY_HINT),
  TREE_PANEL_TITLE: message(KEYS.TREE_PANEL_TITLE),
  TREE_STRUCTURE_NOTE: message(KEYS.TREE_STRUCTURE_NOTE),
  REFRESH: message(KEYS.REFRESH),
  EMPTY_TITLE: message(KEYS.EMPTY_TITLE),
  EMPTY_BODY: message(KEYS.EMPTY_BODY),
  ACTIONS_LABEL: message(KEYS.ACTIONS_LABEL),
  ACTION_CREATE: message(KEYS.ACTION_CREATE),
  ACTION_CREATE_SECTION_LINK: message(KEYS.ACTION_CREATE_SECTION_LINK),
  ACTION_CREATE_EXTERNAL_LINK: message(KEYS.ACTION_CREATE_EXTERNAL_LINK),
  ACTION_LANDING: message(KEYS.ACTION_LANDING),
  ACTION_EDIT_LINK: message(KEYS.ACTION_EDIT_LINK),
  ACTION_RENAME: message(KEYS.ACTION_RENAME),
  ACTION_MOVE_UP: message(KEYS.ACTION_MOVE_UP),
  ACTION_MOVE_DOWN: message(KEYS.ACTION_MOVE_DOWN),
  ACTION_DELETE: message(KEYS.ACTION_DELETE),
  ACTION_BUSY: message(KEYS.ACTION_BUSY),
  SELECT_HINT: message(KEYS.SELECT_HINT),
  MUTATION_ERROR: message(KEYS.MUTATION_ERROR),
  CREATE_DIALOG_TITLE: message(KEYS.CREATE_DIALOG_TITLE),
  CREATE_PARENT_LABEL: message(KEYS.CREATE_PARENT_LABEL),
  CREATE_TITLE_LABEL: message(KEYS.CREATE_TITLE_LABEL),
  CREATE_URL_LABEL: message(KEYS.CREATE_URL_LABEL),
  CREATE_TEMPLATE_LABEL: message(KEYS.CREATE_TEMPLATE_LABEL),
  CREATE_TEMPLATE_LOADING: message(KEYS.CREATE_TEMPLATE_LOADING),
  CREATE_TEMPLATE_EMPTY: message(KEYS.CREATE_TEMPLATE_EMPTY),
  CREATE_SUBMIT: message(KEYS.CREATE_SUBMIT),
  CREATE_CANCEL: message(KEYS.CREATE_CANCEL),
  RENAME_DIALOG_TITLE: message(KEYS.RENAME_DIALOG_TITLE),
  RENAME_TITLE_LABEL: message(KEYS.RENAME_TITLE_LABEL),
  RENAME_SUBMIT: message(KEYS.RENAME_SUBMIT),
  RENAME_CANCEL: message(KEYS.RENAME_CANCEL),
  DELETE_CONFIRM: message(KEYS.DELETE_CONFIRM),
  DELETE_ROOT_BLOCKED: message(KEYS.DELETE_ROOT_BLOCKED),
  CREATE_PARENT_BLOCKED: message(KEYS.CREATE_PARENT_BLOCKED),
  LANDING_DIALOG_TITLE: message(KEYS.LANDING_DIALOG_TITLE),
  LANDING_SECTION_LABEL: message(KEYS.LANDING_SECTION_LABEL),
  LANDING_HINT: message(KEYS.LANDING_HINT),
  LANDING_SUBMIT: message(KEYS.LANDING_SUBMIT),
  LANDING_CANCEL: message(KEYS.LANDING_CANCEL),
  LANDING_PICKER_TITLE: message(KEYS.LANDING_PICKER_TITLE),
  LANDING_NO_PAGE: message(KEYS.LANDING_NO_PAGE),
  LANDING_BLOCKED: message(KEYS.LANDING_BLOCKED),
  SECTION_LINK_DIALOG_TITLE: message(KEYS.SECTION_LINK_DIALOG_TITLE),
  SECTION_LINK_EDIT_TITLE: message(KEYS.SECTION_LINK_EDIT_TITLE),
  SECTION_LINK_TARGET_LABEL: message(KEYS.SECTION_LINK_TARGET_LABEL),
  SECTION_LINK_TARGET_HINT: message(KEYS.SECTION_LINK_TARGET_HINT),
  SECTION_LINK_BROWSE: message(KEYS.SECTION_LINK_BROWSE),
  SECTION_LINK_NO_TARGET: message(KEYS.SECTION_LINK_NO_TARGET),
  SECTION_LINK_INVALID_TARGET: message(KEYS.SECTION_LINK_INVALID_TARGET),
  SECTION_LINK_SUBMIT: message(KEYS.SECTION_LINK_SUBMIT),
  SECTION_LINK_SAVE: message(KEYS.SECTION_LINK_SAVE),
  SECTION_LINK_CANCEL: message(KEYS.SECTION_LINK_CANCEL),
  EXTERNAL_LINK_DIALOG_TITLE: message(KEYS.EXTERNAL_LINK_DIALOG_TITLE),
  EXTERNAL_LINK_EDIT_TITLE: message(KEYS.EXTERNAL_LINK_EDIT_TITLE),
  EXTERNAL_LINK_TEXT_LABEL: message(KEYS.EXTERNAL_LINK_TEXT_LABEL),
  EXTERNAL_LINK_URL_LABEL: message(KEYS.EXTERNAL_LINK_URL_LABEL),
  EXTERNAL_LINK_TARGET_LABEL: message(KEYS.EXTERNAL_LINK_TARGET_LABEL),
  EXTERNAL_LINK_TARGET_SELF: message(KEYS.EXTERNAL_LINK_TARGET_SELF),
  EXTERNAL_LINK_TARGET_BLANK: message(KEYS.EXTERNAL_LINK_TARGET_BLANK),
  EXTERNAL_LINK_TARGET_TOP: message(KEYS.EXTERNAL_LINK_TARGET_TOP),
  EXTERNAL_LINK_TARGET_PARENT: message(KEYS.EXTERNAL_LINK_TARGET_PARENT),
  EXTERNAL_LINK_SUBMIT: message(KEYS.EXTERNAL_LINK_SUBMIT),
  EXTERNAL_LINK_SAVE: message(KEYS.EXTERNAL_LINK_SAVE),
  EXTERNAL_LINK_CANCEL: message(KEYS.EXTERNAL_LINK_CANCEL),
  TREE_PICKER_TITLE: message(KEYS.TREE_PICKER_TITLE),
  TREE_PICKER_CONFIRM: message(KEYS.TREE_PICKER_CONFIRM),
  TREE_PICKER_CANCEL: message(KEYS.TREE_PICKER_CANCEL),
  TREE_PICKER_HINT: message(KEYS.TREE_PICKER_HINT),
  BLOG_NOTE: message(KEYS.BLOG_NOTE),
  SECURE_BADGE: message(KEYS.SECURE_BADGE),
  SECURE_TITLE: message(KEYS.SECURE_TITLE),
  TYPE_SECTION_LINK: message(KEYS.TYPE_SECTION_LINK),
  TYPE_EXTERNAL_LINK: message(KEYS.TYPE_EXTERNAL_LINK),
  TYPE_BLOG: message(KEYS.TYPE_BLOG),
  LANDING_PAGE_ID_LABEL: message(KEYS.LANDING_PAGE_ID_LABEL),
  VALIDATION_URL_NAME_REQUIRED: message(KEYS.VALIDATION_URL_NAME_REQUIRED),
  VALIDATION_URL_NAME_TOO_LONG: message(KEYS.VALIDATION_URL_NAME_TOO_LONG),
  VALIDATION_URL_NAME_CHARS: message(KEYS.VALIDATION_URL_NAME_CHARS),
  VALIDATION_TITLE_REQUIRED: message(KEYS.VALIDATION_TITLE_REQUIRED),
  VALIDATION_TITLE_TOO_LONG: message(KEYS.VALIDATION_TITLE_TOO_LONG),
  VALIDATION_URL_REQUIRED: message(KEYS.VALIDATION_URL_REQUIRED),
  VALIDATION_URL_TOO_LONG: message(KEYS.VALIDATION_URL_TOO_LONG),
  VALIDATION_URL_SCHEME_BLOCKED: message(KEYS.VALIDATION_URL_SCHEME_BLOCKED),
  VALIDATION_URL_INVALID: message(KEYS.VALIDATION_URL_INVALID),
};

/** Alias kept so older tests that look for TREE_READONLY_NOTE still compile if imported. */
export const TREE_READONLY_NOTE_LEGACY = ARCH_MSG.TREE_STRUCTURE_NOTE;
