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
  deleteRole,
  isRoleCreateReady,
  isSystemRoleName,
  loadRole,
  ROLE_BROWSE_GROUPS,
  rolesInBrowseGroup,
  updateRoleDescription,
  updateRoleHomePage,
  type RoleBrowseEntry,
  type RoleBrowseGroupKey,
} from "../api/developer/rolesApi";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
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

function editFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.ROLES_EDIT_FORBIDDEN);
    }
    if (err.status === 404) {
      return panelErrMsg(err, DEV_MSG.ROLES_EDIT_NOT_FOUND);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.ROLES_EDIT_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.ROLES_EDIT_ERROR);
}

function homePageFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.ROLES_HOME_FORBIDDEN);
    }
    if (err.status === 404) {
      return panelErrMsg(err, DEV_MSG.ROLES_HOME_NOT_FOUND);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.ROLES_HOME_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.ROLES_HOME_ERROR);
}

function deleteFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.ROLES_DELETE_FORBIDDEN);
    }
    if (err.status === 409) {
      return panelErrMsg(err, DEV_MSG.ROLES_DELETE_CONFLICT);
    }
    if (err.status === 400) {
      return panelErrMsg(err, DEV_MSG.ROLES_DELETE_INVALID);
    }
  }
  return panelErrMsg(err, DEV_MSG.ROLES_DELETE_ERROR);
}

function membersFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return panelErrMsg(err, DEV_MSG.ROLES_MEMBERS_FORBIDDEN);
    }
    if (err.status === 404) {
      return panelErrMsg(err, DEV_MSG.ROLES_MEMBERS_NOT_FOUND);
    }
  }
  return panelErrMsg(err, DEV_MSG.ROLES_MEMBERS_ERROR);
}

function sameRoleName(left: string, right: string): boolean {
  return left.trim().toLowerCase() === right.trim().toLowerCase();
}

function RoleMembers({
  loading,
  error,
  users,
}: {
  loading: boolean;
  error: string | null;
  /** null until a successful read. An empty array is a real empty role. */
  users: string[] | null;
}): React.ReactElement {
  return (
    <section
      aria-label={DEV_MSG.ROLES_MEMBERS_LABEL}
      data-testid="developer-roles-members"
      style={{ marginBottom: "12px" }}
    >
      <h3 style={{ margin: "0 0 4px", fontSize: "1rem" }} data-testid="developer-roles-members-title">
        {DEV_MSG.ROLES_MEMBERS_TITLE}
      </h3>
      <p style={{ color: catalogColors.muted, margin: "0 0 8px", fontSize: "0.85rem" }}>
        {DEV_MSG.ROLES_MEMBERS_HINT}
      </p>
      {loading ? (
        <CatalogStatus testId="developer-roles-members-loading">
          {DEV_MSG.ROLES_MEMBERS_LOADING}
        </CatalogStatus>
      ) : null}
      {error ? (
        <div role="alert" data-testid="developer-roles-members-error" style={errorAlert}>
          {error}
        </div>
      ) : null}
      {!loading && !error && users != null && users.length === 0 ? (
        <CatalogStatus testId="developer-roles-members-empty">
          {DEV_MSG.ROLES_MEMBERS_EMPTY}
        </CatalogStatus>
      ) : null}
      {!loading && !error && users != null && users.length > 0 ? (
        <ul
          data-testid="developer-roles-members-list"
          style={{ margin: "0", paddingLeft: "1.25rem" }}
        >
          {users.map((userName) => (
            <li
              key={userName}
              data-testid="developer-roles-member"
              data-user-name={userName}
              style={monoCell}
            >
              {userName}
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function RoleGroupSection({
  group,
  roles,
  expanded,
  onToggle,
  onOpenRole,
  onDeleteRole,
  deleteBusy,
}: {
  group: RoleBrowseGroupKey;
  roles: RoleBrowseEntry[];
  expanded: boolean;
  onToggle: () => void;
  onOpenRole: (role: RoleBrowseEntry) => void;
  onDeleteRole: (role: RoleBrowseEntry) => void;
  deleteBusy: boolean;
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
                DEV_MSG.ROLES_COL_HOMEPAGE,
                DEV_MSG.ROLES_COL_COMMUNITIES,
                DEV_MSG.ROLES_COL_WORKFLOWS,
                DEV_MSG.ROLES_COL_ACTIONS,
              ]}
              rows={roles.map((r, index) => ({
                key: r.name || `role-${group}-${index}`,
                dataAttrs: { "data-role-name": r.name },
                onClick: () => onOpenRole(r),
                cells: [
                  <span key="n" style={monoCell}>
                    {r.name}
                  </span>,
                  <span
                    key="d"
                    style={mutedCell}
                    data-role-description={r.name}
                  >
                    {r.description || ""}
                  </span>,
                  <span
                    key="h"
                    style={mutedCell}
                    data-role-homepage={r.name}
                  >
                    {r.homePage || ""}
                  </span>,
                  <span key="c" style={mutedCell}>
                    {joinNames(r.communities)}
                  </span>,
                  <span key="w" style={mutedCell}>
                    {joinNames(r.workflows)}
                  </span>,
                  <button
                    key="del"
                    type="button"
                    data-testid="developer-roles-delete"
                    data-role-name={r.name}
                    aria-label={`${DEV_MSG.ROLES_DELETE} ${r.name}`}
                    title={
                      isSystemRoleName(r.name) ? DEV_MSG.ROLES_DELETE_SYSTEM : undefined
                    }
                    disabled={deleteBusy || isSystemRoleName(r.name)}
                    onClick={(event) => {
                      event.stopPropagation();
                      onDeleteRole(r);
                    }}
                    onKeyDown={(event) => {
                      event.stopPropagation();
                    }}
                    style={{
                      padding: "4px 10px",
                      background: isSystemRoleName(r.name)
                        ? catalogColors.disabled
                        : catalogColors.error,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor:
                        deleteBusy || isSystemRoleName(r.name) ? "not-allowed" : "pointer",
                      font: "inherit",
                    }}
                  >
                    {DEV_MSG.ROLES_DELETE}
                  </button>,
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
 * Admins create one role (name + description) via PUT ?create=true, edit
 * one existing role's description via PUT ?update=true, set or clear one
 * role's home page via PUT ?homePage=true, and delete one non-system role
 * via DELETE after confirm. Opening a role GETs its stored users (read-only).
 * An empty user list is an empty state. HTTP 403 and 404 do not show members.
 * Description and home-page saves do not send users. Membership edits stay
 * out of scope.
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
  const [editName, setEditName] = useState<string | null>(null);
  const [editDescription, setEditDescription] = useState("");
  const [editHomePage, setEditHomePage] = useState("");
  const [editError, setEditError] = useState<string | null>(null);
  const [editNotice, setEditNotice] = useState<string | null>(null);
  const [editBusy, setEditBusy] = useState(false);
  const [homeError, setHomeError] = useState<string | null>(null);
  const [homeNotice, setHomeNotice] = useState<string | null>(null);
  const [homeBusy, setHomeBusy] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteNotice, setDeleteNotice] = useState<string | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  /** null until GET succeeds. [] is a real empty role; errors stay null. */
  const [memberUsers, setMemberUsers] = useState<string[] | null>(null);
  const [membersError, setMembersError] = useState<string | null>(null);
  const [membersLoading, setMembersLoading] = useState(false);
  const mountedRef = useRef(true);
  const createInflight = useRef(false);
  const editInflight = useRef(false);
  const homeInflight = useRef(false);
  const deleteInflight = useRef(false);
  const membersGen = useRef(0);
  const editNameRef = useRef<string | null>(null);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      membersGen.current += 1;
    };
  }, []);

  function clearMembers() {
    membersGen.current += 1;
    setMemberUsers(null);
    setMembersError(null);
    setMembersLoading(false);
  }

  function requestMembers(name: string) {
    const requested = name.trim();
    const gen = ++membersGen.current;
    setMemberUsers(null);
    setMembersError(null);
    setMembersLoading(true);
    void loadRole(requested)
      .then((read) => {
        if (!mountedRef.current || gen !== membersGen.current) return;
        const openName = editNameRef.current;
        if (openName == null || !sameRoleName(openName, requested)) return;
        if (!sameRoleName(read.name, requested)) {
          setMembersLoading(false);
          setMemberUsers(null);
          setMembersError(DEV_MSG.ROLES_MEMBERS_ERROR);
          return;
        }
        setMemberUsers(read.users);
        setMembersError(null);
        setMembersLoading(false);
      })
      .catch((err: unknown) => {
        if (!mountedRef.current || gen !== membersGen.current) return;
        setMembersLoading(false);
        setMemberUsers(null);
        setMembersError(membersFailureMessage(err));
      });
  }

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

  function openDelete(role: RoleBrowseEntry) {
    if (createBusy || editBusy || deleteBusy || !role.name) return;
    if (isSystemRoleName(role.name)) return;
    setDeleteError(null);
    setDeleteNotice(null);
    setPendingDelete(role.name);
  }

  function cancelDelete() {
    if (deleteBusy) return;
    setPendingDelete(null);
  }

  async function handleDelete(): Promise<void> {
    if (!pendingDelete || deleteInflight.current || isSystemRoleName(pendingDelete)) {
      return;
    }
    const name = pendingDelete.trim();
    deleteInflight.current = true;
    setDeleteBusy(true);
    setDeleteError(null);
    setDeleteNotice(null);
    try {
      await deleteRole(name);
      if (!mountedRef.current) return;
      setPendingDelete(null);
      if (editName === name) {
        editNameRef.current = null;
        clearMembers();
        setEditName(null);
        setEditDescription("");
        setEditHomePage("");
        setEditError(null);
        setHomeError(null);
      }
      setDeleteNotice(DEV_MSG.ROLES_DELETED);
      await reload();
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setPendingDelete(null);
      setDeleteError(deleteFailureMessage(err));
    } finally {
      deleteInflight.current = false;
      if (mountedRef.current) {
        setDeleteBusy(false);
      }
    }
  }

  function openCreate() {
    if (editBusy || homeBusy || deleteBusy) return;
    editNameRef.current = null;
    clearMembers();
    setEditName(null);
    setEditError(null);
    setHomeError(null);
    setEditDescription("");
    setEditHomePage("");
    setCreating(true);
    setCreateError(null);
    setCreateNotice(null);
    setEditNotice(null);
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

  function openEdit(role: RoleBrowseEntry) {
    if (createBusy || editBusy || homeBusy || deleteBusy || !role.name) return;
    setCreating(false);
    setCreateError(null);
    setCreateNotice(null);
    setEditError(null);
    setHomeError(null);
    setEditNotice(null);
    setHomeNotice(null);
    editNameRef.current = role.name;
    if (editName !== role.name) {
      setEditDescription(role.description ?? "");
      setEditHomePage(role.homePage ?? "");
      setEditName(role.name);
    }
    requestMembers(role.name);
  }

  function cancelEdit() {
    if (editBusy || homeBusy) return;
    editNameRef.current = null;
    clearMembers();
    setEditName(null);
    setEditError(null);
    setHomeError(null);
    setEditDescription("");
    setEditHomePage("");
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

  async function handleEdit(): Promise<void> {
    if (!editName || editInflight.current || homeInflight.current) {
      return;
    }
    editInflight.current = true;
    setEditBusy(true);
    setEditError(null);
    setEditNotice(null);
    const name = editName.trim();
    const description = editDescription.trim();
    try {
      await updateRoleDescription({ name, description });
      if (!mountedRef.current) return;
      editNameRef.current = null;
      clearMembers();
      setEditName(null);
      setEditDescription("");
      setEditHomePage("");
      setEditNotice(DEV_MSG.ROLES_EDIT_SAVED);
      await reload();
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setEditError(editFailureMessage(err));
    } finally {
      editInflight.current = false;
      if (mountedRef.current) {
        setEditBusy(false);
      }
    }
  }

  async function handleHomePage(): Promise<void> {
    if (!editName || homeInflight.current || editInflight.current) {
      return;
    }
    homeInflight.current = true;
    setHomeBusy(true);
    setHomeError(null);
    setHomeNotice(null);
    const name = editName.trim();
    const homePage = editHomePage.trim();
    try {
      await updateRoleHomePage({ name, homePage });
      if (!mountedRef.current) return;
      editNameRef.current = null;
      clearMembers();
      setEditName(null);
      setEditDescription("");
      setEditHomePage("");
      setHomeNotice(DEV_MSG.ROLES_HOME_SAVED);
      await reload();
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setHomeError(homePageFailureMessage(err));
    } finally {
      homeInflight.current = false;
      if (mountedRef.current) {
        setHomeBusy(false);
      }
    }
  }

  const canCreate = !createBusy && isRoleCreateReady(draftName);
  const detailLocked = editBusy || homeBusy;
  const canSaveDescription = !detailLocked && editName != null && editName.trim().length > 0;
  const canSaveHomePage = canSaveDescription;

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
      {editNotice ? (
        <div data-testid="developer-roles-edit-notice" style={{ color: "#276749", marginBottom: "12px" }}>
          {editNotice}
        </div>
      ) : null}
      {homeNotice ? (
        <div data-testid="developer-roles-homepage-notice" style={{ color: "#276749", marginBottom: "12px" }}>
          {homeNotice}
        </div>
      ) : null}
      {deleteNotice ? (
        <div data-testid="developer-roles-delete-notice" style={{ color: "#276749", marginBottom: "12px" }}>
          {deleteNotice}
        </div>
      ) : null}
      {deleteError ? (
        <div role="alert" data-testid="developer-roles-delete-error" style={errorAlert}>
          {deleteError}
        </div>
      ) : null}
      {editName ? (
        <form
          data-testid="developer-roles-edit-form"
          onSubmit={(event) => {
            event.preventDefault();
            void handleEdit();
          }}
          style={{
            marginBottom: "16px",
            padding: "12px",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
          }}
        >
          {editError ? (
            <div role="alert" data-testid="developer-roles-edit-error" style={errorAlert}>
              {editError}
            </div>
          ) : null}
          {homeError ? (
            <div role="alert" data-testid="developer-roles-homepage-error" style={errorAlert}>
              {homeError}
            </div>
          ) : null}
          <h2 style={{ margin: "0 0 12px" }} data-testid="developer-roles-edit-title">
            {DEV_MSG.ROLES_EDIT_TITLE}
          </h2>
          <p style={{ color: catalogColors.muted, margin: "0 0 12px", fontSize: "0.9rem" }}>
            {DEV_MSG.ROLES_EDIT_HINT}
          </p>
          <div style={fieldStyle}>
            <label htmlFor="developer-roles-edit-name">{DEV_MSG.ROLES_EDIT_NAME}</label>
            <input
              id="developer-roles-edit-name"
              data-testid="developer-roles-edit-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={editName}
              readOnly
              disabled={detailLocked}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="developer-roles-edit-description">
              {DEV_MSG.ROLES_EDIT_DESCRIPTION}
            </label>
            <input
              id="developer-roles-edit-description"
              data-testid="developer-roles-edit-description"
              style={inputStyle}
              value={editDescription}
              disabled={detailLocked}
              onChange={(event) => setEditDescription(event.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="developer-roles-edit-homepage">
              {DEV_MSG.ROLES_HOME_LABEL}
            </label>
            <input
              id="developer-roles-edit-homepage"
              data-testid="developer-roles-edit-homepage"
              style={inputStyle}
              value={editHomePage}
              disabled={detailLocked}
              onChange={(event) => setEditHomePage(event.target.value)}
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.ROLES_HOME_HINT}
            </span>
          </div>
          <RoleMembers loading={membersLoading} error={membersError} users={memberUsers} />
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            <button
              type="submit"
              data-testid="developer-roles-edit-save"
              aria-label={DEV_MSG.ROLES_EDIT_SAVE}
              disabled={!canSaveDescription || homeBusy}
              style={{
                padding: "8px 16px",
                background: canSaveDescription ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSaveDescription ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.ROLES_EDIT_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-roles-homepage-save"
              aria-label={DEV_MSG.ROLES_HOME_SAVE}
              disabled={!canSaveHomePage}
              onClick={() => {
                void handleHomePage();
              }}
              style={{
                padding: "8px 16px",
                background: canSaveHomePage ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSaveHomePage ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.ROLES_HOME_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-roles-edit-cancel"
              disabled={detailLocked}
              onClick={cancelEdit}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.ROLES_EDIT_CANCEL}
            </button>
          </div>
        </form>
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
      ) : editName ? null : (
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
              onOpenRole={openEdit}
              onDeleteRole={openDelete}
              deleteBusy={deleteBusy}
            />
          ))}
        </div>
      )}
      <CatalogConfirmDialog
        open={pendingDelete != null}
        busy={deleteBusy}
        message={
          pendingDelete
            ? `${DEV_MSG.ROLES_DELETE_CONFIRM} ${pendingDelete}`
            : ""
        }
        onCancel={cancelDelete}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
