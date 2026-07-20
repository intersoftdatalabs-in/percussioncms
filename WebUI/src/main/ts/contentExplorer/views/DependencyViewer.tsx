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
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import {
  labelFor,
  synthesiseRelationshipSummary,
  totalKnownEdges,
} from "./dependencyModel";

export interface DependencyItem {
  id?: string;
  folderPath?: string;
  path?: string;
}

export interface DependencyViewerProps {
  /** The item whose dependencies are rendered. */
  item: DependencyItem;
  /**
   * AA-link count for the supplied item. Host computes this from
   * the existing `PSWidgetAssetRelationshipService` data; defaults
   * to 0 (rows other than AA get marked {@code unknown} per the
   * T074 spike).
   */
  aaLinkCount?: number;
  /**
   * Override the summary synthesis (e.g. with a future
   * server-provided full graph). Pure function in, pure shape out.
   */
  summarise?: (item: DependencyItem, aaLinkCount: number) => NodeRelationshipSummary;
  ariaLabel?: string;
  className?: string;
}

export function DependencyViewer(props: DependencyViewerProps): React.JSX.Element {
  const {
    item,
    aaLinkCount = 0,
    summarise = synthesiseRelationshipSummary,
    ariaLabel,
    className,
  } = props;
  const summary: NodeRelationshipSummary = summarise(item, aaLinkCount);
  const total = totalKnownEdges(summary);
  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.DEPENDENCY_TITLE)}
      data-testid="dependency-viewer"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.DEPENDENCY_TITLE)}
        : <code style={{ fontSize: "0.85rem" }}>{summary.nodePath ?? summary.nodeId}</code>
      </h2>
      {summary.clientSideOnly ? (
        <p
          role="status"
          aria-live="polite"
          data-testid="dependency-client-side-preview"
          style={{ color: "#a00", margin: "0 0 8px 0" }}
        >
          {message(EXPLORER_MSG.DEPENDENCY_CLIENT_SIDE_PREVIEW)}
        </p>
      ) : null}
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
              {d.unknown
                ? "—"
                : d.label
                  ? `${d.label}`
                  : `${d.count}`}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
