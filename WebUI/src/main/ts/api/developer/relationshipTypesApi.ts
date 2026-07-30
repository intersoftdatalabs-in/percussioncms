/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { RelationshipTypeDef } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw =
      obj.RelationshipType ??
      obj.relationshipType ??
      obj.RelationshipTypes ??
      obj.relationshipTypes;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/relationshiptypes */
export async function listRelationshipTypes(): Promise<RelationshipTypeDef[]> {
  const payload = await get<unknown>(PATHS.RELATIONSHIP_TYPES);
  return asArray<RelationshipTypeDef>(payload);
}

/** GET /services/relationshiptypes/{idOrName} */
export async function getRelationshipTypeDetail(
  idOrName: string,
): Promise<RelationshipTypeDef> {
  const key = encodeURIComponent(idOrName);
  return get<RelationshipTypeDef>(`${PATHS.RELATIONSHIP_TYPES}/${key}`);
}
