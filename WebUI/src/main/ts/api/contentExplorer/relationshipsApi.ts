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
import type {
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
  if (res.status === 403) {
    throw new RelationshipSummaryAuthError(
      `Authorization denied for ${path}`,
      res.status,
      res.statusText,
    );
  }
  if (!res.ok) {
    throw new Error(
      `Relationship summary request failed: ${res.status} ${res.statusText}`,
    );
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
