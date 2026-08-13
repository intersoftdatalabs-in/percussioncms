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

import { get, post, put } from "../client";
import { PATHS } from "../paths";
import type { ObjectAcl, ObjectAclEntry, ObjectAclPermission, RestGuid } from "./types";

/** Design-time ACL permission names (matches REST Permissions enum). */
export const ACL_PERMISSIONS = [
  "READ",
  "UPDATE",
  "DELETE",
  "RUNTIME_VISIBLE",
  "OWNER",
] as const;

export type AclPermissionName = (typeof ACL_PERMISSIONS)[number];

/** Owner principal for POST /services/acls/ (CreateAclRequest). */
export type CreateAclOwner = {
  name: string;
  type: string;
};

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"Acl":{…}}} so ObjectAclSection can
 * read entries. Flat bodies pass through (site / display-format Object ACL, #3200).
 */
export function unwrapObjectAcl(payload: unknown): ObjectAcl {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.Acl ?? root.acl;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as ObjectAcl;
  }
  return payload as ObjectAcl;
}

/**
 * GET /services/acls/object/{objectGuid}
 *
 * <p>Returns the design-time ACL for a securable object. 404 when no ACL exists.
 */
export async function getAclForObject(objectGuid: string): Promise<ObjectAcl> {
  const key = encodeURIComponent(objectGuid);
  const payload = await get<unknown>(`${PATHS.ACLS}/object/${key}`);
  return unwrapObjectAcl(payload);
}

/**
 * POST /services/acls/ — create a design-time ACL for an object that has none.
 *
 * <p>Body is CreateAclRequest: objectGuid + owner TypedPrincipal. Server creates
 * an ACL with a single OWNER entry for the owner.
 */
export async function createObjectAcl(
  objectGuid: string | RestGuid,
  owner: CreateAclOwner,
): Promise<ObjectAcl> {
  const guid: RestGuid =
    typeof objectGuid === "string" ? { stringValue: objectGuid } : objectGuid;
  const payload = await post<unknown>(PATHS.ACLS, {
    objectGuid: guid,
    owner: {
      name: owner.name,
      type: owner.type,
    },
  });
  return unwrapObjectAcl(payload);
}

/**
 * Normalize entries / permissions to plain arrays for PUT payload.
 * Jackson maps AclList as a JSON array of Acl.
 */
function normalizeAclForSave(acl: ObjectAcl): ObjectAcl {
  const entries = Array.isArray(acl.aclEntries)
    ? acl.aclEntries
    : Array.isArray((acl.aclEntries as { AclEntry?: ObjectAclEntry[] } | undefined)?.AclEntry)
      ? (acl.aclEntries as { AclEntry: ObjectAclEntry[] }).AclEntry
      : [];

  const aclEntries: ObjectAclEntry[] = entries.map((e) => {
    const raw = e.permissions;
    let perms: ObjectAclPermission[] = [];
    if (Array.isArray(raw)) perms = raw;
    else if (Array.isArray((raw as { UserAccessLevel?: ObjectAclPermission[] } | undefined)?.UserAccessLevel)) {
      perms = (raw as { UserAccessLevel: ObjectAclPermission[] }).UserAccessLevel;
    }
    return {
      id: e.id,
      name: e.name,
      aclId: e.aclId,
      principal: e.principal || (e.name ? { name: e.name } : undefined),
      type: e.type,
      permissions: perms.map((p) => ({
        id: p.id,
        permission: p.permission,
      })),
    };
  });

  return {
    id: acl.id,
    name: acl.name,
    description: acl.description,
    objectId: acl.objectId,
    objectType: acl.objectType,
    guid: acl.guid,
    objectGuid: acl.objectGuid,
    aclEntries,
  };
}

/**
 * PUT /services/acls/bulk
 *
 * <p>Persists one or more design-time ACLs (full entry + permission replace via merge).
 */
export async function saveObjectAcl(acl: ObjectAcl): Promise<void> {
  await put<unknown>(`${PATHS.ACLS}/bulk`, [normalizeAclForSave(acl)]);
}
