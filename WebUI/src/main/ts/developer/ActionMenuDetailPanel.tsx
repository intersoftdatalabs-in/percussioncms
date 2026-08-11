/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useState } from "react";
import { getActionMenuDetail } from "../api/developer/actionMenusApi";
import type { ActionMenu } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";

export function ActionMenuDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ActionMenu | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getActionMenuDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.AM_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const params =
    detail != null && Array.isArray(detail.parameters) ? detail.parameters : [];
  const props =
    detail != null && Array.isArray(detail.properties) ? detail.properties : [];

  return (
    <div data-testid="developer-am-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-am-back"
        aria-label={DEV_MSG.AM_BACK}
        style={backButton}
      >
        ← {DEV_MSG.AM_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-am-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-am-detail-loading">{DEV_MSG.AM_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-am-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.AM_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.AM_COL_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.menuType || "—"}</dd>
              <dt>{DEV_MSG.AM_COL_HANDLER}</dt>
              <dd style={{ margin: 0 }}>{detail.handler || "—"}</dd>
              <dt>{DEV_MSG.AM_COL_URL}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.url || "—"}</dd>
              <dt>{DEV_MSG.AM_COL_SORT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.sortRank != null ? String(detail.sortRank) : "—"}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-am-params">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_PARAMS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.AM_PARAMS_HINT}</p>
            {params.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-am-params-empty">
                {DEV_MSG.AM_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-am-params-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_PARAM}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_VALUE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {params.map((p, i) => (
                      <tr
                        key={`${p.name ?? "p"}-${i}`}
                        data-testid={`developer-am-param-row-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.value || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-am-props">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_PROPS}</h3>
            {props.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-am-props-empty">
                {DEV_MSG.AM_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-am-props-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_PROP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_VALUE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {props.map((p, i) => (
                      <tr
                        key={`${p.name ?? "prop"}-${i}`}
                        data-testid={`developer-am-prop-row-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {p.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{p.value || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <ObjectAclSection
            objectGuid={detail.guid?.stringValue}
            objectKind="action-menu"
            testIdPrefix="developer-am-acl"
          />

          <section style={{ marginTop: "16px" }} data-testid="developer-am-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              <li>{DEV_MSG.AM_GAP_WRITE}</li>
              <li>{DEV_MSG.AM_GAP_CHILDREN}</li>
              <li>{DEV_MSG.AM_GAP_VISIBILITY}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
