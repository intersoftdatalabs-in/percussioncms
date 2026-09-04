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

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { isApiError } from "../api/client";
import {
  asCommunityNewSearchRefs,
  communityGuidForWrite,
  createCommunity,
  deleteCommunity,
  getCommunityDetail,
  getCommunityNewSearchDefaults,
  getCommunityVisibility,
  isCommunityWriteReady,
  listAvailableRoles,
  replaceCommunityNewSearchDefaults,
  updateCommunityRoles,
} from "../api/developer/assemblyApi";
import { listSearches } from "../api/developer/searchesApi";
import type {
  CommunityDetail,
  CommunityNewSearchRef,
  CommunityRoleSummary,
  CommunitySummary,
  CommunityVisibleObject,
  RestGuid,
  SearchDef,
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
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import {
  collectSearchRefKeys,
  mergeSearchPickerRows,
  searchRefIsSelected,
  searchRefPrimaryKey,
  selectedPickerPrimaryKeys,
  sameSearchKeySet,
  toNewSearchWriteRefs,
  toggleSearchRefSelection,
} from "./communityNewSearchDefaults";
import {
  COMMUNITY_VISIBILITY_TYPE_OPTIONS,
  filterVisibleObjects,
  visibilityEmptyKind,
  visibilitySummaryCounts,
} from "./communityVisibilityFilters";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const filterControlStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  minWidth: "12rem",
};

const successNoticeStyle: React.CSSProperties = {
  color: "#276749",
  background: "#f0fff4",
  border: "1px solid #9ae6b4",
  borderRadius: "4px",
  padding: "8px 12px",
  marginBottom: "8px",
};

const dirtyNoticeStyle: React.CSSProperties = {
  color: "#744210",
  background: "#fffff0",
  border: "1px solid #f6e05e",
  borderRadius: "4px",
  padding: "8px 12px",
  marginBottom: "8px",
  fontSize: "0.9rem",
};

function asRoles(detail: CommunityDetail | null): CommunityRoleSummary[] {
  if (!detail?.roleList) return [];
  if (Array.isArray(detail.roleList)) return detail.roleList;
  const env = detail.roleList as {
    CommunityRole?: CommunityRoleSummary[] | CommunityRoleSummary;
    roleName?: string;
    roleId?: number;
    roleGuid?: CommunityRoleSummary["roleGuid"];
  };
  if (Array.isArray(env.CommunityRole)) return env.CommunityRole;
  if (env.CommunityRole && typeof env.CommunityRole === "object") {
    return [env.CommunityRole];
  }
  // Jackson/JAXB one-item list: roleList is a bare CommunityRole object.
  if (env.roleName != null || env.roleId != null || env.roleGuid != null) {
    return [env as CommunityRoleSummary];
  }
  return [];
}

function roleKey(r: CommunityRoleSummary, index = 0): string {
  if (r.roleGuid?.stringValue) return r.roleGuid.stringValue;
  if (r.roleId != null) return `id:${r.roleId}`;
  if (r.roleName) return `name:${r.roleName}`;
  // Stable synthetic key so checkbox rows remain selectable even without ids
  return `role-idx:${index}`;
}

function formatSavedRolesNotice(roleCount: number): string {
  return DEV_MSG.COMM_ROLES_SAVED_COUNT.replace("{0}", String(roleCount));
}

function formatVisibilitySummary(shown: number, total: number, filtered: boolean): string {
  if (filtered) {
    return DEV_MSG.COMM_VISIBILITY_SUMMARY.replace("{0}", String(shown)).replace(
      "{1}",
      String(total),
    );
  }
  return DEV_MSG.COMM_VISIBILITY_SUMMARY_ALL.replace("{0}", String(total));
}

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

function createFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.COMM_SAVE_ERROR;
  if (err.status === 409) return DEV_MSG.COMM_DUPLICATE;
  if (err.status === 403) return DEV_MSG.COMM_FORBIDDEN;
  if (err.status === 400) return DEV_MSG.COMM_NAME_INVALID;
  return DEV_MSG.COMM_SAVE_ERROR;
}

function deleteFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.COMM_DELETE_ERROR;
  if (err.status === 403) return DEV_MSG.COMM_FORBIDDEN;
  if (err.status === 409) return DEV_MSG.COMM_IN_USE;
  if (err.status === 404) return DEV_MSG.COMM_MISSING;
  return DEV_MSG.COMM_DELETE_ERROR;
}

function nsdLoadFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.COMM_NSD_ERROR;
  if (err.status === 403) return DEV_MSG.COMM_FORBIDDEN;
  if (err.status === 404) return DEV_MSG.COMM_MISSING;
  return DEV_MSG.COMM_NSD_ERROR;
}

function nsdSaveFallback(err: unknown): string {
  if (!isApiError(err)) return DEV_MSG.COMM_NSD_SAVE_ERROR;
  if (err.status === 403) return DEV_MSG.COMM_FORBIDDEN;
  if (err.status === 400) return DEV_MSG.COMM_NSD_UNKNOWN_SEARCH;
  if (err.status === 409) return DEV_MSG.COMM_NSD_LOCK;
  if (err.status === 404) return DEV_MSG.COMM_MISSING;
  return DEV_MSG.COMM_NSD_SAVE_ERROR;
}

function formatSavedNsdNotice(searchCount: number): string {
  if (searchCount === 0) {
    return DEV_MSG.COMM_NSD_CLEARED;
  }
  return DEV_MSG.COMM_NSD_SAVED_COUNT.replace("{0}", String(searchCount));
}

function resolveCommunityGuid(
  detail: CommunityDetail | null,
  fallback?: RestGuid | null,
): RestGuid | null {
  return communityGuidForWrite(detail, fallback);
}

export function CommunityDetailPanel({
  idOrName,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  onBack: () => void;
  onSaved?: (detail: CommunityDetail | CommunitySummary) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<CommunityDetail | null>(null);
  const [name, setName] = useState("");
  const [allRoles, setAllRoles] = useState<CommunityRoleSummary[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [visibleObjects, setVisibleObjects] = useState<CommunityVisibleObject[]>([]);
  const [visibilityLoading, setVisibilityLoading] = useState(false);
  const [visibilityError, setVisibilityError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState("");
  const [nameFilter, setNameFilter] = useState("");
  const [communityGuid, setCommunityGuid] = useState<RestGuid | null>(null);
  /** Monotonic id so stale visibility responses (type filter / remount) are ignored. */
  const visibilityReqId = useRef(0);
  const nsdReqId = useRef(0);
  const [searchCatalog, setSearchCatalog] = useState<CommunityNewSearchRef[]>([]);
  const [nsdSelectedKeys, setNsdSelectedKeys] = useState<Set<string>>(new Set());
  const [nsdInitialKeys, setNsdInitialKeys] = useState<Set<string>>(new Set());
  const [nsdLoading, setNsdLoading] = useState(false);
  const [nsdError, setNsdError] = useState<string | null>(null);
  const inflight = useRef(false);

  const applyNewSearchDefaults = useCallback(
    (assigned: CommunityNewSearchRef[], catalog: SearchDef[]) => {
      const picker = mergeSearchPickerRows(catalog, assigned);
      const selected = collectSearchRefKeys(assigned);
      setSearchCatalog(picker);
      setNsdSelectedKeys(selected);
      setNsdInitialKeys(selectedPickerPrimaryKeys(picker, selected));
    },
    [],
  );

  const loadNewSearchDefaults = useCallback(
    (key: string) => {
      const req = ++nsdReqId.current;
      setNsdLoading(true);
      setNsdError(null);
      Promise.all([getCommunityNewSearchDefaults(key), listSearches()])
        .then(([defaults, catalog]) => {
          if (req !== nsdReqId.current) return;
          applyNewSearchDefaults(asCommunityNewSearchRefs(defaults.searches), catalog);
          setNsdLoading(false);
        })
        .catch((err: unknown) => {
          if (req !== nsdReqId.current) return;
          setNsdLoading(false);
          setSearchCatalog([]);
          setNsdSelectedKeys(new Set());
          setNsdInitialKeys(new Set());
          setNsdError(panelErrMsg(err, nsdLoadFallback(err)));
        });
    },
    [applyNewSearchDefaults],
  );

  const loadVisibility = useCallback((guid: RestGuid, objectType: string) => {
    const req = ++visibilityReqId.current;
    setVisibilityLoading(true);
    setVisibilityError(null);
    const typeArg = objectType.trim() || undefined;
    getCommunityVisibility(guid, typeArg)
      .then((objs) => {
        if (req !== visibilityReqId.current) return;
        setVisibleObjects(objs);
        setVisibilityLoading(false);
      })
      .catch((err: unknown) => {
        if (req !== visibilityReqId.current) return;
        setVisibilityLoading(false);
        setVisibleObjects([]);
        setVisibilityError(panelErrMsg(err, DEV_MSG.COMM_VISIBILITY_ERROR));
      });
  }, []);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    // Invalidate any in-flight visibility from a prior community / type filter.
    visibilityReqId.current += 1;
    setDetail(null);
    setError(null);
    setNotice(null);
    setVisibleObjects([]);
    setVisibilityError(null);
    setCommunityGuid(null);
    setTypeFilter("");
    setNameFilter("");
    nsdReqId.current += 1;
    setSearchCatalog([]);
    setNsdSelectedKeys(new Set());
    setNsdInitialKeys(new Set());
    setNsdError(null);
    setNsdLoading(true);
    Promise.all([getCommunityDetail(idOrName), listAvailableRoles()])
      .then(([d, roles]) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || idOrName);
        setAllRoles(roles);
        setSelectedKeys(
          new Set(asRoles(d).map((r, i) => roleKey(r, i)).filter((k) => k.length > 0)),
        );
        const g = communityGuidForWrite(d);
        if (g) {
          setCommunityGuid(g);
          const req = ++visibilityReqId.current;
          setVisibilityLoading(true);
          getCommunityVisibility(g)
            .then((objs) => {
              if (cancelled || req !== visibilityReqId.current) return;
              setVisibleObjects(objs);
              setVisibilityLoading(false);
            })
            .catch((err: unknown) => {
              if (cancelled || req !== visibilityReqId.current) return;
              setVisibilityLoading(false);
              setVisibilityError(panelErrMsg(err, DEV_MSG.COMM_VISIBILITY_ERROR));
            });
        } else {
          setVisibilityError(DEV_MSG.COMM_VISIBILITY_NO_GUID);
        }
        loadNewSearchDefaults(d.name || idOrName);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setNsdLoading(false);
        setError(panelErrMsg(err, DEV_MSG.COMM_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
      visibilityReqId.current += 1;
      nsdReqId.current += 1;
    };
  }, [idOrName, loadNewSearchDefaults]);

  const initialKeys = useMemo(
    () =>
      new Set(
        asRoles(detail)
          .map((r, i) => roleKey(r, i))
          .filter((k) => k.length > 0),
      ),
    [detail],
  );
  const dirty =
    selectedKeys.size !== initialKeys.size ||
    [...selectedKeys].some((k) => !initialKeys.has(k));
  const nsdDirty = !sameSearchKeySet(
    selectedPickerPrimaryKeys(searchCatalog, nsdSelectedKeys),
    nsdInitialKeys,
  );

  const displayedObjects = useMemo(
    () => filterVisibleObjects(visibleObjects, nameFilter),
    [visibleObjects, nameFilter],
  );
  const emptyKind = visibilityEmptyKind(
    visibleObjects.length,
    displayedObjects.length,
    typeFilter,
    nameFilter,
  );
  const summary = visibilitySummaryCounts(visibleObjects.length, displayedObjects.length);

  function toggle(key: string) {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
    setNotice(null);
  }

  function handleTypeFilterChange(next: string) {
    setTypeFilter(next);
    setNameFilter("");
    if (communityGuid) {
      loadVisibility(communityGuid, next);
    }
  }

  const writeKey = idOrName || createdKey || name.trim();
  const canCreate = isNew && !busy && isCommunityWriteReady({ name });

  async function applyLoadedCommunity(
    d: CommunityDetail,
    roles: CommunityRoleSummary[],
    key: string,
  ) {
    setDetail(d);
    setName(d.name || key);
    setAllRoles(roles);
    setSelectedKeys(
      new Set(asRoles(d).map((r, i) => roleKey(r, i)).filter((k) => k.length > 0)),
    );
    const g = communityGuidForWrite(d);
    if (g) {
      setCommunityGuid(g);
      const req = ++visibilityReqId.current;
      setVisibilityLoading(true);
      setVisibilityError(null);
      try {
        const objs = await getCommunityVisibility(g);
        if (req !== visibilityReqId.current) return;
        setVisibleObjects(objs);
        setVisibilityLoading(false);
      } catch (err: unknown) {
        if (req !== visibilityReqId.current) return;
        setVisibilityLoading(false);
        setVisibilityError(panelErrMsg(err, DEV_MSG.COMM_VISIBILITY_ERROR));
      }
    } else {
      setVisibilityError(DEV_MSG.COMM_VISIBILITY_NO_GUID);
    }
    loadNewSearchDefaults(d.name || key);
  }

  async function handleCreate(): Promise<void> {
    if (!canCreate || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const created = await createCommunity(name);
      const key = created.name || name.trim();
      setCreatedKey(key);
      setName(key);
      try {
        const [d, roles] = await Promise.all([getCommunityDetail(key), listAvailableRoles()]);
        const merged: CommunityDetail = {
          ...d,
          guid: d.guid ?? created.guid,
          id: d.id ?? created.id,
          name: d.name || created.name || key,
        };
        await applyLoadedCommunity(merged, roles, key);
      } catch {
        setDetail(created as CommunityDetail);
        setCommunityGuid(communityGuidForWrite(created));
        loadNewSearchDefaults(key);
      }
      setNotice(DEV_MSG.COMM_CREATED);
      onSaved?.(created);
    } catch (err: unknown) {
      setError(panelErrMsg(err, createFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || inflight.current) return;
    const guid = resolveCommunityGuid(detail, communityGuid);
    if (!guid) {
      setError(DEV_MSG.COMM_DELETE_NO_GUID);
      return;
    }
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || inflight.current) return;
    const guid = resolveCommunityGuid(detail, communityGuid);
    if (!guid) {
      setError(DEV_MSG.COMM_DELETE_NO_GUID);
      return;
    }
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteCommunity(guid, false);
      setNotice(DEV_MSG.COMM_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      setError(panelErrMsg(err, deleteFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleSave() {
    if (!writeKey || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    const body = allRoles.filter((r, i) => selectedKeys.has(roleKey(r, i)));
    try {
      const saved = await updateCommunityRoles(writeKey, body);
      setDetail(saved);
      const nextRoles = asRoles(saved);
      setSelectedKeys(
        new Set(nextRoles.map((r, i) => roleKey(r, i)).filter((k) => k.length > 0)),
      );
      setNotice(formatSavedRolesNotice(nextRoles.length));
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.COMM_ROLES_SAVE_ERROR));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function toggleNsd(row: CommunityNewSearchRef) {
    setNsdSelectedKeys((prev) => toggleSearchRefSelection(row, prev));
    setNotice(null);
  }

  async function handleSaveNsd() {
    if (!writeKey || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    setNsdError(null);
    const body = toNewSearchWriteRefs(searchCatalog, nsdSelectedKeys);
    try {
      const saved = await replaceCommunityNewSearchDefaults(writeKey, body);
      const assigned = asCommunityNewSearchRefs(saved.searches);
      const selected = collectSearchRefKeys(assigned);
      setNsdSelectedKeys(selected);
      setNsdInitialKeys(selectedPickerPrimaryKeys(searchCatalog, selected));
      setNotice(formatSavedNsdNotice(assigned.length));
    } catch (err: unknown) {
      setError(panelErrMsg(err, nsdSaveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const rolesForTable = allRoles.length > 0 ? allRoles : asRoles(detail);
  const guidLabel = communityGuidForWrite(detail)?.stringValue;

  return (
    <div data-testid="developer-comm-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-comm-back"
        aria-label="Back to communities list"
        style={backButton}
      >
        ← {DEV_MSG.COMM_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-comm-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div
          role="status"
          data-testid="developer-comm-detail-notice"
          style={successNoticeStyle}
        >
          {notice}
        </div>
      ) : null}

      {isNew ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-comm-detail-title">
              {DEV_MSG.COMM_NEW}
            </h2>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.COMM_NAME_HINT}
            </p>
          </header>
          <div style={fieldStyle}>
            <label htmlFor="comm-name">{DEV_MSG.COMM_FORM_NAME}</label>
            <input
              id="comm-name"
              data-testid="developer-comm-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-comm-create"
              aria-label={DEV_MSG.COMM_CREATE}
              disabled={!canCreate}
              onClick={() => void handleCreate()}
              style={{
                padding: "8px 16px",
                background: canCreate ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canCreate ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.COMM_CREATE}
            </button>
            <button
              type="button"
              data-testid="developer-comm-cancel"
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
              {DEV_MSG.COMM_CANCEL}
            </button>
          </div>
        </>
      ) : null}

      {!isNew && !error && detail == null ? (
        <div data-testid="developer-comm-detail-loading">
          {DEV_MSG.COMM_DETAIL_LOADING}
        </div>
      ) : null}

      {!isNew && detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-comm-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
              {detail.name}
              {detail.id != null ? ` · id ${detail.id}` : ""}
              {guidLabel ? ` · ${guidLabel}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.text }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.COMM_META_ID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.id != null ? String(detail.id) : "—"}
              </dd>
              <dt>{DEV_MSG.COMM_META_GUID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {guidLabel || "—"}
              </dd>
            </dl>
            <div style={{ marginTop: "12px" }}>
              <button
                type="button"
                data-testid="developer-comm-delete"
                aria-label={DEV_MSG.COMM_DELETE}
                disabled={busy}
                onClick={requestDelete}
                style={{
                  padding: "8px 16px",
                  background: "#c53030",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy ? "wait" : "pointer",
                }}
              >
                {DEV_MSG.COMM_DELETE}
              </button>
            </div>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-comm-roles">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_ROLES}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.COMM_ROLES_HINT}</p>
            {dirty ? (
              <div data-testid="developer-comm-roles-dirty" style={dirtyNoticeStyle}>
                {DEV_MSG.COMM_ROLES_DIRTY}
              </div>
            ) : null}
            {rolesForTable.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>
                {allRoles.length === 0 && asRoles(detail).length === 0
                  ? DEV_MSG.COMM_NO_ROLES_SYSTEM
                  : DEV_MSG.COMM_NO_ROLES}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-comm-roles-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_MEMBER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ROLE_ID}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rolesForTable.map((r, i) => {
                      const key = roleKey(r, i);
                      const checked = selectedKeys.has(key);
                      return (
                        <tr key={key} style={tableRow}>
                          <td style={{ padding: "8px" }}>
                            <input
                              type="checkbox"
                              data-testid={`developer-comm-role-check-${key}`}
                              checked={checked}
                              onChange={() => toggle(key)}
                              aria-label={`Include role ${r.roleName || key}`}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>{r.roleName || "—"}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {r.roleId != null ? String(r.roleId) : "—"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <div style={{ marginTop: "12px", display: "flex", gap: "8px", alignItems: "center" }}>
              <button
                type="button"
                data-testid="developer-comm-roles-save"
                aria-label="Save community roles"
                disabled={busy || !dirty}
                onClick={() => void handleSave()}
                style={{
                  padding: "8px 16px",
                  background: dirty ? catalogColors.accent : catalogColors.disabled,
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy || !dirty ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.COMM_ROLES_SAVE}
              </button>
            </div>
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-comm-nsd">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_NSD}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.COMM_NSD_HINT}
            </p>
            {nsdDirty ? (
              <div data-testid="developer-comm-nsd-dirty" style={dirtyNoticeStyle}>
                {DEV_MSG.COMM_NSD_DIRTY}
              </div>
            ) : null}
            {nsdLoading ? (
              <div data-testid="developer-comm-nsd-loading">{DEV_MSG.COMM_NSD_LOADING}</div>
            ) : null}
            {nsdError ? (
              <div
                role="alert"
                data-testid="developer-comm-nsd-error"
                style={errorAlert}
              >
                {nsdError}
              </div>
            ) : null}
            {!nsdLoading && !nsdError && searchCatalog.length === 0 ? (
              <p data-testid="developer-comm-nsd-empty" style={{ color: catalogColors.empty }}>
                {DEV_MSG.COMM_NSD_EMPTY}
              </p>
            ) : null}
            {!nsdLoading && !nsdError && searchCatalog.length > 0 ? (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-comm-nsd-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_NSD_COL_INCLUDE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_NSD_COL_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_NSD_COL_LABEL}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {searchCatalog.map((row, i) => {
                      const key = searchRefPrimaryKey(row) || `nsd-idx:${i}`;
                      const checked = searchRefIsSelected(row, nsdSelectedKeys);
                      return (
                        <tr key={key} style={tableRow}>
                          <td style={{ padding: "8px" }}>
                            <input
                              type="checkbox"
                              data-testid={`developer-comm-nsd-check-${key}`}
                              checked={checked}
                              onChange={() => toggleNsd(row)}
                              aria-label={`Include search ${row.name || row.label || key}`}
                            />
                          </td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {row.name || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>{row.label || "—"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : null}
            <div style={{ marginTop: "12px", display: "flex", gap: "8px", alignItems: "center" }}>
              <button
                type="button"
                data-testid="developer-comm-nsd-save"
                aria-label={DEV_MSG.COMM_NSD_SAVE}
                disabled={busy || nsdLoading || !nsdDirty}
                onClick={() => void handleSaveNsd()}
                style={{
                  padding: "8px 16px",
                  background: nsdDirty ? catalogColors.accent : catalogColors.disabled,
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy || nsdLoading || !nsdDirty ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.COMM_NSD_SAVE}
              </button>
            </div>
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-comm-visibility">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_VISIBILITY}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.COMM_VISIBILITY_HINT}
            </p>

            <div
              data-testid="developer-comm-visibility-filters"
              style={{
                display: "flex",
                flexWrap: "wrap",
                gap: "12px",
                marginBottom: "12px",
                alignItems: "flex-end",
              }}
            >
              <label style={{ display: "flex", flexDirection: "column", gap: "4px", fontSize: "0.9rem" }}>
                <span>{DEV_MSG.COMM_VISIBILITY_TYPE_LABEL}</span>
                <select
                  data-testid="developer-comm-visibility-type-filter"
                  value={typeFilter}
                  disabled={!communityGuid || visibilityLoading}
                  onChange={(e) => handleTypeFilterChange(e.target.value)}
                  style={filterControlStyle}
                  aria-label={DEV_MSG.COMM_VISIBILITY_TYPE_LABEL}
                >
                  {COMMUNITY_VISIBILITY_TYPE_OPTIONS.map((opt) => (
                    <option key={opt.value || "all"} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ display: "flex", flexDirection: "column", gap: "4px", fontSize: "0.9rem", flex: "1 1 12rem" }}>
                <span>{DEV_MSG.COMM_VISIBILITY_NAME_LABEL}</span>
                <input
                  type="search"
                  data-testid="developer-comm-visibility-name-filter"
                  value={nameFilter}
                  disabled={visibilityLoading}
                  placeholder={DEV_MSG.COMM_VISIBILITY_NAME_PLACEHOLDER}
                  onChange={(e) => setNameFilter(e.target.value)}
                  style={{ ...filterControlStyle, width: "100%", boxSizing: "border-box" }}
                  aria-label={DEV_MSG.COMM_VISIBILITY_NAME_LABEL}
                />
              </label>
            </div>

            {visibilityLoading ? (
              <div data-testid="developer-comm-visibility-loading">
                {DEV_MSG.COMM_VISIBILITY_LOADING}
              </div>
            ) : null}
            {visibilityError ? (
              <div
                role="alert"
                data-testid="developer-comm-visibility-error"
                style={errorAlert}
              >
                {visibilityError}
              </div>
            ) : null}

            {!visibilityLoading && !visibilityError && emptyKind === "none" ? (
              <p data-testid="developer-comm-visibility-empty" style={{ color: catalogColors.empty }}>
                {DEV_MSG.COMM_VISIBILITY_EMPTY}
              </p>
            ) : null}
            {!visibilityLoading && !visibilityError && emptyKind === "type-filter" ? (
              <p
                data-testid="developer-comm-visibility-empty-type"
                style={{ color: catalogColors.empty }}
              >
                {DEV_MSG.COMM_VISIBILITY_EMPTY_TYPE}
              </p>
            ) : null}
            {!visibilityLoading && !visibilityError && emptyKind === "name-filter" ? (
              <p
                data-testid="developer-comm-visibility-empty-name"
                style={{ color: catalogColors.empty }}
              >
                {DEV_MSG.COMM_VISIBILITY_EMPTY_NAME}
              </p>
            ) : null}

            {!visibilityLoading && !visibilityError && visibleObjects.length > 0 ? (
              <p
                data-testid="developer-comm-visibility-summary"
                style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}
              >
                {formatVisibilitySummary(summary.shown, summary.total, summary.filtered)}
              </p>
            ) : null}

            {displayedObjects.length > 0 ? (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-comm-visibility-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_OBJ_TYPE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_OBJ_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_OBJ_LABEL}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_OBJ_GUID}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {displayedObjects.map((o, i) => (
                      <tr
                        key={
                          o.guid?.stringValue ||
                          `${o.type || "obj"}:${o.name || o.id || i}`
                        }
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {o.type || "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {o.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{o.label || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace", fontSize: "0.85rem" }}>
                          {o.guid?.stringValue ||
                            (o.id != null ? String(o.id) : "—")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>

          <section data-testid="developer-comm-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.COMM_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              <li>{DEV_MSG.COMM_GAP_ACL}</li>
            </ul>
          </section>
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.COMM_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
