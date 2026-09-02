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

import React, { useEffect, useMemo, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { isApiError } from "../api/client";
import { listCommunities } from "../api/developer/assemblyApi";
import {
  createDisplayFormat,
  deleteDisplayFormat,
  getDisplayFormatDetail,
  isDisplayFormatWriteReady,
  normalizeColumns,
  normalizeDisplayFormatName,
  resolveDisplayFormatObjectGuid,
  saveDisplayFormat,
  updateDisplayFormat,
  type DisplayFormatWriteBody,
} from "../api/developer/displayFormatsApi";
import type {
  CommunitySummary,
  DisplayFormat,
  DisplayFormatColumn,
} from "../api/developer/types";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import {
  addDisplayFormatColumn,
  catalogFieldsNotInUse,
  columnsOrderEqual,
  isPackagedDisplayFormat,
  isSysTitleColumn,
  isValidColumnSource,
  moveDisplayFormatColumn,
  reindexColumns,
  removeDisplayFormatColumn,
} from "./displayFormatColumns";
import {
  communityWireKey,
  isAllCommunities,
  normalizeAllowedCommunities,
  selectedKeysFromMap,
  toAllowedCommunitiesWriteBody,
  type AllowedCommunityMap,
} from "./displayFormatCommunities";
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

const actionButton: React.CSSProperties = {
  padding: "4px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
  font: "inherit",
};

export function DisplayFormatDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
  onSaved,
  onDeleted,
  onColumnsSaved,
}: {
  /** null = create mode */
  idOrName: string | null;
  /** GUID from catalog list row when detail payload omits stringValue (#2951). */
  catalogGuid?: string | null;
  onBack: () => void;
  onSaved?: (detail: DisplayFormat) => void;
  onDeleted?: () => void;
  onColumnsSaved?: (detail: DisplayFormat) => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<DisplayFormat | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const [draftColumns, setDraftColumns] = useState<DisplayFormatColumn[]>([]);
  const [addSource, setAddSource] = useState("");
  const [communityCatalog, setCommunityCatalog] = useState<CommunitySummary[]>([]);
  const [loadedCommunityMap, setLoadedCommunityMap] = useState<AllowedCommunityMap>({});
  const [allCommunities, setAllCommunities] = useState(true);
  const [selectedCommunityKeys, setSelectedCommunityKeys] = useState<Set<string>>(
    () => new Set(),
  );
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setDraftColumns([]);
    setAddSource("");
    setLoadedCommunityMap({});
    setAllCommunities(true);
    setSelectedCommunityKeys(new Set());
    setLoading(true);
    getDisplayFormatDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || d.internalName || idOrName);
        setLabel(d.label || d.displayName || "");
        setDescription(d.description || "");
        const cols = reindexColumns(normalizeColumns(d.columns));
        setDraftColumns(cols);
        setAddSource(catalogFieldsNotInUse(cols)[0]?.source || "");
        const communities = normalizeAllowedCommunities(d.allowedCommunities);
        setLoadedCommunityMap(communities);
        setAllCommunities(isAllCommunities(communities));
        setSelectedCommunityKeys(new Set(Object.keys(communities)));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.DF_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedName = normalizeDisplayFormatName(detail?.name || detail?.internalName || idOrName || "");
  const loadedLabel = detail?.label || detail?.displayName || "";
  const loadedDescription = detail?.description || "";
  const dirty =
    isNew ||
    normalizeDisplayFormatName(name) !== loadedName ||
    label !== loadedLabel ||
    description !== loadedDescription;
  const canSave = !busy && dirty && isDisplayFormatWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeDisplayFormatName(name);
  const loadedColumns = useMemo(
    () => (detail != null ? reindexColumns(normalizeColumns(detail.columns)) : []),
    [detail],
  );
  const packaged = isPackagedDisplayFormat(idOrName || createdKey || name);
  const columns = packaged ? loadedColumns : draftColumns;
  const availableFields = catalogFieldsNotInUse(draftColumns);
  const columnsDirty = !packaged && !isNew && !columnsOrderEqual(draftColumns, loadedColumns);
  const canSaveColumns = !busy && columnsDirty && detail != null;
  const objectGuid = resolveDisplayFormatObjectGuid(detail, catalogGuid);
  const loadedCommunityKeys = useMemo(
    () => selectedKeysFromMap(loadedCommunityMap, communityCatalog),
    [loadedCommunityMap, communityCatalog],
  );
  const loadedAllCommunities = isAllCommunities(loadedCommunityMap);
  const communitiesDirty =
    !packaged &&
    !isNew &&
    (allCommunities !== loadedAllCommunities ||
      (!allCommunities &&
        (selectedCommunityKeys.size !== loadedCommunityKeys.size ||
          [...selectedCommunityKeys].some((k) => !loadedCommunityKeys.has(k)))));
  const canSaveCommunities =
    !busy && communitiesDirty && detail != null && (allCommunities || selectedCommunityKeys.size > 0);

  useEffect(() => {
    if (packaged || isNew) {
      return;
    }
    let cancelled = false;
    listCommunities()
      .then((rows) => {
        if (!cancelled) setCommunityCatalog(rows);
      })
      .catch(() => {
        if (!cancelled) setCommunityCatalog([]);
      });
    return () => {
      cancelled = true;
    };
  }, [packaged, isNew, idOrName]);

  useEffect(() => {
    if (communityCatalog.length === 0) {
      return;
    }
    setSelectedCommunityKeys(selectedKeysFromMap(loadedCommunityMap, communityCatalog));
  }, [communityCatalog, loadedCommunityMap]);

  function writeBody(): DisplayFormatWriteBody {
    return {
      name: isNew
        ? normalizeDisplayFormatName(name)
        : detail?.name || detail?.internalName || normalizeDisplayFormatName(name),
      label,
      description,
    };
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409 && isNew) return DEV_MSG.DF_DUPLICATE;
    if (isApiError(err) && err.status === 400) return DEV_MSG.DF_INVALID_NAME;
    if (isApiError(err) && err.status === 403) return DEV_MSG.DF_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.DF_NOT_FOUND;
    return DEV_MSG.DF_SAVE_ERROR;
  }

  function columnsSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return DEV_MSG.DF_COLUMNS_INVALID_SOURCE;
    if (isApiError(err) && err.status === 403) return DEV_MSG.DF_COLUMNS_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.DF_COLUMNS_NOT_FOUND;
    return DEV_MSG.DF_COLUMNS_SAVE_ERROR;
  }

  function communitiesSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return DEV_MSG.DF_COMMUNITIES_UNKNOWN;
    if (isApiError(err) && err.status === 403) return DEV_MSG.DF_COMMUNITIES_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.DF_NOT_FOUND;
    return DEV_MSG.DF_COMMUNITIES_SAVE_ERROR;
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
          ? await createDisplayFormat(writeBody())
          : await saveDisplayFormat(writeKey, writeBody());
      setDetail(saved);
      if (isNew) {
        setCreatedKey(saved.name || saved.internalName || normalizeDisplayFormatName(name));
      }
      setName(saved.name || saved.internalName || name);
      setLabel(saved.label || saved.displayName || "");
      setDescription(saved.description || "");
      const cols = reindexColumns(normalizeColumns(saved.columns));
      setDraftColumns(cols);
      setAddSource(catalogFieldsNotInUse(cols)[0]?.source || "");
      const communities = normalizeAllowedCommunities(saved.allowedCommunities);
      setLoadedCommunityMap(communities);
      setAllCommunities(isAllCommunities(communities));
      setSelectedCommunityKeys(selectedKeysFromMap(communities, communityCatalog));
      setNotice(DEV_MSG.DF_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, saveFallback(err)));
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
      await deleteDisplayFormat(writeKey);
      setNotice(DEV_MSG.DF_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.DF_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.DF_NOT_FOUND
            : isApiError(err) && err.status === 409
              ? DEV_MSG.DF_SAVE_ERROR
              : DEV_MSG.DF_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function handleAddColumn(): void {
    if (packaged || busy || isNew || !isValidColumnSource(addSource)) {
      return;
    }
    const next = addDisplayFormatColumn(draftColumns, addSource);
    setDraftColumns(next);
    const stillAvailable = catalogFieldsNotInUse(next);
    setAddSource(stillAvailable[0]?.source || "");
  }

  async function handleSaveColumns(): Promise<void> {
    if (!canSaveColumns || inflight.current || packaged || !writeKey) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateDisplayFormat(writeKey, {
        name: detail?.name || detail?.internalName || writeKey,
        label: detail?.label || detail?.displayName,
        displayName: detail?.displayName || detail?.label,
        description: detail?.description,
        columns: reindexColumns(draftColumns),
      });
      setDetail(saved);
      setDraftColumns(reindexColumns(normalizeColumns(saved.columns)));
      setNotice(DEV_MSG.DF_COLUMNS_SAVED);
      onColumnsSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, columnsSaveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function handleToggleAllCommunities(checked: boolean): void {
    if (packaged || busy || isNew) {
      return;
    }
    if (checked) {
      setAllCommunities(true);
      setSelectedCommunityKeys(new Set());
      return;
    }
    setAllCommunities(false);
  }

  function handleToggleCommunity(community: CommunitySummary, checked: boolean): void {
    if (packaged || busy || isNew) {
      return;
    }
    const key = communityWireKey(community);
    if (!key) {
      return;
    }
    setSelectedCommunityKeys((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(key);
      } else {
        next.delete(key);
      }
      if (next.size === 0) {
        setAllCommunities(true);
        return next;
      }
      setAllCommunities(false);
      return next;
    });
  }

  async function handleSaveCommunities(): Promise<void> {
    if (!canSaveCommunities || inflight.current || packaged || !writeKey) {
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateDisplayFormat(writeKey, {
        name: detail?.name || detail?.internalName || writeKey,
        label: detail?.label || detail?.displayName,
        displayName: detail?.displayName || detail?.label,
        description: detail?.description,
        allowedCommunities: toAllowedCommunitiesWriteBody(
          allCommunities,
          communityCatalog,
          selectedCommunityKeys,
        ),
      });
      setDetail(saved);
      const communities = normalizeAllowedCommunities(saved.allowedCommunities);
      setLoadedCommunityMap(communities);
      setAllCommunities(isAllCommunities(communities));
      setSelectedCommunityKeys(selectedKeysFromMap(communities, communityCatalog));
      setNotice(DEV_MSG.DF_COMMUNITIES_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, communitiesSaveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.DF_NEW
    : detail?.label || detail?.displayName || detail?.name || idOrName || DEV_MSG.DF_EDIT;

  return (
    <div data-testid="developer-df-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-df-back"
        aria-label={DEV_MSG.DF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.DF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-df-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-df-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-df-detail-loading">{DEV_MSG.DF_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-df-detail-title">
              {title}
            </h2>
            {!isNew && detail?.description && !dirty ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            {!isNew && detail ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.DF_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-df-detail-guid"
                >
                  {objectGuid || "—"}
                </dd>
                <dt>{DEV_MSG.DF_COL_USAGE}</dt>
                <dd style={{ margin: 0 }}>
                  {[
                    detail.validForFolder ? DEV_MSG.DF_USAGE_FOLDER : null,
                    detail.validForViewsAndSearches ? DEV_MSG.DF_USAGE_VIEWS : null,
                    detail.validForRelatedContent ? DEV_MSG.DF_USAGE_RELATED : null,
                  ]
                    .filter(Boolean)
                    .join(", ") || "—"}
                </dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="df-name">{DEV_MSG.DF_FORM_NAME}</label>
            <input
              id="df-name"
              data-testid="developer-df-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.DF_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.DF_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="df-label">{DEV_MSG.DF_FORM_LABEL}</label>
            <input
              id="df-label"
              data-testid="developer-df-label"
              style={inputStyle}
              value={label}
              disabled={busy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="df-desc">{DEV_MSG.DF_FORM_DESCRIPTION}</label>
            <input
              id="df-desc"
              data-testid="developer-df-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-df-save"
              aria-label={DEV_MSG.DF_SAVE}
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
              {DEV_MSG.DF_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-df-cancel"
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
              {DEV_MSG.DF_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-df-delete"
                aria-label={DEV_MSG.DF_DELETE}
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
                {DEV_MSG.DF_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section data-testid="developer-df-columns">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_COLUMNS}</h3>
                <p
                  style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
                  data-testid={packaged ? "developer-df-columns-readonly" : undefined}
                >
                  {packaged ? DEV_MSG.DF_COLUMNS_READONLY : DEV_MSG.DF_COLUMNS_HINT}
                </p>
                {columns.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-df-columns-empty">
                    {DEV_MSG.DF_NONE}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-df-columns-table"
                      style={{
                        width: "100%",
                        borderCollapse: "collapse",
                        fontSize: "0.9rem",
                      }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_POS}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_SOURCE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_COL_LABEL}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_RENDER}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_WIDTH}</th>
                          {!packaged ? (
                            <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_ACTIONS}</th>
                          ) : null}
                        </tr>
                      </thead>
                      <tbody>
                        {columns.map((c, i) => (
                          <tr
                            key={`${c.source ?? "col"}-${c.position ?? i}-${i}`}
                            data-testid={`developer-df-column-row-${i}`}
                            data-df-column-source={c.source || ""}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px" }}>
                              {c.position != null ? String(c.position) : "—"}
                            </td>
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {c.source || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{c.displayName || "—"}</td>
                            <td style={{ padding: "8px" }}>{c.renderType || "—"}</td>
                            <td style={{ padding: "8px" }}>
                              {c.width != null && c.width > 0 ? String(c.width) : "—"}
                            </td>
                            {!packaged ? (
                              <td style={{ padding: "8px" }}>
                                <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                                  <button
                                    type="button"
                                    data-testid={`developer-df-column-up-${i}`}
                                    aria-label={DEV_MSG.DF_COLUMNS_MOVE_UP}
                                    disabled={busy || i === 0}
                                    onClick={() =>
                                      setDraftColumns(moveDisplayFormatColumn(draftColumns, i, -1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.DF_COLUMNS_MOVE_UP}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-df-column-down-${i}`}
                                    aria-label={DEV_MSG.DF_COLUMNS_MOVE_DOWN}
                                    disabled={busy || i === columns.length - 1}
                                    onClick={() =>
                                      setDraftColumns(moveDisplayFormatColumn(draftColumns, i, 1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.DF_COLUMNS_MOVE_DOWN}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-df-column-remove-${i}`}
                                    aria-label={DEV_MSG.DF_COLUMNS_REMOVE}
                                    disabled={busy || isSysTitleColumn(c)}
                                    onClick={() =>
                                      setDraftColumns(removeDisplayFormatColumn(draftColumns, i))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.DF_COLUMNS_REMOVE}
                                  </button>
                                </div>
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                {!packaged ? (
                  <div
                    style={{
                      marginTop: "12px",
                      display: "flex",
                      gap: "8px",
                      flexWrap: "wrap",
                      alignItems: "flex-end",
                    }}
                    data-testid="developer-df-column-editor"
                  >
                    <label
                      htmlFor="df-column-source"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.DF_COLUMNS_SOURCE_PICKER}
                      <select
                        id="df-column-source"
                        data-testid="developer-df-column-source"
                        style={inputStyle}
                        value={addSource}
                        disabled={busy || availableFields.length === 0}
                        onChange={(e) => setAddSource(e.target.value)}
                      >
                        <option value="">{availableFields.length ? "—" : DEV_MSG.DF_NONE}</option>
                        {availableFields.map((f) => (
                          <option key={f.source} value={f.source}>
                            {f.label} ({f.source})
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-df-column-add"
                      aria-label={DEV_MSG.DF_COLUMNS_ADD}
                      disabled={busy || !isValidColumnSource(addSource)}
                      onClick={handleAddColumn}
                      style={{
                        ...actionButton,
                        padding: "8px 12px",
                        background: isValidColumnSource(addSource)
                          ? catalogColors.accent
                          : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        cursor:
                          isValidColumnSource(addSource) && !busy ? "pointer" : "not-allowed",
                      }}
                    >
                      {DEV_MSG.DF_COLUMNS_ADD}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-df-columns-save"
                      aria-label={DEV_MSG.DF_COLUMNS_SAVE}
                      disabled={!canSaveColumns}
                      onClick={() => void handleSaveColumns()}
                      style={{
                        padding: "8px 16px",
                        background: canSaveColumns ? catalogColors.accent : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: canSaveColumns ? "pointer" : "not-allowed",
                      }}
                    >
                      {busy ? DEV_MSG.DF_COLUMNS_SAVING : DEV_MSG.DF_COLUMNS_SAVE}
                    </button>
                  </div>
                ) : null}
              </section>

              <section style={{ marginTop: "16px" }} data-testid="developer-df-communities">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_COMMUNITIES}</h3>
                <p
                  style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
                  data-testid={packaged ? "developer-df-communities-readonly" : undefined}
                >
                  {packaged ? DEV_MSG.DF_COMMUNITIES_READONLY : DEV_MSG.DF_COMMUNITIES_HINT}
                </p>
                {packaged ? (
                  <p style={{ fontSize: "0.9rem" }} data-testid="developer-df-communities-all-label">
                    {DEV_MSG.DF_COMMUNITIES_ALL}
                  </p>
                ) : (
                  <div data-testid="developer-df-community-editor">
                    <label
                      style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}
                    >
                      <input
                        type="checkbox"
                        data-testid="developer-df-communities-all"
                        checked={allCommunities}
                        disabled={busy}
                        onChange={(e) => handleToggleAllCommunities(e.target.checked)}
                      />
                      {DEV_MSG.DF_COMMUNITIES_ALL}
                    </label>
                    <ul style={{ listStyle: "none", margin: 0, padding: 0 }}>
                      {communityCatalog.map((c, i) => {
                        const key = communityWireKey(c) || `comm-${i}`;
                        const checked =
                          !allCommunities &&
                          [...selectedCommunityKeys].some(
                            (k) => k === key || k === c.name || (c.id != null && k === String(c.id)),
                          );
                        return (
                          <li key={key} style={{ marginBottom: "4px" }}>
                            <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                              <input
                                type="checkbox"
                                data-testid={`developer-df-community-${c.name || key}`}
                                data-community-key={key}
                                checked={checked}
                                disabled={busy || allCommunities}
                                onChange={(e) => handleToggleCommunity(c, e.target.checked)}
                              />
                              {c.label || c.name || key}
                            </label>
                          </li>
                        );
                      })}
                    </ul>
                    <button
                      type="button"
                      data-testid="developer-df-communities-save"
                      aria-label={DEV_MSG.DF_COMMUNITIES_SAVE}
                      disabled={!canSaveCommunities}
                      onClick={() => void handleSaveCommunities()}
                      style={{
                        marginTop: "12px",
                        padding: "8px 16px",
                        background: canSaveCommunities ? catalogColors.accent : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: canSaveCommunities ? "pointer" : "not-allowed",
                      }}
                    >
                      {busy ? DEV_MSG.DF_COMMUNITIES_SAVING : DEV_MSG.DF_COMMUNITIES_SAVE}
                    </button>
                  </div>
                )}
              </section>

              <ObjectAclSection
                objectGuid={objectGuid}
                objectKind="display-format"
                testIdPrefix="developer-df-acl"
              />

              <section style={{ marginTop: "16px" }} data-testid="developer-df-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_GAPS}</h3>
                <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  <li>{DEV_MSG.DF_GAP_COLUMNS_EDIT}</li>
                </ul>
              </section>
            </>
          ) : null}
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.DF_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
