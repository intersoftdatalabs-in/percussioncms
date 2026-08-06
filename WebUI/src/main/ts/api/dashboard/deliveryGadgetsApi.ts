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
 * Delivery-tier and integration APIs for remaining Home gadgets:
 * Comments, Cookie Consent, Membership, SEO Audit, Siteimprove.
 *
 * <p>These proxy DTS / integration services. When delivery is not configured
 * the server returns errors — UI should show honest messages.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import { fetchSites } from "../home/homeApi";
import {
  fetchDefaultWorkflowName,
  resolveDefaultActivityPath,
} from "./gadgetApi";

function asRecord(v: unknown): Record<string, unknown> | null {
  return v && typeof v === "object" ? (v as Record<string, unknown>) : null;
}

function unwrapList(data: unknown, rootName: string): unknown[] {
  if (Array.isArray(data)) return data;
  const o = asRecord(data);
  if (!o) return [];
  const named = o[rootName];
  if (Array.isArray(named)) return named;
  if (named && typeof named === "object") return [named];
  for (const v of Object.values(o)) {
    if (Array.isArray(v)) return v;
  }
  return [];
}

function num(v: unknown): number {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

export async function resolveDefaultSiteName(): Promise<string | null> {
  try {
    const sites = await fetchSites();
    return sites[0]?.name?.trim() || null;
  } catch {
    return null;
  }
}

// ---- Comments ----

export interface PageCommentsSummary {
  id?: string;
  pagePath?: string;
  path?: string;
  pageLinkTitle?: string;
  commentCount: number;
  approvedCount: number;
  newCount: number;
  summary?: string;
  datePosted?: string;
}

export function normalizePageCommentsSummary(
  raw: unknown,
): PageCommentsSummary | null {
  const o = asRecord(raw);
  if (!o) return null;
  return {
    id: o.id != null ? String(o.id) : undefined,
    pagePath: o.pagePath != null ? String(o.pagePath) : undefined,
    path: o.path != null ? String(o.path) : undefined,
    pageLinkTitle:
      o.pageLinkTitle != null
        ? String(o.pageLinkTitle)
        : o.title != null
          ? String(o.title)
          : undefined,
    commentCount: num(o.commentCount),
    approvedCount: num(o.approvedCount),
    newCount: num(o.newCount),
    summary: o.summary != null ? String(o.summary) : undefined,
    datePosted: o.datePosted != null ? String(o.datePosted) : undefined,
  };
}

/** GET delivery/comment/pageswithcomments/{site}?max=&start= */
export async function fetchPagesWithComments(
  siteName: string,
  max = 20,
): Promise<PageCommentsSummary[]> {
  const site = siteName.trim();
  if (!site) return [];
  const q = new URLSearchParams();
  if (max > 0) q.set("max", String(max));
  const qs = q.toString() ? `?${q}` : "";
  const data = await get<unknown>(
    `${PATHS.COMMENTS_PAGES_WITH_COMMENTS}/${encodeURIComponent(site)}${qs}`,
  );
  return unwrapList(data, "commentsSummary")
    .map(normalizePageCommentsSummary)
    .filter((r): r is PageCommentsSummary => r != null);
}

export async function fetchDefaultPagesWithComments(
  max = 20,
): Promise<{ site: string | null; pages: PageCommentsSummary[] }> {
  const site = await resolveDefaultSiteName();
  if (!site) return { site: null, pages: [] };
  const pages = await fetchPagesWithComments(site, max);
  return { site, pages };
}

// ---- Cookie consent ----

export interface CookieConsentTotals {
  /** Raw parsed map (site → count or nested). */
  raw: Record<string, unknown>;
  /** Flattened site totals when shape is map of numbers. */
  bySite: Array<{ site: string; total: number }>;
  grandTotal: number;
}

/**
 * GET delivery/consent/log/totals — body may be JSON string or object.
 * Requires DTS indexer service.
 */
export async function fetchCookieConsentTotals(): Promise<CookieConsentTotals> {
  const data = await get<unknown>(PATHS.COOKIE_CONSENT_TOTALS);
  let parsed: unknown = data;
  if (typeof data === "string") {
    try {
      parsed = JSON.parse(data);
    } catch {
      return { raw: { value: data }, bySite: [], grandTotal: 0 };
    }
  }
  const raw = asRecord(parsed) ?? {};
  const bySite: Array<{ site: string; total: number }> = [];
  let grandTotal = 0;
  for (const [k, v] of Object.entries(raw)) {
    if (typeof v === "number" || (typeof v === "string" && !Number.isNaN(Number(v)))) {
      const total = num(v);
      bySite.push({ site: k, total });
      grandTotal += total;
    } else if (v && typeof v === "object") {
      const o = asRecord(v);
      const total = num(o?.total ?? o?.count ?? o?.entries);
      bySite.push({ site: k, total });
      grandTotal += total;
    }
  }
  bySite.sort((a, b) => b.total - a.total || a.site.localeCompare(b.site));
  return { raw, bySite, grandTotal };
}

// ---- Membership ----

export interface MembershipUser {
  email: string;
  status?: string;
  groups?: string;
  createdDate?: string;
}

export function normalizeMembershipUser(raw: unknown): MembershipUser | null {
  const o = asRecord(raw);
  if (!o) return null;
  const email = o.email != null ? String(o.email).trim() : "";
  if (!email) return null;
  return {
    email,
    status: o.status != null ? String(o.status) : undefined,
    groups: o.groups != null ? String(o.groups) : undefined,
    createdDate:
      o.createdDate != null ? String(o.createdDate) : undefined,
  };
}

/** GET delivery/membership/admin/users/{site} */
export async function fetchMembershipUsers(
  siteName: string,
): Promise<MembershipUser[]> {
  const site = siteName.trim();
  if (!site) return [];
  const data = await get<unknown>(
    `${PATHS.MEMBERSHIP_USERS}/${encodeURIComponent(site)}`,
  );
  // PSUserSummaries may wrap as { summaries: [...] } or list
  const list =
    unwrapList(data, "summaries").length > 0
      ? unwrapList(data, "summaries")
      : unwrapList(data, "UserSummary").length > 0
        ? unwrapList(data, "UserSummary")
        : Array.isArray(data)
          ? data
          : unwrapList(data, "users");
  return list
    .map(normalizeMembershipUser)
    .filter((u): u is MembershipUser => u != null);
}

export async function fetchDefaultMembershipUsers(): Promise<{
  site: string | null;
  users: MembershipUser[];
}> {
  const site = await resolveDefaultSiteName();
  if (!site) return { site: null, users: [] };
  const users = await fetchMembershipUsers(site);
  return { site, users };
}

// ---- SEO Audit (non-SEO pages) ----

export type SeoSeverity = "ALL" | "MODERATE" | "MEDIUM" | "HIGH" | "SEVERE";

export interface SeoPageRow {
  path: string;
  severity: number;
  issues: string[];
  summary?: string;
  pageName?: string;
}

export function normalizeSeoPageRow(raw: unknown): SeoPageRow | null {
  const o = asRecord(raw);
  if (!o) return null;
  const path = o.path != null ? String(o.path) : "";
  if (!path) return null;
  const issuesRaw = o.issues;
  let issues: string[] = [];
  if (Array.isArray(issuesRaw)) {
    issues = issuesRaw.map((x) => String(x));
  } else if (issuesRaw && typeof issuesRaw === "object") {
    issues = Object.keys(issuesRaw as object);
  }
  const pageSummary = asRecord(o.pageSummary);
  return {
    path,
    severity: num(o.severity),
    issues,
    summary: o.summary != null ? String(o.summary) : undefined,
    pageName:
      pageSummary?.name != null
        ? String(pageSummary.name)
        : pageSummary?.title != null
          ? String(pageSummary.title)
          : undefined,
  };
}

/**
 * POST pagemanagement/page/nonSEOPages with NonSEOPagesRequest
 * (path, workflow, state, severity, keyword).
 */
export async function fetchNonSeoPages(options?: {
  path?: string;
  workflow?: string;
  state?: string;
  severity?: SeoSeverity;
  keyword?: string;
}): Promise<SeoPageRow[]> {
  const path = options?.path?.trim() || (await resolveDefaultActivityPath());
  let workflow = options?.workflow?.trim() || "";
  if (!workflow) {
    workflow = (await fetchDefaultWorkflowName()) || "Default Workflow";
  }
  const body = {
    NonSEOPagesRequest: {
      path,
      workflow,
      state: options?.state ?? "",
      severity: options?.severity ?? "ALL",
      keyword: options?.keyword ?? "",
    },
  };
  const data = await post<unknown>(PATHS.PAGE_NON_SEO, body);
  return unwrapList(data, "SEOStatistics")
    .map(normalizeSeoPageRow)
    .filter((r): r is SeoPageRow => r != null);
}

// ---- Siteimprove ----

export interface SiteimproveStatus {
  hasToken: boolean;
  tokenPreview?: string;
  siteConfigPresent: boolean;
  siteName: string | null;
  rawConfig?: unknown;
}

/**
 * Siteimprove gadget status: global token + optional per-site publish config.
 * Not a live “metrics” API (that invented path does not exist).
 */
export async function fetchSiteimproveStatus(
  siteName?: string,
): Promise<SiteimproveStatus> {
  const site = siteName?.trim() || (await resolveDefaultSiteName());
  let hasToken = false;
  let tokenPreview: string | undefined;
  try {
    const tokenData = await get<unknown>(PATHS.SITEIMPROVE_TOKEN);
    const rec = asRecord(tokenData) ?? asRecord(asRecord(tokenData)?.metaData);
    const dataField =
      rec?.data != null
        ? String(rec.data)
        : rec?.token != null
          ? String(rec.token)
          : tokenData != null
            ? String(tokenData)
            : "";
    hasToken = Boolean(dataField && dataField !== "null" && dataField.trim());
    if (hasToken) {
      tokenPreview =
        dataField.length > 12 ? `${dataField.slice(0, 6)}…` : "configured";
    }
  } catch {
    hasToken = false;
  }

  let siteConfigPresent = false;
  let rawConfig: unknown;
  if (site) {
    try {
      rawConfig = await get<unknown>(
        `${PATHS.SITEIMPROVE_PUBLISH_CONFIG}/${encodeURIComponent(site)}`,
      );
      siteConfigPresent = rawConfig != null && rawConfig !== "";
    } catch {
      siteConfigPresent = false;
    }
  }

  return {
    hasToken,
    tokenPreview,
    siteConfigPresent,
    siteName: site,
    rawConfig,
  };
}
