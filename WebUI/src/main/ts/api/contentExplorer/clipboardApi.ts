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
 * Paste transport for the modern Content Explorer (US7 / T075).
 *
 * <p>Resolves a {@link ClipboardPasteTransport} call against the
 * existing per-kind REST endpoints; the transport is overridable in
 * tests (Vitest) and the live CMS (browser).</p>
 *
 * <p>Endpoint mapping (verified 2026-07-20 against `paths.ts` and
 * the sitemanage REST services):</p>
 * <ul>
 *   <li>page  → {@code POST /Rhythmyx/rest/pagemanagement/page/copy/{id}}
 *             ({@code PSPageRestService#copy(id, addToRecent)} on the
 *             server).</li>
 *   <li>asset → {@code POST /Rhythmyx/rest/pathmanagement/path/moveItem}
 *             with {@code copy:true}; the pathApi already wraps it.</li>
 *   <li>folder → {@code POST /Rhythmyx/rest/pathmanagement/path/moveItem}
 *              with {@code copy:true}; same pathApi wrapper.</li>
 * </ul>
 * <p>The dispatcher in {@code pasteClipboardItems} chooses the right
 * call per item kind, in clipboard-source order.</p>
 */

import { post } from "../client";
import { PATHS } from "../paths";
import { moveItem } from "./pathApi";
import type {
  ClipboardItem,
  ClipboardPasteResultItem,
  ClipboardPasteSummary,
  PSMoveFolderItem,
} from "./types";

/**
 * Single-item paste transport. Returns `undefined` when the call
 * succeeded (or when the server returned an empty 200), throws
 * otherwise. Overridable for tests via {@link pasteClipboardItems}'s
 * second arg.
 */
export type ClipboardPasteTransport = (
  item: ClipboardItem,
) => Promise<void>;

/**
 * Default per-kind transport. Uses {@code PSPageRestService#copy} for
 * pages (server-side duplicator that respects workflow + community +
 * template) and {@code moveItem({copy:true})} for assets and folders
 * (pathApi wrapper, already wired for US1).
 */
async function defaultPasteTransport(item: ClipboardItem): Promise<void> {
  switch (item.kind) {
    case "page": {
      await post<void>(
        `${PATHS.PAGE_COPY}/${encodeURIComponent(item.id)}?addToRecent=false`,
        {},
      );
      return;
    }
    case "asset":
    case "folder": {
      const body: PSMoveFolderItem = {
        sourcePath: item.path,
        targetPath: item.path,
        copy: true,
      };
      await moveItem(body);
      return;
    }
  }
}

/**
 * Apply a {@link ClipboardPasteTransport} to each item in clipboard
 * order. Reports per-item outcomes so the explorer shell can refresh
 * the tree / list and surface partial-failure messages.
 *
 * <p>Server is authoritative: the call resolves to the per-item
 * promise results via {@link Promise.allSettled}, so a single failure
 * does not block the other items. The returned {@link ClipboardPasteSummary}
 * is the contract for {@link Clipboard.component} consumers.</p>
 */
export async function pasteClipboardItems(
  items: ReadonlyArray<ClipboardItem>,
  operation: "copy" | "cut",
  transport: ClipboardPasteTransport = defaultPasteTransport,
): Promise<ClipboardPasteSummary> {
  const settled = await Promise.allSettled(items.map((it) => transport(it)));
  return {
    operation,
    results: items.map<ClipboardPasteResultItem>((it, idx) => {
      const s = settled[idx];
      if (!s) {
        return { item: it, ok: false, message: "missing settled result" };
      }
      if (s.status === "fulfilled") {
        return { item: it, ok: true };
      }
      const reason = s.reason;
      // Carry the HTTP status through when the rejection is an ApiError so
      // consumers (e.g. ClipboardPanel) can distinguish recoverable 409
      // conflicts from generic 500 / network failures without parsing the
      // message. See Edge Cases #3 / T092c.
      const status =
        reason && typeof reason === "object" && "status" in reason &&
        typeof (reason as { status?: unknown }).status === "number"
          ? (reason as { status: number }).status
          : undefined;
      let message: string;
      if (reason instanceof Error) {
        message = reason.message;
      } else if (status != null) {
        // ApiError (or any { status, statusText }) — build a clear,
        // human-readable message so the UI can render the conflict
        // without parsing JSON.
        const statusText =
          reason && typeof reason === "object" && "statusText" in reason
            ? String((reason as { statusText?: unknown }).statusText ?? "")
            : "";
        message = statusText ? `${status} ${statusText}` : `${status}`;
      } else {
        message = String(reason);
      }
      return status != null
        ? { item: it, ok: false, message, status }
        : { item: it, ok: false, message };
    }),
  };
}
