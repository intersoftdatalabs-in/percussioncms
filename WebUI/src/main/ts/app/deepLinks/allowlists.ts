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

/** Server + client SPA entry allowlist (query contract). */
export const SPA_ENTRIES = [
  "home",
  "publish",
  "workflow",
  "admin",
  "widget-builder",
  "explorer",
  "unavailable",
] as const;

export type SpaEntry = (typeof SPA_ENTRIES)[number];

export const HOME_SECTIONS = [
  "recent",
  "bookmarks",
  "library",
  "search",
  "create",
] as const;

export type HomeSection = (typeof HOME_SECTIONS)[number];

const HOME_SECTION_ALIASES: Record<string, HomeSection> = {
  list: "recent",
  newitem: "create",
  bookmark: "bookmarks",
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

export const ADMIN_TABS = ["tasks", "logs", "notifications", "tools"] as const;

export type AdminTab = (typeof ADMIN_TABS)[number];

const ID_RE = /^[A-Za-z0-9_-]{1,128}$/;
const EXPLORER_PATH_RE = /^[/A-Za-z0-9._-]+$/;

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

export function normalizeId(raw: string | null | undefined): string | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const t = raw.trim();
  return ID_RE.test(t) ? t : undefined;
}

export function normalizeExplorerPath(raw: string | null | undefined): string | undefined {
  if (raw == null || !raw.trim()) return undefined;
  const t = raw.trim();
  if (!t.startsWith("/") || t.length >= 2048 || t.includes("..") || t.includes("://")) {
    return undefined;
  }
  return EXPLORER_PATH_RE.test(t) ? t : undefined;
}
