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

import { del, get, post, put } from "../client";
import {
  normalizeDisplayFormatGuid,
  objectGuidString,
  resolveDisplayFormatObjectGuid,
  unwrapDisplayFormat,
  unwrapDisplayFormatList,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import type { DisplayFormat, DisplayFormatColumn } from "./types";

/**
 * Writable identity fields for POST/PUT /services/displayformats. Name is the
 * catalog key (not renamed on PUT). Columns are not written from this chrome.
 */
export type DisplayFormatWriteBody = Pick<
  DisplayFormat,
  "name" | "internalName" | "label" | "displayName" | "description"
>;

/** Jackson / JAXB root for DisplayFormat (UNWRAP_ROOT_VALUE on POST/PUT). */
export const DISPLAY_FORMAT_ROOT = "DisplayFormat";

// Re-export shared GUID helpers so existing developer imports keep working.
export {
  normalizeDisplayFormatGuid,
  objectGuidString,
  resolveDisplayFormatObjectGuid,
  unwrapDisplayFormat,
  unwrapDisplayFormatList,
};

function asArray(payload: unknown): DisplayFormat[] {
  return unwrapDisplayFormatList(payload);
}

export function normalizeColumns(
  columns: DisplayFormat["columns"],
): DisplayFormatColumn[] {
  if (columns == null) return [];
  if (Array.isArray(columns)) return columns;
  const wrapped = columns.DisplayFormatColumn;
  if (wrapped == null) return [];
  return Array.isArray(wrapped) ? wrapped : [wrapped];
}

/** GET /services/displayformats */
export async function listDisplayFormats(): Promise<DisplayFormat[]> {
  const payload = await get<unknown>(PATHS.DISPLAY_FORMATS);
  return asArray(payload);
}

/** GET /services/displayformats/{idOrName} */
export async function getDisplayFormatDetail(idOrName: string): Promise<DisplayFormat> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.DISPLAY_FORMATS}/${key}`);
  return unwrapDisplayFormat(payload);
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
}

/** True when the name is a safe REST display-format key (no path chars). */
export function isSafeDisplayFormatName(name: string): boolean {
  if (!name) return false;
  return !name.includes("..") && !name.includes("/") && !name.includes("\\") && !name.includes("\0");
}

/** Trim a display-format name for write. Empty / null becomes "". */
export function normalizeDisplayFormatName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create
 * ({@code DisplayFormatAdaptor.requireValidName}): no whitespace, wildcards, or
 * path characters.
 */
export function isValidDisplayFormatName(name: string | undefined | null): boolean {
  const key = normalizeDisplayFormatName(name);
  if (!key) return false;
  if (containsWhitespace(key)) return false;
  if (key.includes("*") || key.includes("%")) return false;
  return isSafeDisplayFormatName(key);
}

/** Save is enabled when the format name is valid (create) or already loaded (edit). */
export function isDisplayFormatWriteReady(opts: { isNew: boolean; name: string }): boolean {
  if (opts.isNew) return isValidDisplayFormatName(opts.name);
  return Boolean(normalizeDisplayFormatName(opts.name));
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapDisplayFormatForWire(
  body: DisplayFormatWriteBody,
): Record<string, DisplayFormatWriteBody> {
  return { [DISPLAY_FORMAT_ROOT]: body };
}

/** POST /services/displayformats — Admin. Name required. Duplicate is 409. Invalid is 400. */
export async function createDisplayFormat(
  body: DisplayFormatWriteBody,
): Promise<DisplayFormat> {
  const payload = await post<unknown>(PATHS.DISPLAY_FORMATS, wrapDisplayFormatForWire(body));
  return unwrapDisplayFormat(payload);
}

/** PUT /services/displayformats/{idOrName} — Admin. Name is not renamed. Missing is 404. */
export async function saveDisplayFormat(
  idOrName: string,
  body: DisplayFormatWriteBody,
): Promise<DisplayFormat> {
  const payload = await put<unknown>(
    `${PATHS.DISPLAY_FORMATS}/${encodeURIComponent(idOrName)}`,
    wrapDisplayFormatForWire(body),
  );
  return unwrapDisplayFormat(payload);
}

/** DELETE /services/displayformats/{idOrName} — Admin. 204 on success; missing is 404. */
export async function deleteDisplayFormat(idOrName: string): Promise<void> {
  await del(`${PATHS.DISPLAY_FORMATS}/${encodeURIComponent(idOrName)}`);
}
