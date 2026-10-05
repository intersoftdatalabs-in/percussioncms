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
 * Open one related content item from the Explorer relationships list (#5218).
 *
 * <p>Probes {@code GET …/itemmanagement/item/fields/{id}} before navigating.
 * Folders, a missing content id, and HTTP 403/404 do not open a window and
 * are not a successful open. The Explorer selection is left unchanged.</p>
 */

import { isApiError } from "../api/client";
import { fetchItemEditorFields } from "../editor/itemFieldsApi";
import {
  closeReservedWindow,
  openEditorHost,
  type OpenEditorHostDeps,
} from "../editor/openEditorHost";

export type OpenRelatedItemReason =
  | "opened"
  | "no_content"
  | "folder"
  | "forbidden"
  | "not_found"
  | "failed";

export interface OpenRelatedItemRequest {
  contentId: number | null | undefined;
  /** True when the relationship row is folder membership. */
  folder: boolean;
}

export interface OpenRelatedItemResult {
  ok: boolean;
  reason: OpenRelatedItemReason;
  /** Extra text for the panel when the open did not succeed. */
  detail?: string;
}

export interface OpenRelatedItemDeps {
  probe?: (itemId: string) => Promise<unknown>;
  reservedWindow?: Window | null;
  open?: (
    input: { id: number; mode: "edit" },
    deps?: OpenEditorHostDeps,
  ) => Promise<boolean>;
}

/** Map a fields probe failure. 403 and 404 are not a successful open. */
export function relatedItemOpenErrorReason(err: unknown): OpenRelatedItemReason {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}

/**
 * Probe a related content id, then open EditorHost. Does not change the
 * Explorer selection and does not add or remove a relationship.
 */
export async function openRelatedItemInEditor(
  request: OpenRelatedItemRequest,
  deps: OpenRelatedItemDeps = {},
): Promise<OpenRelatedItemResult> {
  const reserved = deps.reservedWindow ?? null;
  if (request.folder) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "folder" };
  }
  const id = request.contentId;
  if (id == null || !Number.isFinite(id) || id <= 0) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "no_content" };
  }
  const probe =
    deps.probe ?? ((itemId: string) => fetchItemEditorFields(itemId));
  try {
    await probe(String(id));
  } catch (err) {
    closeReservedWindow(reserved);
    const reason = relatedItemOpenErrorReason(err);
    const detail =
      err instanceof Error
        ? err.message
        : typeof err === "string"
          ? err
          : "";
    return detail && reason === "failed"
      ? { ok: false, reason, detail }
      : { ok: false, reason };
  }
  const open = deps.open ?? openEditorHost;
  let opened = false;
  try {
    opened = await open({ id, mode: "edit" }, { reservedWindow: reserved });
  } catch (err) {
    closeReservedWindow(reserved);
    const detail = err instanceof Error ? err.message : "";
    return { ok: false, reason: "failed", ...(detail ? { detail } : {}) };
  }
  if (!opened) {
    closeReservedWindow(reserved);
    return {
      ok: false,
      reason: "failed",
      detail: "editor window was not opened",
    };
  }
  return { ok: true, reason: "opened" };
}
