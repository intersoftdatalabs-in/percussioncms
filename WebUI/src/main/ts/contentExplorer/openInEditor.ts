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
 * Default opener for explorer selections — navigates to the product editor
 * view using path-first (with id fallback). Mirrors the pattern used by
 * {@code WebUI/src/main/ts/home/HomeShell.tsx} defaultOpenItem so that the
 * explorer and Home shell open items through the same target.
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import {
  EDITOR_WINDOW_FEATURES,
  buildEditorHostUrl,
  editorWindowName,
} from "../editor/editorHostUrl";
import { parseExplorerContentId } from "./menuCatalogLoad";
import { isFolder } from "./selection";

/**
 * Open a content item in the React content editor. Folders stay in Explorer
 * browse — they are not workflowed pages (#3330). Does not open CM1
 * {@code ?view=editor}.
 */
export function openInEditor(item: PSPathItem): void {
  if (isFolder(item)) {
    return;
  }
  const contentId = parseExplorerContentId(item.id);
  if (contentId == null) {
    return;
  }
  if (typeof window === "undefined") {
    return;
  }
  window.open(
    buildEditorHostUrl(contentId, "edit"),
    editorWindowName(contentId),
    EDITOR_WINDOW_FEATURES,
  );
}