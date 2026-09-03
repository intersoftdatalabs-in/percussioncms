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

import { get } from "../client";
import { asJsonRecord, asStringArray } from "../jsonList";
import { PATHS } from "../paths";

/** Workbench Security Design SE-03 navigator folders. */
export const ROLE_BROWSE_GROUPS = ["community", "workflow", "unassigned"] as const;

export type RoleBrowseGroupKey = (typeof ROLE_BROWSE_GROUPS)[number];

/** One role in the Admin SE-03 browse catalog. */
export type RoleBrowseEntry = {
  name: string;
  description?: string;
  /** Grouping keys: community, workflow, and/or unassigned. */
  groups: RoleBrowseGroupKey[];
  /** Community names that include this role (sorted). */
  communities: string[];
  /** Workflow names that include this role (sorted). */
  workflows: string[];
};

/** Admin GET envelope for SE-03 roles browse. */
export type RoleBrowseCatalog = {
  /** Optional filter that was applied; absent/null for the full catalog. */
  group?: RoleBrowseGroupKey | null;
  roles: RoleBrowseEntry[];
};

export const ROLE_BROWSE_CATALOG_ROOT = "RoleBrowseCatalog";

function asRecord(value: unknown): Record<string, unknown> | null {
  return asJsonRecord(value);
}

function isRoleBrowseGroupKey(value: string): value is RoleBrowseGroupKey {
  return (ROLE_BROWSE_GROUPS as readonly string[]).includes(value);
}

/**
 * Normalize optional {@code group} query values. Blank → undefined (full catalog).
 * Unknown non-blank values throw (server would 400).
 */
export function normalizeRoleBrowseGroupFilter(
  raw: string | null | undefined,
): RoleBrowseGroupKey | undefined {
  if (raw == null) return undefined;
  const trimmed = raw.trim().toLowerCase();
  if (!trimmed) return undefined;
  if (!isRoleBrowseGroupKey(trimmed)) {
    throw new Error(
      `Unknown role browse group '${raw}'; expected community, workflow, or unassigned`,
    );
  }
  return trimmed;
}

function normalizeGroups(raw: unknown): RoleBrowseGroupKey[] {
  const out: RoleBrowseGroupKey[] = [];
  for (const g of asStringArray(raw)) {
    const key = g.trim().toLowerCase();
    if (isRoleBrowseGroupKey(key) && !out.includes(key)) {
      out.push(key);
    }
  }
  return out;
}

function normalizeEntry(raw: unknown): RoleBrowseEntry | null {
  const obj = asRecord(raw);
  if (!obj) return null;
  const name = typeof obj.name === "string" ? obj.name.trim() : "";
  if (!name) return null;
  const description =
    typeof obj.description === "string" ? obj.description : undefined;
  return {
    name,
    description,
    groups: normalizeGroups(obj.groups),
    communities: asStringArray(obj.communities),
    workflows: asStringArray(obj.workflows),
  };
}

function unwrapRolesList(payload: unknown): unknown[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  const obj = asRecord(payload);
  if (!obj) return [];
  for (const key of ["roles", "Roles", "RoleBrowseEntry", "role"]) {
    const raw = obj[key];
    if (raw == null) continue;
    if (Array.isArray(raw)) return raw;
    return [raw];
  }
  // Jackson single-item: roles field is one RoleBrowseEntry object.
  if (typeof obj.name === "string" && obj.name.trim()) {
    return [obj];
  }
  return [];
}

/**
 * Unwrap Jackson WRAP_ROOT {@code RoleBrowseCatalog} or a flat catalog body.
 */
export function unwrapRoleBrowseCatalog(payload: unknown): RoleBrowseCatalog {
  if (payload == null) {
    return { roles: [] };
  }
  let body = asRecord(payload);
  if (!body) {
    return { roles: [] };
  }
  const nested = asRecord(
    body[ROLE_BROWSE_CATALOG_ROOT] ?? body.roleBrowseCatalog,
  );
  if (nested) {
    body = nested;
  }

  let group: RoleBrowseGroupKey | null | undefined;
  if (typeof body.group === "string") {
    group = normalizeRoleBrowseGroupFilter(body.group) ?? null;
  } else if (body.group === null) {
    group = null;
  }

  const roles = unwrapRolesList(body.roles ?? body)
    .map(normalizeEntry)
    .filter((r): r is RoleBrowseEntry => r != null);

  return { group, roles };
}

/** Roles that belong under a navigator group (dual community+workflow appear in both). */
export function rolesInBrowseGroup(
  roles: readonly RoleBrowseEntry[],
  group: RoleBrowseGroupKey,
): RoleBrowseEntry[] {
  return roles
    .filter((r) => r.groups.includes(group))
    .slice()
    .sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { sensitivity: "base" }),
    );
}

/**
 * GET /services/roles/catalog — Admin SE-03 browse.
 *
 * @param group optional {@code community} | {@code workflow} | {@code unassigned}
 */
export async function browseRoles(
  group?: RoleBrowseGroupKey | null,
): Promise<RoleBrowseCatalog> {
  const filter = group ? normalizeRoleBrowseGroupFilter(group) : undefined;
  const url = filter
    ? `${PATHS.ROLES_CATALOG}?group=${encodeURIComponent(filter)}`
    : PATHS.ROLES_CATALOG;
  const payload = await get<unknown>(url);
  return unwrapRoleBrowseCatalog(payload);
}
