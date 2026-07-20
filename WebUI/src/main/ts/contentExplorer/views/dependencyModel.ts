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
 * Pure helper for the modern Content Explorer's dependency view (US7 / T078).
 *
 * <p>The DependencyViewer renders the 6 {@link RelationshipDimension}
 * rows listed in `specs/992-react-content-explorer/contracts/capability-matrix.md`
 * P-Adv: outgoing / incoming / AA / taxonomy / local / reverse.
 * Until the gated `rest` enhancement for typed relationship lookup
 * ships (see `specs/992-react-content-explorer/research/relationship-rest-gaps.md`),
 * the only fully-derived row is the AA dimension (which the
 * `PSWidgetAssetRelationship` data can count via the existing AA
 * template lookup); the other 5 rows are marked {@code unknown} so
 * the UI labels the panel "Client-side preview" and avoids
 * pretending to be authoritative.</p>
 *
 * <p>This module is pure (no React, no fetch) — the component layer
 * consumes the returned summary shape.</p>
 */

import type { RelationshipSummary } from "../../api/contentExplorer/types";

/** Minimal item shape consumed by the dependency helper. */
export interface DependencyItemLike {
  id?: string;
  folderPath?: string;
  path?: string;
}

/** All 6 dimensions rendered by the dependency view. */
export const DEPENDENCY_DIMENSIONS = [
  "outgoing",
  "incoming",
  "aa",
  "taxonomy",
  "local",
  "reverse",
] as const;

export type DependencyDimension = (typeof DEPENDENCY_DIMENSIONS)[number];

/** Human-readable labels per dimension, keyed by the dimension id. */
export const DIMENSION_LABELS: Record<DependencyDimension, string> = {
  outgoing: "Outgoing relationships",
  incoming: "Incoming relationships",
  aa: "Active Assembly links",
  taxonomy: "Site / taxonomy edges",
  local: "Local dependencies",
  reverse: "Reverse dependencies",
};

export function labelFor(d: DependencyDimension): string {
  return DIMENSION_LABELS[d];
}

/**
 * The full set of relationship dimensions rendered for a single
 * selected node. Counts are derived client-side until the
 * `rest` enhancement for typed relationship lookup lands (see
 * `specs/992-react-content-explorer/research/relationship-rest-gaps.md`).
 */
export interface NodeRelationshipSummary {
  nodeId: string;
  nodePath?: string;
  dimensions: RelationshipSummary[];
  clientSideOnly: boolean;
}

/**
 * Synthesise a {@link NodeRelationshipSummary} for the supplied item.
 * The AA dimension is the only fully-populated row in 8.2; the
 * others are flagged {@code unknown} to reflect the `rest` gap
 * (matrix P-Adv row reads "Partial: client summary; full graph
 * pending rest enhancement"). The function is pure.
 */
export function synthesiseRelationshipSummary(
  item: DependencyItemLike,
  aaLinkCount: number,
): NodeRelationshipSummary {
  const dimensions: RelationshipSummary[] = DEPENDENCY_DIMENSIONS.map((d) =>
    d === "aa"
      ? {
          dimension: d,
          count: aaLinkCount,
          label: `${aaLinkCount} AA link${aaLinkCount === 1 ? "" : "s"}`,
        }
      : { dimension: d, count: 0, unknown: true },
  );
  return {
    nodeId: item.id ?? "",
    nodePath: item.folderPath ?? item.path,
    dimensions,
    clientSideOnly: true,
  };
}

/** Sum of all known dimension counts (unknown rows contribute 0). */
export function totalKnownEdges(summary: NodeRelationshipSummary): number {
  return summary.dimensions
    .filter((d: RelationshipSummary) => !d.unknown)
    .reduce(
      (acc: number, d: RelationshipSummary) => acc + d.count,
      0,
    );
}
