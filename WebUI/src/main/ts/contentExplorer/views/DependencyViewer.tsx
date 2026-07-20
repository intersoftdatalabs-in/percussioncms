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

import React from "react";
import type { NodeRelationshipSummary } from "./dependencyModel";
import type { RelationshipSummary } from "../../api/contentExplorer/types";
import type { PSNodeRelationshipSummary } from "../../api/contentExplorer/relationship";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import {
  composeFromServerSummary,
  labelFor,
  totalKnownEdges,
} from "./dependencyModel";
import { fetchNodeSummary } from "../../api/contentExplorer/relationshipsApi";

export interface DependencyItem {
  id?: string;
  folderPath?: string;
  path?: string;
}

export interface DependencyViewerProps {
  /** The item whose dependencies are rendered. */
  item: DependencyItem;
  /**
   * AA-link count for the supplied item. Host computes this from the existing AA relationship
   * data on the existing `PSWidgetAssetRelationshipService`; defaults to 0 (the row renders the
   * empty count, never an unknown).
   */
  aaLinkCount?: number;
  /** Optional injection seam for tests: pre-loads the consolidated server summary. */
  loadServerSummary?: (itemId: string) => Promise<PSNodeRelationshipSummary>;
  /** Optional injection seam for tests: summarises server-shape with AA-link count. */
  composeSummary?: (
    item: DependencyItem,
    serverSummary: PSNodeRelationshipSummary,
    aaLinkCount: number,
  ) => NodeRelationshipSummary;
  ariaLabel?: string;
  className?: string;
}

async function defaultLoadServerSummary(
  itemId: string,
): Promise<PSNodeRelationshipSummary> {
  return fetchNodeSummary(itemId);
}

function defaultComposeSummary(
  item: DependencyItem,
  server: PSNodeRelationshipSummary,
  aaLinkCount: number,
): NodeRelationshipSummary {
  return composeFromServerSummary(item, server, aaLinkCount);
}

/**
 * Modern Content Explorer's dependency view (US8 / T103).
 *
 * <p>Renders the 6 relationship dimensions for a single node. As of US8 all 6 dimensions are
 * authoritative — the morning "client-side preview" banner is removed; a transient loading
 * skeleton appears during the {@code fetchNodeSummary} round-trip.
 */
export function DependencyViewer(
  props: DependencyViewerProps,
): React.JSX.Element {
  const {
    item,
    aaLinkCount = 0,
    loadServerSummary = defaultLoadServerSummary,
    composeSummary,
    ariaLabel,
    className,
  } = props;
  const summarise = composeSummary ?? defaultComposeSummary;

  const itemId = item.id ?? "";
  const [state, setState] = React.useState<
    | { kind: "loading" }
    | { kind: "ok"; summary: PSNodeRelationshipSummary }
    | { kind: "auth" }
    | { kind: "error"; message: string }
  >({ kind: "loading" });

  React.useEffect(() => {
    let alive = true;
    setState({ kind: "loading" });
    loadServerSummary(itemId)
      .then((summary) => {
        if (!alive) return;
        setState({ kind: "ok", summary });
      })
      .catch((err: unknown) => {
        if (!alive) return;
        if (
          err &&
          typeof err === "object" &&
          "status" in err &&
          (err as { status: number }).status === 403
        ) {
          setState({ kind: "auth" });
          return;
        }
        setState({
          kind: "error",
          message: err instanceof Error ? err.message : String(err),
        });
      });
    return () => {
      alive = false;
    };
  }, [itemId, loadServerSummary]);

  if (state.kind === "loading") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.DEPENDENCY_TITLE)}
        data-testid="dependency-viewer"
        data-testid-state="loading"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p aria-live="polite">{message(EXPLORER_MSG.DEPENDENCY_LOADING)}</p>
      </section>
    );
  }
  if (state.kind === "auth") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.DEPENDENCY_TITLE)}
        data-testid="dependency-viewer"
        data-testid-state="auth"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="status" aria-live="polite">
          {message(EXPLORER_MSG.PERMISSION_DENIED)}
        </p>
      </section>
    );
  }
  if (state.kind === "error") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.DEPENDENCY_TITLE)}
        data-testid="dependency-viewer"
        data-testid-state="error"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="alert">
          {message(EXPLORER_MSG.DEPENDENCY_ERROR)}: {state.message}
        </p>
      </section>
    );
  }

  const summary: NodeRelationshipSummary = summarise(
    item,
    state.summary,
    aaLinkCount,
  );
  const total = totalKnownEdges(summary);

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.DEPENDENCY_TITLE)}
      data-testid="dependency-viewer"
      data-testid-state="ok"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.DEPENDENCY_TITLE)}:{" "}
        <code style={{ fontSize: "0.85rem" }}>
          {summary.nodePath ?? summary.nodeId}
        </code>
      </h2>
      <p
        aria-live="polite"
        data-testid="dependency-total"
        style={{ color: "#888", margin: "0 0 8px 0" }}
      >
        Known edges: {total}
      </p>
      <ul
        data-testid="dependency-dimensions"
        style={{ listStyle: "none", padding: 0, margin: 0 }}
      >
        {summary.dimensions.map((d) => (
          <li
            key={d.dimension}
            data-testid={`dependency-row-${d.dimension}`}
            style={{
              display: "flex",
              justifyContent: "space-between",
              padding: "4px 0",
              borderBottom: "1px solid #eee",
              color: d.unknown ? "#888" : "#222",
            }}
          >
            <span>{labelFor(d.dimension)}</span>
            <span>
              {d.unknown ? "—" : d.label ? `${d.label}` : `${d.count}`}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
