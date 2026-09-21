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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { extractRestErrorMessage, isApiError } from "../api/client";
import {
  DEFAULT_EXTENSION_HANDLER,
  EXTENSION_DESIGN_GAPS,
  USER_EXTENSION_CONTEXT,
  createExtension,
  deleteExtension,
  extensionClassName,
  formatExtensionInterfaces,
  getExtensionDetail,
  isExtensionWriteReady,
  isImmutableExtension,
  normalizeExtensionName,
  parseExtensionInterfaces,
  saveExtension,
  withoutStaleExtensionWriteGap,
  type ExtensionWriteBody,
} from "../api/developer/extensionsApi";
import {
  emptyMethod,
  emptyMethodParam,
  methodsFingerprint,
  methodsToRows,
  rowsToMethods,
} from "../api/developer/extensionMethods";
import {
  emptyRuntimeParam,
  rowsToRuntimeParams,
  runtimeParamsFingerprint,
  runtimeParamsToRows,
} from "../api/developer/extensionRuntimeParams";
import {
  initParamsFingerprint,
  initParamsToRows,
  mergeInitParametersForWrite,
  type InitParamRow,
} from "../api/developer/extensionInitParams";
import { ExtensionInitParamDialog } from "./ExtensionInitParamDialog";
import type {
  ExtensionDef,
  ExtensionMethodDef,
  ExtensionMethodParam,
} from "../api/developer/types";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
} from "./catalogStyles";
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

export function ExtensionDetailPanel({
  idOrName,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  onBack: () => void;
  onSaved?: (detail: ExtensionDef) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<ExtensionDef | null>(null);
  const [name, setName] = useState("");
  const [handlerName, setHandlerName] = useState(DEFAULT_EXTENSION_HANDLER);
  const [interfacesText, setInterfacesText] = useState("");
  const [className, setClassName] = useState("");
  const [methodRows, setMethodRows] = useState<ExtensionMethodDef[]>([]);
  const [paramRows, setParamRows] = useState<ExtensionMethodParam[]>([]);
  const [initRows, setInitRows] = useState<InitParamRow[]>([]);
  const [initDialogOpen, setInitDialogOpen] = useState(false);
  const [deprecated, setDeprecated] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setLoading(true);
    getExtensionDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.extensionName || idOrName);
        setHandlerName(d.handlerName || DEFAULT_EXTENSION_HANDLER);
        setInterfacesText(formatExtensionInterfaces(d.supportedInterfaces));
        setClassName(extensionClassName(d.initParameters));
        setMethodRows(methodsToRows(d.methods));
        setParamRows(runtimeParamsToRows(d.runtimeParameters));
        setInitRows(initParamsToRows(d.initParameters));
        setDeprecated(Boolean(d.deprecated));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.EX_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const immutable = !isNew && isImmutableExtension(detail);
  const readOnly = immutable || busy;
  const interfaces = parseExtensionInterfaces(interfacesText);
  const loadedName = normalizeExtensionName(detail?.extensionName || idOrName || "");
  const loadedHandler = detail?.handlerName || DEFAULT_EXTENSION_HANDLER;
  const loadedInterfaces = formatExtensionInterfaces(detail?.supportedInterfaces);
  const loadedClassName = extensionClassName(detail?.initParameters);
  const loadedDeprecated = Boolean(detail?.deprecated);
  const loadedMethodsFp = methodsFingerprint(methodsToRows(detail?.methods));
  const loadedParamsFp = runtimeParamsFingerprint(
    runtimeParamsToRows(detail?.runtimeParameters),
  );
  const loadedInitFp = initParamsFingerprint(initParamsToRows(detail?.initParameters));
  // Compare normalized forms so server trim / Jackson map round-trips do not mark dirty on load.
  const dirty =
    isNew ||
    normalizeExtensionName(name) !== loadedName ||
    handlerName.trim() !== loadedHandler.trim() ||
    formatExtensionInterfaces(interfaces) !== loadedInterfaces ||
    className.trim() !== loadedClassName.trim() ||
    deprecated !== loadedDeprecated ||
    methodsFingerprint(methodRows) !== loadedMethodsFp ||
    runtimeParamsFingerprint(paramRows) !== loadedParamsFp ||
    initParamsFingerprint(initRows) !== loadedInitFp;
  const canSave =
    !busy &&
    dirty &&
    isExtensionWriteReady({
      isNew,
      name,
      interfaces,
      className,
      handlerName,
      immutable,
    });
  const writeKey = idOrName || createdKey || normalizeExtensionName(name);
  const canDelete = !isNew && Boolean(writeKey) && !immutable && !busy;

  function updateMethodRow(index: number, patch: Partial<ExtensionMethodDef>): void {
    setMethodRows((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  function removeMethodRow(index: number): void {
    setMethodRows((rows) => rows.filter((_, i) => i !== index));
  }

  function addMethodParam(methodIndex: number): void {
    setMethodRows((rows) =>
      rows.map((row, i) =>
        i === methodIndex
          ? { ...row, parameters: [...(row.parameters ?? []), emptyMethodParam()] }
          : row,
      ),
    );
  }

  function updateMethodParam(
    methodIndex: number,
    paramIndex: number,
    patch: Partial<NonNullable<ExtensionMethodDef["parameters"]>[number]>,
  ): void {
    setMethodRows((rows) =>
      rows.map((row, i) => {
        if (i !== methodIndex) return row;
        const parameters = (row.parameters ?? []).map((p, j) =>
          j === paramIndex ? { ...p, ...patch } : p,
        );
        return { ...row, parameters };
      }),
    );
  }

  function removeMethodParam(methodIndex: number, paramIndex: number): void {
    setMethodRows((rows) =>
      rows.map((row, i) =>
        i === methodIndex
          ? { ...row, parameters: (row.parameters ?? []).filter((_, j) => j !== paramIndex) }
          : row,
      ),
    );
  }

  function updateRuntimeParam(index: number, patch: Partial<ExtensionMethodParam>): void {
    setParamRows((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  function removeRuntimeParam(index: number): void {
    setParamRows((rows) => rows.filter((_, i) => i !== index));
  }

  /**
   * PUT writes initParameters from the dialog plus className. Removed extra keys
   * are sent as null so REST mergeInitParams deletes them.
   */
  function writeBody(): ExtensionWriteBody {
    const initParameters = mergeInitParametersForWrite({
      previous: detail?.initParameters,
      className,
      rows: initRows,
    });
    const body: ExtensionWriteBody = {
      extensionName: isNew
        ? normalizeExtensionName(name)
        : detail?.extensionName || normalizeExtensionName(name),
      handlerName: isNew ? handlerName.trim() || DEFAULT_EXTENSION_HANDLER : detail?.handlerName,
      context: isNew ? USER_EXTENSION_CONTEXT : detail?.context,
      category: detail?.category,
      supportedInterfaces: interfaces,
      initParameters,
      deprecated,
      restoreRequestParamsOnError: Boolean(detail?.restoreRequestParamsOnError),
      version: detail?.version,
      runtimeParameters: rowsToRuntimeParams(paramRows),
      methods: rowsToMethods(methodRows),
    };
    return body;
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409) {
      const fromBody = extractRestErrorMessage(err.body);
      if (fromBody) return fromBody;
      return isNew ? DEV_MSG.EX_DUPLICATE : DEV_MSG.EX_SYSTEM;
    }
    if (isApiError(err) && err.status === 400) {
      if (isNew) {
        const fromBody = extractRestErrorMessage(err.body);
        return fromBody || DEV_MSG.EX_INVALID_NAME;
      }
      const fromBody = extractRestErrorMessage(err.body);
      return fromBody || DEV_MSG.EX_SAVE_ERROR;
    }
    if (isApiError(err) && err.status === 403) return DEV_MSG.EX_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.EX_NOT_FOUND;
    return DEV_MSG.EX_SAVE_ERROR;
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
          ? await createExtension(writeBody())
          : await saveExtension(writeKey, writeBody());
      setDetail(saved);
      const persistedName = (saved.extensionName || saved.fqn || "").trim();
      if (!persistedName) {
        setError(DEV_MSG.EX_MISSING_PERSISTED_NAME);
        return;
      }
      if (isNew) {
        setCreatedKey(saved.fqn || persistedName);
      }
      setName(saved.extensionName || normalizeExtensionName(name));
      setHandlerName(saved.handlerName || DEFAULT_EXTENSION_HANDLER);
      setInterfacesText(formatExtensionInterfaces(saved.supportedInterfaces));
      setClassName(extensionClassName(saved.initParameters));
      setMethodRows(methodsToRows(saved.methods));
      setParamRows(runtimeParamsToRows(saved.runtimeParameters));
      setInitRows(initParamsToRows(saved.initParameters));
      setDeprecated(Boolean(saved.deprecated));
      setNotice(DEV_MSG.EX_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 409) {
        setError(saveFallback(err));
      } else {
        setError(panelErrMsg(err, saveFallback(err)));
      }
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (!canDelete || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    // Create mode never shows delete chrome; guard so a future refactor cannot
    // delete while selected.idOrName === "new" / idOrName is null.
    if (isNew || !canDelete || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteExtension(writeKey);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.EX_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.EX_NOT_FOUND
            : isApiError(err) && err.status === 409
              ? DEV_MSG.EX_SYSTEM
              : DEV_MSG.EX_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.EX_NEW
    : detail?.extensionName || idOrName || DEV_MSG.EX_EDIT;

  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? withoutStaleExtensionWriteGap(detail.designGaps)
      : [...EXTENSION_DESIGN_GAPS];

  return (
    <div data-testid="developer-ex-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ex-back"
        aria-label={DEV_MSG.EX_BACK}
        style={backButton}
      >
        ← {DEV_MSG.EX_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ex-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-ex-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-ex-detail-loading">{DEV_MSG.EX_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ex-detail-title">
              {title}
            </h2>
            {immutable ? (
              <p
                data-testid="developer-ex-immutable-hint"
                style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
              >
                {DEV_MSG.EX_IMMUTABLE_HINT}
              </p>
            ) : null}
            {!isNew && detail ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.EX_COL_FQN}</dt>
                <dd style={{ margin: 0, ...monoCell }}>{detail.fqn || "—"}</dd>
                <dt>{DEV_MSG.EX_COL_CONTEXT}</dt>
                <dd style={{ margin: 0, ...monoCell }}>{detail.context || "—"}</dd>
                <dt>{DEV_MSG.EX_COL_VERSION}</dt>
                <dd style={{ margin: 0 }}>
                  {detail.version != null ? String(detail.version) : "—"}
                </dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="ex-name">{DEV_MSG.EX_FORM_NAME}</label>
            <input
              id="ex-name"
              data-testid="developer-ex-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || readOnly}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.EX_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.EX_NAME_HINT}
              </span>
            )}
          </div>

          <div style={fieldStyle}>
            <label htmlFor="ex-handler">{DEV_MSG.EX_FORM_HANDLER}</label>
            <input
              id="ex-handler"
              data-testid="developer-ex-handler"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={handlerName}
              disabled={!isNew || readOnly}
              onChange={(e) => setHandlerName(e.target.value)}
              autoComplete="off"
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_HANDLER_HINT}
            </span>
          </div>

          <div style={fieldStyle}>
            <label htmlFor="ex-ifaces">{DEV_MSG.EX_FORM_IFACES}</label>
            <textarea
              id="ex-ifaces"
              data-testid="developer-ex-interfaces"
              style={{ ...inputStyle, fontFamily: "monospace", minHeight: "72px" }}
              value={interfacesText}
              disabled={readOnly}
              onChange={(e) => setInterfacesText(e.target.value)}
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_IFACES_HINT}
            </span>
          </div>

          <div style={fieldStyle}>
            <label htmlFor="ex-classname">{DEV_MSG.EX_FORM_CLASSNAME}</label>
            <input
              id="ex-classname"
              data-testid="developer-ex-classname"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={className}
              disabled={readOnly}
              onChange={(e) => setClassName(e.target.value)}
              autoComplete="off"
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_CLASSNAME_HINT}
            </span>
          </div>

          <div style={{ ...fieldStyle, flexDirection: "row", alignItems: "center", gap: "8px" }}>
            <input
              id="ex-deprecated"
              data-testid="developer-ex-deprecated"
              type="checkbox"
              checked={deprecated}
              disabled={readOnly}
              onChange={(e) => setDeprecated(e.target.checked)}
            />
            <label htmlFor="ex-deprecated">{DEV_MSG.EX_FORM_DEPRECATED}</label>
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-ex-save"
              aria-label={DEV_MSG.EX_SAVE}
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
              {DEV_MSG.EX_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-ex-cancel"
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
              {DEV_MSG.EX_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-ex-delete"
                aria-label={DEV_MSG.EX_DELETE}
                disabled={!canDelete}
                onClick={requestDelete}
                style={{
                  padding: "8px 16px",
                  background: canDelete ? "#c53030" : catalogColors.disabled,
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: canDelete ? "pointer" : "not-allowed",
                  marginLeft: "auto",
                }}
              >
                {DEV_MSG.EX_DELETE}
              </button>
            ) : null}
          </div>

          <section data-testid="developer-ex-methods" style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_METHODS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_METHOD_HINT}
            </p>
            {methodRows.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ex-methods-empty">
                {DEV_MSG.EX_METHOD_EMPTY}
              </p>
            ) : (
              methodRows.map((row, i) => (
                <div
                  key={`method-${i}`}
                  data-testid={`developer-ex-method-row-${i}`}
                  style={{
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    padding: "12px",
                    marginBottom: "8px",
                  }}
                >
                  <div style={fieldStyle}>
                    <label htmlFor={`ex-method-name-${i}`}>{DEV_MSG.EX_METHOD_NAME}</label>
                    <input
                      id={`ex-method-name-${i}`}
                      data-testid={`developer-ex-method-name-${i}`}
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                      value={row.name ?? ""}
                      disabled={readOnly}
                      onChange={(e) => updateMethodRow(i, { name: e.target.value })}
                      autoComplete="off"
                    />
                  </div>
                  <div style={fieldStyle}>
                    <label htmlFor={`ex-method-return-${i}`}>{DEV_MSG.EX_METHOD_RETURN}</label>
                    <input
                      id={`ex-method-return-${i}`}
                      data-testid={`developer-ex-method-return-${i}`}
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                      value={row.returnType ?? ""}
                      disabled={readOnly}
                      onChange={(e) => updateMethodRow(i, { returnType: e.target.value })}
                      autoComplete="off"
                    />
                  </div>
                  <div style={fieldStyle}>
                    <label htmlFor={`ex-method-desc-${i}`}>{DEV_MSG.EX_METHOD_DESC}</label>
                    <input
                      id={`ex-method-desc-${i}`}
                      data-testid={`developer-ex-method-desc-${i}`}
                      style={inputStyle}
                      value={row.description ?? ""}
                      disabled={readOnly}
                      onChange={(e) => updateMethodRow(i, { description: e.target.value })}
                      autoComplete="off"
                    />
                  </div>
                  <div style={{ marginBottom: "8px" }}>
                    <div style={{ fontSize: "0.85rem", marginBottom: "4px" }}>
                      {DEV_MSG.EX_METHOD_PARAMS}
                    </div>
                    {(row.parameters ?? []).map((p, j) => (
                      <div
                        key={`param-${i}-${j}`}
                        style={{ display: "flex", gap: "8px", marginBottom: "4px", flexWrap: "wrap" }}
                      >
                        <input
                          data-testid={`developer-ex-method-param-name-${i}-${j}`}
                          aria-label={DEV_MSG.EX_COL_PARAM}
                          style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 8rem" }}
                          value={p.name ?? ""}
                          disabled={readOnly}
                          onChange={(e) => updateMethodParam(i, j, { name: e.target.value })}
                          autoComplete="off"
                        />
                        <input
                          data-testid={`developer-ex-method-param-type-${i}-${j}`}
                          aria-label={DEV_MSG.EX_COL_TYPE}
                          style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 8rem" }}
                          value={p.dataType ?? ""}
                          disabled={readOnly}
                          onChange={(e) => updateMethodParam(i, j, { dataType: e.target.value })}
                          autoComplete="off"
                        />
                        <button
                          type="button"
                          data-testid={`developer-ex-method-param-remove-${i}-${j}`}
                          disabled={readOnly}
                          onClick={() => removeMethodParam(i, j)}
                          style={{
                            padding: "8px",
                            border: `1px solid ${catalogColors.softBorder}`,
                            borderRadius: "4px",
                            background: "transparent",
                            cursor: readOnly ? "not-allowed" : "pointer",
                          }}
                        >
                          {DEV_MSG.EX_METHOD_PARAM_REMOVE}
                        </button>
                      </div>
                    ))}
                    <button
                      type="button"
                      data-testid={`developer-ex-method-param-add-${i}`}
                      disabled={readOnly}
                      onClick={() => addMethodParam(i)}
                      style={{
                        padding: "6px 12px",
                        border: `1px solid ${catalogColors.softBorder}`,
                        borderRadius: "4px",
                        background: "transparent",
                        cursor: readOnly ? "not-allowed" : "pointer",
                      }}
                    >
                      {DEV_MSG.EX_METHOD_PARAM_ADD}
                    </button>
                  </div>
                  <button
                    type="button"
                    data-testid={`developer-ex-method-remove-${i}`}
                    disabled={readOnly}
                    onClick={() => removeMethodRow(i)}
                    style={{
                      padding: "6px 12px",
                      background: readOnly ? catalogColors.disabled : "#c53030",
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: readOnly ? "not-allowed" : "pointer",
                    }}
                  >
                    {DEV_MSG.EX_METHOD_REMOVE}
                  </button>
                </div>
              ))
            )}
            <button
              type="button"
              data-testid="developer-ex-method-add"
              disabled={readOnly}
              onClick={() => setMethodRows((rows) => [...rows, emptyMethod()])}
              style={{
                padding: "8px 16px",
                background: readOnly ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: readOnly ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.EX_METHOD_ADD}
            </button>
          </section>

          <section data-testid="developer-ex-init" style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_INIT_SECTION}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_INIT_HINT}
            </p>
            {initRows.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ex-init-empty">
                {DEV_MSG.EX_INIT_EMPTY}
              </p>
            ) : (
              <ul data-testid="developer-ex-init-summary" style={{ fontFamily: "monospace" }}>
                {initRows.map((row, i) => (
                  <li key={`init-sum-${i}`} data-testid={`developer-ex-init-summary-${i}`}>
                    {row.key}={row.value}
                  </li>
                ))}
              </ul>
            )}
            <button
              type="button"
              data-testid="developer-ex-init-open"
              disabled={busy}
              onClick={(ev) => {
                captureDialogOpener(ev.currentTarget);
                setInitDialogOpen(true);
              }}
              style={{
                padding: "8px 16px",
                background: catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.EX_INIT_OPEN}
            </button>
          </section>

          <section data-testid="developer-ex-params" style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_PARAMS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.EX_RTPARAM_HINT}
            </p>
            {paramRows.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ex-params-empty">
                {DEV_MSG.EX_RTPARAM_EMPTY}
              </p>
            ) : (
              paramRows.map((row, i) => (
                <div
                  key={`rtparam-${i}`}
                  data-testid={`developer-ex-rtparam-row-${i}`}
                  style={{ display: "flex", gap: "8px", marginBottom: "4px", flexWrap: "wrap" }}
                >
                  <input
                    data-testid={`developer-ex-rtparam-name-${i}`}
                    aria-label={DEV_MSG.EX_RTPARAM_NAME}
                    style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 8rem" }}
                    value={row.name ?? ""}
                    disabled={readOnly}
                    onChange={(e) => updateRuntimeParam(i, { name: e.target.value })}
                    autoComplete="off"
                  />
                  <input
                    data-testid={`developer-ex-rtparam-type-${i}`}
                    aria-label={DEV_MSG.EX_RTPARAM_TYPE}
                    style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 8rem" }}
                    value={row.dataType ?? ""}
                    disabled={readOnly}
                    onChange={(e) => updateRuntimeParam(i, { dataType: e.target.value })}
                    autoComplete="off"
                  />
                  <input
                    data-testid={`developer-ex-rtparam-desc-${i}`}
                    aria-label={DEV_MSG.EX_RTPARAM_DESC}
                    style={{ ...inputStyle, flex: "2 1 12rem" }}
                    value={row.description ?? ""}
                    disabled={readOnly}
                    onChange={(e) => updateRuntimeParam(i, { description: e.target.value })}
                    autoComplete="off"
                  />
                  <button
                    type="button"
                    data-testid={`developer-ex-rtparam-remove-${i}`}
                    disabled={readOnly}
                    onClick={() => removeRuntimeParam(i)}
                    style={{
                      padding: "8px",
                      border: `1px solid ${catalogColors.softBorder}`,
                      borderRadius: "4px",
                      background: "transparent",
                      cursor: readOnly ? "not-allowed" : "pointer",
                    }}
                  >
                    {DEV_MSG.EX_RTPARAM_REMOVE}
                  </button>
                </div>
              ))
            )}
            <button
              type="button"
              data-testid="developer-ex-rtparam-add"
              disabled={readOnly}
              onClick={() => setParamRows((rows) => [...rows, emptyRuntimeParam()])}
              style={{
                padding: "8px 16px",
                background: readOnly ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: readOnly ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.EX_RTPARAM_ADD}
            </button>
          </section>

          {detail && gapList.length > 0 ? (
            <section style={{ marginTop: "16px" }} data-testid="developer-ex-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.EX_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {gapList.map((g) => (
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
        message={DEV_MSG.EX_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
      <ExtensionInitParamDialog
        open={initDialogOpen}
        readOnly={readOnly}
        rows={initRows}
        onCancel={() => setInitDialogOpen(false)}
        onApply={(rows) => {
          setInitRows(rows);
          setInitDialogOpen(false);
        }}
      />
    </div>
  );
}
