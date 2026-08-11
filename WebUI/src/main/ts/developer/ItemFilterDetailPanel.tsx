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

import React, { useEffect, useState } from "react";
import { getItemFilterDetail } from "../api/developer/itemFiltersApi";
import type { ItemFilter } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const AUTHTYPE_LABEL: Record<number, string> = {
  0: "All Content",
  1: "All Public Content",
  2: "Custom",
  101: "Site Folder",
};

export function ItemFilterDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ItemFilter | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getItemFilterDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.IF_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  // rules may arrive as array from JSON; narrow without non-null assertions
  const ruleList = detail != null && Array.isArray(detail.rules) ? detail.rules : [];

  return (
    <div data-testid="developer-if-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-if-back"
        aria-label="Back to item filters list"
        style={backButton}
      >
        ← {DEV_MSG.IF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-if-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-if-detail-loading">{DEV_MSG.IF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-if-detail-title">
              {detail.name || idOrName}
            </h2>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.IF_COL_GUID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.filterId?.stringValue || "—"}
              </dd>
              <dt>{DEV_MSG.IF_COL_AUTHTYPE}</dt>
              <dd style={{ margin: 0 }}>
                {detail.legacyAuthtype != null
                  ? AUTHTYPE_LABEL[detail.legacyAuthtype] ||
                    String(detail.legacyAuthtype)
                  : "—"}
              </dd>
              <dt>{DEV_MSG.IF_COL_PARENT}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.parentFilter?.name || "—"}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-if-rules">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.IF_RULES}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.IF_RULES_HINT}</p>
            {ruleList.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-if-rules-empty">
                {DEV_MSG.IF_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-if-rules-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_RULE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_PARAMS}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ruleList.map((r, i) => {
                      const params = (r.params || [])
                        .map((p) => `${p.name ?? ""}=${p.value ?? ""}`)
                        .join("; ");
                      return (
                        <tr
                          key={r.ruleId?.stringValue || `${r.name ?? "rule"}-${i}`}
                          data-testid={`developer-if-rule-row-${i}`}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {r.name || "—"}
                          </td>
                          <td style={{ padding: "8px", color: catalogColors.muted }}>{params}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-if-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.IF_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              <li>{DEV_MSG.IF_GAP_WRITE}</li>
              <li>{DEV_MSG.IF_GAP_RULE_EDIT}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
