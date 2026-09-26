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

/** One published/attempted item from job details (PSSitePublishItem). */
export interface PublishLogItem {
  status?: string;
  operation?: string;
  fileName?: string;
  fileLocation?: string;
  elapsedTime?: number | string;
  contentid?: string | number;
  revisionid?: string | number;
  itemStatusId?: string | number;
  templateid?: string | number;
  folderid?: string | number;
  [key: string]: unknown;
}

/** Normalize details POST response into a list of SitePublishItem rows. */
export function extractLogItems(details: unknown): PublishLogItem[] {
  if (details == null) {
    return [];
  }
  if (Array.isArray(details)) {
    return details as PublishLogItem[];
  }
  if (typeof details !== "object") {
    return [];
  }
  const obj = details as Record<string, unknown>;
  for (const key of ["SitePublishItem", "items", "results"]) {
    const v = obj[key];
    if (Array.isArray(v)) {
      return v as PublishLogItem[];
    }
    if (v && typeof v === "object") {
      return [v as PublishLogItem];
    }
  }
  return [];
}

function trimmedField(value: unknown): string {
  if (value == null) {
    return "";
  }
  return String(value).trim();
}

/**
 * Text an operator copies for one log item: published location, else file name.
 * Blank when both are missing — callers must not treat that as a successful copy.
 */
export function publishLogItemCopyText(item: PublishLogItem): string {
  const location = trimmedField(item.fileLocation);
  if (location.length > 0) {
    return location;
  }
  return trimmedField(item.fileName);
}

export type CopyLogLocationOutcome =
  | { kind: "copied"; text: string }
  | { kind: "missing" }
  | { kind: "clipboard" };

/**
 * Copy one item's location. Does not mutate the item or the log list.
 * Missing text is {@code missing}; a rejected or false write is {@code clipboard}.
 */
export async function copyPublishLogItemLocation(
  item: PublishLogItem,
  writeText: (text: string) => Promise<boolean>,
): Promise<CopyLogLocationOutcome> {
  const text = publishLogItemCopyText(item);
  if (!text) {
    return { kind: "missing" };
  }
  let ok = false;
  try {
    ok = await writeText(text);
  } catch {
    ok = false;
  }
  if (!ok) {
    return { kind: "clipboard" };
  }
  return { kind: "copied", text };
}

/**
 * Clipboard API write. Returns false when the API is missing or rejects
 * so the details panel can stay open with a named failure.
 */
export async function writeClipboardText(text: string): Promise<boolean> {
  if (
    typeof navigator === "undefined" ||
    navigator.clipboard?.writeText == null
  ) {
    return false;
  }
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

/** Client-side filter for log item table (status/operation/location/filename). */
export function filterLogItems(
  items: PublishLogItem[],
  query: string,
): PublishLogItem[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return items;
  }
  return items.filter((item) => {
    const hay = [
      item.status,
      item.operation,
      item.fileName,
      item.fileLocation,
      item.contentid,
    ]
      .map((x) => String(x ?? "").toLowerCase())
      .join(" ");
    return hay.includes(q);
  });
}
