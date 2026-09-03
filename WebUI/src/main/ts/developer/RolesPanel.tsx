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
import {
  browseRoles,
  ROLE_BROWSE_GROUPS,
  rolesInBrowseGroup,
  type RoleBrowseEntry,
  type RoleBrowseGroupKey,
} from "../api/developer/rolesApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell } from "./catalogStyles";
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
 * SE-03 — read-only Roles catalog grouped by community / workflow / unassigned
 * (Workbench Security Design peer). Membership CRUD stays on Admin → Roles /
 * community detail.
 */
export function RolesPanel(): React.ReactElement {
  const [catalog, setCatalog] = useState<RoleBrowseEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = show all three Workbench folders (client-side). */
  const [filter, setFilter] = useState<RoleBrowseGroupKey | null>(null);
  const [expanded, setExpanded] = useState<Set<RoleBrowseGroupKey>>(
    () => new Set(ROLE_BROWSE_GROUPS),
  );
  const mountedRef = useRef(true);

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
    return browseRoles()
      .then((result) => {
        if (!mountedRef.current) return;
        setCatalog(result.roles);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.ROLES_ERROR));
        setCatalog([]);
      });
  }, []);

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
