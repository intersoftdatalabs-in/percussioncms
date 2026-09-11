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

import React, { useEffect, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { isApiError } from "../api/client";
import {
  createLocale,
  deleteLocale,
  getLocaleDetail,
  isLocaleWriteReady,
  normalizeLanguageString,
  updateLocale,
  type LocaleWriteBody,
} from "../api/developer/localesApi";
import type { LocaleDetail, LocaleFormatSummary } from "../api/developer/types";
import { catalogColors, backButton, errorAlert } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

function emptyFormat(): LocaleFormatSummary {
  return {
    textDir: "",
    datePattern: "",
    timePattern: "",
    dateTimePattern: "",
    decimalSep: "",
    groupingSep: "",
    currencyCode: "",
    currencyPattern: "",
    firstDayOfWeek: undefined,
    measurementSystem: "",
    defaultTz: "",
    numberingSystem: "",
    calendar: "",
  };
}

function formatFromDetail(d: LocaleDetail | null | undefined): LocaleFormatSummary {
  const src = d?.format;
  if (!src) {
    return emptyFormat();
  }
  return {
    textDir: src.textDir || "",
    datePattern: src.datePattern || "",
    timePattern: src.timePattern || "",
    dateTimePattern: src.dateTimePattern || "",
    decimalSep: src.decimalSep || "",
    groupingSep: src.groupingSep || "",
    currencyCode: src.currencyCode || "",
    currencyPattern: src.currencyPattern || "",
    firstDayOfWeek: src.firstDayOfWeek,
    measurementSystem: src.measurementSystem || "",
    defaultTz: src.defaultTz || "",
    numberingSystem: src.numberingSystem || "",
    calendar: src.calendar || "",
  };
}

/** JAXB/Jackson may unwrap a one-element designGaps list to a string. */
function formatGapList(detail: LocaleDetail | null | undefined): string[] {
  const raw = detail?.designGaps as unknown;
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.filter((g): g is string => typeof g === "string" && g.length > 0);
  }
  if (typeof raw === "string" && raw.trim()) {
    return [raw];
  }
  return [];
}

function compactFormat(fmt: LocaleFormatSummary): LocaleFormatSummary {
  const out: LocaleFormatSummary = {};
  if (fmt.textDir) out.textDir = fmt.textDir;
  if (fmt.datePattern) out.datePattern = fmt.datePattern;
  if (fmt.timePattern) out.timePattern = fmt.timePattern;
  if (fmt.dateTimePattern) out.dateTimePattern = fmt.dateTimePattern;
  if (fmt.decimalSep) out.decimalSep = fmt.decimalSep;
  if (fmt.groupingSep) out.groupingSep = fmt.groupingSep;
  if (fmt.currencyCode) out.currencyCode = fmt.currencyCode;
  if (fmt.currencyPattern) out.currencyPattern = fmt.currencyPattern;
  if (fmt.firstDayOfWeek != null && !Number.isNaN(fmt.firstDayOfWeek)) {
    out.firstDayOfWeek = fmt.firstDayOfWeek;
  }
  if (fmt.measurementSystem) out.measurementSystem = fmt.measurementSystem;
  if (fmt.defaultTz) out.defaultTz = fmt.defaultTz;
  if (fmt.numberingSystem) out.numberingSystem = fmt.numberingSystem;
  if (fmt.calendar) out.calendar = fmt.calendar;
  return out;
}

export function LocaleDetailPanel({
  idOrLang,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrLang: string | null;
  onBack: () => void;
  onSaved?: (detail: LocaleDetail) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrLang == null && createdKey == null;
  const [detail, setDetail] = useState<LocaleDetail | null>(null);
  const [language, setLanguage] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState("active");
  const [baseLocale, setBaseLocale] = useState(false);
  const [storeFormat, setStoreFormat] = useState(false);
  const [fmt, setFmt] = useState<LocaleFormatSummary>(emptyFormat);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrLang != null);
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrLang == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setLoading(true);
    getLocaleDetail(idOrLang)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setLanguage(d.languageString || "");
        setLabel(d.label || "");
        setDescription(d.description || "");
        setStatus(d.status || "active");
        setBaseLocale(Boolean(d.baseLocale));
        setStoreFormat(Boolean(d.hasFormatProfile) || Boolean(d.format));
        setFmt(formatFromDetail(d));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.LOC_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrLang]);

  const canSave = !busy && isLocaleWriteReady({ isNew, language, label });
  const writeKey =
    idOrLang || createdKey || normalizeLanguageString(language);

  function writeBody(): LocaleWriteBody {
    const body: LocaleWriteBody = {
      label: label.trim(),
      description,
      status,
      baseLocale,
    };
    if (isNew) {
      body.languageString = normalizeLanguageString(language);
    } else if (detail?.languageString) {
      body.languageString = detail.languageString;
    }
    const hadFormat = Boolean(detail?.hasFormatProfile) || Boolean(detail?.format);
    if (storeFormat) {
      body.hasFormatProfile = true;
      body.format = compactFormat(fmt);
    } else if (hadFormat) {
      body.hasFormatProfile = false;
    }
    return body;
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved =
        isNew || !writeKey
          ? await createLocale(writeBody())
          : await updateLocale(writeKey, writeBody());
      setDetail(saved);
      if (isNew) {
        setCreatedKey(
          saved.languageString ||
            (saved.id != null ? String(saved.id) : normalizeLanguageString(language)),
        );
      }
      setLanguage(saved.languageString || language);
      setLabel(saved.label || label);
      setDescription(saved.description || "");
      setStatus(saved.status || status);
      setBaseLocale(Boolean(saved.baseLocale));
      setStoreFormat(Boolean(saved.hasFormatProfile) || Boolean(saved.format));
      setFmt(formatFromDetail(saved));
      setNotice(DEV_MSG.LOC_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 409 && isNew
          ? DEV_MSG.LOC_DUPLICATE
          : DEV_MSG.LOC_SAVE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteLocale(writeKey);
      setNotice(DEV_MSG.LOC_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.LOC_DELETE_ERROR));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.LOC_NEW
    : detail?.label || detail?.languageString || idOrLang || DEV_MSG.LOC_EDIT;

  return (
    <div data-testid="developer-loc-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-loc-back"
        aria-label={DEV_MSG.LOC_BACK}
        style={backButton}
      >
        ← {DEV_MSG.LOC_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-loc-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-loc-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-loc-detail-loading">{DEV_MSG.LOC_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-loc-detail-title">
              {title}
            </h2>
            {!isNew && detail ? (
              <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
                {detail.languageString || ""}
                {detail.id != null ? ` · id ${detail.id}` : ""}
              </div>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="loc-language">{DEV_MSG.LOC_FORM_LANG}</label>
            <input
              id="loc-language"
              data-testid="developer-loc-language"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={language}
              disabled={!isNew || busy}
              onChange={(e) => setLanguage(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.LOC_LANG_READONLY}
              </span>
            ) : null}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="loc-label">{DEV_MSG.LOC_FORM_LABEL}</label>
            <input
              id="loc-label"
              data-testid="developer-loc-label"
              style={inputStyle}
              value={label}
              disabled={busy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="loc-desc">{DEV_MSG.LOC_FORM_DESCRIPTION}</label>
            <input
              id="loc-desc"
              data-testid="developer-loc-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="loc-status">{DEV_MSG.LOC_FORM_STATUS}</label>
            <select
              id="loc-status"
              data-testid="developer-loc-status"
              style={inputStyle}
              value={status}
              disabled={busy}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="active">{DEV_MSG.LOC_STATUS_ACTIVE}</option>
              <option value="inactive">{DEV_MSG.LOC_STATUS_INACTIVE}</option>
            </select>
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              marginBottom: "12px",
            }}
          >
            <input
              id="loc-base"
              type="checkbox"
              data-testid="developer-loc-base"
              checked={baseLocale}
              disabled={busy}
              onChange={(e) => setBaseLocale(e.target.checked)}
            />
            <label htmlFor="loc-base">{DEV_MSG.LOC_FORM_BASE}</label>
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-loc-save"
              aria-label={DEV_MSG.LOC_SAVE}
              disabled={!canSave}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: canSave ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSave ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.LOC_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-loc-cancel"
              disabled={busy}
              onClick={onBack}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.LOC_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-loc-delete"
                aria-label={DEV_MSG.LOC_DELETE}
                disabled={busy}
                onClick={requestDelete}
                style={{
                  padding: "8px 16px",
                  background: "#c53030",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy ? "wait" : "pointer",
                  marginLeft: "auto",
                }}
              >
                {DEV_MSG.LOC_DELETE}
              </button>
            ) : null}
          </div>

          <section style={{ marginBottom: "16px" }} data-testid="developer-loc-format">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.LOC_FORMAT}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.LOC_FORMAT_HINT}
            </p>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
                marginBottom: "12px",
              }}
            >
              <input
                id="loc-format-enable"
                type="checkbox"
                data-testid="developer-loc-format-enable"
                checked={storeFormat}
                disabled={busy}
                onChange={(e) => setStoreFormat(e.target.checked)}
              />
              <label htmlFor="loc-format-enable">{DEV_MSG.LOC_FORMAT_ENABLE}</label>
            </div>
            {storeFormat ? (
              <div data-testid="developer-loc-format-grid">
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-dir">{DEV_MSG.LOC_FMT_DIR}</label>
                  <select
                    id="loc-fmt-dir"
                    data-testid="developer-loc-fmt-dir"
                    style={inputStyle}
                    value={fmt.textDir || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, textDir: e.target.value })}
                  >
                    <option value=""></option>
                    <option value="ltr">ltr</option>
                    <option value="rtl">rtl</option>
                  </select>
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-date">{DEV_MSG.LOC_FMT_DATE}</label>
                  <input
                    id="loc-fmt-date"
                    data-testid="developer-loc-fmt-date"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.datePattern || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, datePattern: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-time">{DEV_MSG.LOC_FMT_TIME}</label>
                  <input
                    id="loc-fmt-time"
                    data-testid="developer-loc-fmt-time"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.timePattern || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, timePattern: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-datetime">{DEV_MSG.LOC_FMT_DATETIME}</label>
                  <input
                    id="loc-fmt-datetime"
                    data-testid="developer-loc-fmt-datetime"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.dateTimePattern || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, dateTimePattern: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-decimal">{DEV_MSG.LOC_FMT_DECIMAL}</label>
                  <input
                    id="loc-fmt-decimal"
                    data-testid="developer-loc-fmt-decimal"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.decimalSep || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, decimalSep: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-grouping">{DEV_MSG.LOC_FMT_GROUPING}</label>
                  <input
                    id="loc-fmt-grouping"
                    data-testid="developer-loc-fmt-grouping"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.groupingSep || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, groupingSep: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-currency-code">{DEV_MSG.LOC_FMT_CURRENCY_CODE}</label>
                  <input
                    id="loc-fmt-currency-code"
                    data-testid="developer-loc-fmt-currency-code"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.currencyCode || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, currencyCode: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-currency-pattern">
                    {DEV_MSG.LOC_FMT_CURRENCY_PATTERN}
                  </label>
                  <input
                    id="loc-fmt-currency-pattern"
                    data-testid="developer-loc-fmt-currency-pattern"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.currencyPattern || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, currencyPattern: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-first-day">{DEV_MSG.LOC_FMT_FIRST_DAY}</label>
                  <input
                    id="loc-fmt-first-day"
                    data-testid="developer-loc-fmt-first-day"
                    type="number"
                    min={1}
                    max={7}
                    style={inputStyle}
                    value={fmt.firstDayOfWeek != null ? String(fmt.firstDayOfWeek) : ""}
                    disabled={busy}
                    onChange={(e) => {
                      const raw = e.target.value;
                      setFmt({
                        ...fmt,
                        firstDayOfWeek: raw === "" ? undefined : Number(raw),
                      });
                    }}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-measure">{DEV_MSG.LOC_FMT_MEASURE}</label>
                  <select
                    id="loc-fmt-measure"
                    data-testid="developer-loc-fmt-measure"
                    style={inputStyle}
                    value={fmt.measurementSystem || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, measurementSystem: e.target.value })}
                  >
                    <option value=""></option>
                    <option value="us">us</option>
                    <option value="uk">uk</option>
                    <option value="metric">metric</option>
                  </select>
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-tz">{DEV_MSG.LOC_FMT_TZ}</label>
                  <input
                    id="loc-fmt-tz"
                    data-testid="developer-loc-fmt-tz"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={fmt.defaultTz || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, defaultTz: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-numbering">{DEV_MSG.LOC_FMT_NUMBERING}</label>
                  <input
                    id="loc-fmt-numbering"
                    data-testid="developer-loc-fmt-numbering"
                    style={inputStyle}
                    value={fmt.numberingSystem || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, numberingSystem: e.target.value })}
                  />
                </div>
                <div style={fieldStyle}>
                  <label htmlFor="loc-fmt-calendar">{DEV_MSG.LOC_FMT_CALENDAR}</label>
                  <input
                    id="loc-fmt-calendar"
                    data-testid="developer-loc-fmt-calendar"
                    style={inputStyle}
                    value={fmt.calendar || ""}
                    disabled={busy}
                    onChange={(e) => setFmt({ ...fmt, calendar: e.target.value })}
                  />
                </div>
              </div>
            ) : (
              <p
                style={{ color: catalogColors.empty }}
                data-testid="developer-loc-format-empty"
              >
                {isNew ? DEV_MSG.LOC_FORMAT_EMPTY : DEV_MSG.LOC_FORMAT_CLEAR_HINT}
              </p>
            )}
          </section>

          {formatGapList(detail).length > 0 ? (
            <section data-testid="developer-loc-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.LOC_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {formatGapList(detail).map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.LOC_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
