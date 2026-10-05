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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Typed fetch wrappers for the modern Content Explorer's relationship API (US8 / T101).
 *
 * <p>Wire shape mirrors {@code projects/sitemanage/.../share/relationship/data} and
 * {@code rest/src/main/java/com/percussion/rest/relationsummary/RelationshipSummaryResource}.
 *
 * <p>Endpoints (all GETs; CSRF-exempt; 403 on AuthZ denial via
 * {@code WebApplicationException(FORBIDDEN)} from the rest adaptor):
 *
 * <pre>
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/outgoing
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/incoming
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/taxonomy
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/local
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/reverse
 *   /Rhythmyx/rest/content-explorer/relationships/{itemId}/summary
 * </pre>
 */
import { del, post } from "../client";
import type {
  PSExplorerRelationshipEdge,
  PSExplorerRelationshipList,
  PSLocalDependencySummary,
  PSNodeRelationshipSummary,
  PSRelationshipSummary,
  PSTaxonomySummary,
} from "./relationship";

const BASE_PATH = "/Rhythmyx/rest/content-explorer/relationships";

/** Thrown when the sitemanage service returns Optional.empty() — AuthZ denial / id-resolution failure. */
export class RelationshipSummaryAuthError extends Error {
  readonly status: number;
  readonly statusText: string;
  constructor(message: string, status: number, statusText: string) {
    super(message);
    this.name = "RelationshipSummaryAuthError";
    this.status = status;
    this.statusText = statusText;
  }
}

async function fetchOne<T>(path: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(path, {
    method: "GET",
    headers: { Accept: "application/json" },
    credentials: "same-origin",
    signal,
  });
  if (res.status === 403 || res.status === 404) {
    throw new RelationshipSummaryAuthError(
      res.status === 404
        ? `Item not found for ${path}`
        : `Authorization denied for ${path}`,
      res.status,
      res.statusText,
    );
  }
  if (!res.ok) {
    const err = new Error(
      `Relationship summary request failed: ${res.status} ${res.statusText}`,
    );
    (err as Error & { status: number }).status = res.status;
    throw err;
  }
  return (await res.json()) as T;
}

export function fetchOutgoing(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSRelationshipSummary> {
  return fetchOne<PSRelationshipSummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/outgoing`,
    signal,
  );
}

export function fetchIncoming(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSRelationshipSummary> {
  return fetchOne<PSRelationshipSummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/incoming`,
    signal,
  );
}

export function fetchTaxonomy(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSTaxonomySummary> {
  return fetchOne<PSTaxonomySummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/taxonomy`,
    signal,
  );
}

export function fetchLocal(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSLocalDependencySummary> {
  return fetchOne<PSLocalDependencySummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/local`,
    signal,
  );
}

export function fetchReverse(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSRelationshipSummary> {
  return fetchOne<PSRelationshipSummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/reverse`,
    signal,
  );
}

export function fetchNodeSummary(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSNodeRelationshipSummary> {
  return fetchOne<PSNodeRelationshipSummary>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/summary`,
    signal,
  );
}

/**
 * Convenience: fetch all six dimensions in parallel. Returns the consolidated summary on
 * success and rejects with the first failing error (catchable per-call with the per-endpoint
 * fetches above).
 */
export async function fetchAllDimensions(itemId: string, signal?: AbortSignal) {
  const [outgoing, incoming, taxonomy, local, reverse] = await Promise.all([
    fetchOutgoing(itemId, signal),
    fetchIncoming(itemId, signal),
    fetchTaxonomy(itemId, signal),
    fetchLocal(itemId, signal),
    fetchReverse(itemId, signal),
  ]);
  return { outgoing, incoming, taxonomy, local, reverse };
}

function unwrapEdges(raw: unknown): PSExplorerRelationshipEdge[] {
  if (!raw || typeof raw !== "object") {
    return [];
  }
  const record = raw as Record<string, unknown>;
  const body =
    record.PSExplorerRelationshipList &&
    typeof record.PSExplorerRelationshipList === "object"
      ? (record.PSExplorerRelationshipList as Record<string, unknown>)
      : record;
  const items = body.items;
  if (!Array.isArray(items)) {
    return [];
  }
  return items.map((row) => {
    const rec = row as Record<string, unknown>;
    const relationshipId = Number(rec.relationshipId);
    if (!Number.isFinite(relationshipId) || relationshipId <= 0) {
      throw Object.assign(new Error("relationshipId is missing"), { status: 400 });
    }
    const slotId = Number(rec.slotId ?? 0);
    const sortRank = Number(rec.sortRank ?? 0);
    return {
      relationshipId,
      configName: String(rec.configName ?? ""),
      category: String(rec.category ?? ""),
      dependentId: Number(rec.dependentId ?? 0),
      label: String(rec.label ?? ""),
      slotId: Number.isFinite(slotId) && slotId > 0 ? slotId : 0,
      sortRank: Number.isFinite(sortRank) ? sortRank : 0,
    };
  });
}

export async function fetchRelationshipEdges(
  itemId: string,
  signal?: AbortSignal,
): Promise<PSExplorerRelationshipEdge[]> {
  const raw = await fetchOne<PSExplorerRelationshipList | Record<string, unknown>>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/edges`,
    signal,
  );
  return unwrapEdges(raw);
}

export async function addRelationshipEdge(
  itemId: string,
  targetItemId: string,
  configName: string,
): Promise<PSExplorerRelationshipEdge> {
  const target = targetItemId.trim();
  const typeName = configName.trim();
  if (!target || !typeName) {
    throw Object.assign(new Error("target and relationship type are required"), {
      status: 400,
    });
  }
  if (isFolderRelationshipCategory(typeName)) {
    throw Object.assign(new Error("Folder relationships cannot be added"), {
      status: 409,
    });
  }
  const raw = await post<PSExplorerRelationshipEdge | Record<string, unknown>>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/edges`,
    { targetItemId: target, configName: typeName },
  );
  const record =
    raw && typeof raw === "object" && "PSExplorerRelationshipEdge" in raw
      ? (raw.PSExplorerRelationshipEdge as Record<string, unknown>)
      : (raw as Record<string, unknown>);
  const relationshipId = Number(record?.relationshipId);
  if (!Number.isFinite(relationshipId) || relationshipId <= 0) {
    throw Object.assign(new Error("relationship was not created"), { status: 409 });
  }
  return {
    relationshipId,
    configName: String(record.configName ?? typeName),
    category: String(record.category ?? ""),
    dependentId: Number(record.dependentId ?? 0),
    label: String(record.label ?? ""),
  };
}

export async function removeRelationshipEdge(
  itemId: string,
  relationshipId: number,
): Promise<void> {
  if (!(Number.isFinite(relationshipId) && relationshipId > 0)) {
    throw Object.assign(new Error("relationshipId must be a positive id"), {
      status: 400,
    });
  }
  await del<void>(
    `${BASE_PATH}/${encodeURIComponent(itemId)}/edges/${relationshipId}`,
  );
}

/** Folder membership is not an owned content relationship (#4988). */
export function isFolderRelationshipCategory(category: string): boolean {
  const normalized = category.trim().toLowerCase();
  return normalized === "rs_folder" || normalized === "folder";
}

function isActiveAssemblyToken(value: string): boolean {
  const token = value.trim().toLowerCase().replace(/[\s_-]+/g, "");
  return (
    token === "rsaa" ||
    token === "aa" ||
    token === "activeassembly" ||
    token === "rsactiveassembly" ||
    token.endsWith("activeassembly")
  );
}

/**
 * Active Assembly rows are the only Explorer edges the editor reorder API can move.
 * The server category is {@code rs_activeassembly}. Folder membership is never
 * reorderable, even if a label looks like Active Assembly.
 */
export function isActiveAssemblyRelationship(
  edge: Pick<PSExplorerRelationshipEdge, "category" | "configName">,
): boolean {
  if (
    isFolderRelationshipCategory(edge.category) ||
    isFolderRelationshipCategory(edge.configName)
  ) {
    return false;
  }
  if (isActiveAssemblyToken(edge.category ?? "")) {
    return true;
  }
  const config = (edge.configName ?? "").trim().toLowerCase().replace(/[\s_-]+/g, "");
  return config === "activeassembly" || config === "rsactiveassembly" || config === "rsaa";
}

function assemblySlotKey(edge: PSExplorerRelationshipEdge): string {
  const slotId = edge.slotId ?? 0;
  if (Number.isFinite(slotId) && slotId > 0) {
    return `slot:${slotId}`;
  }
  return "slot:unspecified";
}

/**
 * Whether one Active Assembly edge can move one step among siblings in the same slot.
 * Rows with no slot id share one group so older payloads still move as a single list.
 */
export function relationshipMoveEnds(
  edges: readonly PSExplorerRelationshipEdge[],
  edge: PSExplorerRelationshipEdge,
): { up: boolean; down: boolean } {
  if (!isActiveAssemblyRelationship(edge) || !(edge.relationshipId > 0)) {
    return { up: false, down: false };
  }
  const slot = assemblySlotKey(edge);
  const siblings = edges.filter(
    (candidate) =>
      isActiveAssemblyRelationship(candidate) &&
      candidate.relationshipId > 0 &&
      assemblySlotKey(candidate) === slot,
  );
  const index = siblings.findIndex(
    (candidate) => candidate.relationshipId === edge.relationshipId,
  );
  if (index < 0 || siblings.length < 2) {
    return { up: false, down: false };
  }
  return { up: index > 0, down: index < siblings.length - 1 };
}

/** Edges the panel may delete. Folder rows are left in place. */
export function removableOwnedEdges(
  edges: readonly PSExplorerRelationshipEdge[],
): PSExplorerRelationshipEdge[] {
  return edges.filter((edge) => !isFolderRelationshipCategory(edge.category));
}

/**
 * Deletes every owned non-folder relationship, in list order.
 * The first HTTP failure rejects so the caller must not claim full success.
 */
export async function removeAllOwnedRelationshipEdges(
  itemId: string,
  edges: readonly PSExplorerRelationshipEdge[],
  remove: (
    itemId: string,
    relationshipId: number,
  ) => Promise<void> = removeRelationshipEdge,
): Promise<void> {
  for (const edge of removableOwnedEdges(edges)) {
    await remove(itemId, edge.relationshipId);
  }
}
