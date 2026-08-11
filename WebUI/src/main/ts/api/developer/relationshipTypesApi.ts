/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { RelationshipTypeDef } from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 */
export const RELATIONSHIP_TYPE_DESIGN_GAPS: string[] = [
  "Relationship type create / update / delete not supported via this API",
  "Cloning field override editor not supported via this API",
  "Effect condition and execution-context edit not supported via this API",
];

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

function withGaps(t: RelationshipTypeDef): RelationshipTypeDef {
  return {
    ...t,
    designGaps:
      t.designGaps && t.designGaps.length > 0
        ? t.designGaps
        : [...RELATIONSHIP_TYPE_DESIGN_GAPS],
  };
}

/** GET /services/relationshiptypes — list omits designGaps on the wire (REST-GAPS-02). */
export async function listRelationshipTypes(): Promise<RelationshipTypeDef[]> {
  const payload = await get<unknown>(PATHS.RELATIONSHIP_TYPES);
  return asArray<RelationshipTypeDef>(payload);
}

/** GET /services/relationshiptypes/{idOrName} */
export async function getRelationshipTypeDetail(
  idOrName: string,
): Promise<RelationshipTypeDef> {
  const key = encodeURIComponent(idOrName);
  const detail = await get<RelationshipTypeDef>(`${PATHS.RELATIONSHIP_TYPES}/${key}`);
  return withGaps(detail);
}
