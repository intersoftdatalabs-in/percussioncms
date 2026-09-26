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
 * Explorer checkout-owner lookup (#4910). Read-only; HTTP 403 stays on the panel.
 */

import { get, isApiError } from "../api/client";
import { PATHS } from "../api/paths";

export interface CheckoutOwnerInfo {
  checkOutUser: string;
  currentUser: string;
  itemName: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

export function unwrapCheckoutOwner(payload: unknown): CheckoutOwnerInfo {
  const root = asRecord(payload);
  const body =
    asRecord(
      root?.EditorItemLockInfo ??
        root?.editorItemLockInfo ??
        root?.ItemUserInfo ??
        root?.itemUserInfo,
    ) ??
    root ??
    {};
  return {
    itemName: String(body.itemName ?? body.ItemName ?? ""),
    checkOutUser: String(body.checkOutUser ?? body.CheckOutUser ?? ""),
    currentUser: String(body.currentUser ?? body.CurrentUser ?? ""),
  };
}

export async function lookupCheckoutOwner(itemId: string): Promise<CheckoutOwnerInfo> {
  const id = String(itemId ?? "").trim();
  if (!id) {
    throw new Error("lookupCheckoutOwner requires itemId");
  }
  const res = await get<unknown>(PATHS.editorItemCheckoutOwner(id));
  return unwrapCheckoutOwner(res);
}

export function checkoutOwnerErrorReason(err: unknown): "forbidden" | "failed" {
  if (isApiError(err) && err.status === 403) {
    return "forbidden";
  }
  return "failed";
}
