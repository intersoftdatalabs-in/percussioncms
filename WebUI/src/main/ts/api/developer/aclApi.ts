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

/** Jackson root for {@code AclList} (PUT /services/acls/bulk). */
export const ACL_LIST_ROOT = "AclList";

/** Jackson root for {@code CreateAclRequest} (POST /services/acls/). */
export const CREATE_ACL_REQUEST_ROOT = "CreateAclRequest";

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
 * <p>Body is Jackson-wrapped CreateAclRequest. A flat body fails server
 * UNWRAP_ROOT_VALUE (same class as UserPreference #2708 / #3378).
 */
export async function createObjectAcl(
  objectGuid: string | RestGuid,
  owner: CreateAclOwner,
): Promise<ObjectAcl> {
  const guid: RestGuid =
    typeof objectGuid === "string" ? { stringValue: objectGuid } : objectGuid;
  const payload = await post<unknown>(
    PATHS.ACLS,
    wrapCreateAclRequestForWire(guid, owner),
  );
  return unwrapObjectAcl(payload);
}

function parseGuidParts(guid: RestGuid | undefined): {
  type?: number;
  uuid?: number;
} {
  if (!guid) return {};
  if (guid.type != null && guid.type > 0 && guid.uuid != null && guid.uuid > 0) {
    return { type: guid.type, uuid: guid.uuid };
  }
  const raw = guid.stringValue?.trim();
  if (!raw) return { type: guid.type, uuid: guid.uuid };
  const parts = raw.split("-");
  if (parts.length < 3) return { type: guid.type, uuid: guid.uuid };
  const type = Number(parts[1]);
  const uuid = Number(parts[2]);
  return {
    type: Number.isFinite(type) ? type : guid.type,
    uuid: Number.isFinite(uuid) ? uuid : guid.uuid,
  };
}

/**
 * Normalize entries / permissions to the REST DTO shape for PUT.
 *
 * <p>{@code Principal} is name-only. {@code TypedPrincipal} carries type.
 * A raw JSON array is rejected by UNWRAP_ROOT_VALUE — callers must wrap with
 * {@link wrapAclListForWire}.
 */
export function normalizeAclForSave(acl: ObjectAcl): ObjectAcl {
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
    const principalName = e.name || e.principal?.name || e.type?.name;
    return {
      id: e.id,
      name: e.name,
      aclId: e.aclId,
      principal: principalName ? { name: principalName } : undefined,
      type:
        e.type?.type || e.type?.name || principalName
          ? {
              name: e.type?.name || principalName,
              type: e.type?.type,
            }
          : undefined,
      permissions: perms.map((p) => ({
        id: p.id,
        permission: p.permission,
      })),
    };
  });

  const objectGuid = acl.objectGuid;
  const parts = parseGuidParts(objectGuid);
  const name =
    typeof acl.name === "string" && acl.name.trim() ? acl.name.trim() : "ACL";
  return {
    id: acl.id,
    name,
    description: acl.description,
    objectId: acl.objectId && acl.objectId > 0 ? acl.objectId : parts.uuid,
    objectType: acl.objectType && acl.objectType > 0 ? acl.objectType : parts.type,
    guid: acl.guid,
    objectGuid,
    aclEntries,
  };
}

/**
 * PUT body for {@code /services/acls/bulk}.
 *
 * <p>Production CXF Jackson uses WRAP/UNWRAP_ROOT_VALUE. A bare array is HTTP 400
 * (Display Format Object ACL Save, #3378 / QA #2640).
 */
export function wrapAclListForWire(acl: ObjectAcl): { AclList: ObjectAcl[] } {
  return { [ACL_LIST_ROOT]: [normalizeAclForSave(acl)] };
}

/**
 * POST body for {@code /services/acls/}.
 */
export function wrapCreateAclRequestForWire(
  objectGuid: RestGuid,
  owner: CreateAclOwner,
): { CreateAclRequest: { objectGuid: RestGuid; owner: CreateAclOwner } } {
  return {
    [CREATE_ACL_REQUEST_ROOT]: {
      objectGuid,
      owner: {
        name: owner.name,
        type: owner.type,
      },
    },
  };
}

/**
 * PUT /services/acls/bulk
 *
 * <p>Persists one or more design-time ACLs (full entry + permission replace via merge).
 */
export async function saveObjectAcl(acl: ObjectAcl): Promise<void> {
  await put<unknown>(`${PATHS.ACLS}/bulk`, wrapAclListForWire(acl));
}
