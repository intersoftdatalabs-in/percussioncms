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

import { post } from "../api/client";
import { PATHS } from "../api/paths";

export interface ItemCreateRequest {
  contentType: string;
  folderPath: string;
  name?: string;
  templateId?: string;
}

/**
 * Jackson / JAXB root for sitemanage {@code PSItemCreateRequest}
 * ({@code @XmlRootElement(name = "ItemCreateRequest")}). CXF JAXB rejects a
 * bare {@code contentType} field (Home Create asset stays on {@code about:blank}).
 */
export const ITEM_CREATE_REQUEST_ROOT = "ItemCreateRequest";

export type ItemCreateRequestEnvelope = {
  ItemCreateRequest: ItemCreateRequest;
};

export interface ItemCreateResult {
  itemId: string;
  folderPath: string;
  name: string;
  contentType: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Wrap create fields under {@link ITEM_CREATE_REQUEST_ROOT}.
 * Does not double-wrap an already-enveloped payload.
 */
export function wrapItemCreateRequest(
  request: ItemCreateRequest | ItemCreateRequestEnvelope,
): ItemCreateRequestEnvelope {
  const rec = asRecord(request);
  if (rec != null) {
    const nested = rec[ITEM_CREATE_REQUEST_ROOT];
    if (asRecord(nested) != null) {
      return { ItemCreateRequest: nested as ItemCreateRequest };
    }
  }
  return { ItemCreateRequest: request as ItemCreateRequest };
}

export function unwrapItemCreateResult(payload: unknown): ItemCreateResult {
  const root = asRecord(payload);
  const body =
    asRecord(root?.ItemCreateResult ?? root?.itemCreateResult) ?? root ?? {};
  return {
    itemId: String(body.itemId ?? body.ItemId ?? ""),
    folderPath: String(body.folderPath ?? body.FolderPath ?? ""),
    name: String(body.name ?? body.Name ?? ""),
    contentType: String(body.contentType ?? body.ContentType ?? ""),
  };
}

export async function createEditorItem(
  req: ItemCreateRequest,
): Promise<ItemCreateResult> {
  const res = await post<unknown>(
    PATHS.ITEM_CREATE,
    wrapItemCreateRequest(req),
  );
  const out = unwrapItemCreateResult(res);
  if (!out.itemId.trim()) {
    throw new Error("Create returned no item id");
  }
  return out;
}
