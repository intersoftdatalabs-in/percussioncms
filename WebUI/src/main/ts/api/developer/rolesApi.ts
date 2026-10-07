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

import { del, get, put } from "../client";
import { asJsonRecord, asStringArray } from "../jsonList";
import { PATHS } from "../paths";

/** Workbench Security Design SE-03 navigator folders. */
export const ROLE_BROWSE_GROUPS = ["community", "workflow", "unassigned"] as const;

/**
 * Roles the role service refuses to delete ({@code PSRoleService.SYSTEM_ROLES}).
 * Comparison is case-insensitive.
 */
export const SYSTEM_ROLE_NAMES = ["System", "Default"] as const;

export type RoleBrowseGroupKey = (typeof ROLE_BROWSE_GROUPS)[number];

/** One role in the Admin SE-03 browse catalog. */
export type RoleBrowseEntry = {
  name: string;
  description?: string;
  /** Stored landing page when set. Absent when the role has none. */
  homePage?: string;
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
  const homePageRaw = typeof obj.homePage === "string" ? obj.homePage.trim() : "";
  const entry: RoleBrowseEntry = {
    name,
    description,
    groups: normalizeGroups(obj.groups),
    communities: asStringArray(obj.communities),
    workflows: asStringArray(obj.workflows),
  };
  if (homePageRaw) {
    entry.homePage = homePageRaw;
  }
  return entry;
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
 * Unwrap a catalog body. Live CXF uses {@code WRAP_ROOT_VALUE}, so the payload is
 * {@code { RoleBrowseCatalog: { roles } }}. A flat {@code { roles }} body is also accepted.
 * One-item lists arrive as strings or a single object.
 */
export function unwrapRoleBrowseCatalog(payload: unknown): RoleBrowseCatalog {
  if (payload == null) {
    return { roles: [] };
  }
  const root = asRecord(payload);
  if (!root) {
    return { roles: [] };
  }
  const body =
    asRecord(root.RoleBrowseCatalog) ??
    asRecord(root.roleBrowseCatalog) ??
    root;

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

/** Wire body for PUT /services/roles/. Description and home page are optional. */
export type RoleCreateBody = {
  name: string;
  description?: string;
  /** Sent only by the home-page save. Blank clears. */
  homePage?: string;
};

/** Role returned by a successful create. */
export type RoleCreateResult = {
  name: string;
  description?: string;
};

/** Blank and whitespace-only names are rejected before the request. */
export function isRoleCreateReady(name: string | null | undefined): boolean {
  return typeof name === "string" && name.trim().length > 0;
}

/** True for {@link SYSTEM_ROLE_NAMES}. Blank is not a system role. */
export function isSystemRoleName(name: string | null | undefined): boolean {
  if (typeof name !== "string") return false;
  const trimmed = name.trim().toLowerCase();
  if (!trimmed) return false;
  return SYSTEM_ROLE_NAMES.some((system) => system.toLowerCase() === trimmed);
}

/** GET or DELETE /services/roles/{roleName}. The name is encoded once. */
export function roleReadUrl(name: string): string {
  return `${PATHS.ROLES}/${encodeURIComponent(name.trim())}`;
}

/** DELETE /services/roles/{roleName}. The name is encoded once. */
export function roleDeleteUrl(name: string): string {
  return roleReadUrl(name);
}

/** One role as returned by GET /services/roles/{roleName}. Users are read-only. */
export type RoleRead = {
  name: string;
  description?: string;
  homePage?: string;
  /** Stored user names. Empty when the role has none. Never invented from an error. */
  users: string[];
};

/**
 * User names on a role read. Accepts a string array, a Jackson one-item string,
 * or a JAXB {@code { user }} / {@code { users }} wrap. Blank names are dropped.
 * Unknown object keys are ignored so an error body cannot become a member list.
 */
export function normalizeRoleUsers(raw: unknown): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const name of collectRoleUserNames(raw, 0)) {
    const trimmed = name.trim();
    if (!trimmed || seen.has(trimmed)) continue;
    seen.add(trimmed);
    out.push(trimmed);
  }
  return out;
}

function collectRoleUserNames(raw: unknown, depth: number): string[] {
  if (depth > 4 || raw == null) return [];
  if (typeof raw === "string" || Array.isArray(raw)) {
    return asStringArray(raw);
  }
  const obj = asRecord(raw);
  if (!obj) return [];
  if ("user" in obj || "User" in obj) {
    return collectRoleUserNames(obj.user ?? obj.User, depth + 1);
  }
  if ("users" in obj || "Users" in obj) {
    return collectRoleUserNames(obj.users ?? obj.Users, depth + 1);
  }
  return [];
}

/**
 * Unwrap a flat Role body or a {@code {Role:{…}}} envelope from GET.
 * Missing {@code users} is an empty membership, not a failed read.
 * Throws when the payload has no name so callers do not show a nameless user list.
 */
export function unwrapRoleRead(payload: unknown): RoleRead {
  const obj = asRecord(payload);
  const wrapped = obj ? (asRecord(obj.Role) ?? asRecord(obj.role)) : null;
  const body = wrapped ?? obj;
  if (!body) {
    throw new Error("Role read returned an empty body");
  }
  const name = typeof body.name === "string" ? body.name.trim() : "";
  if (!name) {
    throw new Error("Role read returned no name");
  }
  const result: RoleRead = {
    name,
    users: normalizeRoleUsers(body.users ?? body.Users),
  };
  if (typeof body.description === "string") {
    result.description = body.description;
  }
  const homePage = typeof body.homePage === "string" ? body.homePage.trim() : "";
  if (homePage) {
    result.homePage = homePage;
  }
  return result;
}

/**
 * GET /services/roles/{roleName}. HTTP 403 and 404 reject before users are read,
 * so an error body cannot be shown as membership.
 */
export async function loadRole(name: string): Promise<RoleRead> {
  const trimmed = typeof name === "string" ? name.trim() : "";
  if (!isRoleCreateReady(trimmed)) {
    throw new Error("Role name is required");
  }
  const payload = await get<unknown>(roleReadUrl(trimmed));
  return unwrapRoleRead(payload);
}

/**
 * DELETE /services/roles/{roleName}. Blank names and system roles throw before fetch.
 * HTTP 400, 403, and 409 reject; this function does not resolve for those.
 */
export async function deleteRole(name: string): Promise<void> {
  const trimmed = typeof name === "string" ? name.trim() : "";
  if (!isRoleCreateReady(trimmed)) {
    throw new Error("Role name is required");
  }
  if (isSystemRoleName(trimmed)) {
    throw new Error("Cannot delete system role");
  }
  await del<unknown>(roleDeleteUrl(trimmed));
}

/** PUT /services/roles/?create=true — always the create path, never an update. */
export function roleCreateUrl(): string {
  return `${PATHS.ROLES}/?create=true`;
}

/** JAXB / Jackson {@code WRAP_ROOT_VALUE} name for {@code Role}. */
export const ROLE_WIRE_ROOT = "Role";

/**
 * Build the PUT body. A flat object fails server {@code UNWRAP_ROOT_VALUE}
 * ({@code unexpected element name; expected Role}).
 */
export function wrapRoleCreateForWire(
  body: RoleCreateBody,
): Record<string, RoleCreateBody> {
  return { [ROLE_WIRE_ROOT]: body };
}

/**
 * Unwrap a flat Role body or a {@code {Role:{…}}} envelope.
 * Throws when the payload has no name so callers do not treat an empty body as success.
 */
export function unwrapCreatedRole(payload: unknown): RoleCreateResult {
  const obj = asRecord(payload);
  const wrapped = obj ? asRecord(obj.Role) : null;
  const body = wrapped ?? obj;
  if (!body) {
    throw new Error("Role create returned an empty body");
  }
  const name = typeof body.name === "string" ? body.name.trim() : "";
  if (!name) {
    throw new Error("Role create returned no name");
  }
  const description =
    typeof body.description === "string" ? body.description : undefined;
  return { name, description };
}

/**
 * PUT /services/roles/?create=true — Admin create via adaptor createRole.
 * Duplicate names are HTTP 400 and are not updated. Blank names throw before fetch.
 */
export async function createRole(input: RoleCreateBody): Promise<RoleCreateResult> {
  const name = input.name.trim();
  if (!isRoleCreateReady(name)) {
    throw new Error("Role name is required");
  }
  const body: RoleCreateBody = { name };
  const description = input.description?.trim();
  if (description) {
    body.description = description;
  }
  const payload = await put<unknown>(roleCreateUrl(), wrapRoleCreateForWire(body));
  return unwrapCreatedRole(payload);
}

/** PUT /services/roles/?update=true — description of an existing role, never a create. */
export function roleUpdateDescriptionUrl(): string {
  return `${PATHS.ROLES}/?update=true`;
}

/**
 * PUT /services/roles/?update=true — Admin description edit.
 * Does not send users (the server keeps stored members). A blank description clears.
 * HTTP 400, 403, and 404 reject; this function does not return a role for those.
 */
export async function updateRoleDescription(
  input: RoleCreateBody,
): Promise<RoleCreateResult> {
  const name = input.name.trim();
  if (!isRoleCreateReady(name)) {
    throw new Error("Role name is required");
  }
  const description = input.description?.trim() ?? "";
  const body: RoleCreateBody = { name, description };
  const payload = await put<unknown>(
    roleUpdateDescriptionUrl(),
    wrapRoleCreateForWire(body),
  );
  return unwrapCreatedRole(payload);
}

/** PUT /services/roles/?homePage=true — home page of an existing role, never a create. */
export function roleUpdateHomePageUrl(): string {
  return `${PATHS.ROLES}/?homePage=true`;
}

/** Role returned by a successful home-page save. A missing homePage means it was cleared. */
export type RoleHomePageResult = {
  name: string;
  description?: string;
  homePage?: string;
};

/**
 * Unwrap a home-page save. A missing homePage is a clear, not a failed response.
 * Throws when the payload has no name so callers do not treat an empty body as success.
 */
export function unwrapUpdatedRoleHomePage(payload: unknown): RoleHomePageResult {
  const obj = asRecord(payload);
  const wrapped = obj ? asRecord(obj.Role) : null;
  const body = wrapped ?? obj;
  if (!body) {
    throw new Error("Role update returned an empty body");
  }
  const name = typeof body.name === "string" ? body.name.trim() : "";
  if (!name) {
    throw new Error("Role update returned no name");
  }
  const result: RoleHomePageResult = { name };
  if (typeof body.description === "string") {
    result.description = body.description;
  }
  if (typeof body.homePage === "string" && body.homePage.trim()) {
    result.homePage = body.homePage.trim();
  }
  return result;
}

/**
 * PUT /services/roles/?homePage=true — Admin home-page edit.
 * Does not send description or users (the server keeps both). A blank home page clears.
 * HTTP 400, 403, and 404 reject; this function does not return a role for those.
 */
export async function updateRoleHomePage(input: {
  name: string;
  homePage?: string;
}): Promise<RoleHomePageResult> {
  const name = input.name.trim();
  if (!isRoleCreateReady(name)) {
    throw new Error("Role name is required");
  }
  const homePage = input.homePage?.trim() ?? "";
  const payload = await put<unknown>(
    roleUpdateHomePageUrl(),
    wrapRoleCreateForWire({ name, homePage }),
  );
  return unwrapUpdatedRoleHomePage(payload);
}
