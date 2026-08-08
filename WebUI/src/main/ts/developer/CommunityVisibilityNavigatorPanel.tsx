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
  getCommunityVisibility,
  listCommunities,
} from "../api/developer/assemblyApi";
import type {
  CommunitySummary,
  CommunityVisibleObject,
  RestGuid,
} from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import {
  catalogColors,
  monoCell,
  mutedCell,
} from "./catalogStyles";
import {
  groupVisibleObjectsByType,
  visibleObjectRowKey,
  type VisibilityTypeGroup,
} from "./communityVisibilityGroups";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

type CommunityLoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; objects: CommunityVisibleObject[] };

function communityKey(c: CommunitySummary, index: number): string {
  if (c.guid?.stringValue) return c.guid.stringValue;
  if (c.name) return `name:${c.name}`;
  if (c.id != null) return `id:${c.id}`;
  return `comm-idx:${index}`;
}

function communityGuid(c: CommunitySummary): RestGuid | null {
  if (c.guid?.stringValue || c.guid?.uuid != null || c.guid?.longValue != null) {
    return c.guid;
  }
  return null;
}

function communityTitle(c: CommunitySummary): string {
  return c.label || c.name || (c.id != null ? String(c.id) : "—");
}

const treeListStyle: React.CSSProperties = {
  listStyle: "none",
  margin: 0,
  padding: 0,
};

const groupButtonStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "8px",
  width: "100%",
  textAlign: "left",
  padding: "8px 10px",
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: "4px",
  background: "#fff",
  cursor: "pointer",
  font: "inherit",
  color: catalogColors.text,
};

const nestedListStyle: React.CSSProperties = {
  listStyle: "none",
  margin: "4px 0 8px 16px",
  padding: "0 0 0 8px",
  borderLeft: `2px solid ${catalogColors.headerBorder}`,
};

const typeHeaderStyle: React.CSSProperties = {
  ...groupButtonStyle,
  background: "#f7fafc",
  fontWeight: 600,
  fontSize: "0.95rem",
};

const objectRowStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(6rem, 10rem) minmax(8rem, 1fr) minmax(8rem, 1fr) minmax(8rem, 1.2fr)",
  gap: "8px",
  padding: "6px 10px",
  borderBottom: `1px solid ${catalogColors.headerBorder}`,
  fontSize: "0.9rem",
  alignItems: "baseline",
};

/**
 * SE-05 Community visibility navigator: design objects grouped by community,
 * then by object type. Uses existing getCommunityVisibility (lazy per community).
 */
export function CommunityVisibilityNavigatorPanel(): React.ReactElement {
  const [communities, setCommunities] = useState<CommunitySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expandedCommunities, setExpandedCommunities] = useState<Set<string>>(
    () => new Set(),
  );
  const [expandedTypes, setExpandedTypes] = useState<Set<string>>(() => new Set());
  const [loadByKey, setLoadByKey] = useState<Record<string, CommunityLoadState>>({});
  /** Monotonic per-community request ids so stale visibility responses are ignored. */
  const reqIds = useRef<Record<string, number>>({});

  useEffect(() => {
    let cancelled = false;
    listCommunities()
      .then((list) => {
        if (!cancelled) setCommunities(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.CVN_ERROR));
        setCommunities([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sortedCommunities = useMemo(() => {
    if (!communities) return [];
    return [...communities].sort((a, b) =>
      communityTitle(a).localeCompare(communityTitle(b), undefined, {
        sensitivity: "base",
      }),
    );
  }, [communities]);

  const loadVisibility = useCallback((key: string, guid: RestGuid) => {
    const nextReq = (reqIds.current[key] ?? 0) + 1;
    reqIds.current[key] = nextReq;
    setLoadByKey((prev) => ({ ...prev, [key]: { status: "loading" } }));
    getCommunityVisibility(guid)
      .then((objects) => {
        if (reqIds.current[key] !== nextReq) return;
        setLoadByKey((prev) => ({
          ...prev,
          [key]: { status: "ready", objects },
        }));
      })
      .catch((err: unknown) => {
        if (reqIds.current[key] !== nextReq) return;
        setLoadByKey((prev) => ({
          ...prev,
          [key]: {
            status: "error",
            message: panelErrMsg(err, DEV_MSG.CVN_VISIBILITY_ERROR),
          },
        }));
      });
  }, []);

  function toggleCommunity(c: CommunitySummary, index: number) {
    const key = communityKey(c, index);
    const expanding = !expandedCommunities.has(key);
    setExpandedCommunities((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

    if (!expanding) return;

    const guid = communityGuid(c);
    if (!guid) {
      setLoadByKey((prev) => ({
        ...prev,
        [key]: { status: "error", message: DEV_MSG.CVN_NO_GUID },
      }));
      return;
    }
    // Expand path: load when idle or previous error (retry). Keep ready cache.
    const current = loadByKey[key];
    if (!current || current.status === "idle" || current.status === "error") {
      loadVisibility(key, guid);
    }
  }

  function toggleType(communityKeyStr: string, typeKey: string) {
    const compound = `${communityKeyStr}::${typeKey}`;
    setExpandedTypes((prev) => {
      const next = new Set(prev);
      if (next.has(compound)) next.delete(compound);
      else next.add(compound);
      return next;
    });
  }

  if (error) {
    return (
      <CatalogStatus testId="developer-cvn-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (communities == null) {
    return (
      <CatalogStatus testId="developer-cvn-loading">{DEV_MSG.CVN_LOADING}</CatalogStatus>
    );
  }
  if (communities.length === 0) {
    return (
      <CatalogStatus testId="developer-cvn-empty">{DEV_MSG.CVN_EMPTY}</CatalogStatus>
    );
  }

  return (
    <div data-testid="developer-cvn-panel">
      <CatalogHint>{DEV_MSG.CVN_HINT}</CatalogHint>
      <p
        style={{ color: catalogColors.muted, fontSize: "0.9rem", marginTop: 0 }}
        data-testid="developer-cvn-intro"
      >
        {DEV_MSG.CVN_INTRO}
      </p>

      <ul
        role="tree"
        aria-label={DEV_MSG.CVN_TREE_LABEL}
        data-testid="developer-cvn-tree"
        style={treeListStyle}
      >
        {sortedCommunities.map((c, index) => {
          const key = communityKey(c, index);
          const expanded = expandedCommunities.has(key);
          const load = loadByKey[key] ?? { status: "idle" as const };
          const groups: VisibilityTypeGroup[] =
            load.status === "ready"
              ? groupVisibleObjectsByType(load.objects)
              : [];

          return (
            <li
              key={key}
              role="treeitem"
              aria-expanded={expanded}
              data-testid={`developer-cvn-community-${key}`}
              style={{ marginBottom: "8px" }}
            >
              <button
                type="button"
                style={groupButtonStyle}
                data-testid={`developer-cvn-community-toggle-${key}`}
                aria-expanded={expanded}
                onClick={() => toggleCommunity(c, index)}
              >
                <span aria-hidden="true" style={{ width: "1rem", flexShrink: 0 }}>
                  {expanded ? "▾" : "▸"}
                </span>
                <span style={{ fontWeight: 600 }}>{communityTitle(c)}</span>
                {c.name ? (
                  <span style={{ ...monoCell, color: catalogColors.muted, fontSize: "0.85rem" }}>
                    {c.name}
                  </span>
                ) : null}
              </button>

              {expanded ? (
                <div
                  data-testid={`developer-cvn-community-body-${key}`}
                  style={{ marginTop: "6px" }}
                >
                  {load.status === "loading" || load.status === "idle" ? (
                    <div
                      data-testid={`developer-cvn-community-loading-${key}`}
                      style={{ ...mutedCell, padding: "8px 12px" }}
                    >
                      {DEV_MSG.CVN_VISIBILITY_LOADING}
                    </div>
                  ) : null}
                  {load.status === "error" ? (
                    <div
                      role="alert"
                      data-testid={`developer-cvn-community-error-${key}`}
                      style={{
                        color: "#c53030",
                        background: "#fff5f5",
                        border: "1px solid #feb2b2",
                        borderRadius: "4px",
                        padding: "8px 12px",
                        margin: "4px 0 4px 16px",
                      }}
                    >
                      {load.message}
                    </div>
                  ) : null}
                  {load.status === "ready" && groups.length === 0 ? (
                    <div
                      data-testid={`developer-cvn-community-empty-${key}`}
                      style={{ ...mutedCell, padding: "8px 12px" }}
                    >
                      {DEV_MSG.CVN_COMMUNITY_EMPTY}
                    </div>
                  ) : null}
                  {load.status === "ready" && groups.length > 0 ? (
                    <ul
                      role="group"
                      data-testid={`developer-cvn-type-groups-${key}`}
                      style={nestedListStyle}
                    >
                      {groups.map((g) => {
                        const typeCompound = `${key}::${g.typeKey}`;
                        const typeExpanded = expandedTypes.has(typeCompound);
                        return (
                          <li
                            key={g.typeKey}
                            role="treeitem"
                            aria-expanded={typeExpanded}
                            data-testid={`developer-cvn-type-${key}-${g.typeKey}`}
                            style={{ marginBottom: "6px" }}
                          >
                            <button
                              type="button"
                              style={typeHeaderStyle}
                              data-testid={`developer-cvn-type-toggle-${key}-${g.typeKey}`}
                              aria-expanded={typeExpanded}
                              onClick={() => toggleType(key, g.typeKey)}
                            >
                              <span aria-hidden="true" style={{ width: "1rem" }}>
                                {typeExpanded ? "▾" : "▸"}
                              </span>
                              <span>{g.label}</span>
                              <span
                                style={{
                                  ...mutedCell,
                                  fontSize: "0.85rem",
                                  marginLeft: "auto",
                                }}
                              >
                                {g.objects.length}
                              </span>
                            </button>
                            {typeExpanded ? (
                              <ul
                                role="group"
                                data-testid={`developer-cvn-objects-${key}-${g.typeKey}`}
                                style={{
                                  listStyle: "none",
                                  margin: "0 0 0 8px",
                                  padding: 0,
                                  border: `1px solid ${catalogColors.headerBorder}`,
                                  borderRadius: "4px",
                                  overflow: "hidden",
                                }}
                              >
                                <li
                                  style={{
                                    ...objectRowStyle,
                                    fontWeight: 600,
                                    background: "#edf2f7",
                                    borderBottom: `1px solid ${catalogColors.headerBorder}`,
                                  }}
                                  aria-hidden="true"
                                >
                                  <span>{DEV_MSG.COMM_COL_OBJ_TYPE}</span>
                                  <span>{DEV_MSG.COMM_COL_OBJ_NAME}</span>
                                  <span>{DEV_MSG.COMM_COL_OBJ_LABEL}</span>
                                  <span>{DEV_MSG.COMM_COL_OBJ_GUID}</span>
                                </li>
                                {g.objects.map((o, oi) => (
                                  <li
                                    key={visibleObjectRowKey(o, oi)}
                                    role="treeitem"
                                    data-testid={`developer-cvn-object-${visibleObjectRowKey(o, oi)}`}
                                    style={objectRowStyle}
                                  >
                                    <span style={monoCell}>{o.type || "—"}</span>
                                    <span style={monoCell}>{o.name || "—"}</span>
                                    <span>{o.label || "—"}</span>
                                    <span style={{ ...monoCell, fontSize: "0.85rem" }}>
                                      {o.guid?.stringValue ||
                                        (o.id != null ? String(o.id) : "—")}
                                    </span>
                                  </li>
                                ))}
                              </ul>
                            ) : null}
                          </li>
                        );
                      })}
                    </ul>
                  ) : null}
                </div>
              ) : null}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
