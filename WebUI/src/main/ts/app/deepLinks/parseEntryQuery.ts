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

import {
  isSpaEntry,
  normalizeAdminTab,
  normalizeArchitectureSite,
  normalizeDesignSection,
  normalizeDeveloperSection,
  normalizeExplorerPath,
  normalizeHomeSection,
  normalizeId,
  normalizePublishSection,
  normalizeWorkflowTab,
  type SpaEntry,
} from "./allowlists";
import { detectSpaBasename, pathAfterBasename } from "./spaBasename";

/**
 * Parsed server-driven SPA entry (query contract only — never hash on server).
 */
export interface ParsedSpaEntry {
  entry: SpaEntry;
  /** Client route path under basename (e.g. /home/library, /publish/logs) */
  clientPath: string;
  section?: string;
  tab?: string;
  siteId?: string;
  serverId?: string;
  /** Item publishing history lookup (Publish Status/Logs). */
  itemId?: string;
  /** Publishing job to preselect (Publish Logs, issue #6215). */
  jobId?: string;
  /** Demand-publish request to surface (Publish Status, issue #6215). */
  requestId?: string;
  path?: string;
  /** Architecture site name (path or query; #3094). */
  site?: string;
  /** Active Assembly item id (query contract). */
  contentId?: string;
  /** Active Assembly page/snippet template id (query contract). */
  templateId?: string;
  /** Content Editor mode ({@code edit} or {@code view}). */
  mode?: string;
}

/**
 * Shared field source for the per-entry builders below. The query contract
 * ({@link parseEntryQuery}) and the path contract ({@link parseClientPath})
 * populate the same shape: {@code section}/{@code tab} carry the query
 * params in one case and the second path segment in the other, while
 * {@code siteFromPath} is only set for path-based architecture URLs.
 */
interface EntryFieldSource {
  section: string | null | undefined;
  tab: string | null | undefined;
  params: URLSearchParams;
  siteFromPath?: string | undefined;
}

/**
 * Legacy entry aliases shared by the query and path contracts.
 */
function aliasEntry(entryRaw: string): string {
  if (entryRaw === "widgetbuilder") {
    return "widget-builder";
  }
  if (entryRaw === "arch" || entryRaw === "navigation") {
    return "architecture";
  }
  return entryRaw;
}

/**
 * Strip the leading {@code ?} (when present) and split a query string.
 */
function toQueryParams(search: string): URLSearchParams {
  const q = search.startsWith("?") ? search.slice(1) : search;
  return new URLSearchParams(q);
}

function buildHomeEntry(src: EntryFieldSource): ParsedSpaEntry {
  const section = normalizeHomeSection(src.section);
  return {
    entry: "home",
    section,
    clientPath: section ? `/home/${section}` : "/home",
  };
}

function readIds(params: URLSearchParams): {
  siteId?: string;
  serverId?: string;
  itemId?: string;
  jobId?: string;
  requestId?: string;
} {
  return {
    siteId: normalizeId(params.get("siteId")),
    serverId: normalizeId(params.get("serverId")),
    itemId: normalizeId(params.get("itemId")),
    jobId: normalizeId(params.get("jobId")),
    requestId: normalizeId(params.get("requestId")),
  };
}

function withQueryString(base: string, qs: URLSearchParams): string {
  const qstr = qs.toString();
  return qstr ? `${base}?${qstr}` : base;
}

function buildPublishEntry(src: EntryFieldSource): ParsedSpaEntry {
  const section = normalizePublishSection(src.section);
  const { siteId, serverId, itemId, jobId, requestId } = readIds(src.params);
  const qs = new URLSearchParams();
  if (siteId) qs.set("siteId", siteId);
  if (serverId) qs.set("serverId", serverId);
  if (itemId) qs.set("itemId", itemId);
  if (jobId) qs.set("jobId", jobId);
  if (requestId) qs.set("requestId", requestId);
  const base = section ? `/publish/${section}` : "/publish";
  return { entry: "publish", section, siteId, serverId, itemId, jobId, requestId, clientPath: withQueryString(base, qs) };
}

function buildWorkflowEntry(src: EntryFieldSource): ParsedSpaEntry {
  // #3088: fold legacy workflow entry into unified Admin shell paths
  const tab = normalizeWorkflowTab(src.tab ?? src.section) ?? "workflow";
  return {
    entry: "workflow",
    tab,
    clientPath: `/admin/${tab}`,
  };
}

function buildAdminEntry(src: EntryFieldSource): ParsedSpaEntry {
  const tab = normalizeAdminTab(src.tab ?? src.section);
  return {
    entry: "admin",
    tab,
    clientPath: tab ? `/admin/${tab}` : "/admin",
  };
}

function buildDeveloperEntry(src: EntryFieldSource): ParsedSpaEntry {
  const section = normalizeDeveloperSection(src.section ?? src.tab);
  return {
    entry: "developer",
    section,
    clientPath: section ? `/developer/${section}` : "/developer",
  };
}

function buildDesignEntry(src: EntryFieldSource): ParsedSpaEntry {
  const section = normalizeDesignSection(src.section ?? src.tab);
  return {
    entry: "design",
    section,
    clientPath: section ? `/design/${section}` : "/design",
  };
}

function buildArchitectureEntry(src: EntryFieldSource): ParsedSpaEntry {
  // Prefer path segment when site is a simple token; else query site= (#3094)
  const siteFromPath = normalizeArchitectureSite(src.siteFromPath);
  const site = siteFromPath ?? normalizeArchitectureSite(src.params.get("site"));
  if (site) {
    return {
      entry: "architecture",
      site,
      clientPath: `/architecture/${encodeURIComponent(site)}`,
    };
  }
  return { entry: "architecture", clientPath: "/architecture" };
}

function buildExplorerEntry(src: EntryFieldSource): ParsedSpaEntry {
  const path = normalizeExplorerPath(src.params.get("path"));
  let clientPath = "/explorer";
  if (path) {
    clientPath += `?path=${encodeURIComponent(path)}`;
  }
  return { entry: "explorer", path, clientPath };
}

function buildAssemblyEntry(src: EntryFieldSource): ParsedSpaEntry {
  const contentId = normalizeId(src.params.get("contentId"));
  const templateId = normalizeId(src.params.get("templateId"));
  const qs = new URLSearchParams();
  if (contentId) qs.set("contentId", contentId);
  if (templateId) qs.set("templateId", templateId);
  return {
    entry: "assembly",
    contentId,
    templateId,
    clientPath: withQueryString("/assembly", qs),
  };
}

function buildEditorEntry(src: EntryFieldSource): ParsedSpaEntry {
  const contentId = normalizeId(src.params.get("contentId"));
  const mode = normalizeId(src.params.get("mode"));
  const qs = new URLSearchParams();
  if (contentId) qs.set("contentId", contentId);
  if (mode) qs.set("mode", mode);
  return {
    entry: "editor",
    contentId,
    mode,
    clientPath: withQueryString("/editor", qs),
  };
}

/**
 * Per-entry builders behind both parse entry points. A dispatch table keeps
 * the entry points flat (no switch) so no single function accumulates the
 * branching budget of all entries.
 */
const ENTRY_BUILDERS: Record<SpaEntry, (src: EntryFieldSource) => ParsedSpaEntry> = {
  home: buildHomeEntry,
  publish: buildPublishEntry,
  workflow: buildWorkflowEntry,
  admin: buildAdminEntry,
  "widget-builder": () => ({ entry: "widget-builder", clientPath: "/widget-builder" }),
  developer: buildDeveloperEntry,
  design: buildDesignEntry,
  architecture: buildArchitectureEntry,
  explorer: buildExplorerEntry,
  profile: () => ({ entry: "profile", clientPath: "/profile" }),
  assembly: buildAssemblyEntry,
  editor: buildEditorEntry,
  unavailable: () => ({ entry: "unavailable", clientPath: "/unavailable" }),
};

/**
 * Parse {@code window.location.search} (or a synthetic query string) into an
 * allowlisted SPA entry and client route path.
 */
export function parseEntryQuery(
  search: string = typeof window !== "undefined" ? window.location.search : "",
): ParsedSpaEntry {
  const params = toQueryParams(search);

  const entryRaw = aliasEntry((params.get("entry") || "home").trim().toLowerCase());
  const entry: SpaEntry = isSpaEntry(entryRaw) ? entryRaw : "home";

  return ENTRY_BUILDERS[entry]({
    section: params.get("section") ?? params.get("initialScreen"),
    tab: params.get("tab"),
    params,
  });
}

/**
 * Rebuild an allowlisted query entry URL for login return / server redirects.
 * Always uses the app-tree spa.jsp (query contract — never path or hash).
 */
export function toSpaEntryUrl(parsed: ParsedSpaEntry): string {
  const params = new URLSearchParams();
  params.set("entry", parsed.entry);
  if (parsed.section) params.set("section", parsed.section);
  if (parsed.tab) params.set("tab", parsed.tab);
  if (parsed.siteId) params.set("siteId", parsed.siteId);
  if (parsed.serverId) params.set("serverId", parsed.serverId);
  if (parsed.itemId) params.set("itemId", parsed.itemId);
  if (parsed.jobId) params.set("jobId", parsed.jobId);
  if (parsed.requestId) params.set("requestId", parsed.requestId);
  if (parsed.path) params.set("path", parsed.path);
  if (parsed.site) params.set("site", parsed.site);
  if (parsed.contentId) params.set("contentId", parsed.contentId);
  if (parsed.templateId) params.set("templateId", parsed.templateId);
  if (parsed.mode) params.set("mode", parsed.mode);
  return `/cm/app/spa.jsp?${params.toString()}`;
}

/**
 * Split a BrowserRouter client path into its entry segment and the segment
 * after it, tolerating a missing leading slash and one trailing slash.
 */
function splitClientPath(clientPath: string): { entryRaw: string; second?: string } {
  let pathOnly = clientPath || "/home";
  if (!pathOnly.startsWith("/")) {
    pathOnly = `/${pathOnly}`;
  }
  // Drop trailing slash except root
  if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
    pathOnly = pathOnly.slice(0, -1);
  }
  const segments = pathOnly.split("/").filter(Boolean);
  return { entryRaw: segments[0] || "home", second: segments[1] };
}

/**
 * Decode an architecture path segment without letting malformed
 * {@code %} sequences throw URIError during navigation.
 */
function decodePathSegment(second: string | undefined): string | undefined {
  if (second == null) {
    return undefined;
  }
  try {
    return decodeURIComponent(second);
  } catch {
    return second;
  }
}
/**
 * Parse a BrowserRouter client path (after basename) plus optional search into
 * a {@link ParsedSpaEntry}. Used for mid-session 401 return when the address
 * bar is path-based (PR-9) rather than {@code spa.jsp?entry=…}.
 *
 * @param clientPath e.g. {@code /home/library} or {@code /publish/logs}
 * @param search e.g. {@code ?path=/Sites/x} or empty
 */
export function parseClientPath(
  clientPath: string,
  search: string = "",
): ParsedSpaEntry {
  const params = toQueryParams(search);
  const { entryRaw, second } = splitClientPath(clientPath);
  const aliased = aliasEntry(entryRaw.toLowerCase());
  const entry: SpaEntry = isSpaEntry(aliased) ? aliased : "home";

  return ENTRY_BUILDERS[entry]({
    section: second,
    tab: second,
    params,
    siteFromPath: decodePathSegment(second),
  });
}

/**
 * Resolve SPA return URL for login from either query entry or path-based URL.
 */
export function resolveSpaReturnFromLocation(
  pathname: string = typeof window !== "undefined" ? window.location.pathname : "/cm/app/spa.jsp",
  search: string = typeof window !== "undefined" ? window.location.search : "",
): string {
  const params = toQueryParams(search);
  if (params.has("entry") || pathname.includes("spa.jsp")) {
    return toSpaEntryUrl(parseEntryQuery(search));
  }
  const basename = detectSpaBasename(pathname);
  const clientPath = pathAfterBasename(pathname, basename);
  if (!clientPath) {
    return toSpaEntryUrl(parseEntryQuery(search));
  }
  return toSpaEntryUrl(parseClientPath(clientPath, search));
}
