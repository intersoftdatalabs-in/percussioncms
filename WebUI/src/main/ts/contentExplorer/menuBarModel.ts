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
 * DCE-style Explorer top menu bar model (#2731 / parent #2400).
 *
 * <p>Mirrors the top-level groups in Desktop Content Explorer
 * {@code ContentExplorerMenu.xml}: Content / View / Help. Leaf command
 * ids are product shell chrome (not server action menus); server-driven
 * nested actions stay on {@link ActionToolbar}.</p>
 */

import { EXPLORER_MSG } from "./messages";

/** Stable command ids invoked by the Explorer menu bar. */
export type ExplorerMenuCommandId =
  | "content-search"
  | "content-clipboard-add"
  | "content-site-copy"
  | "view-refresh"
  | "view-search"
  | "view-security"
  | "view-translations"
  | "view-relationships"
  | "view-dependencies"
  | "view-clipboard"
  | "help-explorer"
  | "help-about";

export type ExplorerMenuBarGroupId = "content" | "view" | "help";

export interface ExplorerMenuBarItem {
  /** Stable id for test selectors and handlers. */
  id: ExplorerMenuCommandId;
  /** TMX message key for the label. */
  labelKey: string;
  /** Optional aria-label key (falls back to labelKey). */
  ariaLabelKey?: string;
  /** Optional stable data-testid (legacy shell toggles reuse these). */
  testId?: string;
  /** When true, item is rendered as a checked toggle (aria-checked). */
  toggle?: boolean;
  /** Optional disabled predicate flag name resolved by the host. */
  disabledWhen?: "noSelection" | "noClipboardContext" | "noSiteContext";
}

export interface ExplorerMenuBarGroup {
  id: ExplorerMenuBarGroupId;
  /** TMX message key for the top-level menu label. */
  labelKey: string;
  /** Access key hint (display only; not auto-bound for SPA a11y). */
  mnemonic?: string;
  items: ReadonlyArray<ExplorerMenuBarItem>;
}

/**
 * Static DCE-equivalent grouping for the modern Explorer shell chrome.
 * Pure data — no React / i18n resolution — so Vitest can assert structure
 * without a DOM.
 */
export function buildExplorerMenuBarGroups(): ReadonlyArray<ExplorerMenuBarGroup> {
  return [
    {
      id: "content",
      labelKey: EXPLORER_MSG.MENU_CONTENT,
      mnemonic: "C",
      items: [
        {
          id: "content-search",
          labelKey: EXPLORER_MSG.SEARCH_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_SEARCH_ARIA,
          testId: "explorer-menu-content-search",
        },
        {
          id: "content-clipboard-add",
          labelKey: EXPLORER_MSG.CLIPBOARD_ADD,
          ariaLabelKey: EXPLORER_MSG.CLIPBOARD_ADD,
          testId: "explorer-clipboard-add",
          disabledWhen: "noSelection",
        },
        {
          id: "content-site-copy",
          labelKey: EXPLORER_MSG.SITE_COPY_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_SITE_COPY_ARIA,
          testId: "explorer-content-site-copy",
          toggle: true,
          disabledWhen: "noSiteContext",
        },
      ],
    },
    {
      id: "view",
      labelKey: EXPLORER_MSG.MENU_VIEW,
      mnemonic: "V",
      items: [
        {
          id: "view-refresh",
          labelKey: EXPLORER_MSG.MENU_VIEW_REFRESH,
          testId: "explorer-menu-view-refresh",
        },
        {
          id: "view-search",
          labelKey: EXPLORER_MSG.SEARCH_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_SEARCH_ARIA,
          testId: "explorer-toggle-search",
          toggle: true,
        },
        {
          id: "view-security",
          labelKey: EXPLORER_MSG.SECURITY_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_SECURITY_ARIA,
          testId: "explorer-toggle-security",
          toggle: true,
        },
        {
          id: "view-translations",
          labelKey: EXPLORER_MSG.TRANSLATIONS_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_TRANSLATIONS_ARIA,
          testId: "explorer-toggle-translations",
          toggle: true,
        },
        {
          id: "view-relationships",
          labelKey: EXPLORER_MSG.RELATIONSHIPS_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_RELATIONSHIPS_ARIA,
          testId: "explorer-toggle-relationships",
          toggle: true,
        },
        {
          id: "view-dependencies",
          labelKey: EXPLORER_MSG.DEPENDENCY_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_DEPENDENCIES_ARIA,
          testId: "explorer-toggle-dependencies",
          toggle: true,
        },
        {
          id: "view-clipboard",
          labelKey: EXPLORER_MSG.CLIPBOARD_TITLE,
          ariaLabelKey: EXPLORER_MSG.TOGGLE_CLIPBOARD_ARIA,
          testId: "explorer-toggle-clipboard",
          toggle: true,
          disabledWhen: "noClipboardContext",
        },
      ],
    },
    {
      id: "help",
      labelKey: EXPLORER_MSG.MENU_HELP,
      mnemonic: "H",
      items: [
        {
          id: "help-explorer",
          labelKey: EXPLORER_MSG.MENU_HELP_EXPLORER,
          testId: "explorer-menu-help-explorer",
        },
        {
          id: "help-about",
          labelKey: EXPLORER_MSG.MENU_HELP_ABOUT,
          testId: "explorer-menu-help-about",
        },
      ],
    },
  ];
}

/** Top-level group ids in DCE order (Content → View → Help). */
export function explorerMenuBarGroupIds(): ReadonlyArray<ExplorerMenuBarGroupId> {
  return buildExplorerMenuBarGroups().map((g) => g.id);
}
