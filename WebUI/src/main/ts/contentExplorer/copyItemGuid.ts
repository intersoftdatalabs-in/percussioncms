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

import type { PSPathItem } from "../api/contentExplorer/types";
import { isFolder } from "./selection";

/**
 * Outcome of Content → Copy item id (#4989 / parent #4530).
 * None, folder, missing id, and clipboard failure do not write a success
 * and must not change the selection.
 */
export type CopyItemGuidResult =
  | { status: "copied"; guid: string }
  | { status: "none" }
  | { status: "folder"; name: string }
  | { status: "no-id"; name: string }
  | { status: "failed"; guid: string };

function displayName(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name.length > 0) {
    return name;
  }
  const path = (item.path ?? "").trim();
  return path.length > 0 ? path : "item";
}

/**
 * Copy the selected page or asset content id (GUID). An empty selection
 * writes nothing. A folder is named and skipped — its folder id is not the
 * content id. A missing id and a clipboard rejection are not success.
 */
export async function copySelectedItemGuid(input: {
  item: PSPathItem | null | undefined;
  writeClipboard: (text: string) => Promise<void>;
}): Promise<CopyItemGuidResult> {
  const item = input.item ?? null;
  if (item == null) {
    return { status: "none" };
  }
  if (isFolder(item)) {
    return { status: "folder", name: displayName(item) };
  }
  const guid = item.id == null ? "" : String(item.id).trim();
  if (guid.length === 0) {
    return { status: "no-id", name: displayName(item) };
  }
  try {
    await input.writeClipboard(guid);
  } catch {
    return { status: "failed", guid };
  }
  return { status: "copied", guid };
}
