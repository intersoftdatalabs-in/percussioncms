/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useState } from "react";
import { getSearchDetail } from "../api/developer/searchesApi";
import type { SearchDef } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";

export function SearchDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<SearchDef | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getSearchDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.SR_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const fields =
    detail != null && Array.isArray(detail.fields) ? detail.fields : [];

  return (
    <div data-testid="developer-sr-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-sr-back"
        aria-label={DEV_MSG.SR_BACK}
        style={backButton}
      >
        ← {DEV_MSG.SR_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-sr-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-sr-detail-loading">{DEV_MSG.SR_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-sr-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.SR_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.SR_COL_DF}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.displayFormatId || "—"}</dd>
              <dt>{DEV_MSG.SR_COL_MAX}</dt>
              <dd style={{ margin: 0 }}>
                {detail.maximumResultSize != null ? String(detail.maximumResultSize) : "—"}
              </dd>
              <dt>{DEV_MSG.SR_COL_CASE}</dt>
              <dd style={{ margin: 0 }}>
                {detail.caseSensitive ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-sr-fields">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SR_FIELDS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SR_FIELDS_HINT}</p>
            {fields.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-sr-fields-empty">
                {DEV_MSG.SR_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-sr-fields-table"
                  style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_FIELD}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_OP}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_VALUE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_FTYPE}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fields.map((f, i) => (
                      <tr
                        key={`${f.fieldName ?? "f"}-${i}`}
                        data-testid={`developer-sr-field-row-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.fieldName || f.displayName || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{f.operator || "—"}</td>
                        <td style={{ padding: "8px" }}>{f.fieldValue || "—"}</td>
                        <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <ObjectAclSection
            objectGuid={detail.guid?.stringValue}
            objectKind="search"
            testIdPrefix="developer-sr-acl"
          />

          <section style={{ marginTop: "16px" }} data-testid="developer-sr-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SR_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {(detail.designGaps && detail.designGaps.length
                ? detail.designGaps
                : [DEV_MSG.SR_GAP_WRITE, DEV_MSG.SR_GAP_FIELDS, DEV_MSG.SR_GAP_VIEWS]
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
