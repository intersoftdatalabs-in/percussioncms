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
  path?: string;
}

/**
 * Parse {@code window.location.search} (or a synthetic query string) into an
 * allowlisted SPA entry and client route path.
 */
export function parseEntryQuery(
  search: string = typeof window !== "undefined" ? window.location.search : "",
): ParsedSpaEntry {
  const q = search.startsWith("?") ? search.slice(1) : search;
  const params = new URLSearchParams(q);

  let entryRaw = (params.get("entry") || "home").trim().toLowerCase();
  // Legacy alias
  if (entryRaw === "widgetbuilder") {
    entryRaw = "widget-builder";
  }
  const entry: SpaEntry = isSpaEntry(entryRaw) ? entryRaw : "home";

  const sectionParam = params.get("section") ?? params.get("initialScreen");
  const tabParam = params.get("tab");
  const siteId = normalizeId(params.get("siteId"));
  const serverId = normalizeId(params.get("serverId"));
  const path = normalizeExplorerPath(params.get("path"));

  switch (entry) {
    case "home": {
      const section = normalizeHomeSection(sectionParam);
      return {
        entry,
        section,
        clientPath: section ? `/home/${section}` : "/home",
      };
    }
    case "publish": {
      const section = normalizePublishSection(sectionParam);
      let clientPath = section ? `/publish/${section}` : "/publish";
      const qs = new URLSearchParams();
      if (siteId) qs.set("siteId", siteId);
      if (serverId) qs.set("serverId", serverId);
      const qstr = qs.toString();
      if (qstr) clientPath += `?${qstr}`;
      return { entry, section, siteId, serverId, clientPath };
    }
    case "workflow": {
      // #3088: fold legacy workflow entry into unified Admin shell paths
      const tab = normalizeWorkflowTab(tabParam ?? sectionParam) ?? "workflow";
      return {
        entry,
        tab,
        clientPath: `/admin/${tab}`,
      };
    }
    case "admin": {
      const tab = normalizeAdminTab(tabParam ?? sectionParam);
      return {
        entry,
        tab,
        clientPath: tab ? `/admin/${tab}` : "/admin",
      };
    }
    case "widget-builder":
      return { entry, clientPath: "/widget-builder" };
    case "developer": {
      const section = normalizeDeveloperSection(sectionParam ?? tabParam);
      return {
        entry,
        section,
        clientPath: section ? `/developer/${section}` : "/developer",
      };
    }
    case "design": {
      const section = normalizeDesignSection(sectionParam ?? tabParam);
      return {
        entry,
        section,
        clientPath: section ? `/design/${section}` : "/design",
      };
    }
    case "explorer": {
      let clientPath = "/explorer";
      if (path) {
        clientPath += `?path=${encodeURIComponent(path)}`;
      }
      return { entry, path, clientPath };
    }
    case "profile":
      return { entry, clientPath: "/profile" };
    case "unavailable":
      return { entry, clientPath: "/unavailable" };
    default:
      return { entry: "home", clientPath: "/home" };
  }
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
  if (parsed.path) params.set("path", parsed.path);
  return `/cm/app/spa.jsp?${params.toString()}`;
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
  const q = search.startsWith("?") ? search.slice(1) : search;
  const params = new URLSearchParams(q);
  const siteId = normalizeId(params.get("siteId"));
  const serverId = normalizeId(params.get("serverId"));
  const explorerPath = normalizeExplorerPath(params.get("path"));

  let pathOnly = clientPath || "/home";
  if (!pathOnly.startsWith("/")) {
    pathOnly = `/${pathOnly}`;
  }
  // Drop trailing slash except root
  if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
    pathOnly = pathOnly.slice(0, -1);
  }
  const segments = pathOnly.split("/").filter(Boolean);
  let entryRaw = (segments[0] || "home").toLowerCase();
  if (entryRaw === "widgetbuilder") {
    entryRaw = "widget-builder";
  }
  const entry: SpaEntry = isSpaEntry(entryRaw) ? entryRaw : "home";
  const second = segments[1];

  switch (entry) {
    case "home": {
      const section = normalizeHomeSection(second);
      return {
        entry,
        section,
        clientPath: section ? `/home/${section}` : "/home",
      };
    }
    case "publish": {
      const section = normalizePublishSection(second);
      let cp = section ? `/publish/${section}` : "/publish";
      const qs = new URLSearchParams();
      if (siteId) qs.set("siteId", siteId);
      if (serverId) qs.set("serverId", serverId);
      const qstr = qs.toString();
      if (qstr) cp += `?${qstr}`;
      return { entry, section, siteId, serverId, clientPath: cp };
    }
    case "workflow": {
      // Legacy path prefix still parseable; client path is Admin (#3088)
      const tab = normalizeWorkflowTab(second) ?? "workflow";
      return {
        entry,
        tab,
        clientPath: `/admin/${tab}`,
      };
    }
    case "admin": {
      const tab = normalizeAdminTab(second);
      return {
        entry,
        tab,
        clientPath: tab ? `/admin/${tab}` : "/admin",
      };
    }
    case "widget-builder":
      return { entry, clientPath: "/widget-builder" };
    case "design": {
      const section = normalizeDesignSection(second);
      return {
        entry,
        section,
        clientPath: section ? `/design/${section}` : "/design",
      };
    }
    case "developer": {
      const section = normalizeDeveloperSection(second);
      return {
        entry,
        section,
        clientPath: section ? `/developer/${section}` : "/developer",
      };
    }
    case "explorer": {
      let cp = "/explorer";
      if (explorerPath) {
        cp += `?path=${encodeURIComponent(explorerPath)}`;
      }
      return { entry, path: explorerPath, clientPath: cp };
    }
    case "profile":
      return { entry, clientPath: "/profile" };
    case "unavailable":
      return { entry, clientPath: "/unavailable" };
    default:
      return { entry: "home", clientPath: "/home" };
  }
}

/**
 * Resolve SPA return URL for login from either query entry or path-based URL.
 */
export function resolveSpaReturnFromLocation(
  pathname: string = typeof window !== "undefined" ? window.location.pathname : "/cm/app/spa.jsp",
  search: string = typeof window !== "undefined" ? window.location.search : "",
): string {
  const q = search.startsWith("?") ? search.slice(1) : search;
  const params = new URLSearchParams(q);
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
