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
 * Architecture / Navigation SPA chrome keys (#3094 / #3095 / #3096 / parent #3092).
 * English after {@code @} is the source fallback when TMX is not loaded.
 * Small key set only — no multi-locale mass TMX backfill in this slice.
 */
const KEYS = {
  TITLE: "perc.ui.architecture.modern@Architecture",
  INTRO:
    "perc.ui.architecture.modern@Browse and edit site navigation trees (navons / sections). Landing-page and section-link dialogs land in a follow-on slice.",
  SHELL_LOADING: "perc.ui.architecture.modern@Loading Architecture…",
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
  TREE_PANEL_TITLE: "perc.ui.architecture.modern@Navigation tree",
  TREE_STRUCTURE_NOTE:
    "perc.ui.architecture.modern@Select a section, then use Create, Rename, Move, or Delete. Landing page and section-link editors ship later.",
  REFRESH: "perc.ui.architecture.modern@Refresh",
  // Kept for older tests / deep links that still assert empty-shell keys
  EMPTY_TITLE: "perc.ui.architecture.modern@No site selected",
  EMPTY_BODY:
    "perc.ui.architecture.modern@Choose a site from the list above to browse its navigation tree (navons / sections).",
  // Structure actions (#3096)
  ACTIONS_LABEL: "perc.ui.architecture.modern@Structure actions",
  ACTION_CREATE: "perc.ui.architecture.modern@Create section",
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
  TREE_PANEL_TITLE: message(KEYS.TREE_PANEL_TITLE),
  TREE_STRUCTURE_NOTE: message(KEYS.TREE_STRUCTURE_NOTE),
  REFRESH: message(KEYS.REFRESH),
  EMPTY_TITLE: message(KEYS.EMPTY_TITLE),
  EMPTY_BODY: message(KEYS.EMPTY_BODY),
  ACTIONS_LABEL: message(KEYS.ACTIONS_LABEL),
  ACTION_CREATE: message(KEYS.ACTION_CREATE),
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
};

/** Alias kept so older tests that look for TREE_READONLY_NOTE still compile if imported. */
export const TREE_READONLY_NOTE_LEGACY = ARCH_MSG.TREE_STRUCTURE_NOTE;
