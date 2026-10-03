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
import { isApiError } from "../api/client";
import {
  browseRoles,
  createRole,
  isRoleCreateReady,
  ROLE_BROWSE_GROUPS,
  rolesInBrowseGroup,
  type RoleBrowseEntry,
  type RoleBrowseGroupKey,
} from "../api/developer/rolesApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, errorAlert, monoCell, mutedCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function groupLabel(group: RoleBrowseGroupKey): string {
  switch (group) {
    case "community":
      return DEV_MSG.ROLES_GROUP_COMMUNITY;
    case "workflow":
      return DEV_MSG.ROLES_GROUP_WORKFLOW;
    case "unassigned":
      return DEV_MSG.ROLES_GROUP_UNASSIGNED;
  }
}

const filterChipStyle = (active: boolean): React.CSSProperties => ({
  padding: "6px 12px",
  borderRadius: "4px",
  border: `1px solid ${active ? catalogColors.accent : catalogColors.softBorder}`,
  background: active ? catalogColors.accent : catalogColors.surface,
  color: active ? "#fff" : catalogColors.text,
  cursor: "pointer",
  font: "inherit",
  fontWeight: active ? 600 : 400,
});

const groupHeaderStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "8px",
  width: "100%",
  textAlign: "left",
  padding: "8px 10px",
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: "4px",
  background: "#f7fafc",
  cursor: "pointer",
  font: "inherit",
  fontWeight: 600,
  color: catalogColors.text,
};

function joinNames(names: string[]): string {
  return names.length > 0 ? names.join(", ") : "—";
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

function createFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.ROLES_CREATE_FORBIDDEN);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.ROLES_CREATE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.ROLES_CREATE_ERROR);
}

function RoleGroupSection({
  group,
  roles,
  expanded,
  onToggle,
}: {
  group: RoleBrowseGroupKey;
  roles: RoleBrowseEntry[];
  expanded: boolean;
  onToggle: () => void;
}): React.ReactElement {
  const label = groupLabel(group);
  return (
    <section
      data-testid={`developer-roles-group-${group}`}
      style={{ marginBottom: "16px" }}
    >
      <button
        type="button"
        data-testid={`developer-roles-group-toggle-${group}`}
        aria-expanded={expanded}
        onClick={onToggle}
        style={groupHeaderStyle}
      >
        <span aria-hidden="true">{expanded ? "▾" : "▸"}</span>
        <span>
          {label} ({roles.length})
        </span>
      </button>
      {expanded ? (
        roles.length === 0 ? (
          <CatalogStatus testId={`developer-roles-group-empty-${group}`}>
            {DEV_MSG.ROLES_GROUP_EMPTY}
          </CatalogStatus>
        ) : (
          <div style={{ marginTop: "8px" }}>
            <SimpleCatalogTable
              tableTestId={`developer-roles-table-${group}`}
              rowTestId={`developer-roles-row-${group}`}
              columns={[
                DEV_MSG.ROLES_COL_NAME,
                DEV_MSG.ROLES_COL_DESCRIPTION,
                DEV_MSG.ROLES_COL_COMMUNITIES,
                DEV_MSG.ROLES_COL_WORKFLOWS,
              ]}
              rows={roles.map((r, index) => ({
                key: r.name || `role-${group}-${index}`,
                dataAttrs: { "data-role-name": r.name },
                cells: [
                  <span key="n" style={monoCell}>
                    {r.name}
                  </span>,
                  <span key="d" style={mutedCell}>
                    {r.description || ""}
                  </span>,
                  <span key="c" style={mutedCell}>
                    {joinNames(r.communities)}
                  </span>,
                  <span key="w" style={mutedCell}>
                    {joinNames(r.workflows)}
                  </span>,
                ],
              }))}
            />
          </div>
        )
      ) : null}
    </section>
  );
}

/**
 * SE-03 Roles catalog grouped by community / workflow / unassigned.
 * Admins create one role (name + description) via PUT ?create=true.
 * The catalog reloads only after create succeeds. Membership edits stay out of scope.
 */
export function RolesPanel(): React.ReactElement {
  const [catalog, setCatalog] = useState<RoleBrowseEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = show all three Workbench folders (client-side). */
  const [filter, setFilter] = useState<RoleBrowseGroupKey | null>(null);
  const [expanded, setExpanded] = useState<Set<RoleBrowseGroupKey>>(
    () => new Set(ROLE_BROWSE_GROUPS),
  );
  const [creating, setCreating] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [draftDescription, setDraftDescription] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createNotice, setCreateNotice] = useState<string | null>(null);
  const [createBusy, setCreateBusy] = useState(false);
  const mountedRef = useRef(true);
  const createInflight = useRef(false);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback(() => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    setCatalog(null);
    setError(null);
    // Pass chip filter to GET ?group= so server-side filter matches product-docs.
    return browseRoles(filter)
      .then((result) => {
        if (!mountedRef.current) return;
        setCatalog(result.roles);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.ROLES_ERROR));
        setCatalog([]);
      });
  }, [filter]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const groupsToShow = useMemo(
    (): RoleBrowseGroupKey[] => (filter ? [filter] : [...ROLE_BROWSE_GROUPS]),
    [filter],
  );

  const grouped = useMemo(() => {
    const roles = catalog ?? [];
    const map = {} as Record<RoleBrowseGroupKey, RoleBrowseEntry[]>;
    for (const g of ROLE_BROWSE_GROUPS) {
      map[g] = rolesInBrowseGroup(roles, g);
    }
    return map;
  }, [catalog]);

  function toggleGroup(group: RoleBrowseGroupKey) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(group)) next.delete(group);
      else next.add(group);
      return next;
    });
  }

  function openCreate() {
    setCreating(true);
    setCreateError(null);
    setCreateNotice(null);
    setDraftName("");
    setDraftDescription("");
  }

  function cancelCreate() {
    if (createBusy) return;
    setCreating(false);
    setCreateError(null);
    setDraftName("");
    setDraftDescription("");
  }

  async function handleCreate(): Promise<void> {
    if (!isRoleCreateReady(draftName) || createInflight.current) {
      return;
    }
    createInflight.current = true;
    setCreateBusy(true);
    setCreateError(null);
    setCreateNotice(null);
    const name = draftName.trim();
    const description = draftDescription.trim();
    try {
      await createRole({
        name,
        description: description || undefined,
      });
      if (!mountedRef.current) return;
      setCreating(false);
      setDraftName("");
      setDraftDescription("");
      setCreateNotice(DEV_MSG.ROLES_CREATED);
      // Show every group so a workflow-assigned new role is visible after reload.
      if (filter !== null) {
        setFilter(null);
      } else {
        await reload();
      }
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setCreateError(createFailureMessage(err));
    } finally {
      createInflight.current = false;
      if (mountedRef.current) {
        setCreateBusy(false);
      }
    }
  }

  const canCreate = !createBusy && isRoleCreateReady(draftName);

  if (error) {
    return (
      <CatalogStatus testId="developer-roles-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (catalog == null) {
    return (
      <CatalogStatus testId="developer-roles-loading">
        {DEV_MSG.ROLES_LOADING}
      </CatalogStatus>
    );
  }

  return (
    <div data-testid="developer-roles-panel">
      <CatalogHint>{DEV_MSG.ROLES_HINT}</CatalogHint>
      {createNotice ? (
        <div data-testid="developer-roles-create-notice" style={{ color: "#276749", marginBottom: "12px" }}>
          {createNotice}
        </div>
      ) : null}
      {creating ? (
        <form
          data-testid="developer-roles-create-form"
          onSubmit={(event) => {
            event.preventDefault();
            void handleCreate();
          }}
          style={{
            marginBottom: "16px",
            padding: "12px",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
          }}
        >
          {createError ? (
            <div role="alert" data-testid="developer-roles-create-error" style={errorAlert}>
              {createError}
            </div>
          ) : null}
          <h2 style={{ margin: "0 0 12px" }} data-testid="developer-roles-create-title">
            {DEV_MSG.ROLES_CREATE_TITLE}
          </h2>
          <div style={fieldStyle}>
            <label htmlFor="developer-roles-create-name">{DEV_MSG.ROLES_CREATE_NAME}</label>
            <input
              id="developer-roles-create-name"
              data-testid="developer-roles-create-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={draftName}
              disabled={createBusy}
              autoComplete="off"
              onChange={(event) => setDraftName(event.target.value)}
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.ROLES_CREATE_NAME_REQUIRED}
            </span>
          </div>
          <div style={fieldStyle}>
            <label htmlFor="developer-roles-create-description">
              {DEV_MSG.ROLES_CREATE_DESCRIPTION}
            </label>
            <input
              id="developer-roles-create-description"
              data-testid="developer-roles-create-description"
              style={inputStyle}
              value={draftDescription}
              disabled={createBusy}
              onChange={(event) => setDraftDescription(event.target.value)}
            />
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            <button
              type="submit"
              data-testid="developer-roles-create-save"
              aria-label={DEV_MSG.ROLES_CREATE_SAVE}
              disabled={!canCreate}
              style={{
                padding: "8px 16px",
                background: canCreate ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canCreate ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.ROLES_CREATE_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-roles-create-cancel"
              disabled={createBusy}
              onClick={cancelCreate}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.ROLES_CREATE_CANCEL}
            </button>
          </div>
        </form>
      ) : (
        <div style={{ marginBottom: "16px" }}>
          <button
            type="button"
            data-testid="developer-roles-create"
            onClick={openCreate}
            style={{
              padding: "8px 16px",
              background: catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
            }}
          >
            {DEV_MSG.ROLES_CREATE}
          </button>
        </div>
      )}
      <div
        role="group"
        aria-label={DEV_MSG.ROLES_FILTER_LABEL}
        data-testid="developer-roles-filters"
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "8px",
          marginBottom: "16px",
        }}
      >
        <button
          type="button"
          data-testid="developer-roles-filter-all"
          aria-pressed={filter == null}
          onClick={() => setFilter(null)}
          style={filterChipStyle(filter == null)}
        >
          {DEV_MSG.ROLES_FILTER_ALL}
        </button>
        {ROLE_BROWSE_GROUPS.map((g) => (
          <button
            key={g}
            type="button"
            data-testid={`developer-roles-filter-${g}`}
            aria-pressed={filter === g}
            onClick={() => setFilter(g)}
            style={filterChipStyle(filter === g)}
          >
            {groupLabel(g)}
          </button>
        ))}
      </div>

      {catalog.length === 0 ? (
        <CatalogStatus testId="developer-roles-empty">
          {DEV_MSG.ROLES_EMPTY}
        </CatalogStatus>
      ) : (
        <div data-testid="developer-roles-groups">
          {groupsToShow.map((g) => (
            <RoleGroupSection
              key={g}
              group={g}
              roles={grouped[g]}
              expanded={expanded.has(g)}
              onToggle={() => toggleGroup(g)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
