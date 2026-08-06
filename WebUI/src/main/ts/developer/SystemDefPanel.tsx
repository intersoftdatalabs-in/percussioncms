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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState } from "react";
import { getSystemDef } from "../api/developer/systemDefApi";
import type { SystemDefDetail } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/**
 * P0.8 — content-editor system definition field catalog (CD-16 read).
 */
export function SystemDefPanel(): React.ReactElement {
  const [detail, setDetail] = useState<SystemDefDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getSystemDef()
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SYS_ERROR));
        setDetail(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error)
    return (
      <CatalogStatus testId="developer-sys-error" error>
        {error}
      </CatalogStatus>
    );
  if (detail == null)
    return (
      <CatalogStatus testId="developer-sys-loading">{DEV_MSG.SYS_LOADING}</CatalogStatus>
    );

  const fields = detail.fields || [];

  return (
    <div data-testid="developer-sys-panel">
      <CatalogHint>{DEV_MSG.SYS_HINT}</CatalogHint>

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 8px" }} data-testid="developer-sys-title">
          {DEV_MSG.SYS_TITLE}
        </h2>
        <dl style={metaGrid}>
          <dt>{DEV_MSG.SYS_META_FIELD_COUNT}</dt>
          <dd style={{ margin: 0 }}>
            {detail.fieldCount != null ? String(detail.fieldCount) : String(fields.length)}
          </dd>
          <dt>{DEV_MSG.SYS_META_CACHE}</dt>
          <dd style={{ margin: 0, ...monoCell }}>
            {detail.cacheTimeoutMinutes != null
              ? `${detail.cacheTimeoutMinutes} ${DEV_MSG.SYS_META_CACHE_UNIT}`
              : "—"}
          </dd>
        </dl>
      </header>

      <section data-testid="developer-sys-fields">
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SYS_FIELDS}</h3>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SYS_FIELDS_HINT}</p>
        {fields.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-sys-empty">
            {DEV_MSG.SYS_EMPTY}
          </p>
        ) : (
          <SimpleCatalogTable
            tableTestId="developer-sys-fields-table"
            rowTestId="developer-sys-field-row"
            columns={[
              DEV_MSG.SYS_COL_FIELD,
              DEV_MSG.SYS_COL_DATATYPE,
              DEV_MSG.SYS_COL_OCCURRENCE,
              DEV_MSG.SYS_COL_REQUIRED,
              DEV_MSG.SYS_COL_SEARCH,
              DEV_MSG.SYS_COL_READONLY,
            ]}
            rows={fields.map((f, i) => ({
              key: f.name || `f-${i}`,
              cells: [
                <span key="n" style={monoCell}>
                  {f.name || "—"}
                </span>,
                f.dataType || "—",
                f.occurrence || "—",
                f.required == null ? "—" : f.required ? DEV_MSG.YES : DEV_MSG.NO,
                f.searchable == null
                  ? "—"
                  : f.searchable
                    ? DEV_MSG.YES
                    : DEV_MSG.NO,
                f.readOnly == null ? "—" : f.readOnly ? DEV_MSG.YES : DEV_MSG.NO,
              ],
            }))}
          />
        )}
      </section>

      {detail.designGaps && detail.designGaps.length > 0 ? (
        <section style={{ marginTop: "16px" }} data-testid="developer-sys-gaps">
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SYS_GAPS}</h3>
          <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
            {detail.designGaps.map((g) => (
              <li key={g}>{g}</li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}
