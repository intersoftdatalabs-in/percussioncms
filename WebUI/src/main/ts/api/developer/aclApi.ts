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

import { get, put } from "../client";
import { PATHS } from "../paths";
import type { ObjectAcl, ObjectAclEntry, ObjectAclPermission } from "./types";

/** Design-time ACL permission names (matches REST Permissions enum). */
export const ACL_PERMISSIONS = [
  "READ",
  "UPDATE",
  "DELETE",
  "RUNTIME_VISIBLE",
  "OWNER",
] as const;

export type AclPermissionName = (typeof ACL_PERMISSIONS)[number];

/**
 * GET /services/acls/object/{objectGuid}
 *
 * <p>Returns the design-time ACL for a securable object. 404 when no ACL exists.
 */
export async function getAclForObject(objectGuid: string): Promise<ObjectAcl> {
  const key = encodeURIComponent(objectGuid);
  return get<ObjectAcl>(`${PATHS.ACLS}/object/${key}`);
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
