/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { listControls } from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, mutedCell, openButtonStyle } from "./catalogStyles";
import { ControlCreatePanel } from "./ControlCreatePanel";
import { ControlDetailPanel } from "./ControlDetailPanel";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

type SelectedControl = { name: string } | "new" | null;

/**
 * P0.19 — content editor control catalog (UI-01 read + user create/save/delete).
 */
export function ControlsPanel(): React.ReactElement {
  const [items, setItems] = useState<ControlDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedControl>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback((opts?: { showLoading?: boolean }) => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    if (opts?.showLoading) {
      setItems(null);
    }
    setError(null);
    return listControls()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.CTL_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items]
      .filter((c) => (c.name || "").trim().length > 0)
      .sort((a, b) =>
        (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
      );
  }, [items]);

  async function handleCreated(detail: ControlDef): Promise<void> {
    const name = (detail.name || "").trim();
    await reload();
    if (!mountedRef.current) {
      return;
    }
    if (name) {
      setSelected({ name });
    } else {
      setSelected(null);
    }
  }

  async function handleDeleted(): Promise<void> {
    await reload();
    if (!mountedRef.current) {
      return;
    }
    setSelected(null);
    setNotice(DEV_MSG.CTL_DELETED);
  }

  if (selected === "new") {
    return (
      <ControlCreatePanel onBack={() => setSelected(null)} onCreated={handleCreated} />
    );
  }

  if (selected) {
    return (
      <ControlDetailPanel
        name={selected.name}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  return (
    <div data-testid="developer-ctl-panel">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
          gap: "12px",
          flexWrap: "wrap",
        }}
      >
        <CatalogHint>{DEV_MSG.CTL_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-ctl-new"
          onClick={() => {
            setNotice(null);
            setSelected("new");
          }}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.CTL_NEW}
        </button>
      </div>
      {notice ? (
        <div
          role="status"
          aria-live="polite"
          aria-atomic="true"
          data-testid="developer-ctl-notice"
          style={{ color: "#276749", marginBottom: "12px" }}
        >
          {notice}
        </div>
      ) : null}
      {error ? (
        <CatalogStatus testId="developer-ctl-error" error>
          {error}
        </CatalogStatus>
      ) : items == null ? (
        <CatalogStatus testId="developer-ctl-loading">{DEV_MSG.CTL_LOADING}</CatalogStatus>
      ) : sorted.length === 0 ? (
        <CatalogStatus testId="developer-ctl-empty">{DEV_MSG.CTL_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-ctl-table"
          rowTestId="developer-ctl-row"
          columns={[
            DEV_MSG.CTL_COL_NAME,
            DEV_MSG.CTL_COL_DISPLAY,
            DEV_MSG.CTL_COL_SCOPE,
            DEV_MSG.CTL_COL_DIM,
            DEV_MSG.CTL_COL_FLAGS,
          ]}
          rows={sorted.map((c, index) => {
            const openKey = (c.name || "").trim();
            const flags: string[] = [];
            if (c.deprecated) flags.push(DEV_MSG.CTL_FLAG_DEPRECATED);
            return {
              key: `${openKey}-${index}`,
              dataAttrs: { "data-ctl-name": openKey },
              onClick: () => {
                setNotice(null);
                setSelected({ name: openKey });
              },
              cells: [
                <button
                  key="open"
                  type="button"
                  data-testid="developer-ctl-open"
                  data-ctl-name={openKey}
                  aria-label={`Open ${openKey}`}
                  onClick={(ev) => {
                    ev.stopPropagation();
                    setNotice(null);
                    setSelected({ name: openKey });
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {openKey}
                </button>,
                c.displayName || "—",
                c.scope || "—",
                <span key="dim" style={mutedCell}>
                  {c.dimension || "—"}
                </span>,
                <span key="f" style={{ fontSize: "0.85rem" }}>
                  {flags.length ? flags.join(", ") : "—"}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}
