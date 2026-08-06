/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * APIs for previously shell-only gadgets: bulk asset upload, themes, reports hub.
 */

import { get } from "../client";
import { getCsrfToken } from "../csrf";
import { PATHS } from "../paths";
import {
  fetchContentActivity,
  fetchFormsForDefaultSite,
  resolveDefaultActivityPath,
} from "./gadgetApi";
import {
  fetchNonSeoPages,
  fetchDefaultPagesWithComments,
} from "./deliveryGadgetsApi";

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

/**
 * Asset upload servlet URL (classic bulk upload gadget).
 * Mapped as {@code /uploadAssetFile} under the CM webapp (typically {@code /cm}).
 */
export function resolveAssetUploadUrl(): string {
  try {
    if (typeof window !== "undefined" && window.location?.pathname) {
      const p = window.location.pathname;
      if (p === "/cm" || p.startsWith("/cm/")) {
        return "/cm/uploadAssetFile";
      }
      if (p === "/Rhythmyx" || p.startsWith("/Rhythmyx/")) {
        return "/Rhythmyx/uploadAssetFile";
      }
    }
  } catch {
    /* ignore */
  }
  return "/cm/uploadAssetFile";
}

export type UploadAssetType = "file" | "image";

export interface BulkUploadResult {
  fileName: string;
  ok: boolean;
  assetName?: string;
  error?: string;
}

/**
 * Upload one file via PSAssetUploadServlet multipart form.
 * Params: folder, assetType, file part; optional approveOnUpload.
 */
export async function uploadAssetFile(options: {
  file: File;
  folder?: string;
  assetType?: UploadAssetType;
  approveOnUpload?: boolean;
}): Promise<BulkUploadResult> {
  const fileName = options.file.name;
  const form = new FormData();
  form.append("folder", options.folder?.trim() || "/Assets/uploads/");
  form.append("assetType", options.assetType || "file");
  if (options.approveOnUpload) {
    form.append("approveOnUpload", "true");
  }
  form.append("file", options.file, fileName);

  const headers = new Headers();
  headers.set("Accept", "application/json, text/plain, */*");
  const csrf = getCsrfToken();
  if (csrf) {
    headers.set(csrf.headerName, csrf.token);
  }

  const response = await fetch(resolveAssetUploadUrl(), {
    method: "POST",
    headers,
    credentials: "same-origin",
    body: form,
  });

  let body: unknown;
  try {
    const text = await response.text();
    try {
      body = text ? JSON.parse(text) : undefined;
    } catch {
      body = text;
    }
  } catch {
    body = undefined;
  }

  if (!response.ok) {
    const o = asRecord(body);
    return {
      fileName,
      ok: false,
      error:
        (o?.error != null && String(o.error)) ||
        `HTTP ${response.status}`,
    };
  }
  const o = asRecord(body);
  return {
    fileName,
    ok: true,
    assetName: o?.result != null ? String(o.result) : fileName,
  };
}

export async function uploadAssetFiles(
  files: FileList | File[],
  options?: {
    folder?: string;
    assetType?: UploadAssetType;
    approveOnUpload?: boolean;
  },
): Promise<BulkUploadResult[]> {
  const list = Array.from(files);
  const out: BulkUploadResult[] = [];
  for (const file of list) {
    out.push(
      await uploadAssetFile({
        file,
        folder: options?.folder,
        assetType: options?.assetType,
        approveOnUpload: options?.approveOnUpload,
      }),
    );
  }
  return out;
}

// ---- Themes (Sitewide Framework) ----

export interface ThemeSummary {
  name: string;
  thumbUrl?: string;
  cssFilePath?: string;
}

export function normalizeThemeSummary(raw: unknown): ThemeSummary | null {
  const o = asRecord(raw);
  if (!o) return null;
  const name = o.name != null ? String(o.name).trim() : "";
  if (!name) return null;
  return {
    name,
    thumbUrl: o.thumbUrl != null ? String(o.thumbUrl) : undefined,
    cssFilePath: o.cssFilePath != null ? String(o.cssFilePath) : undefined,
  };
}

/** GET pagemanagement/theme/summary/all */
export async function fetchThemeSummaries(): Promise<ThemeSummary[]> {
  const data = await get<unknown>(PATHS.THEME_SUMMARY_ALL);
  return unwrapList(data, "ThemeSummary")
    .map(normalizeThemeSummary)
    .filter((t): t is ThemeSummary => t != null);
}

// ---- Reports hub (aggregates existing product data) ----

export interface ReportsHubSnapshot {
  path: string;
  seoIssuePages: number;
  formsCount: number;
  formsNewSubmissions: number;
  pagesWithComments: number;
  activityRows: number;
  activityNewItems: number;
}

export async function fetchReportsHubSnapshot(): Promise<ReportsHubSnapshot> {
  const path = await resolveDefaultActivityPath();
  const [seo, forms, comments, activity] = await Promise.allSettled([
    fetchNonSeoPages({ severity: "ALL" }),
    fetchFormsForDefaultSite(),
    fetchDefaultPagesWithComments(50),
    fetchContentActivity(path, "days", 30),
  ]);

  const seoRows = seo.status === "fulfilled" ? seo.value : [];
  const formsPack =
    forms.status === "fulfilled" ? forms.value : { site: null, forms: [] };
  const commentsPack =
    comments.status === "fulfilled"
      ? comments.value
      : { site: null, pages: [] };
  const activityRows = activity.status === "fulfilled" ? activity.value : [];

  return {
    path,
    seoIssuePages: seoRows.length,
    formsCount: formsPack.forms.length,
    formsNewSubmissions: formsPack.forms.reduce(
      (n, f) => n + (f.newSubmissions || 0),
      0,
    ),
    pagesWithComments: commentsPack.pages.length,
    activityRows: activityRows.length,
    activityNewItems: activityRows.reduce((n, r) => n + r.newItems, 0),
  };
}

