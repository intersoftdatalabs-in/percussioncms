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

import { get, post, put } from "../api/client";
import { PATHS } from "../api/paths";
import type { EditorCheckoutUserInfo } from "./editorCheckout";

export interface ItemEditorField {
  name: string;
  value: string;
}

export interface ItemEditorFields {
  contentId: string;
  contentType: string;
  name: string;
  checkoutUser: string;
  /** CMS tip revision; PUT with a mismatch is HTTP 409. */
  revision?: number;
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
  const rawRev = body.revision ?? body.Revision;
  const revision =
    typeof rawRev === "number"
      ? rawRev
      : typeof rawRev === "string" && rawRev.trim()
        ? Number(rawRev)
        : 0;
  return {
    contentId: String(body.contentId ?? body.ContentId ?? ""),
    contentType: String(body.contentType ?? body.ContentType ?? ""),
    name: String(body.name ?? body.Name ?? ""),
    checkoutUser: String(body.checkoutUser ?? body.CheckoutUser ?? ""),
    revision: Number.isFinite(revision) ? revision : 0,
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

function unwrapUserInfo(payload: unknown): EditorCheckoutUserInfo {
  const root = asRecord(payload);
  const body =
    asRecord(
      root?.EditorItemLockInfo ??
        root?.editorItemLockInfo ??
        root?.ItemUserInfo ??
        root?.itemUserInfo,
    ) ?? root ?? {};
  return {
    itemName: String(body.itemName ?? body.ItemName ?? ""),
    checkOutUser: String(body.checkOutUser ?? body.CheckOutUser ?? ""),
    currentUser: String(body.currentUser ?? body.CurrentUser ?? ""),
    assignmentType: String(body.assignmentType ?? body.AssignmentType ?? ""),
  };
}

export async function checkoutEditorItem(
  itemId: string,
): Promise<EditorCheckoutUserInfo> {
  const res = await post<unknown>(PATHS.editorItemCheckout(itemId), {});
  return unwrapUserInfo(res);
}

export async function checkinEditorItem(itemId: string): Promise<void> {
  await post(PATHS.editorItemCheckin(itemId), {});
}
