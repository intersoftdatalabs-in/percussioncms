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

import { getCsrfToken } from "../api/csrf";
import {
  get,
  SessionRedirectError,
  type ApiError,
} from "../api/client";
import { redirectToLoginOnUnauthorized } from "../app/auth/sessionHandlers";
import { PATHS } from "../api/paths";

export interface ItemEditorBinaryMeta {
  contentId: string;
  field: string;
  filename: string;
  contentType: string;
  present: boolean;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

export function unwrapBinaryMeta(payload: unknown): ItemEditorBinaryMeta {
  const root = asRecord(payload);
  const body =
    asRecord(root?.ItemEditorBinaryMeta ?? root?.itemEditorBinaryMeta) ??
    root ??
    {};
  return {
    contentId: String(body.contentId ?? body.ContentId ?? ""),
    field: String(body.field ?? body.Field ?? ""),
    filename: String(body.filename ?? body.Filename ?? ""),
    contentType: String(body.contentType ?? body.ContentType ?? ""),
    present: Boolean(body.present ?? body.Present),
  };
}

export function binaryFieldUrl(itemId: string, field: string): string {
  return `${PATHS.ITEM_EDITOR_BINARY}/${encodeURIComponent(itemId)}/${encodeURIComponent(field)}`;
}

export async function fetchItemEditorBinary(
  itemId: string,
  field: string,
): Promise<ItemEditorBinaryMeta> {
  const res = await get<unknown>(binaryFieldUrl(itemId, field));
  return unwrapBinaryMeta(res);
}

export async function uploadItemEditorBinary(
  itemId: string,
  field: string,
  file: File,
): Promise<ItemEditorBinaryMeta> {
  const form = new FormData();
  form.append("file", file, file.name);
  const headers = new Headers();
  headers.set("Accept", "application/json, text/plain, */*");
  const csrf = getCsrfToken();
  if (csrf) {
    headers.set(csrf.headerName, csrf.token);
  }
  const response = await fetch(binaryFieldUrl(itemId, field), {
    method: "PUT",
    headers,
    credentials: "same-origin",
    body: form,
  });
  if (response.status === 401) {
    redirectToLoginOnUnauthorized({ reason: "api-401" });
    throw new SessionRedirectError();
  }
  const text = await response.text();
  let parsed: unknown;
  try {
    parsed = text ? JSON.parse(text) : undefined;
  } catch {
    parsed = text;
  }
  if (!response.ok) {
    const error: ApiError = {
      status: response.status,
      statusText: response.statusText,
      body: parsed,
    };
    throw error;
  }
  return unwrapBinaryMeta(parsed);
}
