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
import {
  localeDescriptionWrite,
  savedLocaleDescription,
  storedLocaleDescription,
} from "./localeDescription";
import { LOC_DESC_MSG } from "./localeDescriptionMessages";
import {
  localeDatePatternWrite,
  savedLocaleDatePattern,
  storedLocaleDatePattern,
} from "./localeDatePattern";
import { LOC_DATE_MSG } from "./localeDatePatternMessages";
import {
  localeTimePatternWrite,
  savedLocaleTimePattern,
  storedLocaleTimePattern,
} from "./localeTimePattern";
import { LOC_TIME_MSG } from "./localeTimePatternMessages";
import {
  localeDateTimePatternWrite,
  savedLocaleDateTimePattern,
  storedLocaleDateTimePattern,
} from "./localeDateTimePattern";
import { LOC_DATETIME_MSG } from "./localeDateTimePatternMessages";
import { localeLabelWrite, savedLocaleLabel, storedLocaleLabel } from "./localeLabel";
import { LOC_LABEL_MSG } from "./localeLabelMessages";
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
  const [labelShown, setLabelShown] = useState("");
  const [labelDraft, setLabelDraft] = useState("");
  const [labelEditing, setLabelEditing] = useState(false);
  const [labelError, setLabelError] = useState<string | null>(null);
  const [labelNotice, setLabelNotice] = useState<string | null>(null);
  const [labelBusy, setLabelBusy] = useState(false);
  const [description, setDescription] = useState("");
  const [descriptionShown, setDescriptionShown] = useState("");
  const [descriptionDraft, setDescriptionDraft] = useState("");
  const [descriptionEditing, setDescriptionEditing] = useState(false);
  const [descriptionError, setDescriptionError] = useState<string | null>(null);
  const [descriptionNotice, setDescriptionNotice] = useState<string | null>(null);
  const [descriptionBusy, setDescriptionBusy] = useState(false);
  const [datePatternShown, setDatePatternShown] = useState("");
  const [datePatternDraft, setDatePatternDraft] = useState("");
  const [datePatternEditing, setDatePatternEditing] = useState(false);
  const [datePatternError, setDatePatternError] = useState<string | null>(null);
  const [datePatternNotice, setDatePatternNotice] = useState<string | null>(null);
  const [datePatternBusy, setDatePatternBusy] = useState(false);
  const [timePatternShown, setTimePatternShown] = useState("");
  const [timePatternDraft, setTimePatternDraft] = useState("");
  const [timePatternEditing, setTimePatternEditing] = useState(false);
  const [timePatternError, setTimePatternError] = useState<string | null>(null);
  const [timePatternNotice, setTimePatternNotice] = useState<string | null>(null);
  const [timePatternBusy, setTimePatternBusy] = useState(false);
  const [dateTimePatternShown, setDateTimePatternShown] = useState("");
  const [dateTimePatternDraft, setDateTimePatternDraft] = useState("");
  const [dateTimePatternEditing, setDateTimePatternEditing] = useState(false);
  const [dateTimePatternError, setDateTimePatternError] = useState<string | null>(null);
  const [dateTimePatternNotice, setDateTimePatternNotice] = useState<string | null>(null);
  const [dateTimePatternBusy, setDateTimePatternBusy] = useState(false);
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
  const descriptionInflight = useRef(false);
  const labelInflight = useRef(false);
  const datePatternInflight = useRef(false);
  const timePatternInflight = useRef(false);
  const dateTimePatternInflight = useRef(false);

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
        setLabelShown(storedLocaleLabel(d.label));
        setLabelDraft("");
        setLabelEditing(false);
        setLabelError(null);
        setLabelNotice(null);
        setDescription(d.description || "");
        setDescriptionShown(storedLocaleDescription(d.description));
        setDescriptionDraft("");
        setDescriptionEditing(false);
        setDescriptionError(null);
        setDescriptionNotice(null);
        setDatePatternShown(storedLocaleDatePattern(d.format?.datePattern));
        setDatePatternDraft("");
        setDatePatternEditing(false);
        setDatePatternError(null);
        setDatePatternNotice(null);
        setTimePatternShown(storedLocaleTimePattern(d.format?.timePattern));
        setTimePatternDraft("");
        setTimePatternEditing(false);
        setTimePatternError(null);
        setTimePatternNotice(null);
        setDateTimePatternShown(storedLocaleDateTimePattern(d.format?.dateTimePattern));
        setDateTimePatternDraft("");
        setDateTimePatternEditing(false);
        setDateTimePatternError(null);
        setDateTimePatternNotice(null);
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

  const canSave =
    !busy &&
    !descriptionBusy &&
    !descriptionEditing &&
    !labelBusy &&
    !labelEditing &&
    !datePatternBusy &&
    !datePatternEditing &&
    !timePatternBusy &&
    !timePatternEditing &&
    !dateTimePatternBusy &&
    !dateTimePatternEditing &&
    isLocaleWriteReady({ isNew, language, label });
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

  function descriptionSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return LOC_DESC_MSG.INVALID;
    if (isApiError(err) && err.status === 403) return LOC_DESC_MSG.FORBIDDEN;
    if (isApiError(err) && err.status === 409) return LOC_DESC_MSG.CONFLICT;
    return LOC_DESC_MSG.ERROR;
  }

  function labelSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return LOC_LABEL_MSG.INVALID;
    if (isApiError(err) && err.status === 403) return LOC_LABEL_MSG.FORBIDDEN;
    if (isApiError(err) && err.status === 409) return LOC_LABEL_MSG.CONFLICT;
    return LOC_LABEL_MSG.ERROR;
  }

  function datePatternSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return LOC_DATE_MSG.INVALID;
    if (isApiError(err) && err.status === 403) return LOC_DATE_MSG.FORBIDDEN;
    if (isApiError(err) && err.status === 409) return LOC_DATE_MSG.CONFLICT;
    return LOC_DATE_MSG.ERROR;
  }

  function timePatternSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return LOC_TIME_MSG.INVALID;
    if (isApiError(err) && err.status === 403) return LOC_TIME_MSG.FORBIDDEN;
    if (isApiError(err) && err.status === 409) return LOC_TIME_MSG.CONFLICT;
    return LOC_TIME_MSG.ERROR;
  }

  function dateTimePatternSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return LOC_DATETIME_MSG.INVALID;
    if (isApiError(err) && err.status === 403) return LOC_DATETIME_MSG.FORBIDDEN;
    if (isApiError(err) && err.status === 409) return LOC_DATETIME_MSG.CONFLICT;
    return LOC_DATETIME_MSG.ERROR;
  }

  function dedicatedWriteBlocked(): boolean {
    return (
      busy ||
      labelBusy ||
      descriptionBusy ||
      datePatternBusy ||
      timePatternBusy ||
      dateTimePatternBusy ||
      labelInflight.current ||
      descriptionInflight.current ||
      datePatternInflight.current ||
      timePatternInflight.current ||
      dateTimePatternInflight.current ||
      inflight.current
    );
  }

  function startLabelEdit(): void {
    if (
      isNew ||
      !detail ||
      dedicatedWriteBlocked() ||
      descriptionEditing ||
      datePatternEditing ||
      timePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    setLabelDraft(labelShown);
    setLabelError(null);
    setLabelNotice(null);
    setLabelEditing(true);
  }

  function cancelLabelEdit(): void {
    if (labelBusy || labelInflight.current) return;
    setLabelEditing(false);
    setLabelDraft("");
    setLabelError(null);
  }

  async function handleLabelSave(): Promise<void> {
    if (
      isNew ||
      !detail ||
      !writeKey ||
      !labelEditing ||
      dedicatedWriteBlocked()
    ) {
      return;
    }
    const sent = localeLabelWrite(detail, labelDraft);
    if (sent === "blank") {
      setLabelError(LOC_LABEL_MSG.BLANK);
      setLabelNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setLabelEditing(false);
      setLabelDraft("");
      setLabelError(null);
      return;
    }
    labelInflight.current = true;
    setLabelBusy(true);
    setBusy(true);
    setLabelError(null);
    setLabelNotice(null);
    setError(null);
    setNotice(null);
    const previousShown = labelShown;
    try {
      const saved = await updateLocale(writeKey, sent);
      const accepted = savedLocaleLabel(sent, detail, saved);
      if (accepted == null) {
        setLabel(previousShown);
        setLabelShown(previousShown);
        setLabelError(LOC_LABEL_MSG.ERROR);
        setLabelNotice(null);
        return;
      }
      const nextDetail: LocaleDetail = { ...detail, label: accepted };
      setDetail(nextDetail);
      setLabel(accepted);
      setLabelShown(accepted);
      setLabelEditing(false);
      setLabelDraft("");
      setLabelNotice(LOC_LABEL_MSG.SAVED);
      onSaved?.(nextDetail);
    } catch (err: unknown) {
      setLabel(previousShown);
      setLabelShown(previousShown);
      setLabelError(panelErrMsg(err, labelSaveFallback(err)));
      setLabelNotice(null);
    } finally {
      labelInflight.current = false;
      setLabelBusy(false);
      setBusy(false);
    }
  }

  function startDescriptionEdit(): void {
    if (
      isNew ||
      !detail ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      datePatternEditing ||
      timePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    setDescriptionDraft(descriptionShown);
    setDescriptionError(null);
    setDescriptionNotice(null);
    setDescriptionEditing(true);
  }

  function cancelDescriptionEdit(): void {
    if (descriptionBusy || descriptionInflight.current) return;
    setDescriptionEditing(false);
    setDescriptionDraft("");
    setDescriptionError(null);
  }

  async function handleDescriptionSave(): Promise<void> {
    if (
      isNew ||
      !detail ||
      !writeKey ||
      !descriptionEditing ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      datePatternEditing ||
      timePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    const sent = localeDescriptionWrite(detail, descriptionDraft);
    if (sent === "unchanged") {
      setDescriptionEditing(false);
      setDescriptionDraft("");
      setDescriptionError(null);
      return;
    }
    descriptionInflight.current = true;
    setDescriptionBusy(true);
    setBusy(true);
    setDescriptionError(null);
    setDescriptionNotice(null);
    setError(null);
    setNotice(null);
    const previousShown = descriptionShown;
    try {
      const saved = await updateLocale(writeKey, sent);
      const accepted = savedLocaleDescription(sent, detail, saved);
      if (accepted == null) {
        setDescription(previousShown);
        setDescriptionShown(previousShown);
        setDescriptionError(LOC_DESC_MSG.ERROR);
        setDescriptionNotice(null);
        return;
      }
      const nextDetail: LocaleDetail = { ...detail, description: accepted };
      setDetail(nextDetail);
      setDescription(accepted);
      setDescriptionShown(accepted);
      setDescriptionEditing(false);
      setDescriptionDraft("");
      setDescriptionNotice(accepted ? LOC_DESC_MSG.SAVED : LOC_DESC_MSG.CLEARED);
      onSaved?.(nextDetail);
    } catch (err: unknown) {
      setDescription(previousShown);
      setDescriptionShown(previousShown);
      setDescriptionError(panelErrMsg(err, descriptionSaveFallback(err)));
      setDescriptionNotice(null);
    } finally {
      descriptionInflight.current = false;
      setDescriptionBusy(false);
      setBusy(false);
    }
  }

  function startDatePatternEdit(): void {
    if (
      isNew ||
      !detail ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      timePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    setDatePatternDraft(datePatternShown);
    setDatePatternError(null);
    setDatePatternNotice(null);
    setDatePatternEditing(true);
  }

  function cancelDatePatternEdit(): void {
    if (datePatternBusy || datePatternInflight.current) return;
    setDatePatternEditing(false);
    setDatePatternDraft("");
    setDatePatternError(null);
  }

  async function handleDatePatternSave(): Promise<void> {
    if (
      isNew ||
      !detail ||
      !writeKey ||
      !datePatternEditing ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      timePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    const sent = localeDatePatternWrite(detail, datePatternDraft);
    if (sent === "unchanged") {
      setDatePatternEditing(false);
      setDatePatternDraft("");
      setDatePatternError(null);
      return;
    }
    datePatternInflight.current = true;
    setDatePatternBusy(true);
    setBusy(true);
    setDatePatternError(null);
    setDatePatternNotice(null);
    setError(null);
    setNotice(null);
    const previousShown = datePatternShown;
    try {
      const saved = await updateLocale(writeKey, sent);
      const accepted = savedLocaleDatePattern(sent, detail, saved);
      if (accepted == null) {
        setDatePatternShown(previousShown);
        setFmt((prev) => ({ ...prev, datePattern: previousShown }));
        setDatePatternError(LOC_DATE_MSG.ERROR);
        setDatePatternNotice(null);
        return;
      }
      const nextFormat: LocaleFormatSummary = {
        ...(detail.format || {}),
        datePattern: accepted,
      };
      const nextDetail: LocaleDetail = {
        ...detail,
        hasFormatProfile: true,
        format: nextFormat,
      };
      setDetail(nextDetail);
      setDatePatternShown(accepted);
      setFmt((prev) => ({ ...prev, datePattern: accepted }));
      setStoreFormat(true);
      setDatePatternEditing(false);
      setDatePatternDraft("");
      setDatePatternNotice(accepted ? LOC_DATE_MSG.SAVED : LOC_DATE_MSG.CLEARED);
      onSaved?.(nextDetail);
    } catch (err: unknown) {
      setDatePatternShown(previousShown);
      setFmt((prev) => ({ ...prev, datePattern: previousShown }));
      setDatePatternError(panelErrMsg(err, datePatternSaveFallback(err)));
      setDatePatternNotice(null);
    } finally {
      datePatternInflight.current = false;
      setDatePatternBusy(false);
      setBusy(false);
    }
  }

  function startTimePatternEdit(): void {
    if (
      isNew ||
      !detail ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      datePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    setTimePatternDraft(timePatternShown);
    setTimePatternError(null);
    setTimePatternNotice(null);
    setTimePatternEditing(true);
  }

  function cancelTimePatternEdit(): void {
    if (timePatternBusy || timePatternInflight.current) return;
    setTimePatternEditing(false);
    setTimePatternDraft("");
    setTimePatternError(null);
  }

  async function handleTimePatternSave(): Promise<void> {
    if (
      isNew ||
      !detail ||
      !writeKey ||
      !timePatternEditing ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      datePatternEditing ||
      dateTimePatternEditing
    ) {
      return;
    }
    const sent = localeTimePatternWrite(detail, timePatternDraft);
    if (sent === "unchanged") {
      setTimePatternEditing(false);
      setTimePatternDraft("");
      setTimePatternError(null);
      return;
    }
    timePatternInflight.current = true;
    setTimePatternBusy(true);
    setBusy(true);
    setTimePatternError(null);
    setTimePatternNotice(null);
    setError(null);
    setNotice(null);
    const previousShown = timePatternShown;
    try {
      const saved = await updateLocale(writeKey, sent);
      const accepted = savedLocaleTimePattern(sent, detail, saved);
      if (accepted == null) {
        setTimePatternShown(previousShown);
        setFmt((prev) => ({ ...prev, timePattern: previousShown }));
        setTimePatternError(LOC_TIME_MSG.ERROR);
        setTimePatternNotice(null);
        return;
      }
      const nextFormat: LocaleFormatSummary = {
        ...(detail.format || {}),
        timePattern: accepted,
      };
      const nextDetail: LocaleDetail = {
        ...detail,
        hasFormatProfile: true,
        format: nextFormat,
      };
      setDetail(nextDetail);
      setTimePatternShown(accepted);
      setFmt((prev) => ({ ...prev, timePattern: accepted }));
      setStoreFormat(true);
      setTimePatternEditing(false);
      setTimePatternDraft("");
      setTimePatternNotice(accepted ? LOC_TIME_MSG.SAVED : LOC_TIME_MSG.CLEARED);
      onSaved?.(nextDetail);
    } catch (err: unknown) {
      setTimePatternShown(previousShown);
      setFmt((prev) => ({ ...prev, timePattern: previousShown }));
      setTimePatternError(panelErrMsg(err, timePatternSaveFallback(err)));
      setTimePatternNotice(null);
    } finally {
      timePatternInflight.current = false;
      setTimePatternBusy(false);
      setBusy(false);
    }
  }

  function startDateTimePatternEdit(): void {
    if (
      isNew ||
      !detail ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      datePatternEditing ||
      timePatternEditing
    ) {
      return;
    }
    setDateTimePatternDraft(dateTimePatternShown);
    setDateTimePatternError(null);
    setDateTimePatternNotice(null);
    setDateTimePatternEditing(true);
  }

  function cancelDateTimePatternEdit(): void {
    if (dateTimePatternBusy || dateTimePatternInflight.current) return;
    setDateTimePatternEditing(false);
    setDateTimePatternDraft("");
    setDateTimePatternError(null);
  }

  async function handleDateTimePatternSave(): Promise<void> {
    if (
      isNew ||
      !detail ||
      !writeKey ||
      !dateTimePatternEditing ||
      dedicatedWriteBlocked() ||
      labelEditing ||
      descriptionEditing ||
      datePatternEditing ||
      timePatternEditing
    ) {
      return;
    }
    const sent = localeDateTimePatternWrite(detail, dateTimePatternDraft);
    if (sent === "unchanged") {
      setDateTimePatternEditing(false);
      setDateTimePatternDraft("");
      setDateTimePatternError(null);
      return;
    }
    dateTimePatternInflight.current = true;
    setDateTimePatternBusy(true);
    setBusy(true);
    setDateTimePatternError(null);
    setDateTimePatternNotice(null);
    setError(null);
    setNotice(null);
    const previousShown = dateTimePatternShown;
    try {
      const saved = await updateLocale(writeKey, sent);
      const accepted = savedLocaleDateTimePattern(sent, detail, saved);
      if (accepted == null) {
        setDateTimePatternShown(previousShown);
        setFmt((prev) => ({ ...prev, dateTimePattern: previousShown }));
        setDateTimePatternError(LOC_DATETIME_MSG.ERROR);
        setDateTimePatternNotice(null);
        return;
      }
      const nextFormat: LocaleFormatSummary = {
        ...(detail.format || {}),
        dateTimePattern: accepted,
      };
      const nextDetail: LocaleDetail = {
        ...detail,
        hasFormatProfile: true,
        format: nextFormat,
      };
      setDetail(nextDetail);
      setDateTimePatternShown(accepted);
      setFmt((prev) => ({ ...prev, dateTimePattern: accepted }));
      setStoreFormat(true);
      setDateTimePatternEditing(false);
      setDateTimePatternDraft("");
      setDateTimePatternNotice(accepted ? LOC_DATETIME_MSG.SAVED : LOC_DATETIME_MSG.CLEARED);
      onSaved?.(nextDetail);
    } catch (err: unknown) {
      setDateTimePatternShown(previousShown);
      setFmt((prev) => ({ ...prev, dateTimePattern: previousShown }));
      setDateTimePatternError(panelErrMsg(err, dateTimePatternSaveFallback(err)));
      setDateTimePatternNotice(null);
    } finally {
      dateTimePatternInflight.current = false;
      setDateTimePatternBusy(false);
      setBusy(false);
    }
  }

  async function handleSave(): Promise<void> {
    if (
      !canSave ||
      inflight.current ||
      descriptionInflight.current ||
      labelInflight.current ||
      datePatternInflight.current ||
      timePatternInflight.current ||
      dateTimePatternInflight.current
    ) {
      return;
    }
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
      setLabelShown(storedLocaleLabel(saved.label || label));
      setLabelDraft("");
      setLabelEditing(false);
      setLabelError(null);
      setLabelNotice(null);
      setDescription(saved.description || "");
      setDescriptionShown(storedLocaleDescription(saved.description));
      setDescriptionDraft("");
      setDescriptionEditing(false);
      setDescriptionError(null);
      setDescriptionNotice(null);
      setDatePatternShown(storedLocaleDatePattern(saved.format?.datePattern));
      setDatePatternDraft("");
      setDatePatternEditing(false);
      setDatePatternError(null);
      setDatePatternNotice(null);
      setTimePatternShown(storedLocaleTimePattern(saved.format?.timePattern));
      setTimePatternDraft("");
      setTimePatternEditing(false);
      setTimePatternError(null);
      setTimePatternNotice(null);
      setDateTimePatternShown(storedLocaleDateTimePattern(saved.format?.dateTimePattern));
      setDateTimePatternDraft("");
      setDateTimePatternEditing(false);
      setDateTimePatternError(null);
      setDateTimePatternNotice(null);
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
              disabled={busy || labelBusy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          {!isNew && detail ? (
            <section
              data-testid="developer-loc-set-label"
              aria-label={LOC_LABEL_MSG.ACTION}
              style={{ marginBottom: "16px" }}
            >
              <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_LABEL_MSG.ACTION}</h3>
              <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
                {LOC_LABEL_MSG.HINT}
              </p>
              <p
                data-testid="developer-loc-set-label-text"
                data-loc-label={labelShown}
                style={{ marginTop: 0 }}
              >
                {labelShown}
              </p>
              {labelError ? (
                <div
                  role="alert"
                  data-testid="developer-loc-set-label-error"
                  style={{ color: catalogColors.error, marginBottom: "8px" }}
                >
                  {labelError}
                </div>
              ) : null}
              {labelNotice ? (
                <div
                  data-testid="developer-loc-set-label-notice"
                  style={{ color: "#276749", marginBottom: "8px" }}
                >
                  {labelNotice}
                </div>
              ) : null}
              {labelEditing ? (
                <div data-testid="developer-loc-set-label-editor">
                  <label htmlFor="loc-set-label-input">{LOC_LABEL_MSG.FIELD}</label>
                  <input
                    id="loc-set-label-input"
                    data-testid="developer-loc-set-label-input"
                    style={inputStyle}
                    value={labelDraft}
                    disabled={labelBusy}
                    onChange={(e) => setLabelDraft(e.target.value)}
                  />
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-loc-set-label-save"
                      aria-label={LOC_LABEL_MSG.SAVE}
                      disabled={labelBusy}
                      onClick={() => void handleLabelSave()}
                      style={{
                        padding: "8px 16px",
                        background: labelBusy ? catalogColors.disabled : catalogColors.accent,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: labelBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_LABEL_MSG.SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-loc-set-label-cancel"
                      disabled={labelBusy}
                      onClick={cancelLabelEdit}
                      style={{
                        padding: "8px 16px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: labelBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_LABEL_MSG.CANCEL}
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  data-testid="developer-loc-set-label-edit"
                  aria-label={LOC_LABEL_MSG.ACTION}
                  disabled={
                    busy ||
                    labelBusy ||
                    descriptionBusy ||
                    datePatternBusy ||
                    timePatternBusy ||
                    dateTimePatternBusy ||
                    descriptionEditing ||
                    datePatternEditing ||
                    timePatternEditing ||
                    dateTimePatternEditing
                  }
                  onClick={startLabelEdit}
                  style={{
                    padding: "4px 10px",
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    cursor: busy ? "wait" : "pointer",
                  }}
                >
                  {LOC_LABEL_MSG.ACTION}
                </button>
              )}
            </section>
          ) : null}
          <div style={fieldStyle}>
            <label htmlFor="loc-desc">{DEV_MSG.LOC_FORM_DESCRIPTION}</label>
            <input
              id="loc-desc"
              data-testid="developer-loc-description"
              style={inputStyle}
              value={description}
              disabled={busy || descriptionBusy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          {!isNew && detail ? (
            <section
              data-testid="developer-loc-set-description"
              aria-label={LOC_DESC_MSG.ACTION}
              style={{ marginBottom: "16px" }}
            >
              <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_DESC_MSG.ACTION}</h3>
              <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
                {LOC_DESC_MSG.HINT}
              </p>
              <p
                data-testid="developer-loc-set-description-text"
                data-loc-description={descriptionShown}
                style={{ marginTop: 0 }}
              >
                {descriptionShown}
              </p>
              {descriptionError ? (
                <div
                  role="alert"
                  data-testid="developer-loc-set-description-error"
                  style={{ color: catalogColors.error, marginBottom: "8px" }}
                >
                  {descriptionError}
                </div>
              ) : null}
              {descriptionNotice ? (
                <div
                  data-testid="developer-loc-set-description-notice"
                  style={{ color: "#276749", marginBottom: "8px" }}
                >
                  {descriptionNotice}
                </div>
              ) : null}
              {descriptionEditing ? (
                <div data-testid="developer-loc-set-description-editor">
                  <label htmlFor="loc-set-description-input">{LOC_DESC_MSG.FIELD}</label>
                  <input
                    id="loc-set-description-input"
                    data-testid="developer-loc-set-description-input"
                    style={inputStyle}
                    value={descriptionDraft}
                    disabled={descriptionBusy}
                    onChange={(e) => setDescriptionDraft(e.target.value)}
                  />
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-loc-set-description-save"
                      aria-label={LOC_DESC_MSG.SAVE}
                      disabled={descriptionBusy}
                      onClick={() => void handleDescriptionSave()}
                      style={{
                        padding: "8px 16px",
                        background: descriptionBusy ? catalogColors.disabled : catalogColors.accent,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: descriptionBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DESC_MSG.SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-loc-set-description-cancel"
                      disabled={descriptionBusy}
                      onClick={cancelDescriptionEdit}
                      style={{
                        padding: "8px 16px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: descriptionBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DESC_MSG.CANCEL}
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  data-testid="developer-loc-set-description-edit"
                  aria-label={LOC_DESC_MSG.ACTION}
                  disabled={
                    busy ||
                    descriptionBusy ||
                    labelBusy ||
                    datePatternBusy ||
                    timePatternBusy ||
                    dateTimePatternBusy ||
                    labelEditing ||
                    datePatternEditing ||
                    timePatternEditing ||
                    dateTimePatternEditing
                  }
                  onClick={startDescriptionEdit}
                  style={{
                    padding: "4px 10px",
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    cursor: busy ? "wait" : "pointer",
                  }}
                >
                  {LOC_DESC_MSG.ACTION}
                </button>
              )}
            </section>
          ) : null}
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
          {!isNew && detail ? (
            <section
              data-testid="developer-loc-set-date-pattern"
              aria-label={LOC_DATE_MSG.ACTION}
              style={{ marginBottom: "16px" }}
            >
              <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_DATE_MSG.ACTION}</h3>
              <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
                {LOC_DATE_MSG.HINT}
              </p>
              <p
                data-testid="developer-loc-set-date-pattern-text"
                data-loc-date-pattern={datePatternShown}
                style={{ marginTop: 0, fontFamily: "monospace" }}
              >
                {datePatternShown}
              </p>
              {datePatternError ? (
                <div
                  role="alert"
                  data-testid="developer-loc-set-date-pattern-error"
                  style={{ color: catalogColors.error, marginBottom: "8px" }}
                >
                  {datePatternError}
                </div>
              ) : null}
              {datePatternNotice ? (
                <div
                  data-testid="developer-loc-set-date-pattern-notice"
                  style={{ color: "#276749", marginBottom: "8px" }}
                >
                  {datePatternNotice}
                </div>
              ) : null}
              {datePatternEditing ? (
                <div data-testid="developer-loc-set-date-pattern-editor">
                  <label htmlFor="loc-set-date-pattern-input">{LOC_DATE_MSG.FIELD}</label>
                  <input
                    id="loc-set-date-pattern-input"
                    data-testid="developer-loc-set-date-pattern-input"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={datePatternDraft}
                    disabled={datePatternBusy}
                    onChange={(e) => setDatePatternDraft(e.target.value)}
                  />
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-loc-set-date-pattern-save"
                      aria-label={LOC_DATE_MSG.SAVE}
                      disabled={datePatternBusy}
                      onClick={() => void handleDatePatternSave()}
                      style={{
                        padding: "8px 16px",
                        background: datePatternBusy ? catalogColors.disabled : catalogColors.accent,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: datePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DATE_MSG.SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-loc-set-date-pattern-cancel"
                      disabled={datePatternBusy}
                      onClick={cancelDatePatternEdit}
                      style={{
                        padding: "8px 16px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: datePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DATE_MSG.CANCEL}
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  data-testid="developer-loc-set-date-pattern-edit"
                  aria-label={LOC_DATE_MSG.ACTION}
                  disabled={
                    busy ||
                    datePatternBusy ||
                    timePatternBusy ||
                    dateTimePatternBusy ||
                    labelBusy ||
                    descriptionBusy ||
                    labelEditing ||
                    descriptionEditing ||
                    timePatternEditing ||
                    dateTimePatternEditing
                  }
                  onClick={startDatePatternEdit}
                  style={{
                    padding: "4px 10px",
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    cursor: busy ? "wait" : "pointer",
                  }}
                >
                  {LOC_DATE_MSG.ACTION}
                </button>
              )}
            </section>
          ) : null}
          {!isNew && detail ? (
            <section
              data-testid="developer-loc-set-time-pattern"
              aria-label={LOC_TIME_MSG.ACTION}
              style={{ marginBottom: "16px" }}
            >
              <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_TIME_MSG.ACTION}</h3>
              <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
                {LOC_TIME_MSG.HINT}
              </p>
              <p
                data-testid="developer-loc-set-time-pattern-text"
                data-loc-time-pattern={timePatternShown}
                style={{ marginTop: 0, fontFamily: "monospace" }}
              >
                {timePatternShown}
              </p>
              {timePatternError ? (
                <div
                  role="alert"
                  data-testid="developer-loc-set-time-pattern-error"
                  style={{ color: catalogColors.error, marginBottom: "8px" }}
                >
                  {timePatternError}
                </div>
              ) : null}
              {timePatternNotice ? (
                <div
                  data-testid="developer-loc-set-time-pattern-notice"
                  style={{ color: "#276749", marginBottom: "8px" }}
                >
                  {timePatternNotice}
                </div>
              ) : null}
              {timePatternEditing ? (
                <div data-testid="developer-loc-set-time-pattern-editor">
                  <label htmlFor="loc-set-time-pattern-input">{LOC_TIME_MSG.FIELD}</label>
                  <input
                    id="loc-set-time-pattern-input"
                    data-testid="developer-loc-set-time-pattern-input"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={timePatternDraft}
                    disabled={timePatternBusy}
                    onChange={(e) => setTimePatternDraft(e.target.value)}
                  />
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-loc-set-time-pattern-save"
                      aria-label={LOC_TIME_MSG.SAVE}
                      disabled={timePatternBusy}
                      onClick={() => void handleTimePatternSave()}
                      style={{
                        padding: "8px 16px",
                        background: timePatternBusy ? catalogColors.disabled : catalogColors.accent,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: timePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_TIME_MSG.SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-loc-set-time-pattern-cancel"
                      disabled={timePatternBusy}
                      onClick={cancelTimePatternEdit}
                      style={{
                        padding: "8px 16px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: timePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_TIME_MSG.CANCEL}
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  data-testid="developer-loc-set-time-pattern-edit"
                  aria-label={LOC_TIME_MSG.ACTION}
                  disabled={
                    busy ||
                    timePatternBusy ||
                    datePatternBusy ||
                    dateTimePatternBusy ||
                    labelBusy ||
                    descriptionBusy ||
                    labelEditing ||
                    descriptionEditing ||
                    datePatternEditing ||
                    dateTimePatternEditing
                  }
                  onClick={startTimePatternEdit}
                  style={{
                    padding: "4px 10px",
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    cursor: busy ? "wait" : "pointer",
                  }}
                >
                  {LOC_TIME_MSG.ACTION}
                </button>
              )}
            </section>
          ) : null}
          {!isNew && detail ? (
            <section
              data-testid="developer-loc-set-date-time-pattern"
              aria-label={LOC_DATETIME_MSG.ACTION}
              style={{ marginBottom: "16px" }}
            >
              <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_DATETIME_MSG.ACTION}</h3>
              <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
                {LOC_DATETIME_MSG.HINT}
              </p>
              <p
                data-testid="developer-loc-set-date-time-pattern-text"
                data-loc-date-time-pattern={dateTimePatternShown}
                style={{ marginTop: 0, fontFamily: "monospace" }}
              >
                {dateTimePatternShown}
              </p>
              {dateTimePatternError ? (
                <div
                  role="alert"
                  data-testid="developer-loc-set-date-time-pattern-error"
                  style={{ color: catalogColors.error, marginBottom: "8px" }}
                >
                  {dateTimePatternError}
                </div>
              ) : null}
              {dateTimePatternNotice ? (
                <div
                  data-testid="developer-loc-set-date-time-pattern-notice"
                  style={{ color: "#276749", marginBottom: "8px" }}
                >
                  {dateTimePatternNotice}
                </div>
              ) : null}
              {dateTimePatternEditing ? (
                <div data-testid="developer-loc-set-date-time-pattern-editor">
                  <label htmlFor="loc-set-date-time-pattern-input">{LOC_DATETIME_MSG.FIELD}</label>
                  <input
                    id="loc-set-date-time-pattern-input"
                    data-testid="developer-loc-set-date-time-pattern-input"
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={dateTimePatternDraft}
                    disabled={dateTimePatternBusy}
                    onChange={(e) => setDateTimePatternDraft(e.target.value)}
                  />
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-loc-set-date-time-pattern-save"
                      aria-label={LOC_DATETIME_MSG.SAVE}
                      disabled={dateTimePatternBusy}
                      onClick={() => void handleDateTimePatternSave()}
                      style={{
                        padding: "8px 16px",
                        background: dateTimePatternBusy
                          ? catalogColors.disabled
                          : catalogColors.accent,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: dateTimePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DATETIME_MSG.SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-loc-set-date-time-pattern-cancel"
                      disabled={dateTimePatternBusy}
                      onClick={cancelDateTimePatternEdit}
                      style={{
                        padding: "8px 16px",
                        background: "transparent",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        cursor: dateTimePatternBusy ? "wait" : "pointer",
                      }}
                    >
                      {LOC_DATETIME_MSG.CANCEL}
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  type="button"
                  data-testid="developer-loc-set-date-time-pattern-edit"
                  aria-label={LOC_DATETIME_MSG.ACTION}
                  disabled={
                    busy ||
                    dateTimePatternBusy ||
                    datePatternBusy ||
                    timePatternBusy ||
                    labelBusy ||
                    descriptionBusy ||
                    labelEditing ||
                    descriptionEditing ||
                    datePatternEditing ||
                    timePatternEditing
                  }
                  onClick={startDateTimePatternEdit}
                  style={{
                    padding: "4px 10px",
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    cursor: busy ? "wait" : "pointer",
                  }}
                >
                  {LOC_DATETIME_MSG.ACTION}
                </button>
              )}
            </section>
          ) : null}

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
