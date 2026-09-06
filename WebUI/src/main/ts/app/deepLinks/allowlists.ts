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

/** Server + client SPA entry allowlist (query contract). */
export const SPA_ENTRIES = [
  "home",
  "publish",
  "workflow",
  "admin",
  "widget-builder",
  "developer",
  "design",
  "architecture",
  "explorer",
  "profile",
  "assembly",
  "editor",
  "unavailable",
] as const;

export type SpaEntry = (typeof SPA_ENTRIES)[number];

export const HOME_SECTIONS = [
  "recent",
  "bookmarks",
  "library",
  "search",
  "create",
  "gadgets",
] as const;

export type HomeSection = (typeof HOME_SECTIONS)[number];

const HOME_SECTION_ALIASES: Record<string, HomeSection> = {
  list: "recent",
  newitem: "create",
  bookmark: "bookmarks",
  // Former peer dashboard → Home gadgets (PR-7 product lock)
  dash: "gadgets",
  dashboard: "gadgets",
  widgets: "gadgets",
  gadget: "gadgets",
};

export const PUBLISH_SECTIONS = [
  "sites",
  "status",
  "logs",
  "design",
  "runtime",
  "editions",
] as const;

export type PublishSection = (typeof PUBLISH_SECTIONS)[number];

const PUBLISH_SECTION_ALIASES: Record<string, PublishSection> = {
  site: "sites",
  log: "logs",
  edition: "editions",
};

export const WORKFLOW_TABS = [
  "workflow",
  "roles",
  "users",
  "categories",
] as const;

export type WorkflowTab = (typeof WORKFLOW_TABS)[number];

/**
 * Unified Admin shell tabs (#3088). Includes former Workflow administration
 * surfaces (workflow / roles / users / categories) so deep links and path
 * segments share one product area under {@code /admin/*}.
 */
export const ADMIN_TABS = [
  "tasks",
  "logs",
  "notifications",
  "tools",
  "workflow",
  "roles",
  "users",
  "categories",
] as const;

export type AdminTab = (typeof ADMIN_TABS)[number];

/** Developer module sections (lockstep with DeveloperShell). */
export const DEVELOPER_SECTIONS = [
  "content-types",
  "templates",
  "slots",
  "keywords",
  "locales",
  "shared-fields",
  "system-def",
  "item-filters",
  "display-formats",
  "action-menus",
  "searches",
  "views",
  "extensions",
  "relationship-types",
  "workflows",
  "server-configs",
  "application-files",
  "file-explorer",
  "database-explorer",
  "problems",
  "ce-controls",
  "sites",
  "communities",
  "roles",
  "community-visibility",
  "pipelines",
  "preferences",
] as const;

export type DeveloperSection = (typeof DEVELOPER_SECTIONS)[number];

/** Design SPA sections (lockstep with DesignShell) — #2808 template library. */
export const DESIGN_SECTIONS = ["templates"] as const;

export type DesignSection = (typeof DESIGN_SECTIONS)[number];

const DESIGN_SECTION_ALIASES: Record<string, DesignSection> = {
  template: "templates",
  tpl: "templates",
  library: "templates",
  "template-library": "templates",
};

const DEVELOPER_SECTION_ALIASES: Record<string, DeveloperSection> = {
  contenttypes: "content-types",
  content: "content-types",
  ctypes: "content-types",
  locale: "locales",
  i18n: "locales",
  sharedfields: "shared-fields",
  shared: "shared-fields",
  "shared-field": "shared-fields",
  systemdef: "system-def",
  system: "system-def",
  "sys-def": "system-def",
  itemfilters: "item-filters",
  filters: "item-filters",
  displayformats: "display-formats",
  "display-format": "display-formats",
  df: "display-formats",
  actionmenus: "action-menus",
  menus: "action-menus",
  actions: "action-menus",
  search: "searches",
  searches: "searches",
  view: "views",
  views: "views",
  extensions: "extensions",
  extension: "extensions",
  exits: "extensions",
  "relationship-types": "relationship-types",
  relationshiptypes: "relationship-types",
  relationships: "relationship-types",
  "rel-types": "relationship-types",
  workflows: "workflows",
  workflow: "workflows",
  wfs: "workflows",
  "server-configs": "server-configs",
  serverconfigs: "server-configs",
  configs: "server-configs",
  configurations: "server-configs",
  "application-files": "application-files",
  applicationfiles: "application-files",
  "app-files": "application-files",
  "app-resources": "application-files",
  "cms-resources": "application-files",
  sy05: "application-files",
  "file-explorer": "file-explorer",
  fileexplorer: "file-explorer",
  "file-explorer-browse": "file-explorer",
  "database-explorer": "database-explorer",
  databaseexplorer: "database-explorer",
  "db-explorer": "database-explorer",
  "database-explorer-browse": "database-explorer",
  problems: "problems",
  problem: "problems",
  validation: "problems",
  "design-problems": "problems",
  "ce-controls": "ce-controls",
  cecontrols: "ce-controls",
  controls: "ce-controls",
  control: "ce-controls",
  sites: "sites",
  site: "sites",
  cxviews: "views",
  communities: "communities",
  community: "communities",
  roles: "roles",
  role: "roles",
  "security-roles": "roles",
  se03: "roles",
  "community-visibility": "community-visibility",
  communityvisibility: "community-visibility",
  "comm-visibility": "community-visibility",
  "visibility-navigator": "community-visibility",
  se05: "community-visibility",
  pipeline: "pipelines",
  applications: "pipelines",
  "xml-apps": "pipelines",
  preferences: "preferences",
  preference: "preferences",
  prefs: "preferences",
  settings: "preferences",
  security: "preferences",
  "security-prefs": "preferences",
};

const ID_RE = /^[A-Za-z0-9_-]{1,128}$/;

/**
 * Explorer deep-link path charset for the **query contract** (server Location +
 * client parse). Intentionally ASCII-safe for JSP redirects / XSS hygiene.
 * Full CMS folder names may be richer in-app; only deep-link query paths are
 * constrained. Path validation for explorer entry lives here (SPA query contract).
 */
export const EXPLORER_PATH_RE = /^[/A-Za-z0-9._-]+$/;

export function isSpaEntry(value: string): value is SpaEntry {
  return (SPA_ENTRIES as readonly string[]).includes(value);
}

export function normalizeHomeSection(raw: string | null | undefined): HomeSection | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase();
  if ((HOME_SECTIONS as readonly string[]).includes(n)) {
    return n as HomeSection;
  }
  return HOME_SECTION_ALIASES[n];
}

export function normalizePublishSection(
  raw: string | null | undefined,
): PublishSection | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase();
  if ((PUBLISH_SECTIONS as readonly string[]).includes(n)) {
    return n as PublishSection;
  }
  return PUBLISH_SECTION_ALIASES[n];
}

export function normalizeWorkflowTab(raw: string | null | undefined): WorkflowTab | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase();
  return (WORKFLOW_TABS as readonly string[]).includes(n) ? (n as WorkflowTab) : undefined;
}

export function normalizeAdminTab(raw: string | null | undefined): AdminTab | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase();
  return (ADMIN_TABS as readonly string[]).includes(n) ? (n as AdminTab) : undefined;
}

export function normalizeDeveloperSection(
  raw: string | null | undefined,
): DeveloperSection | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase().replace(/_/g, "-");
  if ((DEVELOPER_SECTIONS as readonly string[]).includes(n)) {
    return n as DeveloperSection;
  }
  return DEVELOPER_SECTION_ALIASES[n];
}

export function normalizeDesignSection(
  raw: string | null | undefined,
): DesignSection | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const n = raw.trim().toLowerCase().replace(/_/g, "-");
  if ((DESIGN_SECTIONS as readonly string[]).includes(n)) {
    return n as DesignSection;
  }
  return DESIGN_SECTION_ALIASES[n];
}

/**
 * Site name for Architecture deep links / path segments (#3094).
 * Allows common CMS site name characters; rejects traversal and schemes.
 */
export function normalizeArchitectureSite(
  raw: string | null | undefined,
): string | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const t = raw.trim();
  if (t.length > 128 || t.includes("://") || t.includes("..") || t.includes("/")) {
    return undefined;
  }
  // Letters, digits, spaces, common punctuation used in demo site names
  if (!/^[A-Za-z0-9._ -]{1,128}$/.test(t)) {
    return undefined;
  }
  return t;
}

export function normalizeId(raw: string | null | undefined): string | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const t = raw.trim();
  return ID_RE.test(t) ? t : undefined;
}

/**
 * Allowlist explorer deep-link paths. Rejects traversal **segments** ({@code ..}),
 * not the substring {@code ..} inside a folder name (e.g. {@code /Sites/foo..bar}).
 */
export function normalizeExplorerPath(raw: string | null | undefined): string | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const t = raw.trim();
  if (!t.startsWith("/") || t.length >= 2048 || t.includes("://")) {
    return undefined;
  }
  // Segment-aware: only reject ".." as a path segment (not "foo..bar")
  for (const segment of t.split("/")) {
    if (segment === "..") {
      return undefined;
    }
  }
  return EXPLORER_PATH_RE.test(t) ? t : undefined;
}
