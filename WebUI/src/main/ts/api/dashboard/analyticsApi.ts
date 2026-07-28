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
 * Analytics / Google Setup APIs for Home Gadgets.
 *
 * <p>Classic paths ({@code perc_path_constants}):
 * {@code /analytics/provider/config}, {@code isProfileConfigured/{site}},
 * profiles. Required before Traffic and What's Working (effectiveness) can
 * query Google Analytics.</p>
 *
 * <p><strong>Never surface password/credentials in UI</strong> — REST may
 * return an encrypted password field; strip it from all display DTOs.</p>
 */

import { del, get } from "../client";
import { PATHS } from "../paths";
import { fetchSites } from "../home/homeApi";

/** Safe view of stored provider config (no secrets). */
export interface AnalyticsProviderStatus {
  /** True when a provider config row exists with a user id. */
  configured: boolean;
  /** Service account / user id (never password). */
  userId: string | null;
  /**
   * Site → profile mapping summary from extraParams
   * ({@code profileId|webPropertyId|apiKey}).
   */
  siteProfiles: SiteProfileMapping[];
}

export interface SiteProfileMapping {
  siteName: string;
  /** True when extraParams has a non-empty mapping for this site. */
  mapped: boolean;
  /** Optional profile id (first segment of mapping). */
  profileId?: string;
  /** Optional web property / tracking id (second segment). */
  webPropertyId?: string;
}

function asRecord(v: unknown): Record<string, unknown> | null {
  return v && typeof v === "object" ? (v as Record<string, unknown>) : null;
}

/**
 * Unwrap Jackson {@code providerConfig} root or flat object.
 */
export function unwrapProviderConfig(data: unknown): Record<string, unknown> | null {
  if (data == null || data === "") {
    return null;
  }
  const root = asRecord(data);
  if (!root) {
    return null;
  }
  const nested = asRecord(root.providerConfig);
  if (nested) {
    return nested;
  }
  // Flat wire (some Jackson configs)
  if ("userid" in root || "uid" in root || "userId" in root || "extraParams" in root) {
    return root;
  }
  return root;
}

/**
 * Parse extraParams map: either {@code extraParamsMap}, flat {@code params},
 * or {@code extraParams.entry[{key,value}]}.
 */
export function extractExtraParamsMap(
  config: Record<string, unknown>,
): Record<string, string> {
  const out: Record<string, string> = {};
  const mapLike = asRecord(config.extraParamsMap) ?? asRecord(config.params);
  if (mapLike) {
    for (const [k, v] of Object.entries(mapLike)) {
      if (v != null && String(v).trim()) {
        out[k] = String(v);
      }
    }
  }
  const extra = asRecord(config.extraParams);
  const entries = extra?.entry;
  if (Array.isArray(entries)) {
    for (const e of entries) {
      const r = asRecord(e);
      if (r?.key != null && r.value != null && String(r.value).trim()) {
        out[String(r.key)] = String(r.value);
      }
    }
  }
  return out;
}

export function parseSiteProfileMapping(
  siteName: string,
  raw: string | undefined,
): SiteProfileMapping {
  if (!raw || !raw.trim()) {
    return { siteName, mapped: false };
  }
  const parts = raw.split("|");
  return {
    siteName,
    mapped: true,
    profileId: parts[0]?.trim() || undefined,
    webPropertyId: parts[1]?.trim() || undefined,
  };
}

/**
 * Normalize stored config into a password-free status object.
 */
export function normalizeAnalyticsProviderStatus(
  data: unknown,
): AnalyticsProviderStatus {
  const config = unwrapProviderConfig(data);
  if (!config) {
    return { configured: false, userId: null, siteProfiles: [] };
  }
  const userIdRaw =
    config.userid ?? config.uid ?? config.userId ?? null;
  const userId =
    userIdRaw != null && String(userIdRaw).trim()
      ? String(userIdRaw).trim()
      : null;
  const extra = extractExtraParamsMap(config);
  const siteProfiles = Object.keys(extra)
    .sort((a, b) => a.localeCompare(b))
    .map((site) => parseSiteProfileMapping(site, extra[site]));
  return {
    configured: Boolean(userId) || siteProfiles.length > 0,
    userId,
    siteProfiles,
  };
}

/**
 * Load analytics provider config status (safe for UI).
 * Returns {@code configured: false} when no config is stored (null body).
 */
export async function fetchAnalyticsProviderStatus(): Promise<AnalyticsProviderStatus> {
  try {
    const data = await get<unknown>(PATHS.ANALYTICS_CONFIG);
    return normalizeAnalyticsProviderStatus(data);
  } catch (err: unknown) {
    // Some installs may 404 when no metadata key exists
    if (err && typeof err === "object" && "status" in err) {
      const status = (err as { status: number }).status;
      if (status === 404 || status === 204) {
        return { configured: false, userId: null, siteProfiles: [] };
      }
    }
    throw err;
  }
}

/**
 * Per-site profile flag from server
 * ({@code GET …/isProfileConfigured/{sitename}} → "true"|"false").
 */
export async function isSiteAnalyticsProfileConfigured(
  siteName: string,
): Promise<boolean> {
  const site = siteName.trim();
  if (!site) {
    return false;
  }
  const data = await get<unknown>(
    `${PATHS.ANALYTICS_IS_PROFILE_CONFIGURED}/${encodeURIComponent(site)}`,
  );
  if (typeof data === "boolean") {
    return data;
  }
  if (typeof data === "string") {
    return data.trim().toLowerCase() === "true";
  }
  return Boolean(data);
}

/**
 * Merge CMS sites with config mappings + live isProfileConfigured checks.
 */
export async function fetchGoogleSetupSummary(): Promise<{
  provider: AnalyticsProviderStatus;
  sites: Array<{
    siteName: string;
    profileConfigured: boolean;
    mapping?: SiteProfileMapping;
  }>;
}> {
  const provider = await fetchAnalyticsProviderStatus();
  const mappingBySite = new Map(
    provider.siteProfiles.map((p) => [p.siteName, p]),
  );
  let siteNames: string[] = [];
  try {
    const sites = await fetchSites();
    siteNames = sites
      .map((s) => s.name?.trim())
      .filter((n): n is string => Boolean(n));
  } catch {
    // Fall back to keys from config only
    siteNames = provider.siteProfiles.map((p) => p.siteName);
  }
  // Include mapped sites that may no longer appear in site list
  for (const m of provider.siteProfiles) {
    if (!siteNames.includes(m.siteName)) {
      siteNames.push(m.siteName);
    }
  }

  const sites: Array<{
    siteName: string;
    profileConfigured: boolean;
    mapping?: SiteProfileMapping;
  }> = [];

  for (const siteName of siteNames) {
    const mapping = mappingBySite.get(siteName);
    let profileConfigured = Boolean(mapping?.mapped);
    if (provider.configured) {
      try {
        profileConfigured = await isSiteAnalyticsProfileConfigured(siteName);
      } catch {
        // keep mapping-based guess
      }
    }
    sites.push({
      siteName,
      profileConfigured,
      mapping,
    });
  }

  return { provider, sites };
}

/** True when provider credentials exist (Traffic/Effectiveness prerequisite). */
export async function isAnalyticsProviderConfigured(): Promise<boolean> {
  const status = await fetchAnalyticsProviderStatus();
  return status.configured;
}

/** Clear stored analytics config (DELETE). */
export async function deleteAnalyticsProviderConfig(): Promise<void> {
  await del(PATHS.ANALYTICS_CONFIG);
}
