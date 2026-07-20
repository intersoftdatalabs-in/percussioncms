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
 * Pure helper for the modern Content Explorer's dependency view (US8 / T102).
 *
 * <p>The DependencyViewer renders the 6 {@link DependencyDimension}
 * rows listed in `specs/992-react-content-explorer/contracts/capability-matrix.md`
 * P-Adv: outgoing / incoming / AA / taxonomy / local / reverse.
 *
 * <p>As of US8 (spec 992), all 6 dimensions are authoritative. The server-supplied
 * consolidated summary (returned by {@link fetchNodeSummary} in
 * {@code relationshipsApi.ts}) is composed into the dependency view's row shape here.
 * The morning "unknown / clientSidePreview" branch is retained only as a unit-test
 * fallback (`synthesiseRelationshipSummary`); no production paths use it now that
 * US8 ships.</p>
 *
 * <p>This module is pure (no React, no fetch) — the component layer consumes the
 * returned summary shape.</p>
 */

import type {
  PSNodeRelationshipSummary as ServerNodeSummary,
  PSRelationshipSummary as ServerRelationSummary,
} from "../../api/contentExplorer/relationship";
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
 * selected node. Counts are derived from the server-supplied summary
 * when {@link NodeRelationshipSummary} is built via
 * {@link composeFromServerSummary}; the legacy client-side fallback
 * (`synthesiseRelationshipSummary`) flags the 5 non-AA rows as
 * {@code unknown} and exists for unit-test legibility only.
 */
export interface NodeRelationshipSummary {
  nodeId: string;
  nodePath?: string;
  dimensions: RelationshipSummary[];
  clientSideOnly: boolean;
}

/**
 * Compose a {@link NodeRelationshipSummary} from the sitemanage
 * consolidated summary (US8 / T102). Six dimensions are sourced:
 *
 * <ul>
 *   <li>outgoing — server-supplied row count + per-type breakdown.
 *   <li>incoming — same shape, AA-aware count.
 *   <li>aa — sourced host-side (the host shell already knows the AA-link
 *       count from {@code PSWidgetAssetRelationshipService}). Accepts
 *       {@code aaLinkCount >= 0}; 0 yields the empty-render shape.
 *   <li>taxonomy — node path list (the dependency view renders the
 *       count + first few nodes inline).
 *   <li>local — local-link rows (rendered with count + a "+N more"
 *       overflow link).
 *   <li>reverse — server-supplied row count; expected equal to
 *       {@code incoming} for the canonical translation category.
 * </ul>
 */
export function composeFromServerSummary(
  item: DependencyItemLike,
  server: ServerNodeSummary,
  aaLinkCount: number,
): NodeRelationshipSummary {
  return {
    nodeId: item.id ?? "",
    nodePath: item.folderPath ?? item.path,
    clientSideOnly: false,
    dimensions: [
      toRelationSummary("outgoing", server.outgoing),
      toRelationSummary("incoming", server.incoming),
      {
        dimension: "aa",
        count: aaLinkCount,
        label: `${aaLinkCount} AA link${aaLinkCount === 1 ? "" : "s"}`,
      },
      toRelationSummary("taxonomy", {
        count: server.taxonomy.count,
        byType: server.taxonomy.nodes.map((node) => ({ type: node, count: 1 })),
      }),
      {
        dimension: "local",
        count: server.local.count,
        label: `${server.local.count} local link${server.local.count === 1 ? "" : "s"}`,
      },
      toRelationSummary("reverse", server.reverse),
    ],
  };
}

/** Convert the server-typed summary to the component-typed `RelationshipSummary`. */
function toRelationSummary(
  dimension: RelationshipSummary["dimension"],
  server: ServerRelationSummary,
): RelationshipSummary {
  if (
    dimension === "outgoing" ||
    dimension === "incoming" ||
    dimension === "reverse" ||
    dimension === "aa"
  ) {
    return {
      dimension,
      count: server.count,
      label:
        server.byType.length === 0
          ? `${server.count} link${server.count === 1 ? "" : "s"}`
          : server.byType.map((b) => `${b.count} ${b.type}`).join(", "),
    };
  }
  // taxonomy is reported as a node-path list in PSNodeRelationshipSummary; keep clientSideOnly=false
  // because the server has the data but the type-bridge collapses it into a byType-style label.
  return {
    dimension: "taxonomy",
    count: server.count,
    label:
      server.byType.length === 0
        ? `${server.count} nodes`
        : `${server.count} node${server.count === 1 ? "" : "s"}`,
  };
}

/**
 * Synthesise a {@link NodeRelationshipSummary} for the supplied item using only client-side
 * data. The AA dimension is fully populated; the other five rows are flagged {@code unknown}
 * to reflect the absence of server data.
 *
 * <p>Retained for unit-test legibility. Production code paths must use
 * {@link composeFromServerSummary} now that US8 ships.</p>
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
    .reduce((acc: number, d: RelationshipSummary) => acc + d.count, 0);
}
