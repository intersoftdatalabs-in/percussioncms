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
 * Site-level copy / delete for Navigation chrome (#3303 / parent #3092).
 *
 * Reuses sitemanage site REST — same contract as CM1 Architecture
 * ({@code PercSiteService} / {@code perc_page_manager.delete_site}).
 */

import { del, get, post } from "../client";
import type { PSSiteCopyRequest } from "../contentExplorer/types";
import { PATHS } from "../paths";

/** Fields expected by {@code PSSiteCopyRequest} (srcSite / copySite / assetFolder). */
export interface SiteCopyFields {
  srcSite: string;
  copySite: string;
  assetFolder?: string;
}

export function siteCopyUrl(): string {
  return `${PATHS.SITES_ALL}/copy`;
}

export function siteCopyInfoUrl(): string {
  return `${PATHS.SITES_ALL}/copysiteinfo`;
}

export function siteDeleteUrl(siteName: string): string {
  const name = siteName.trim();
  if (!name) {
    throw new Error("Site name is required");
  }
  return `${PATHS.SITES_ALL}/${encodeURIComponent(name)}`;
}

export function siteImportingUrl(siteName: string): string {
  const name = siteName.trim();
  if (!name) {
    throw new Error("Site name is required");
  }
  return `${PATHS.SITES_ALL}/isSiteImporting/${encodeURIComponent(name)}`;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function hasMapEntries(entries: unknown): boolean {
  if (entries == null) {
    return false;
  }
  if (Array.isArray(entries)) {
    return entries.length > 0;
  }
  if (typeof entries === "object") {
    return Object.keys(entries as Record<string, unknown>).length > 0;
  }
  return String(entries).trim().length > 0;
}

/**
 * True when {@code GET /sitemanage/site/copysiteinfo} reports an in-flight copy
 * (legacy {@code result.psmap.entries} non-empty).
 */
export function isSiteCopyInProgress(payload: unknown): boolean {
  if (payload == null) {
    return false;
  }
  if (typeof payload === "boolean") {
    return payload;
  }
  const root = asRecord(payload);
  if (!root) {
    return false;
  }
  const map =
    asRecord(root.psmap) ??
    asRecord(root.PSMapWrapper) ??
    asRecord(root.psMapWrapper) ??
    root;
  return (
    hasMapEntries(map.entries) ||
    hasMapEntries(map.Entries) ||
    hasMapEntries(root.entries)
  );
}

/** Optional asset folder: omit empty / root-only paths. */
export function normalizeCopyAssetFolder(
  folder: string | undefined | null,
): string | undefined {
  if (folder == null) {
    return undefined;
  }
  const t = folder.trim();
  if (t.length === 0 || t === "/" || t === "\\") {
    return undefined;
  }
  return t;
}

/**
 * Map Explorer {@link PSSiteCopyRequest} (source/target) onto the sitemanage
 * {@code SiteCopyRequest} wire body.
 */
export function buildSiteCopyRequestBody(
  fields: SiteCopyFields | PSSiteCopyRequest,
): { SiteCopyRequest: { srcSite: string; copySite: string; assetFolder?: string } } {
  const rec = fields as SiteCopyFields & PSSiteCopyRequest;
  const src = String(rec.srcSite ?? rec.sourceSite ?? "").trim();
  const dest = String(rec.copySite ?? rec.targetSite ?? "").trim();
  if (!src) {
    throw new Error("Source site is required");
  }
  if (!dest) {
    throw new Error("Copy site name is required");
  }
  const assetFolder = normalizeCopyAssetFolder(
    rec.assetFolder ?? rec.targetFolder,
  );
  const inner: { srcSite: string; copySite: string; assetFolder?: string } = {
    srcSite: src,
    copySite: dest,
  };
  if (assetFolder) {
    inner.assetFolder = assetFolder;
  }
  return { SiteCopyRequest: inner };
}

export function suggestCopySiteName(srcSite: string): string {
  const src = srcSite.trim();
  if (!src) {
    return "";
  }
  return `${src}-copy`;
}

export async function loadSiteCopyInfo(): Promise<unknown> {
  return get<unknown>(siteCopyInfoUrl());
}

export async function isSiteBeingImported(siteName: string): Promise<boolean> {
  const payload = await get<unknown>(siteImportingUrl(siteName));
  if (typeof payload === "boolean") {
    return payload;
  }
  const text = String(payload ?? "")
    .trim()
    .toLowerCase();
  return text === "true" || text === "1" || text === "yes";
}

export async function deleteManagedSite(siteName: string): Promise<void> {
  await del<unknown>(siteDeleteUrl(siteName));
}

export async function copyManagedSite(
  fields: SiteCopyFields | PSSiteCopyRequest,
): Promise<unknown> {
  return post<unknown>(siteCopyUrl(), buildSiteCopyRequestBody(fields));
}
