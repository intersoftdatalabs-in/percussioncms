/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Real sitemanage-backed APIs for Home Gadgets widgets.
 *
 * <p>Classic Shindig gadgets called these paths via {@code perc_path_constants}.
 * Invented React paths such as {@code /services/activity/contentactivity?limit=}
 * and {@code /services/dashboardmanagement/gadget/workflow-status} do not exist.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import { fetchSites } from "../home/homeApi";

/** Classic duration types for content activity. */
export type ActivityDurationType = "days" | "weeks" | "months" | "years";

/** One row from {@code PSContentActivity}. */
export interface ContentActivityRow {
  siteName?: string;
  name: string;
  path?: string;
  publishedItems: number;
  pendingItems: number;
  newItems: number;
  updatedItems: number;
  archivedItems: number;
}

/** One page/item property row from {@code PSItemProperties}. */
export interface ItemPropertiesRow {
  id?: string;
  name?: string;
  status?: string;
  workflow?: string;
  path?: string;
  type?: string;
  lastModifier?: string;
  lastModifiedDate?: string;
}

/** Aggregated status bucket for Pages By Status. */
export interface WorkflowStatusBucket {
  /** Workflow state name (e.g. Draft, Pending). */
  state: string;
  count: number;
  /** Sample item names for display (capped). */
  sampleNames: string[];
}

export interface PagesByStatusResult {
  path: string;
  workflow: string;
  buckets: WorkflowStatusBucket[];
  totalItems: number;
}

function asRecord(v: unknown): Record<string, unknown> | null {
  return v && typeof v === "object" ? (v as Record<string, unknown>) : null;
}

/**
 * Unwrap Jackson root-name list envelopes used by sitemanage
 * (e.g. {@code { ContentActivity: [...] }} or a bare array).
 */
export function unwrapNamedList(data: unknown, rootName: string): unknown[] {
  if (Array.isArray(data)) {
    return data;
  }
  const obj = asRecord(data);
  if (!obj) {
    return [];
  }
  const named = obj[rootName];
  if (Array.isArray(named)) {
    return named;
  }
  if (named && typeof named === "object") {
    return [named];
  }
  // Some responses nest under the same key as a map of arrays
  for (const v of Object.values(obj)) {
    if (Array.isArray(v)) {
      return v;
    }
  }
  return [];
}

function num(v: unknown): number {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

export function normalizeContentActivityRow(
  raw: unknown,
): ContentActivityRow | null {
  const o = asRecord(raw);
  if (!o) {
    return null;
  }
  const name = o.name != null ? String(o.name).trim() : "";
  if (!name) {
    return null;
  }
  return {
    siteName: o.siteName != null ? String(o.siteName) : undefined,
    name,
    path: o.path != null ? String(o.path) : undefined,
    publishedItems: num(o.publishedItems),
    pendingItems: num(o.pendingItems),
    newItems: num(o.newItems),
    updatedItems: num(o.updatedItems),
    archivedItems: num(o.archivedItems),
  };
}

/**
 * Content activity for a path and duration window.
 * Classic: POST {@code /activitymanagement/activity/contentactivity}.
 *
 * @param path - CMS path, e.g. {@code /Sites/} or {@code /Sites/Demo}
 * @param durationType - days | weeks | months | years
 * @param duration - integer duration as string (classic wire)
 */
export async function fetchContentActivity(
  path: string,
  durationType: ActivityDurationType = "days",
  duration: string | number = 30,
): Promise<ContentActivityRow[]> {
  const body = {
    ContentActivityRequest: {
      path,
      durationType,
      duration: String(duration),
    },
  };
  const data = await post<unknown>(PATHS.ACTIVITY_CONTENT, body);
  return unwrapNamedList(data, "ContentActivity")
    .map(normalizeContentActivityRow)
    .filter((r): r is ContentActivityRow => r != null);
}

/**
 * Resolve a sensible default activity path: first site under {@code /Sites/},
 * or {@code /Sites/} when no sites exist.
 */
export async function resolveDefaultActivityPath(): Promise<string> {
  try {
    const sites = await fetchSites();
    const name = sites[0]?.name?.trim();
    if (name) {
      return `/Sites/${name}`;
    }
  } catch {
    /* fall through */
  }
  return "/Sites/";
}

/**
 * Default workflow label from {@code GET .../workflows/metadata/default}.
 * {@code PSEnumVals.entries[0].value} is the workflow label/name.
 */
export async function fetchDefaultWorkflowName(): Promise<string | null> {
  const data = await get<unknown>(PATHS.WORKFLOW_METADATA_DEFAULT);
  const obj = asRecord(data);
  if (!obj) {
    return null;
  }
  // { EnumVals: { entries: [...] } } or flat { entries: [...] }
  const root = asRecord(obj.EnumVals) ?? obj;
  const entries = root.entries;
  if (!Array.isArray(entries) || entries.length === 0) {
    return null;
  }
  const first = asRecord(entries[0]);
  if (!first) {
    return null;
  }
  const value = first.value != null ? String(first.value).trim() : "";
  return value || null;
}

export function normalizeItemPropertiesRow(
  raw: unknown,
): ItemPropertiesRow | null {
  const o = asRecord(raw);
  if (!o) {
    return null;
  }
  return {
    id: o.id != null ? String(o.id) : undefined,
    name: o.name != null ? String(o.name) : undefined,
    status: o.status != null ? String(o.status) : undefined,
    workflow: o.workflow != null ? String(o.workflow) : undefined,
    path: o.path != null ? String(o.path) : undefined,
    type: o.type != null ? String(o.type) : undefined,
    lastModifier: o.lastModifier != null ? String(o.lastModifier) : undefined,
    lastModifiedDate:
      o.lastModifiedDate != null ? String(o.lastModifiedDate) : undefined,
  };
}

/**
 * Items under path in a workflow (all states when {@code state} is empty).
 * Classic: POST {@code /pathmanagement/path/item/wfState}.
 */
export async function fetchItemsByWorkflowState(
  path: string,
  workflow: string,
  state = "",
): Promise<ItemPropertiesRow[]> {
  const body = {
    ItemByWfStateRequest: {
      path,
      workflow,
      state: state ?? "",
    },
  };
  const data = await post<unknown>(PATHS.PATH_ITEM_BY_WF_STATE, body);
  return unwrapNamedList(data, "ItemProperties")
    .map(normalizeItemPropertiesRow)
    .filter((r): r is ItemPropertiesRow => r != null);
}

/**
 * Aggregate items by workflow status for the Pages By Status gadget.
 */
export function groupItemsByStatus(
  items: ItemPropertiesRow[],
  sampleLimit = 3,
): WorkflowStatusBucket[] {
  const map = new Map<string, { count: number; sampleNames: string[] }>();
  for (const item of items) {
    const state = (item.status && item.status.trim()) || "Unknown";
    let bucket = map.get(state);
    if (!bucket) {
      bucket = { count: 0, sampleNames: [] };
      map.set(state, bucket);
    }
    bucket.count += 1;
    if (
      bucket.sampleNames.length < sampleLimit &&
      item.name &&
      item.name.trim()
    ) {
      bucket.sampleNames.push(item.name.trim());
    }
  }
  return Array.from(map.entries())
    .map(([state, v]) => ({
      state,
      count: v.count,
      sampleNames: v.sampleNames,
    }))
    .sort((a, b) => b.count - a.count || a.state.localeCompare(b.state));
}

/**
 * Load Pages By Status for the first site + default workflow.
 */
export async function fetchPagesByStatusSummary(options?: {
  path?: string;
  workflow?: string;
}): Promise<PagesByStatusResult> {
  let path = options?.path?.trim() || "";
  if (!path) {
    path = await resolveDefaultActivityPath();
  }
  let workflow = options?.workflow?.trim() || "";
  if (!workflow) {
    workflow = (await fetchDefaultWorkflowName()) || "Default Workflow";
  }
  const items = await fetchItemsByWorkflowState(path, workflow, "");
  const buckets = groupItemsByStatus(items);
  return {
    path,
    workflow,
    buckets,
    totalItems: items.length,
  };
}
