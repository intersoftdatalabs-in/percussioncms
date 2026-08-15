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
 * New copy / promotable version for Explorer (itemmanagement REST).
 */

import { post } from "../client";
import { PATHS } from "../paths";

export interface ItemCopyResult {
  itemId: string;
  folderPath: string;
  promotable: boolean;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

export function unwrapItemCopyResult(payload: unknown): ItemCopyResult {
  const obj = asRecord(payload);
  if (obj == null) {
    throw new Error("Copy result was empty");
  }
  const inner =
    obj.ItemCopyResult && typeof obj.ItemCopyResult === "object"
      ? (obj.ItemCopyResult as Record<string, unknown>)
      : obj;
  const itemId = String(inner.itemId ?? inner.ItemId ?? "").trim();
  if (!itemId) {
    throw new Error("Copy result was missing item id");
  }
  return {
    itemId,
    folderPath: String(inner.folderPath ?? inner.FolderPath ?? ""),
    promotable: Boolean(inner.promotable ?? inner.Promotable),
  };
}

export async function createNewCopy(itemId: string): Promise<ItemCopyResult> {
  const id = encodeURIComponent(String(itemId).trim());
  const res = await post<unknown>(`${PATHS.ITEM_NEW_COPY}/${id}`);
  return unwrapItemCopyResult(res);
}

export async function createPromotableVersion(
  itemId: string,
): Promise<ItemCopyResult> {
  const id = encodeURIComponent(String(itemId).trim());
  const res = await post<unknown>(`${PATHS.ITEM_PROMOTABLE_VERSION}/${id}`);
  return unwrapItemCopyResult(res);
}
