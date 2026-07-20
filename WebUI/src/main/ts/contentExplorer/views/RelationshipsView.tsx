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
 * IA (information architecture) Relationships view (US7 / T079).
 *
 * <p>Renders the same 6-dimension dependency model as
 * {@link DependencyViewer} but grouped by direction (outgoing /
 * incoming) with an explicit "IA focus" filter — the taxonomy
 * dimension is elevated, and AA links are demoted to a footer so
 * the IA team can scan the relationship density without the link
 * count drowning out the node / taxonomy view they care about.</p>
 *
 * <p>The matrix P-Adv note about the "rest enhancement pending" still
 * applies — the IA view is a client-side summary until the same
 * typed-relationship REST endpoint ships.</p>
 */

import React from "react";
import type {
  DependencyItem as DependencyItemShared,
} from "./DependencyViewer";
import type { NodeRelationshipSummary } from "./dependencyModel";
import type { RelationshipSummary } from "../../api/contentExplorer/types";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import {
  labelFor,
  synthesiseRelationshipSummary,
} from "./dependencyModel";

export interface RelationshipsViewProps {
  item: DependencyItemShared;
  aaLinkCount?: number;
  summarise?: (item: DependencyItemShared, aaLinkCount: number) => NodeRelationshipSummary;
  ariaLabel?: string;
  className?: string;
}

/**
 * The four IA-relevant dimensions, in the order rendered above AA.
 * AA is demoted to a footer row.
 */
const IA_PRIMARY: ReadonlyArray<"outgoing" | "incoming" | "taxonomy" | "local"> = [
  "outgoing",
  "incoming",
  "taxonomy",
  "local",
];

export function RelationshipsView(
  props: RelationshipsViewProps,
): React.JSX.Element {
  const {
    item,
    aaLinkCount = 0,
    summarise = synthesiseRelationshipSummary,
    ariaLabel,
    className,
  } = props;
  const summary = summarise(item, aaLinkCount);

  const primary = summary.dimensions.filter((d: RelationshipSummary) =>
    IA_PRIMARY.some((k) => k === d.dimension),
  );
  const aaRow = summary.dimensions.find((d: RelationshipSummary) => d.dimension === "aa");
  const reverseRow = summary.dimensions.find((d: RelationshipSummary) => d.dimension === "reverse");

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
      data-testid="relationships-view"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        : <code style={{ fontSize: "0.85rem" }}>{summary.nodePath ?? summary.nodeId}</code>
      </h2>
      {summary.clientSideOnly ? (
        <p
          role="status"
          aria-live="polite"
          data-testid="relationships-client-side-preview"
          style={{ color: "#a00", margin: "0 0 8px 0" }}
        >
          {message(EXPLORER_MSG.RELATIONSHIPS_CLIENT_SIDE_PREVIEW)}
        </p>
      ) : null}
      <ul
        data-testid="relationships-primary"
        style={{ listStyle: "none", padding: 0, margin: 0 }}
      >
        {primary.map((d: RelationshipSummary) => (
          <li
            key={d.dimension}
            data-testid={`relationships-row-${d.dimension}`}
            style={{
              display: "flex",
              justifyContent: "space-between",
              padding: "4px 0",
              borderBottom: "1px solid #eee",
              color: d.unknown ? "#888" : "#222",
            }}
          >
            <span>{labelFor(d.dimension)}</span>
            <span>{d.unknown ? "\u2014" : d.label ?? `${d.count}`}</span>
          </li>
        ))}
      </ul>
      <details style={{ marginTop: 12 }}>
        <summary>Supplementary links</summary>
        <ul style={{ listStyle: "none", padding: 0 }} data-testid="relationships-extra">
          {aaRow ? (
            <li data-testid="relationships-row-aa">
              {labelFor("aa")}: {aaRow.unknown ? "\u2014" : aaRow.label ?? `${aaRow.count}`}
            </li>
          ) : null}
          {reverseRow ? (
            <li data-testid="relationships-row-reverse">
              {labelFor("reverse")}: {reverseRow.unknown ? "\u2014" : reverseRow.label ?? `${reverseRow.count}`}
            </li>
          ) : null}
        </ul>
      </details>
    </section>
  );
}
