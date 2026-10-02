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
 * Open one existing Explorer translation variant in the React editor (#5037).
 *
 * <p>Probes {@code GET …/itemmanagement/item/fields/{id}} before navigating.
 * Folders, a missing content id, and HTTP 403/404 do not open a window and
 * are not a successful open.</p>
 */

import { isApiError } from "../api/client";
import { fetchItemEditorFields } from "../editor/itemFieldsApi";
import {
  closeReservedWindow,
  openEditorHost,
  type OpenEditorHostDeps,
} from "../editor/openEditorHost";

export type OpenTranslationVariantReason =
  | "opened"
  | "no_variant"
  | "folder"
  | "forbidden"
  | "not_found"
  | "failed";

export interface OpenTranslationVariantRequest {
  contentId: number | null | undefined;
  /** True when the Explorer selection is a folder or site. */
  folder: boolean;
}

export interface OpenTranslationVariantResult {
  ok: boolean;
  reason: OpenTranslationVariantReason;
  /** Extra text for the panel when the open did not succeed. */
  detail?: string;
}

export interface OpenTranslationVariantDeps {
  probe?: (itemId: string) => Promise<unknown>;
  reservedWindow?: Window | null;
  open?: (
    input: { id: number; mode: "edit" },
    deps?: OpenEditorHostDeps,
  ) => Promise<boolean>;
}

/** Map a fields probe failure. 403 and 404 are not a successful open. */
export function translationVariantOpenErrorReason(
  err: unknown,
): OpenTranslationVariantReason {
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
 * Probe an existing variant, then open EditorHost. Does not create a locale.
 */
export async function openTranslationVariantInEditor(
  request: OpenTranslationVariantRequest,
  deps: OpenTranslationVariantDeps = {},
): Promise<OpenTranslationVariantResult> {
  const reserved = deps.reservedWindow ?? null;
  if (request.folder) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "folder" };
  }
  const id = request.contentId;
  if (id == null || !Number.isFinite(id) || id <= 0) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "no_variant" };
  }
  const probe =
    deps.probe ?? ((itemId: string) => fetchItemEditorFields(itemId));
  try {
    await probe(String(id));
  } catch (err) {
    closeReservedWindow(reserved);
    const reason = translationVariantOpenErrorReason(err);
    const detail =
      err instanceof Error
        ? err.message
        : typeof err === "string"
          ? err
          : "";
    return detail && reason === "failed" ? { ok: false, reason, detail } : { ok: false, reason };
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
    return { ok: false, reason: "failed", detail: "editor window was not opened" };
  }
  return { ok: true, reason: "opened" };
}
