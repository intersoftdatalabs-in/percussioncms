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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useMemo, useRef, useState } from "react";
import { listWorkflows } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import {
  useDialogEscape,
  useDialogFocusTrap,
} from "../architecture/useDialogEscape";
import {
  catalogColors,
  mutedCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export interface WorkflowAssociationDialogProps {
  open: boolean;
  /** Workflow names already on the allowed-workflow edge (case-insensitive). */
  associatedNames: string[];
  /** Injectable catalog loader; defaults to the live workflow catalog. */
  loadWorkflows?: () => Promise<WorkflowDef[]>;
  /** Called once with the picked workflow; the caller stages the edge. */
  onAssociate: (name: string, def: WorkflowDef) => void;
  onCancel: () => void;
}

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 640,
  width: "100%",
  maxHeight: "90vh",
  overflow: "auto",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

function workflowNameOf(def: WorkflowDef): string {
  return (def.workflowName || "").trim();
}

/**
 * FR §5.5 dialog associate for Content Type ↔ Allowed Workflows.
 * Lists the workflow catalog, selects one design object, and stages it via
 * {@code onAssociate}; cancel (button or Escape) performs no server call and
 * mutates no edges.
 */
export function WorkflowAssociationDialog({
  open,
  associatedNames,
  loadWorkflows,
  onAssociate,
  onCancel,
}: WorkflowAssociationDialogProps): React.ReactElement | null {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [defs, setDefs] = useState<WorkflowDef[]>([]);
  const [query, setQuery] = useState("");
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDivElement | null>(null);

  useDialogEscape(open, loading, onCancel);
  useDialogFocusTrap(open, dialogRef);

  useEffect(() => {
    if (!open) {
      return;
    }
    let cancelled = false;
    const load = loadWorkflows ?? listWorkflows;
    setLoading(true);
    setError(null);
    setSelectedName(null);
    setQuery("");
    load()
      .then((list) => {
        if (cancelled) return;
        setDefs(Array.isArray(list) ? list : []);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.WF_ERROR));
        setDefs([]);
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, loadWorkflows]);

  const associated = useMemo(() => {
    const names = new Set<string>();
    for (const n of associatedNames) {
      const key = (n || "").trim().toLowerCase();
      if (key) names.add(key);
    }
    return names;
  }, [associatedNames]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return defs
      .filter((d) => workflowNameOf(d).length > 0)
      .filter((d) => {
        if (!q) return true;
        const name = workflowNameOf(d).toLowerCase();
        const desc = (d.workflowDescription || "").toLowerCase();
        return name.includes(q) || desc.includes(q);
      });
  }, [defs, query]);

  const selected = useMemo(
    () => filtered.find((d) => workflowNameOf(d) === selectedName) ?? null,
    [filtered, selectedName],
  );
  const selectedIsDuplicate =
    selected != null && associated.has(workflowNameOf(selected).toLowerCase());

  if (!open) {
    return null;
  }

  function handleAssociate() {
    if (selected == null || selectedIsDuplicate) {
      return;
    }
    onAssociate(workflowNameOf(selected), selected);
  }

  return (
    <div
      ref={dialogRef}
      style={overlayStyle}
      data-testid="developer-wf-assoc-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="developer-wf-assoc-title"
      aria-describedby="developer-wf-assoc-hint"
    >
      <div style={panelStyle}>
        <h2 id="developer-wf-assoc-title" style={{ marginTop: 0 }}>
          {DEV_MSG.CT_WF_ASSOC_TITLE}
        </h2>
        <p
          id="developer-wf-assoc-hint"
          style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
        >
          {DEV_MSG.CT_WF_ASSOC_HINT}
        </p>

        {error ? (
          <div
            role="alert"
            data-testid="developer-wf-assoc-error"
            style={{ color: catalogColors.error, marginBottom: 8 }}
          >
            {error}
          </div>
        ) : null}

        <div
          style={{
            display: "flex",
            gap: 8,
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <label htmlFor="wf-assoc-filter" style={{ fontSize: "0.9rem" }}>
            {DEV_MSG.CATALOG_SEARCH_LABEL}
          </label>
          <input
            id="wf-assoc-filter"
            data-testid="developer-wf-assoc-search"
            style={{
              flex: 1,
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              padding: "6px 8px",
              font: "inherit",
            }}
            placeholder={DEV_MSG.CATALOG_SEARCH_PLACEHOLDER}
            value={query}
            disabled={loading}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>

        {loading ? (
          <p style={mutedCell} data-testid="developer-wf-assoc-loading">
            {DEV_MSG.WF_LOADING}
          </p>
        ) : filtered.length === 0 ? (
          <p style={mutedCell} data-testid="developer-wf-assoc-empty">
            {query.trim() ? DEV_MSG.WF_SEARCH_EMPTY : DEV_MSG.WF_EMPTY}
          </p>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={tableHeaderRow}>
                <th style={{ textAlign: "left", padding: "6px 8px" }}>
                  <span className="visually-hidden">{DEV_MSG.CT_WF_ASSOC_TITLE}</span>
                </th>
                <th style={{ textAlign: "left", padding: "6px 8px" }}>
                  {DEV_MSG.WF_COL_NAME}
                </th>
                <th style={{ textAlign: "left", padding: "6px 8px" }}>
                  {DEV_MSG.WF_COL_DESC}
                </th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((d) => {
                const name = workflowNameOf(d);
                const dup = associated.has(name.toLowerCase());
                return (
                  <tr key={name} style={tableRow} data-testid={`developer-wf-assoc-row-${name}`}>
                    <td style={{ padding: "6px 8px" }}>
                      <input
                        type="radio"
                        name="wf-assoc-pick"
                        data-testid={`developer-wf-assoc-pick-${name}`}
                        checked={selectedName === name}
                        onChange={() => setSelectedName(name)}
                        aria-label={name}
                      />
                    </td>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>{name}</td>
                    <td style={{ padding: "6px 8px", ...mutedCell }}>
                      {d.workflowDescription || "—"}
                      {dup ? ` (${DEV_MSG.CT_WF_ASSOC_DUP_SHORT})` : ""}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}

        {selectedIsDuplicate ? (
          <div
            role="status"
            data-testid="developer-wf-assoc-dup"
            style={{ color: catalogColors.error, marginTop: 8 }}
          >
            {DEV_MSG.CT_WF_ASSOC_DUP}
          </div>
        ) : null}

        <div
          style={{
            display: "flex",
            gap: 8,
            justifyContent: "flex-end",
            marginTop: 16,
          }}
        >
          <button
            type="button"
            data-testid="developer-wf-assoc-cancel"
            onClick={onCancel}
            style={{
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              padding: "8px 12px",
              cursor: "pointer",
              font: "inherit",
              background: "transparent",
            }}
          >
            {DEV_MSG.CATALOG_CONFIRM_CANCEL}
          </button>
          <button
            type="button"
            data-testid="developer-wf-assoc-confirm"
            disabled={selected == null || selectedIsDuplicate}
            onClick={handleAssociate}
            style={{
              border: `1px solid ${catalogColors.accent}`,
              borderRadius: 4,
              padding: "8px 12px",
              cursor:
                selected == null || selectedIsDuplicate ? "not-allowed" : "pointer",
              font: "inherit",
              background: catalogColors.accent,
              color: "#fff",
            }}
          >
            {DEV_MSG.CT_WF_ASSOC_CONFIRM}
          </button>
        </div>
      </div>
    </div>
  );
}
