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
import { getLocaleDetail } from "../api/developer/localesApi";
import type { LocaleDetail } from "../api/developer/types";
import { catalogColors, backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function LocaleDetailPanel({
  idOrLang,
  onBack,
}: {
  idOrLang: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<LocaleDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getLocaleDetail(idOrLang)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.LOC_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrLang]);

  return (
    <div data-testid="developer-loc-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-loc-back"
        style={backButton}
      >
        ← {DEV_MSG.LOC_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-loc-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-loc-detail-loading">{DEV_MSG.LOC_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-loc-detail-title">
              {detail.label || detail.languageString || idOrLang}
            </h2>
            <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
              {detail.languageString || ""}
              {detail.id != null ? ` · id ${detail.id}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.LOC_COL_STATUS}</dt>
              <dd style={{ margin: 0 }}>{detail.status || "—"}</dd>
              <dt>{DEV_MSG.LOC_COL_BASE}</dt>
              <dd style={{ margin: 0 }}>
                {detail.baseLocale == null
                  ? "—"
                  : detail.baseLocale
                    ? DEV_MSG.YES
                    : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.LOC_COL_FORMAT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hasFormatProfile
                  ? DEV_MSG.LOC_FORMAT_YES
                  : DEV_MSG.LOC_FORMAT_NO}
              </dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-loc-format">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.LOC_FORMAT}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.LOC_FORMAT_HINT}</p>
            {detail.format ? (
              <dl style={metaGrid} data-testid="developer-loc-format-grid">
                <dt>{DEV_MSG.LOC_FMT_DIR}</dt>
                <dd style={{ margin: 0 }}>{detail.format.textDir || "—"}</dd>
                <dt>{DEV_MSG.LOC_FMT_DATE}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.datePattern || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_TIME}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.timePattern || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_DATETIME}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.dateTimePattern || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_DECIMAL}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.decimalSep || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_GROUPING}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.groupingSep || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_CURRENCY}</dt>
                <dd style={{ margin: 0 }}>
                  {[detail.format.currencyCode, detail.format.currencyPattern]
                    .filter(Boolean)
                    .join(" · ") || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_FIRST_DAY}</dt>
                <dd style={{ margin: 0 }}>
                  {detail.format.firstDayOfWeek != null
                    ? String(detail.format.firstDayOfWeek)
                    : "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_MEASURE}</dt>
                <dd style={{ margin: 0 }}>{detail.format.measurementSystem || "—"}</dd>
                <dt>{DEV_MSG.LOC_FMT_TZ}</dt>
                <dd style={{ margin: 0, ...monoCell }}>
                  {detail.format.defaultTz || "—"}
                </dd>
                <dt>{DEV_MSG.LOC_FMT_NUMBERING}</dt>
                <dd style={{ margin: 0 }}>{detail.format.numberingSystem || "—"}</dd>
                <dt>{DEV_MSG.LOC_FMT_CALENDAR}</dt>
                <dd style={{ margin: 0 }}>{detail.format.calendar || "—"}</dd>
              </dl>
            ) : (
              <p style={{ color: catalogColors.empty }} data-testid="developer-loc-format-empty">
                {DEV_MSG.LOC_FORMAT_EMPTY}
              </p>
            )}
          </section>

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-loc-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.LOC_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {detail.designGaps.map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
