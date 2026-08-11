/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useState } from "react";
import { getWorkflowDetail } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function WorkflowDetailPanel({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<WorkflowDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getWorkflowDetail(name)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.WF_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const steps =
    detail != null && Array.isArray(detail.workflowSteps) ? detail.workflowSteps : [];

  return (
    <div data-testid="developer-wf-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-wf-back"
        aria-label="Back to workflows list"
        style={backButton}
      >
        ← {DEV_MSG.WF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-wf-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-wf-detail-loading">{DEV_MSG.WF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-wf-detail-title">
              {detail.workflowName || name}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.WF_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.workflowName || "—"}</dd>
              <dt>{DEV_MSG.WF_COL_DESC}</dt>
              <dd style={{ margin: 0 }}>{detail.workflowDescription || "—"}</dd>
              <dt>{DEV_MSG.WF_COL_DEFAULT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.defaultWorkflow ? DEV_MSG.WF_YES : DEV_MSG.WF_NO}
              </dd>
              <dt>{DEV_MSG.WF_COL_STAGING}</dt>
              <dd style={{ margin: 0 }}>{detail.stagingRoleNames || "—"}</dd>
            </dl>
          </header>

          <section data-testid="developer-wf-steps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_STEPS}</h3>
            {steps.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.WF_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-wf-steps-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_STEP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_PERMS}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_ROLES}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {steps.map((s, i) => {
                      const roles = Array.isArray(s.stepRoles)
                        ? s.stepRoles
                            .map((r) => r.roleName)
                            .filter(Boolean)
                            .join(", ")
                        : "";
                      const perms = Array.isArray(s.permissionNames)
                        ? s.permissionNames.join(", ")
                        : "";
                      return (
                        <tr
                          key={`${s.stepName ?? "s"}-${i}`}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {s.stepName || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>{perms || "—"}</td>
                          <td style={{ padding: "8px" }}>{roles || "—"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-wf-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {(detail.designGaps && detail.designGaps.length
                ? detail.designGaps
                : [DEV_MSG.WF_GAP_GRAPH, DEV_MSG.WF_GAP_WRITE, DEV_MSG.WF_GAP_CT]
              ).map((g, i) => (
                <li key={`${g}-${i}`}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
