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

import React, { useEffect, useMemo, useState } from "react";
import {
  asContentTypeText,
  contentTypeSelectionKey,
  listContentTypes,
  unwrapContentTypeList,
} from "../api/developer/contentTypesApi";
import type { ContentTypeSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  applyObjectSorter,
  customOrderFromRows,
  DEFAULT_OBJECT_SORTER_MODE,
  isObjectSorterMode,
  loadObjectSorterPreference,
  moveObjectSorterId,
  saveObjectSorterPreference,
  type ObjectSorterMode,
  type ObjectSorterPreference,
  type ObjectSorterRow,
} from "./objectSorter";

function asCatalogText(value: unknown): string {
  return asContentTypeText(value);
}

function asContentTypeCatalog(raw: unknown): ContentTypeSummary[] {
  return unwrapContentTypeList(raw);
}

export function contentTypeToSorterRow(ct: ContentTypeSummary): ObjectSorterRow {
  const name = asCatalogText(ct.name);
  const label = asCatalogText(ct.label);
  const id = contentTypeSelectionKey(ct) || name;
  return { id, name, label };
}

function modeLabel(mode: ObjectSorterMode): string {
  switch (mode) {
    case "name-asc":
      return DEV_MSG.OS_MODE_NAME_ASC;
    case "name-desc":
      return DEV_MSG.OS_MODE_NAME_DESC;
    case "label-desc":
      return DEV_MSG.OS_MODE_LABEL_DESC;
    case "custom":
      return DEV_MSG.OS_MODE_CUSTOM;
    case "label-asc":
    default:
      return DEV_MSG.OS_MODE_LABEL_ASC;
  }
}

const selectStyle: React.CSSProperties = {
  padding: "6px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

/**
 * Developer Object Sorter — organize the current Content Types catalog in-session
 * (#4344 / Workbench §12.3). Sort preference is session-only (no REST peer).
 */
export function ObjectSorterPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pref, setPref] = useState<ObjectSorterPreference>(() =>
    loadObjectSorterPreference(),
  );

  useEffect(() => {
    let cancelled = false;
    listContentTypes()
      .then((list) => {
        if (!cancelled) setItems(asContentTypeCatalog(list));
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.OS_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function persist(next: ObjectSorterPreference): void {
    setPref(next);
    saveObjectSorterPreference(next);
  }

  const sorted = useMemo(() => {
    if (!items) return [];
    return applyObjectSorter(items, contentTypeToSorterRow, pref);
  }, [items, pref]);

  function handleModeChange(raw: string): void {
    if (!isObjectSorterMode(raw)) {
      return;
    }
    if (raw === "custom") {
      const order = customOrderFromRows(
        applyObjectSorter(items ?? [], contentTypeToSorterRow, {
          ...pref,
          mode: pref.mode === "custom" ? DEFAULT_OBJECT_SORTER_MODE : pref.mode,
        }).map(contentTypeToSorterRow),
      );
      persist({ ...pref, mode: "custom", customOrder: order });
      return;
    }
    persist({ ...pref, mode: raw });
  }

  function handleMove(id: string, direction: "up" | "down"): void {
    const order =
      pref.customOrder.length > 0
        ? pref.customOrder
        : customOrderFromRows(sorted.map(contentTypeToSorterRow));
    persist({
      ...pref,
      mode: "custom",
      customOrder: moveObjectSorterId(order, id, direction),
    });
  }

  if (error) {
    return (
      <CatalogStatus testId="developer-os-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (items == null) {
    return (
      <CatalogStatus testId="developer-os-loading">{DEV_MSG.OS_LOADING}</CatalogStatus>
    );
  }

  return (
    <div data-testid="developer-os-panel" data-os-mode={pref.mode}>
      <CatalogHint>{DEV_MSG.OS_HINT}</CatalogHint>
      <p
        data-testid="developer-os-session-note"
        style={{ color: catalogColors.muted, fontSize: "0.9rem", marginBottom: "12px" }}
      >
        {DEV_MSG.OS_SESSION_ONLY}
      </p>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "8px",
          alignItems: "center",
          marginBottom: "12px",
        }}
      >
        <label htmlFor="developer-os-mode" style={{ fontWeight: 600 }}>
          {DEV_MSG.OS_SORT_LABEL}
        </label>
        <select
          id="developer-os-mode"
          data-testid="developer-os-mode"
          value={pref.mode}
          onChange={(e) => handleModeChange(e.target.value)}
          style={selectStyle}
        >
          {(["label-asc", "label-desc", "name-asc", "name-desc", "custom"] as const).map(
            (mode) => (
              <option key={mode} value={mode}>
                {modeLabel(mode)}
              </option>
            ),
          )}
        </select>
      </div>
      {sorted.length === 0 ? (
        <CatalogStatus testId="developer-os-empty">{DEV_MSG.OS_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-os-table"
          rowTestId="developer-os-row"
          columns={[
            DEV_MSG.OS_COL_NAME,
            DEV_MSG.OS_COL_LABEL,
            pref.mode === "custom" ? DEV_MSG.OS_COL_ORDER : "",
          ]}
          rows={sorted.map((ct, index) => {
            const row = contentTypeToSorterRow(ct);
            return {
              key: row.id || `row-${index}`,
              dataAttrs: {
                "data-os-id": row.id,
                "data-os-name": row.name,
              },
              cells: [
                <span key="n" data-testid="developer-os-name" style={mutedCell}>
                  {row.name || "—"}
                </span>,
                <span key="l" data-testid="developer-os-label">
                  {row.label || row.name || "—"}
                </span>,
                pref.mode === "custom" ? (
                  <span key="ord" style={{ display: "inline-flex", gap: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-os-move-up"
                      data-os-id={row.id}
                      disabled={index === 0}
                      aria-label={`${DEV_MSG.OS_MOVE_UP} ${row.name || row.label}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
                        handleMove(row.id, "up");
                      }}
                      style={{
                        ...openButtonStyle,
                        opacity: index === 0 ? 0.4 : 1,
                        cursor: index === 0 ? "default" : "pointer",
                      }}
                    >
                      {DEV_MSG.OS_MOVE_UP}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-os-move-down"
                      data-os-id={row.id}
                      disabled={index === sorted.length - 1}
                      aria-label={`${DEV_MSG.OS_MOVE_DOWN} ${row.name || row.label}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
                        handleMove(row.id, "down");
                      }}
                      style={{
                        ...openButtonStyle,
                        opacity: index === sorted.length - 1 ? 0.4 : 1,
                        cursor: index === sorted.length - 1 ? "default" : "pointer",
                      }}
                    >
                      {DEV_MSG.OS_MOVE_DOWN}
                    </button>
                  </span>
                ) : (
                  <span key="ord" />
                ),
              ],
            };
          })}
        />
      )}
    </div>
  );
}
