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

/**
 * Create a new content item from the React Content Editor host (#4646).
 *
 * <p>POST {@code /services/itemmanagement/item/create} via {@code createEditorItem}.
 * HTTP 403 / 400 / 404 are failures, not success.</p>
 */

import { isApiError } from "../api/client";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import type { EditorHostMode } from "./editorHostUrl";
import type { ItemCreateRequest } from "./itemCreateApi";

export type EditorCreateErrorReason =
  | "forbidden"
  | "not_found"
  | "bad_request"
  | "incomplete"
  | "failed";

/** Create is available whenever the host is not in promote mode. */
export function canCreateFromEditor(mode: EditorHostMode): boolean {
  return mode !== "promote";
}

export function editorCreateRequestReady(
  contentType: string,
  folderPath: string,
): boolean {
  return contentType.trim().length > 0 && folderPath.trim().length > 0;
}

export function buildEditorCreateRequest(
  contentType: string,
  folderPath: string,
  name?: string,
  templateId?: string,
): ItemCreateRequest | null {
  const type = contentType.trim();
  const folder = folderPath.trim();
  if (!editorCreateRequestReady(type, folder)) {
    return null;
  }
  const req: ItemCreateRequest = { contentType: type, folderPath: folder };
  const n = (name ?? "").trim();
  if (n) {
    req.name = n;
  }
  const t = (templateId ?? "").trim();
  if (t) {
    req.templateId = t;
  }
  return req;
}

export function parseCreateLandingContentId(
  itemId: string | number | null | undefined,
): number | null {
  return parseExplorerContentId(itemId);
}

export function editorCreateErrorReason(err: unknown): EditorCreateErrorReason {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
    if (err.status === 400) {
      return "bad_request";
    }
  }
  return "failed";
}
