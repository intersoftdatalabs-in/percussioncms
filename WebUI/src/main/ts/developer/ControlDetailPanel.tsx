/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useState } from "react";
import { getControlDetail } from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function ControlDetailPanel({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ControlDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getControlDetail(name)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.CTL_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const params =
    detail != null && Array.isArray(detail.parameters) ? detail.parameters : [];
  const gaps =
    detail != null && detail.designGaps && detail.designGaps.length
      ? detail.designGaps
      : [DEV_MSG.CTL_GAP_USER, DEV_MSG.CTL_GAP_XSL, DEV_MSG.CTL_GAP_SYS];

  return (
    <div data-testid="developer-ctl-detail">
      <button type="button" onClick={onBack} data-testid="developer-ctl-back" style={backButton}>
        ← {DEV_MSG.CTL_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ctl-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-ctl-detail-loading">{DEV_MSG.CTL_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ctl-detail-title">
              {detail.displayName || detail.name || name}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.CTL_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.CTL_COL_SCOPE}</dt>
              <dd style={{ margin: 0 }}>{detail.scope || "—"}</dd>
              <dt>{DEV_MSG.CTL_COL_DIM}</dt>
              <dd style={{ margin: 0 }}>{detail.dimension || "—"}</dd>
              <dt>{DEV_MSG.CTL_COL_CHOICES}</dt>
              <dd style={{ margin: 0 }}>{detail.choiceSet || "—"}</dd>
              <dt>{DEV_MSG.CTL_COL_DESC}</dt>
              <dd style={{ margin: 0 }}>{detail.description || "—"}</dd>
            </dl>
          </header>

          <section data-testid="developer-ctl-params">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CTL_PARAMS}</h3>
            {params.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.CTL_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-ctl-params-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_PARAM}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_TYPE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_REQ}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_DEFAULT}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {params.map((p, i) => (
                      <tr
                        key={`${p.name ?? "p"}-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.dataType || p.paramType || "—"}</td>
                        <td style={{ padding: "8px" }}>
                          {p.required ? DEV_MSG.CTL_YES : DEV_MSG.CTL_NO}
                        </td>
                        <td style={{ padding: "8px" }}>{p.defaultValue || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-ctl-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CTL_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {gaps.map((g, i) => (
                <li key={`${g}-${i}`}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
