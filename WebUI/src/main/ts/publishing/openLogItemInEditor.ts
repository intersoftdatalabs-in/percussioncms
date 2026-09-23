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
 * Open a publish-log item row in the React content editor (#4766).
 *
 * <p>Probes {@code GET …/itemmanagement/item/fields/{id}} before opening a
 * window. HTTP 403 and 404 are failures — they must not look like a successful
 * open.</p>
 */

import { isApiError } from "../api/client";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import { fetchItemEditorFields } from "../editor/itemFieldsApi";
import {
  closeReservedWindow,
  openEditorHost,
  type OpenEditorHostDeps,
} from "../editor/openEditorHost";

export type OpenLogItemReason =
  | "opened"
  | "missing_id"
  | "forbidden"
  | "not_found"
  | "failed";

export interface OpenLogItemResult {
  ok: boolean;
  reason: OpenLogItemReason;
}

export interface OpenLogItemDeps {
  probe?: (itemId: string) => Promise<unknown>;
  /**
   * Popup reserved on the click gesture ({@code about:blank}) so the later
   * navigation is not blocked after the fields probe.
   */
  reservedWindow?: Window | null;
  open?: (
    input: { id: number; mode: "edit" },
    deps?: OpenEditorHostDeps,
  ) => Promise<boolean>;
}

/** Map REST failures so 403/404 are not a successful editor open. */
export function logItemOpenErrorReason(err: unknown): OpenLogItemReason {
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
 * Probe the content id, then open the editor host. Missing ids and 403/404
 * never open a window.
 */
export async function openLogItemInEditor(
  contentId: string | number | null | undefined,
  deps: OpenLogItemDeps = {},
): Promise<OpenLogItemResult> {
  const reserved = deps.reservedWindow ?? null;
  const id = parseExplorerContentId(contentId ?? undefined);
  if (id == null) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "missing_id" };
  }
  const probe = deps.probe ?? ((itemId: string) => fetchItemEditorFields(itemId));
  try {
    await probe(String(id));
  } catch (err) {
    closeReservedWindow(reserved);
    return { ok: false, reason: logItemOpenErrorReason(err) };
  }
  const open = deps.open ?? openEditorHost;
  let opened = false;
  try {
    opened = await open({ id, mode: "edit" }, { reservedWindow: reserved });
  } catch {
    closeReservedWindow(reserved);
    return { ok: false, reason: "failed" };
  }
  if (!opened) {
    closeReservedWindow(reserved);
    return { ok: false, reason: "failed" };
  }
  return { ok: true, reason: "opened" };
}
