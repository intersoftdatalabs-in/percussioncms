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
 * TypeScript mirrors of the modern Content Explorer's relationship API (US8 / T100).
 *
 * <p>Mirror of the sitemanage DTOs in
 * {@code projects/sitemanage/src/main/java/com/percussion/share/relationship/data/}. Do not
 * invent fields; if a field is missing server-side, file a sitemanage change with a
 * service-contract test (T052a) and threat-model note (T052b) first.
 *
 * <p>Wire envelope (Jackson {@code @JsonRootName} on the server side):
 *
 * <pre>
 *   { "PSRelationshipSummary": { count, byType: [{type, count}] } }
 *   { "PSTaxonomySummary":     { count, nodes: [string] } }
 *   { "PSLocalDependencySummary": { count, links: [{type, targetId}] } }
 *   { "PSNodeRelationshipSummary": {
 *       outgoing: PSRelationshipSummary,
 *       incoming: PSRelationshipSummary,
 *       taxonomy: PSTaxonomySummary,
 *       local: PSLocalDependencySummary,
 *       reverse: PSRelationshipSummary,
 *     } }
 * </pre>
 */
export interface RelationshipTypeBucket {
  type: string;
  count: number;
}

export interface PSRelationshipSummary {
  count: number;
  byType: RelationshipTypeBucket[];
}

export interface PSTaxonomySummary {
  count: number;
  nodes: string[];
}

export interface PSLocalDependencyLink {
  /** One of "local", "linked", "shared". */
  type: string;
  targetId: string;
}

export interface PSLocalDependencySummary {
  count: number;
  links: PSLocalDependencyLink[];
}

export interface PSNodeRelationshipSummary {
  outgoing: PSRelationshipSummary;
  incoming: PSRelationshipSummary;
  taxonomy: PSTaxonomySummary;
  local: PSLocalDependencySummary;
  reverse: PSRelationshipSummary;
}

export type RelationshipDimension =
  "outgoing" | "incoming" | "aa" | "taxonomy" | "local" | "reverse";

export const RELATIONSHIP_DIMENSIONS: readonly RelationshipDimension[] = [
  "outgoing",
  "incoming",
  "aa",
  "taxonomy",
  "local",
  "reverse",
] as const;
