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

import React, { useCallback, useEffect, useState } from "react";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { loadSectionTree } from "../api/architecture/sectionApi";
import type { NavTreeNode } from "../api/architecture/types";
import { fetchSites } from "../api/home/homeApi";
import { catalogColors } from "../developer/catalogStyles";
import { NavTree } from "./NavTree";
import { SitePicker } from "./SitePicker";
import { ARCH_MSG } from "./messages";

export interface ArchitectureShellProps {
  /**
   * Optional site name from SPA path {@code /architecture/:site} or deep-link
   * query. When present and found in the site list, it becomes the selection.
   */
  initialSite?: string | null;
  /**
   * When true (SPA AppLayout), shell is under product chrome — tighter padding.
   */
  embedded?: boolean;
}

type SitesLoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; names: string[] };

type TreeLoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; root: NavTreeNode | null };

/**
 * Architecture / Navigation SPA shell (#3094 shell + #3095 read-only tree).
 *
 * <p>Site picker + {@code GET /section/tree/{site}} browse. Mutations (create /
 * edit / reorder / delete) are out of scope until Slice D.</p>
 */
export const ArchitectureShell: React.FC<ArchitectureShellProps> = ({
  initialSite = null,
  embedded = false,
}) => {
  const [sitesState, setSitesState] = useState<SitesLoadState>({
    status: "loading",
  });
  const [selectedSite, setSelectedSite] = useState<string | null>(() => {
    const t = initialSite != null ? String(initialSite).trim() : "";
    return t.length > 0 ? t : null;
  });
  const [treeState, setTreeState] = useState<TreeLoadState>({ status: "idle" });
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);

  // Honor route/deep-link site when prop changes
  useEffect(() => {
    const t = initialSite != null ? String(initialSite).trim() : "";
    if (t.length > 0) {
      setSelectedSite(t);
    }
  }, [initialSite]);

  // Load site list once
  useEffect(() => {
    let cancelled = false;
    setSitesState({ status: "loading" });
    void (async () => {
      try {
        const list = await fetchSites();
        if (cancelled) return;
        const names = list
          .map((s) => (s.name != null ? String(s.name).trim() : ""))
          .filter((n) => n.length > 0)
          .sort((a, b) => a.localeCompare(b));
        setSitesState({ status: "ready", names });
        // If no selection yet, pick first site when available
        setSelectedSite((prev) => {
          if (prev && names.includes(prev)) return prev;
          if (prev && names.length === 0) return prev; // keep deep-link name for tree attempt
          if (prev && !names.includes(prev) && names.length > 0) {
            // Deep-link site not in list — still try loading with that name
            return prev;
          }
          if (!prev && names.length > 0) return names[0];
          return prev;
        });
      } catch (err) {
        if (cancelled) return;
        if (isSessionRedirectError(err)) return;
        setSitesState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.SITES_ERROR),
        });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Load tree when site or refresh changes
  useEffect(() => {
    if (!selectedSite) {
      setTreeState({ status: "idle" });
      setSelectedNodeId(null);
      return;
    }
    let cancelled = false;
    setTreeState({ status: "loading" });
    setSelectedNodeId(null);
    void (async () => {
      try {
        const root = await loadSectionTree(selectedSite);
        if (cancelled) return;
        setTreeState({ status: "ready", root });
      } catch (err) {
        if (cancelled) return;
        if (isSessionRedirectError(err)) return;
        setTreeState({
          status: "error",
          message: formatApiError(err, ARCH_MSG.TREE_ERROR),
        });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedSite, refreshToken]);

  const onRefresh = useCallback(() => {
    setRefreshToken((n) => n + 1);
  }, []);

  const siteNames =
    sitesState.status === "ready" ? sitesState.names : [];
  // Immutable options: never mutate arrays during render
  const siteOptions =
    selectedSite && !siteNames.includes(selectedSite)
      ? [{ name: selectedSite }, ...siteNames.map((name) => ({ name }))]
      : siteNames.map((name) => ({ name }));

  const treeLoading = treeState.status === "loading";
  const treeError =
    treeState.status === "error" ? treeState.message : null;
  const treeRoot =
    treeState.status === "ready" ? treeState.root : null;

  return (
    <div
      className="perc-architecture-shell"
      data-testid="perc-architecture-shell"
      data-embedded={embedded ? "true" : "false"}
      data-site={selectedSite ?? ""}
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: embedded ? "8px 12px 20px" : "20px",
        maxWidth: "960px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "16px" }}>
        <h1
          style={{ marginBottom: "8px" }}
          data-testid="architecture-shell-title"
        >
          {ARCH_MSG.TITLE}
        </h1>
        <p
          style={{ margin: 0, color: catalogColors.muted, maxWidth: "48rem" }}
          data-testid="architecture-shell-intro"
        >
          {ARCH_MSG.INTRO}
        </p>
      </header>

      <section
        aria-label={ARCH_MSG.SITE_LABEL}
        data-testid="architecture-toolbar"
        style={{
          display: "flex",
          flexWrap: "wrap",
          alignItems: "center",
          gap: "0.75rem 1rem",
          marginBottom: "12px",
        }}
      >
        {sitesState.status === "loading" ? (
          <p
            style={{ margin: 0, color: catalogColors.muted }}
            data-testid="architecture-sites-loading"
            aria-live="polite"
          >
            {ARCH_MSG.SITES_LOADING}
          </p>
        ) : sitesState.status === "error" ? (
          <p
            style={{ margin: 0, color: catalogColors.error }}
            role="alert"
            data-testid="architecture-sites-error"
          >
            {sitesState.message}
          </p>
        ) : siteNames.length === 0 && !selectedSite ? (
          <p
            style={{ margin: 0, color: catalogColors.empty }}
            data-testid="architecture-sites-empty"
          >
            {ARCH_MSG.SITES_EMPTY}
          </p>
        ) : (
          <SitePicker
            sites={siteOptions}
            selectedSite={selectedSite}
            onChange={(name) => setSelectedSite(name)}
          />
        )}
        <button
          type="button"
          data-testid="architecture-refresh"
          onClick={onRefresh}
          disabled={!selectedSite || treeLoading}
          style={{
            padding: "0.4rem 0.85rem",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: 4,
            background: !selectedSite || treeLoading ? "#f0f0f0" : "#fff",
            color: !selectedSite || treeLoading ? "#999" : "#222",
            cursor:
              !selectedSite || treeLoading ? "not-allowed" : "pointer",
            fontSize: "0.9rem",
          }}
        >
          {ARCH_MSG.REFRESH}
        </button>
      </section>

      {!selectedSite ? (
        <section
          aria-labelledby="architecture-empty-title"
          data-testid="architecture-empty-state"
          style={{
            border: `1px solid ${catalogColors.headerBorder}`,
            borderRadius: "8px",
            padding: "1.25rem 1.5rem",
            background: "#f8fafc",
          }}
        >
          <h2
            id="architecture-empty-title"
            style={{
              marginTop: 0,
              marginBottom: "0.5rem",
              fontSize: "1.1rem",
              color: "#1a202c",
            }}
            data-testid="architecture-empty-title"
          >
            {ARCH_MSG.EMPTY_TITLE}
          </h2>
          <p
            style={{
              margin: "0 0 0.75rem",
              color: catalogColors.muted,
              lineHeight: 1.5,
            }}
            data-testid="architecture-empty-body"
          >
            {ARCH_MSG.EMPTY_BODY}
          </p>
          <p
            style={{
              margin: 0,
              color: catalogColors.empty,
              fontSize: "0.95rem",
            }}
            data-testid="architecture-site-hint"
          >
            {ARCH_MSG.SITE_NONE}
          </p>
        </section>
      ) : (
        <section
          aria-labelledby="architecture-tree-heading"
          data-testid="architecture-tree-panel"
        >
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              alignItems: "baseline",
              justifyContent: "space-between",
              gap: "0.5rem",
              marginBottom: "8px",
            }}
          >
            <h2
              id="architecture-tree-heading"
              style={{
                margin: 0,
                fontSize: "1.05rem",
                color: "#1a202c",
              }}
              data-testid="architecture-tree-heading"
            >
              {ARCH_MSG.TREE_PANEL_TITLE}
            </h2>
            <p
              style={{
                margin: 0,
                color: catalogColors.empty,
                fontSize: "0.85rem",
              }}
              data-testid="architecture-site-hint"
            >
              {ARCH_MSG.SITE_HINT.replace("{0}", selectedSite)}
            </p>
          </div>
          <NavTree
            root={treeRoot}
            loading={treeLoading}
            error={treeError}
            selectedId={selectedNodeId}
            onSelect={(node) => setSelectedNodeId(node.id)}
          />
          <p
            style={{
              margin: "0.75rem 0 0",
              color: catalogColors.empty,
              fontSize: "0.85rem",
            }}
            data-testid="architecture-readonly-note"
          >
            {ARCH_MSG.TREE_READONLY_NOTE}
          </p>
        </section>
      )}
    </div>
  );
};

export default ArchitectureShell;
