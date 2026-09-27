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
 * IA (information architecture) Relationships view (US8 / T103).
 *
 * <p>Renders the same 6-dimension dependency model as
 * {@link DependencyViewer} but grouped by direction (outgoing / incoming) with an
 * explicit "IA focus" filter — the taxonomy dimension is elevated, and AA links are
 * demoted to a footer so the IA team can scan the relationship density without the link
 * count drowning out the node / taxonomy view they care about.</p>
 *
 * <p>As of US8 the consolidated server summary populates all 6 dimensions; the morning
 * client-side summary fallback is kept for test legibility but is no longer wired to the
 * production flow.</p>
 */

import React from "react";
import type { DependencyItem as DependencyItemShared } from "./DependencyViewer";
import type { NodeRelationshipSummary } from "./dependencyModel";
import type { RelationshipSummary } from "../../api/contentExplorer/types";
import type { PSNodeRelationshipSummary } from "../../api/contentExplorer/relationship";
import { message } from "../../i18n/message";
import { EXPLORER_MSG } from "../messages";
import { composeFromServerSummary, labelFor } from "./dependencyModel";
import { parseExplorerContentId } from "../../api/contentExplorer/pathItemId";
import type { PSExplorerRelationshipEdge } from "../../api/contentExplorer/relationship";
import {
  fetchNodeSummary,
  fetchRelationshipEdges,
  removeRelationshipEdge,
} from "../../api/contentExplorer/relationshipsApi";

export interface RelationshipsViewProps {
  item: DependencyItemShared;
  aaLinkCount?: number;
  /** Optional injection seam for tests: pre-loads the consolidated server summary. */
  loadServerSummary?: (itemId: string) => Promise<PSNodeRelationshipSummary>;
  /** Optional injection seam: removable relationships owned by the item. */
  loadEdges?: (itemId: string) => Promise<PSExplorerRelationshipEdge[]>;
  /** Optional injection seam: delete one owned relationship. */
  removeEdge?: (itemId: string, relationshipId: number) => Promise<void>;
  /** Optional injection seam for tests: summarises server-shape with AA-link count. */
  composeSummary?: (
    item: DependencyItemShared,
    serverSummary: PSNodeRelationshipSummary,
    aaLinkCount: number,
  ) => NodeRelationshipSummary;
  ariaLabel?: string;
  className?: string;
}

const IA_PRIMARY: ReadonlyArray<
  "outgoing" | "incoming" | "taxonomy" | "local"
> = ["outgoing", "incoming", "taxonomy", "local"];

/**
 * REST {@code /relationships/{id}} needs a numeric content id. Explorer
 * rows often carry a GUID ({@code 1-101-708}); taxonomy treats a
 * non-path string as a JCR path and the summary returns 403.
 */
export function relationshipSummaryItemId(
  raw: string | number | undefined,
): string {
  if (raw == null || raw === "") {
    return "";
  }
  const parsed = parseExplorerContentId(raw);
  if (parsed != null) {
    return String(parsed);
  }
  // Unparseable titles (timestamped percSimpleText names, slugs) must not
  // be sent as /relationships/{id} — that 403s and looks like a permission
  // error for Admin (#3811). Match DependencyViewer: empty id, no fetch.
  return "";
}

async function defaultLoadServerSummary(
  itemId: string,
): Promise<PSNodeRelationshipSummary> {
  return fetchNodeSummary(itemId);
}

function defaultComposeSummary(
  item: DependencyItemShared,
  server: PSNodeRelationshipSummary,
  aaLinkCount: number,
): NodeRelationshipSummary {
  return composeFromServerSummary(item, server, aaLinkCount);
}

export function RelationshipsView(
  props: RelationshipsViewProps,
): React.JSX.Element {
  const {
    item,
    aaLinkCount = 0,
    loadServerSummary = defaultLoadServerSummary,
    loadEdges = fetchRelationshipEdges,
    removeEdge = removeRelationshipEdge,
    composeSummary,
    ariaLabel,
    className,
  } = props;
  const summarise = composeSummary ?? defaultComposeSummary;

  const itemId = relationshipSummaryItemId(item.id);
  const [reloadToken, setReloadToken] = React.useState(0);
  const [confirmId, setConfirmId] = React.useState<number | null>(null);
  const [removedNotice, setRemovedNotice] = React.useState(false);
  const [removeError, setRemoveError] = React.useState<string | null>(null);
  const [removing, setRemoving] = React.useState(false);
  const [edges, setEdges] = React.useState<PSExplorerRelationshipEdge[]>([]);
  const [state, setState] = React.useState<
    | { kind: "loading" }
    | { kind: "ok"; summary: PSNodeRelationshipSummary }
    | { kind: "auth" }
    | { kind: "error"; message: string }
  >({ kind: "loading" });

  React.useEffect(() => {
    let alive = true;
    if (!itemId) {
      // No item id → don't fire a network round-trip; the rest endpoint
      // would 404 on the empty path segment. Render the auth placeholder
      // instead (per the bot review on PR #1410).
      setState({ kind: "auth" });
      setEdges([]);
      setRemovedNotice(false);
      return;
    }
    setState({ kind: "loading" });
    setConfirmId(null);
    setRemoveError(null);
    loadServerSummary(itemId)
      .then(async (summary) => {
        if (!alive) return;
        let nextEdges: PSExplorerRelationshipEdge[] = [];
        try {
          nextEdges = await loadEdges(itemId);
        } catch (edgeErr: unknown) {
          if (!alive) return;
          setEdges([]);
          setRemoveError(removeFailureMessage(edgeErr));
          setState({ kind: "ok", summary });
          return;
        }
        if (!alive) return;
        setEdges(nextEdges);
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
  }, [itemId, loadServerSummary, loadEdges, reloadToken]);

  function statusOf(err: unknown): number | undefined {
    if (err && typeof err === "object" && "status" in err) {
      const status = (err as { status: unknown }).status;
      return typeof status === "number" ? status : undefined;
    }
    return undefined;
  }

  function removeFailureMessage(err: unknown): string {
    const status = statusOf(err);
    if (status === 400) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_400);
    if (status === 403) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_403);
    if (status === 409) return message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_FAILED_409);
    return err instanceof Error ? err.message : String(err);
  }

  async function confirmRemove(relationshipId: number): Promise<void> {
    if (!itemId || removing) return;
    setRemoving(true);
    setRemoveError(null);
    setRemovedNotice(false);
    try {
      await removeEdge(itemId, relationshipId);
      setConfirmId(null);
      setRemovedNotice(true);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      setRemovedNotice(false);
      setRemoveError(removeFailureMessage(err));
      setConfirmId(null);
    } finally {
      setRemoving(false);
    }
  }

  if (state.kind === "loading") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
        data-testid-state="loading"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p aria-live="polite">{message(EXPLORER_MSG.RELATIONSHIPS_LOADING)}</p>
      </section>
    );
  }
  if (state.kind === "auth") {
    return (
      <section
        role="region"
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
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
        aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
        data-testid="relationships-view"
        data-testid-state="error"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="alert">
          {message(EXPLORER_MSG.RELATIONSHIPS_ERROR)}: {state.message}
        </p>
      </section>
    );
  }

  const summary: NodeRelationshipSummary = summarise(
    item,
    state.summary,
    aaLinkCount,
  );
  const primary = summary.dimensions.filter((d: RelationshipSummary) =>
    IA_PRIMARY.some((k) => k === d.dimension),
  );
  const aaRow = summary.dimensions.find(
    (d: RelationshipSummary) => d.dimension === "aa",
  );
  const reverseRow = summary.dimensions.find(
    (d: RelationshipSummary) => d.dimension === "reverse",
  );

  return (
    <section
      role="region"
      aria-label={ariaLabel ?? message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}
      data-testid="relationships-view"
      data-testid-state="ok"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <h2 style={{ fontSize: "1rem", margin: "0 0 8px 0" }}>
        {message(EXPLORER_MSG.RELATIONSHIPS_TITLE)}:{" "}
        <code style={{ fontSize: "0.85rem" }}>
          {summary.nodePath ?? summary.nodeId}
        </code>
      </h2>
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
            <span>{d.unknown ? "\u2014" : (d.label ?? `${d.count}`)}</span>
          </li>
        ))}
      </ul>
      <div data-testid="relationships-remove-section" style={{ marginTop: 12 }}>
        <h3 style={{ fontSize: "0.95rem", margin: "0 0 8px 0" }}>
          {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_DO)}
        </h3>
        {removedNotice ? (
          <p role="status" data-testid="relationships-removed">
            {message(EXPLORER_MSG.RELATIONSHIPS_REMOVED)}
          </p>
        ) : null}
        {removeError ? (
          <p role="alert" data-testid="relationships-remove-error">
            {removeError}
          </p>
        ) : null}
        {edges.length === 0 ? (
          <p data-testid="relationships-remove-empty">
            {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_EMPTY)}
          </p>
        ) : (
          <ul
            data-testid="relationships-edge-list"
            style={{ listStyle: "none", padding: 0, margin: 0 }}
          >
            {edges.map((edge) => (
              <li
                key={edge.relationshipId}
                data-testid={`relationships-edge-${edge.relationshipId}`}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 8,
                  padding: "4px 0",
                }}
              >
                <span>{edge.label}</span>
                <button
                  type="button"
                  data-testid={`relationships-remove-${edge.relationshipId}`}
                  onClick={() => {
                    setRemovedNotice(false);
                    setRemoveError(null);
                    setConfirmId(edge.relationshipId);
                  }}
                >
                  {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE)}
                </button>
              </li>
            ))}
          </ul>
        )}
        {confirmId != null ? (
          <div
            role="dialog"
            aria-modal="true"
            data-testid="relationships-remove-dialog"
            style={{ marginTop: 8, padding: 8, border: "1px solid #ccc" }}
          >
            <p>{message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CONFIRM)}</p>
            <button
              type="button"
              data-testid="relationships-remove-cancel"
              onClick={() => setConfirmId(null)}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_CANCEL)}
            </button>
            <button
              type="button"
              data-testid="relationships-remove-confirm"
              disabled={removing}
              onClick={() => {
                void confirmRemove(confirmId);
              }}
            >
              {message(EXPLORER_MSG.RELATIONSHIPS_REMOVE_DO)}
            </button>
          </div>
        ) : null}
      </div>
      <details style={{ marginTop: 12 }}>
        <summary>Supplementary links</summary>
        <ul
          style={{ listStyle: "none", padding: 0 }}
          data-testid="relationships-extra"
        >
          {aaRow ? (
            <li data-testid="relationships-row-aa">
              {labelFor("aa")}:{" "}
              {aaRow.unknown ? "\u2014" : (aaRow.label ?? `${aaRow.count}`)}
            </li>
          ) : null}
          {reverseRow ? (
            <li data-testid="relationships-row-reverse">
              {labelFor("reverse")}:{" "}
              {reverseRow.unknown
                ? "\u2014"
                : (reverseRow.label ?? `${reverseRow.count}`)}
            </li>
          ) : null}
        </ul>
      </details>
    </section>
  );
}
