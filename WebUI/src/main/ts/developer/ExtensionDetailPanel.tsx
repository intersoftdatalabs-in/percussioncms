/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useState } from "react";
import { getExtensionDetail } from "../api/developer/extensionsApi";
import type { ExtensionDef } from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function ExtensionDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ExtensionDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getExtensionDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.EX_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const ifaces =
    detail != null && Array.isArray(detail.supportedInterfaces)
      ? detail.supportedInterfaces
      : [];
  const params =
    detail != null && Array.isArray(detail.runtimeParameters)
      ? detail.runtimeParameters
      : [];

  return (
    <div data-testid="developer-ex-detail">
      <button type="button" onClick={onBack} data-testid="developer-ex-back" style={backButton}>
        ← {DEV_MSG.EX_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ex-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-ex-detail-loading">{DEV_MSG.EX_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ex-detail-title">
              {detail.extensionName || idOrName}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.EX_COL_FQN}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.fqn || "—"}</dd>
              <dt>{DEV_MSG.EX_COL_HANDLER}</dt>
              <dd style={{ margin: 0 }}>{detail.handlerName || "—"}</dd>
              <dt>{DEV_MSG.EX_COL_CONTEXT}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.context || "—"}</dd>
              <dt>{DEV_MSG.EX_COL_VERSION}</dt>
              <dd style={{ margin: 0 }}>
                {detail.version != null ? String(detail.version) : "—"}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-ex-ifaces">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_IFACES}</h3>
            {ifaces.length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.EX_NONE}</p>
            ) : (
              <ul style={{ fontFamily: "monospace", fontSize: "0.85rem" }}>
                {ifaces.map((x) => (
                  <li key={x}>{x}</li>
                ))}
              </ul>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-ex-params">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_PARAMS}</h3>
            {params.length === 0 ? (
              <p style={{ color: "#718096" }} data-testid="developer-ex-params-empty">
                {DEV_MSG.EX_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-ex-params-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_PARAM}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_TYPE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {params.map((p, i) => (
                      <tr
                        key={`${p.name ?? "p"}-${i}`}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.dataType || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-ex-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_GAPS}</h3>
            <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
              <li>{DEV_MSG.EX_GAP_INSTALL}</li>
              <li>{DEV_MSG.EX_GAP_EDIT}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
