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

/** Classic Assets tree root for Assets By Status. */
export const ASSETS_ROOT_PATH = "/Assets";

/**
 * Assets By Status — same wfState API as pages, rooted under {@code /Assets}.
 */
export async function fetchAssetsByStatusSummary(options?: {
  path?: string;
  workflow?: string;
}): Promise<PagesByStatusResult> {
  return fetchPagesByStatusSummary({
    path: options?.path?.trim() || ASSETS_ROOT_PATH,
    workflow: options?.workflow,
  });
}

/** One process monitor row. */
export interface ProcessMonitorRow {
  designator: string;
  name: string;
  status?: string;
  message?: string;
}

/**
 * Parse classic {@code PSMonitorList} / {@code PSMonitor} wire (stats map).
 */
export function normalizeProcessMonitors(data: unknown): ProcessMonitorRow[] {
  const list = unwrapNamedList(data, "monitor");
  // also try "Monitor" / nested list
  const raw =
    list.length > 0
      ? list
      : unwrapNamedList(data, "Monitor").length > 0
        ? unwrapNamedList(data, "Monitor")
        : Array.isArray(data)
          ? data
          : [];

  const out: ProcessMonitorRow[] = [];
  for (const item of raw) {
    const o = asRecord(item);
    if (!o) continue;
    const stats = asRecord(o.stats) ?? o;
    const entries =
      asRecord(stats.entries) ??
      (stats.entries && typeof stats.entries === "object"
        ? (stats.entries as Record<string, unknown>)
        : stats);
    const ent = asRecord(entries) ?? {};
    const designator =
      (ent.designator != null && String(ent.designator)) ||
      (o.designator != null && String(o.designator)) ||
      "";
    const name =
      (ent.name != null && String(ent.name)) ||
      (o.name != null && String(o.name)) ||
      designator ||
      "Monitor";
    const status =
      (ent.status != null && String(ent.status)) ||
      (o.status != null && String(o.status)) ||
      undefined;
    const message =
      (ent.message != null && String(ent.message)) ||
      (o.message != null && String(o.message)) ||
      undefined;
    out.push({ designator: designator || name, name, status, message });
  }
  return out;
}

/** Classic PROCESS_STATUS_ALL: GET sitemanage/monitor/all */
export async function fetchProcessMonitors(): Promise<ProcessMonitorRow[]> {
  const data = await get<unknown>(PATHS.MONITOR_ALL);
  return normalizeProcessMonitors(data);
}

export interface GlobalVariableEntry {
  name: string;
  value: string;
}

/**
 * Parse global variables metadata {@code data} field (JSON string or object).
 * Classic key: {@code percglobalvariables}.
 */
export function parseGlobalVariablesData(dataField: unknown): GlobalVariableEntry[] {
  let parsed: unknown = dataField;
  if (typeof dataField === "string") {
    const t = dataField.trim();
    if (!t) return [];
    try {
      parsed = JSON.parse(t);
    } catch {
      // plain text / non-JSON — single synthetic entry
      return [{ name: "data", value: t }];
    }
  }
  const out: GlobalVariableEntry[] = [];
  const walk = (node: unknown, prefix = ""): void => {
    if (node == null) return;
    if (typeof node === "string" || typeof node === "number" || typeof node === "boolean") {
      if (prefix) {
        out.push({ name: prefix, value: String(node) });
      }
      return;
    }
    if (Array.isArray(node)) {
      node.forEach((v, i) => walk(v, prefix ? `${prefix}[${i}]` : `[${i}]`));
      return;
    }
    if (typeof node === "object") {
      const o = node as Record<string, unknown>;
      // Common shapes: { name, value } arrays, or map of name→value
      if (Array.isArray(o.variables)) {
        for (const v of o.variables) {
          const r = asRecord(v);
          if (r && r.name != null) {
            out.push({
              name: String(r.name),
              value: r.value != null ? String(r.value) : "",
            });
          }
        }
        return;
      }
      for (const [k, v] of Object.entries(o)) {
        const key = prefix ? `${prefix}.${k}` : k;
        if (v != null && typeof v === "object" && !Array.isArray(v)) {
          const r = asRecord(v);
          if (r && ("value" in r || "val" in r)) {
            out.push({
              name: key,
              value: String(r.value ?? r.val ?? ""),
            });
          } else {
            walk(v, key);
          }
        } else if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
          out.push({ name: key, value: String(v) });
        } else {
          walk(v, key);
        }
      }
    }
  };
  walk(parsed);
  return out;
}

/** Load global variables metadata key {@code percglobalvariables}. */
export async function fetchGlobalVariables(): Promise<GlobalVariableEntry[]> {
  const data = await get<unknown>(
    `${PATHS.METADATA_FIND}/${encodeURIComponent("percglobalvariables")}`,
  );
  const root = asRecord(data);
  // { metaData: { key, data } } or flat { key, data }
  const body = asRecord(root?.metaData) ?? root;
  if (!body) {
    return [];
  }
  return parseGlobalVariablesData(body.data);
}

export interface FormSummaryRow {
  id?: string;
  name: string;
  title?: string;
  state?: string;
  site?: string;
  totalSubmissions: number;
  newSubmissions: number;
}

export function normalizeFormSummary(raw: unknown): FormSummaryRow | null {
  const o = asRecord(raw);
  if (!o) return null;
  const name = o.name != null ? String(o.name).trim() : "";
  if (!name) return null;
  return {
    id: o.id != null ? String(o.id) : undefined,
    name,
    title: o.title != null ? String(o.title) : undefined,
    state: o.state != null ? String(o.state) : undefined,
    site: o.site != null ? String(o.site) : undefined,
    totalSubmissions: num(o.totalSubmissions),
    newSubmissions: num(o.newSubmissions),
  };
}

/**
 * Form Tracker: GET assetmanagement/asset/forms/{site}.
 */
export async function fetchFormsForSite(siteName: string): Promise<FormSummaryRow[]> {
  const site = siteName.trim();
  if (!site) {
    return [];
  }
  const data = await get<unknown>(
    `${PATHS.ASSET_FORMS}/${encodeURIComponent(site)}`,
  );
  return unwrapNamedList(data, "FormSummary")
    .map(normalizeFormSummary)
    .filter((r): r is FormSummaryRow => r != null);
}

/** Forms for the first site (or empty if none). Propagates API errors. */
export async function fetchFormsForDefaultSite(): Promise<{
  site: string | null;
  forms: FormSummaryRow[];
}> {
  const sites = await fetchSites();
  const site = sites[0]?.name?.trim() || null;
  if (!site) {
    return { site: null, forms: [] };
  }
  const forms = await fetchFormsForSite(site);
  return { site, forms };
}

// ---------------------------------------------------------------------------
// Traffic + What's Working (effectiveness) — activitymanagement
// ---------------------------------------------------------------------------

/** Granularity for content traffic (matches {@code PSDateRange.Granularity}). */
export type TrafficGranularity = "DAY" | "WEEK" | "MONTH" | "YEAR";

/**
 * Traffic series keys (matches {@code PSTrafficTypeEnum#toString()}).
 * Visits requires Google Analytics profile mapping.
 */
export type TrafficSeriesKey =
  | "LIVE_PAGES"
  | "NEW_PAGES"
  | "UPDATED_PAGES"
  | "TAKE_DOWNS"
  | "VISITS";

export const DEFAULT_TRAFFIC_SERIES: TrafficSeriesKey[] = [
  "LIVE_PAGES",
  "NEW_PAGES",
  "UPDATED_PAGES",
  "TAKE_DOWNS",
  "VISITS",
];

export interface ContentTrafficResult {
  site?: string;
  path: string;
  startDate: string;
  endDate: string;
  /** Chart points aligned by date. */
  points: TrafficChartPoint[];
  totalVisits: number;
  totalLivePages: number;
}

export interface TrafficChartPoint {
  date: string;
  visits: number;
  livePages: number;
  newPages: number;
  pageUpdates: number;
  takeDowns: number;
}

export interface EffectivenessRow {
  name: string;
  effectiveness: number;
}

/** Format date as classic traffic wire {@code MM/dd/yyyy}. */
export function formatTrafficDate(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const yyyy = d.getFullYear();
  return `${mm}/${dd}/${yyyy}`;
}

function asNumberArray(v: unknown): number[] {
  if (!Array.isArray(v)) {
    return [];
  }
  return v.map((x) => {
    const n = Number(x);
    return Number.isFinite(n) ? n : 0;
  });
}

function asStringArray(v: unknown): string[] {
  if (!Array.isArray(v)) {
    return [];
  }
  return v.map((x) => String(x ?? ""));
}

/**
 * Normalize {@code PSContentTraffic} / {@code ContentTraffic} wire to chart points.
 */
export function normalizeContentTraffic(
  data: unknown,
  path: string,
  startDate: string,
  endDate: string,
): ContentTrafficResult {
  const root = asRecord(data);
  const body =
    (root && asRecord(root.ContentTraffic)) ||
    (root && asRecord(root.contentTraffic)) ||
    root ||
    {};

  const dates = asStringArray(body.dates);
  const visits = asNumberArray(body.visits);
  const livePages = asNumberArray(body.livePages);
  const newPages = asNumberArray(body.newPages);
  const pageUpdates = asNumberArray(body.pageUpdates);
  const takeDowns = asNumberArray(body.takeDowns);

  const len = Math.max(
    dates.length,
    visits.length,
    livePages.length,
    newPages.length,
    pageUpdates.length,
    takeDowns.length,
  );

  const points: TrafficChartPoint[] = [];
  for (let i = 0; i < len; i++) {
    points.push({
      date: dates[i] || `p${i + 1}`,
      visits: visits[i] ?? 0,
      livePages: livePages[i] ?? 0,
      newPages: newPages[i] ?? 0,
      pageUpdates: pageUpdates[i] ?? 0,
      takeDowns: takeDowns[i] ?? 0,
    });
  }

  return {
    site: body.site != null ? String(body.site) : undefined,
    path,
    startDate,
    endDate,
    points,
    totalVisits: visits.reduce((a, b) => a + b, 0),
    totalLivePages: livePages.reduce((a, b) => a + b, 0),
  };
}

/**
 * Content traffic for a path (classic Traffic gadget).
 * POST {@code /activitymanagement/activity/contenttraffic}.
 */
export async function fetchContentTraffic(options: {
  path: string;
  startDate: string;
  endDate: string;
  granularity?: TrafficGranularity;
  trafficRequested?: TrafficSeriesKey[];
  usage?: "pageviews" | "uniquepageviews";
}): Promise<ContentTrafficResult> {
  const path = options.path.trim();
  const granularity = options.granularity ?? "DAY";
  const trafficRequested = options.trafficRequested ?? DEFAULT_TRAFFIC_SERIES;
  const body = {
    ContentTrafficRequest: {
      path,
      startDate: options.startDate,
      endDate: options.endDate,
      granularity,
      usage: options.usage ?? "uniquepageviews",
      trafficRequested,
    },
  };
  const data = await post<unknown>(PATHS.ACTIVITY_TRAFFIC, body);
  return normalizeContentTraffic(
    data,
    path,
    options.startDate,
    options.endDate,
  );
}

/**
 * Traffic for the first site (or {@code /Sites/}) over the last N days.
 */
export async function fetchDefaultContentTraffic(
  daysRange = 30,
  granularity: TrafficGranularity = "DAY",
): Promise<ContentTrafficResult> {
  const path = await resolveDefaultActivityPath();
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - Math.max(1, daysRange));
  return fetchContentTraffic({
    path,
    startDate: formatTrafficDate(start),
    endDate: formatTrafficDate(end),
    granularity,
  });
}

export function normalizeEffectivenessRows(data: unknown): EffectivenessRow[] {
  return unwrapNamedList(data, "Effectiveness")
    .map((raw) => {
      const o = asRecord(raw);
      if (!o) return null;
      const name = o.name != null ? String(o.name).trim() : "";
      if (!name) return null;
      const effectiveness = Number(o.effectiveness ?? 0);
      return {
        name,
        effectiveness: Number.isFinite(effectiveness) ? effectiveness : 0,
      };
    })
    .filter((r): r is EffectivenessRow => r != null);
}

/**
 * What's Working / effectiveness.
 * POST {@code /activitymanagement/activity/effectiveness}.
 * Requires Google Analytics setup (server throws if not configured).
 */
export async function fetchEffectiveness(options: {
  path: string;
  durationType?: ActivityDurationType;
  duration?: string | number;
  usage?: "pageviews" | "unique_pageviews";
  threshold?: number;
}): Promise<EffectivenessRow[]> {
  const body = {
    EffectivenessRequest: {
      path: options.path.trim(),
      durationType: options.durationType ?? "days",
      duration: String(options.duration ?? 30),
      usage: options.usage ?? "unique_pageviews",
      threshold: options.threshold ?? 1,
    },
  };
  const data = await post<unknown>(PATHS.ACTIVITY_EFFECTIVENESS, body);
  return normalizeEffectivenessRows(data);
}

/** Effectiveness for first site / default path. */
export async function fetchDefaultEffectiveness(
  durationDays = 30,
): Promise<{ path: string; rows: EffectivenessRow[] }> {
  const path = await resolveDefaultActivityPath();
  const rows = await fetchEffectiveness({
    path,
    durationType: "days",
    duration: durationDays,
  });
  return { path, rows };
}
