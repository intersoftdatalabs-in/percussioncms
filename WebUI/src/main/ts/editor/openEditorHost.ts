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

import { findItemByPath } from "../api/contentExplorer/pathApi";
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import {
  EDITOR_WINDOW_FEATURES,
  buildEditorHostUrl,
  editorWindowName,
  type EditorHostMode,
} from "./editorHostUrl";

export interface OpenEditorHostInput {
  id?: string | number | null;
  path?: string | null;
  mode?: EditorHostMode;
}

export interface OpenEditorHostDeps {
  openWindow?: (
    url: string,
    target?: string,
    features?: string,
  ) => Window | null;
  findByPath?: (path: string) => Promise<{ id?: string | number }>;
}

function defaultOpenWindow(
  url: string,
  target?: string,
  features?: string,
): Window | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.open(url, target, features);
}

/**
 * Open the React Content Editor host for an item id or CMS path.
 * Does not navigate to leftover {@code ?view=editor}.
 */
export async function openEditorHost(
  input: OpenEditorHostInput,
  deps: OpenEditorHostDeps = {},
): Promise<boolean> {
  let contentId = parseExplorerContentId(input.id ?? undefined);
  const path = input.path != null ? String(input.path).trim() : "";
  if (contentId == null && path) {
    const findByPath = deps.findByPath ?? findItemByPath;
    try {
      const item = await findByPath(path);
      contentId = parseExplorerContentId(item?.id);
    } catch {
      return false;
    }
  }
  if (contentId == null) {
    return false;
  }
  const opened = (deps.openWindow ?? defaultOpenWindow)(
    buildEditorHostUrl(contentId, input.mode ?? "edit"),
    editorWindowName(contentId),
    EDITOR_WINDOW_FEATURES,
  );
  return opened != null;
}
