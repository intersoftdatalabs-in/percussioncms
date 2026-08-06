/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useState } from "react";
import { getRelationshipTypeDetail } from "../api/developer/relationshipTypesApi";
import type { RelationshipTypeDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function RelationshipTypeDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<RelationshipTypeDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getRelationshipTypeDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.RT_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const effects = detail != null && Array.isArray(detail.effects) ? detail.effects : [];
  const sysProps =
    detail != null && Array.isArray(detail.systemProperties) ? detail.systemProperties : [];
  const userProps =
    detail != null && Array.isArray(detail.userProperties) ? detail.userProperties : [];

  return (
    <div data-testid="developer-rt-detail">
      <button type="button" onClick={onBack} data-testid="developer-rt-back" style={backButton}>
        ← {DEV_MSG.RT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-rt-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-rt-detail-loading">{DEV_MSG.RT_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-rt-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.RT_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.RT_COL_CATEGORY}</dt>
              <dd style={{ margin: 0 }}>{detail.categoryLabel || detail.category || "—"}</dd>
              <dt>{DEV_MSG.RT_COL_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.type || "—"}</dd>
              <dt>{DEV_MSG.RT_COL_DESC}</dt>
              <dd style={{ margin: 0 }}>{detail.description || "—"}</dd>
            </dl>
          </header>

          <section data-testid="developer-rt-effects">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_EFFECTS}</h3>
            {effects.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-rt-effects-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_EFFECT}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_ENDPOINT}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_EXTREF}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {effects.map((e, i) => (
                      <tr
                        key={`${e.name ?? "e"}-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {e.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{e.activationEndPoint || "—"}</td>
                        <td style={{ padding: "8px", ...monoCell, fontSize: "0.85rem" }}>
                          {e.extensionRef || "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-rt-sysprops">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_SYS_PROPS}</h3>
            {sysProps.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-rt-sysprops-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_PROP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_VALUE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sysProps.map((p, i) => (
                      <tr
                        key={`${p.name ?? "p"}-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.value ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-rt-userprops">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_USER_PROPS}</h3>
            {userProps.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-rt-userprops-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_PROP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_VALUE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {userProps.map((p, i) => (
                      <tr
                        key={`${p.name ?? "up"}-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.value ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-rt-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {(detail.designGaps && detail.designGaps.length
                ? detail.designGaps
                : [DEV_MSG.RT_GAP_WRITE, DEV_MSG.RT_GAP_CLONE, DEV_MSG.RT_GAP_EFFECTS]
              ).map((g) => (
                <li key={g}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
