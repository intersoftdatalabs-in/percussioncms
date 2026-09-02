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
  ACTION_MENU_TYPE_ITEM,
  ACTION_MENU_TYPES,
  createActionMenu,
  deleteActionMenu,
  getActionMenuDetail,
  isActionMenuWriteReady,
  normalizeActionMenuName,
  saveActionMenu,
  withoutStaleActionMenuWriteGap,
  type ActionMenuWriteBody,
} from "../api/developer/actionMenusApi";
import { resolveActionMenuObjectGuid } from "../api/displayFormatGuid";
import type { ActionMenu } from "../api/developer/types";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";

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

function typeFromDetail(detail: ActionMenu | null, fallback: string): string {
  if (detail?.menuType && detail.menuType.trim()) return detail.menuType.trim();
  return fallback;
}

export function ActionMenuDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  /** GUID from catalog list row when detail wire omits stringValue (#3380). */
  catalogGuid?: string | null;
  onBack: () => void;
  onSaved?: (detail: ActionMenu) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<ActionMenu | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [menuType, setMenuType] = useState(ACTION_MENU_TYPE_ITEM);
  const [url, setUrl] = useState("");
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
    getActionMenuDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || idOrName);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setMenuType(typeFromDetail(d, ACTION_MENU_TYPE_ITEM));
        setUrl(d.url || "");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.AM_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedName = normalizeActionMenuName(detail?.name || idOrName || "");
  const loadedLabel = detail?.label || "";
  const loadedDescription = detail?.description || "";
  const loadedType = typeFromDetail(detail, ACTION_MENU_TYPE_ITEM);
  const loadedUrl = detail?.url || "";
  const dirty =
    isNew ||
    normalizeActionMenuName(name) !== loadedName ||
    label !== loadedLabel ||
    description !== loadedDescription ||
    menuType !== loadedType ||
    url !== loadedUrl;
  const canSave = !busy && dirty && isActionMenuWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeActionMenuName(name);
  const objectGuid = resolveActionMenuObjectGuid(detail, catalogGuid);

  function writeBody(): ActionMenuWriteBody {
    const body: ActionMenuWriteBody = {
      name: isNew ? normalizeActionMenuName(name) : detail?.name || normalizeActionMenuName(name),
      label,
      description,
      menuType,
    };
    if (url.trim()) {
      body.url = url.trim();
    }
    return body;
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409) {
      const fromBody = extractRestErrorMessage(err.body);
      if (fromBody) return fromBody;
      return isNew ? DEV_MSG.AM_DUPLICATE : DEV_MSG.AM_SYSTEM;
    }
    if (isApiError(err) && err.status === 400) {
      if (isNew) return DEV_MSG.AM_INVALID_NAME;
      const fromBody = extractRestErrorMessage(err.body);
      return fromBody || DEV_MSG.AM_SAVE_ERROR;
    }
    if (isApiError(err) && err.status === 403) return DEV_MSG.AM_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.AM_NOT_FOUND;
    return DEV_MSG.AM_SAVE_ERROR;
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
          ? await createActionMenu(writeBody())
          : await saveActionMenu(writeKey, writeBody());
      setDetail(saved);
      const persistedName = (saved.name || "").trim();
      if (!persistedName) {
        setError(DEV_MSG.AM_MISSING_PERSISTED_NAME);
        return;
      }
      if (isNew) {
        setCreatedKey(persistedName);
      }
      setName(persistedName);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setMenuType(typeFromDetail(saved, menuType));
      setUrl(saved.url || "");
      setNotice(DEV_MSG.AM_SAVED);
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
      await deleteActionMenu(writeKey);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.AM_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.AM_NOT_FOUND
            : isApiError(err) && err.status === 409
              ? DEV_MSG.AM_SYSTEM
              : DEV_MSG.AM_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.AM_NEW
    : detail?.label || detail?.name || idOrName || DEV_MSG.AM_EDIT;

  const params = detail != null && Array.isArray(detail.parameters) ? detail.parameters : [];
  const props = detail != null && Array.isArray(detail.properties) ? detail.properties : [];
  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? withoutStaleActionMenuWriteGap(detail.designGaps)
      : [DEV_MSG.AM_GAP_CHILDREN, DEV_MSG.AM_GAP_VISIBILITY, DEV_MSG.AM_GAP_UI03];

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

      {notice ? (
        <div data-testid="developer-am-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-am-detail-loading">{DEV_MSG.AM_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-am-detail-title">
              {title}
            </h2>
            {!isNew && detail?.description && !dirty ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            {!isNew && objectGuid ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.AM_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-am-detail-guid"
                >
                  {objectGuid}
                </dd>
                <dt>{DEV_MSG.AM_COL_HANDLER}</dt>
                <dd style={{ margin: 0 }}>{detail?.handler || "—"}</dd>
                <dt>{DEV_MSG.AM_COL_SORT}</dt>
                <dd style={{ margin: 0 }}>
                  {detail?.sortRank != null ? String(detail.sortRank) : "—"}
                </dd>
              </dl>
            ) : !isNew ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.AM_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-am-detail-guid"
                >
                  —
                </dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="am-name">{DEV_MSG.AM_FORM_NAME}</label>
            <input
              id="am-name"
              data-testid="developer-am-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.AM_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.AM_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-label">{DEV_MSG.AM_FORM_LABEL}</label>
            <input
              id="am-label"
              data-testid="developer-am-label"
              style={inputStyle}
              value={label}
              disabled={busy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-desc">{DEV_MSG.AM_FORM_DESCRIPTION}</label>
            <input
              id="am-desc"
              data-testid="developer-am-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-type">{DEV_MSG.AM_FORM_TYPE}</label>
            <select
              id="am-type"
              data-testid="developer-am-type"
              style={inputStyle}
              value={menuType}
              disabled={busy}
              onChange={(e) => setMenuType(e.target.value)}
            >
              {ACTION_MENU_TYPES.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>
          <div style={fieldStyle}>
            <label htmlFor="am-url">{DEV_MSG.AM_FORM_URL}</label>
            <input
              id="am-url"
              data-testid="developer-am-url"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={url}
              disabled={busy}
              onChange={(e) => setUrl(e.target.value)}
              autoComplete="off"
            />
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-am-save"
              aria-label={DEV_MSG.AM_SAVE}
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
              {DEV_MSG.AM_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-am-cancel"
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
              {DEV_MSG.AM_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-am-delete"
                aria-label={DEV_MSG.AM_DELETE}
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
                {DEV_MSG.AM_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
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
                objectGuid={objectGuid}
                objectKind="action-menu"
                testIdPrefix="developer-am-acl"
              />

              <section style={{ marginTop: "16px" }} data-testid="developer-am-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.AM_GAPS}</h3>
                <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {gapList.map((g) => (
                    <li key={g}>{g}</li>
                  ))}
                </ul>
              </section>
            </>
          ) : null}
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.AM_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
