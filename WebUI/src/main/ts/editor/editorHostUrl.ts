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

import { withCmsContextPrefix } from "../assembly/assemblyHostUrl";

export const EDITOR_WINDOW_FEATURES =
  "popup=yes,width=960,height=800,scrollbars=yes,resizable=yes";

export type EditorHostMode = "edit" | "view" | "promote";

export function normalizeEditorMode(
  raw: string | null | undefined,
): EditorHostMode {
  const mode = raw?.trim().toLowerCase();
  if (mode === "view") {
    return "view";
  }
  if (mode === "promote") {
    return "promote";
  }
  return "edit";
}

export function buildEditorHostUrl(
  contentId: number,
  mode: EditorHostMode = "edit",
): string {
  const q = new URLSearchParams();
  q.set("entry", "editor");
  q.set("contentId", String(contentId));
  q.set("mode", mode);
  return withCmsContextPrefix(`/cm/app/spa.jsp?${q.toString()}`);
}

export function editorWindowName(contentId: number): string {
  return `percEditor_${contentId}`;
}
