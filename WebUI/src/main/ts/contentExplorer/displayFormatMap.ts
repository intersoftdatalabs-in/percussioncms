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
 * Map REST display-format column definitions onto DetailList column ids
 * and resolve cell values from {@link PSPathItem} + displayProperties
 * (path list {@code columnData}).
 *
 * <p>Pure helpers for #2400 / FR-027 — unit-tested without rendering.</p>
 */

import type { DisplayFormatColumn } from "../api/contentExplorer/displayFormatsApi";
import { isNumericDisplayFormatId } from "../api/contentExplorer/pathApi";
import type { PSPathItem } from "../api/contentExplorer/types";
import type { DetailColumnId, DetailDisplayFormat } from "./DetailList";

/** Re-export the pathmanagement id predicate so callers share one implementation. */
export { isNumericDisplayFormatId };

/** Known CX system field sources → list column ids. */
const SOURCE_TO_COLUMN: Record<string, DetailColumnId> = {
  sys_title: "title",
  sys_contentid: "name",
  sys_contenttypename: "type",
  sys_contenttype: "type",
  sys_workflow: "workflow",
  sys_workflowid: "workflow",
  sys_contentlastmodifieddate: "modified",
  sys_contentmodifieddate: "modified",
  name: "name",
  title: "title",
  type: "type",
  path: "path",
  category: "category",
  modified: "modified",
  workflow: "workflow",
};

/**
 * Map display-format columns to DetailList column ids (order-preserving,
 * de-duplicated). Unknown sources are skipped; empty result falls back to
 * the DetailList default (name/type/path).
 */
export function mapDisplayFormatToDetailColumns(
  columns: DisplayFormatColumn[] | undefined | null,
): DetailColumnId[] {
  if (!columns || columns.length === 0) {
    return [];
  }
  const out: DetailColumnId[] = [];
  for (const col of columns) {
    if (!col) continue;
    const source = (col.source ?? col.displayName ?? "").trim().toLowerCase();
    if (!source) continue;
    const mapped =
      SOURCE_TO_COLUMN[source] ??
      SOURCE_TO_COLUMN[source.replace(/^sys_/, "")] ??
      undefined;
    if (mapped && !out.includes(mapped)) {
      out.push(mapped);
    }
  }
  return out;
}

export function toDetailDisplayFormat(
  columns: DisplayFormatColumn[] | undefined | null,
): DetailDisplayFormat | undefined {
  const mapped = mapDisplayFormatToDetailColumns(columns);
  if (mapped.length === 0) return undefined;
  return { columns: mapped };
}

/**
 * Resolve a cell value preferring displayProperties / columnData map keys
 * used by pathmanagement when a displayFormatId is applied.
 */
export function resolvePathItemProperty(
  item: PSPathItem,
  ...keys: string[]
): string {
  const props = item.displayProperties;
  if (props && typeof props === "object") {
    for (const key of keys) {
      const v = props[key];
      if (v != null && String(v).length > 0) {
        return String(v);
      }
    }
    // Case-insensitive fallback (server keys vary by casing).
    const entries = Object.entries(props);
    for (const key of keys) {
      const lower = key.toLowerCase();
      for (const [k, v] of entries) {
        if (k.toLowerCase() === lower && v != null && String(v).length > 0) {
          return String(v);
        }
      }
    }
  }
  return "";
}

/**
 * Extract the native uuid from a Percussion Guid wire string.
 *
 * <p>{@code PSGuid.toString()} is {@code {hostId}-{type}-{uuid}} (three
 * numeric segments). Untyped form is {@code {hostId}-{uuid}}. In both
 * contracts the last segment is the uuid used as pathmanagement
 * {@code displayFormatId}. Reject any other hyphen layout so a mid-string
 * type id cannot be mistaken for the uuid.</p>
 */
export function uuidFromPercussionGuidString(guidStr: string): string {
  const parts = guidStr.trim().split("-");
  if (parts.length !== 2 && parts.length !== 3) {
    return "";
  }
  if (!parts.every((p) => /^\d+$/.test(p))) {
    return "";
  }
  const uuid = parts[parts.length - 1];
  return isNumericDisplayFormatId(uuid) ? uuid : "";
}

/**
 * Numeric id accepted by pathmanagement {@code displayFormatId}
 * ({@code Integer}). Names such as {@code FolderList} 400 the list.
 *
 * <p>Prefers {@code displayId > 0}, then Guid uuid, then the uuid segment
 * of a typed/untyped Guid string. {@code displayId === 0} is not a valid
 * pathmanagement id (unpersisted / unset) — do not stringify it.
 * Returns empty when no numeric id can be resolved — callers must not
 * send the empty string as {@code displayFormatId}.</p>
 */
export function resolvePathmanagementDisplayFormatId(df: {
  displayId?: number;
  guid?: { uuid?: number | string; stringValue?: string };
  guidString?: string;
}): string {
  if (df.displayId != null && Number.isFinite(df.displayId) && df.displayId > 0) {
    return String(df.displayId);
  }
  const uuid = df.guid?.uuid;
  const uuidNum = typeof uuid === "number" ? uuid : Number(uuid);
  if (Number.isFinite(uuidNum) && uuidNum > 0) {
    return String(uuidNum);
  }
  const guidStr = (df.guid?.stringValue || df.guidString || "").trim();
  if (guidStr) {
    return uuidFromPercussionGuidString(guidStr);
  }
  return "";
}

/**
 * Stable key for a display format option in the shell selector.
 *
 * <p>{@code displayId === 0} is never used as the option value: that id is
 * not accepted by pathmanagement, so keying React {@code &lt;option&gt;}
 * as {@code "0"} would select a format that then 400s the folder list.
 * Fall through to Guid uuid, then {@code internalName}/{@code name}.</p>
 */
export function displayFormatOptionKey(df: {
  displayId?: number;
  name?: string;
  internalName?: string;
  guid?: { uuid?: number | string; stringValue?: string };
  guidString?: string;
}): string {
  return (
    resolvePathmanagementDisplayFormatId(df) ||
    df.internalName ||
    df.name ||
    ""
  );
}
