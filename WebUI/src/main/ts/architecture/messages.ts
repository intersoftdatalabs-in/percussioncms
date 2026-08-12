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
 * Architecture / Navigation SPA chrome keys (#3094 / #3095 / parent #3092).
 * English after {@code @} is the source fallback when TMX is not loaded.
 * Small key set only — no multi-locale mass TMX backfill in this slice.
 */
const KEYS = {
  TITLE: "perc.ui.architecture.modern@Architecture",
  INTRO:
    "perc.ui.architecture.modern@Browse site navigation trees (navons / sections). Structure editing lands in a follow-on slice.",
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
  TREE_READONLY_NOTE:
    "perc.ui.architecture.modern@Read-only view. Create, edit, reorder, and delete land in a later Architecture slice.",
  REFRESH: "perc.ui.architecture.modern@Refresh",
  // Kept for older tests / deep links that still assert empty-shell keys
  EMPTY_TITLE: "perc.ui.architecture.modern@No site selected",
  EMPTY_BODY:
    "perc.ui.architecture.modern@Choose a site from the list above to browse its navigation tree (navons / sections).",
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
  TREE_READONLY_NOTE: message(KEYS.TREE_READONLY_NOTE),
  REFRESH: message(KEYS.REFRESH),
  EMPTY_TITLE: message(KEYS.EMPTY_TITLE),
  EMPTY_BODY: message(KEYS.EMPTY_BODY),
};
