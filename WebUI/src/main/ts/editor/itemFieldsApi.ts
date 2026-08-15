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

import { get, put } from "../api/client";
import { PATHS } from "../api/paths";

export interface ItemEditorField {
  name: string;
  value: string;
}

export interface ItemEditorFields {
  contentId: string;
  contentType: string;
  name: string;
  checkoutUser: string;
  fields: ItemEditorField[];
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function unwrapFields(payload: unknown): ItemEditorFields {
  const root = asRecord(payload);
  const body =
    asRecord(root?.ItemEditorFields ?? root?.itemEditorFields) ?? root ?? {};
  const rawFields = body.fields ?? body.Fields;
  const list = Array.isArray(rawFields) ? rawFields : [];
  return {
    contentId: String(body.contentId ?? body.ContentId ?? ""),
    contentType: String(body.contentType ?? body.ContentType ?? ""),
    name: String(body.name ?? body.Name ?? ""),
    checkoutUser: String(body.checkoutUser ?? body.CheckoutUser ?? ""),
    fields: list
      .map((row) => {
        const rec = asRecord(row);
        if (!rec) {
          return null;
        }
        const name = String(rec.name ?? rec.Name ?? "").trim();
        if (!name) {
          return null;
        }
        return {
          name,
          value: String(rec.value ?? rec.Value ?? ""),
        };
      })
      .filter((row): row is ItemEditorField => row != null),
  };
}

export async function fetchItemEditorFields(
  itemId: string,
): Promise<ItemEditorFields> {
  const res = await get<unknown>(
    `${PATHS.ITEM_EDITOR_FIELDS}/${encodeURIComponent(itemId)}`,
  );
  return unwrapFields(res);
}

export async function saveItemEditorFields(
  itemId: string,
  payload: ItemEditorFields,
): Promise<ItemEditorFields> {
  const res = await put<unknown>(
    `${PATHS.ITEM_EDITOR_FIELDS}/${encodeURIComponent(itemId)}`,
    payload,
  );
  return unwrapFields(res);
}

export async function checkoutEditorItem(itemId: string): Promise<void> {
  await get(`${PATHS.ITEM_WORKFLOW_CHECKOUT}${encodeURIComponent(itemId)}`);
}

export async function checkinEditorItem(itemId: string): Promise<void> {
  await get(`${PATHS.ITEM_WORKFLOW_CHECKIN}${encodeURIComponent(itemId)}`);
}
