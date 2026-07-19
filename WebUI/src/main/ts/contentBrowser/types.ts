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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ContentBrowser host integration types (US2 — feature 992-react-content-explorer).
 *
 * <p>Mirror of {@code specs/992-react-content-explorer/contracts/content-browser-host.md}.
 * Imported by both the {@code ContentBrowser} component and any host that
 * embeds it (asset picker, page picker, AA ContentBrowserDialog, folder
 * picker, optional Home Library consumer from 989-react-cui-widget-builder).</p>
 */

import type { SelectionResult, SelectionItem } from "../api/contentExplorer/types";

export type ContentBrowserMode = "select" | "browse";
export type ContentBrowserRoots = "sites" | "assets" | "all" | string[];

export interface ContentBrowserProps {
  /** "select" requires user confirmation; "browse" may omit. */
  mode?: ContentBrowserMode;
  /** Allow multiple items. */
  multiSelect?: boolean;
  /** Folders selectable. */
  allowFolderSelect?: boolean;
  /** Items (non-folder) selectable. */
  allowItemSelect?: boolean;
  /** If set, only items whose type/category is in this list are confirmable. */
  allowedTypes?: string[] | null;
  /** Optional category filter. */
  allowedCategories?: string[] | null;
  /** Starting folder path; defaults to product root. */
  initialPath?: string | null;
  /** Root set; "all" matches path services default. */
  roots?: ContentBrowserRoots;
  /** Show search pane when API is available (US5). */
  enableSearch?: boolean;
  /** Dialog title; TMX key preferred. */
  title?: string | null;
  /** Required in select mode. */
  onConfirm?: (selection: SelectionResult) => void;
  onCancel?: () => void;
  onError?: (message: string) => void;
}

export type { SelectionItem, SelectionResult };