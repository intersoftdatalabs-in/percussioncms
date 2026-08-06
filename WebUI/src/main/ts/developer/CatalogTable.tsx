/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React from "react";
import { catalogColors, tableHeaderRow, tableRow } from "./catalogStyles";

export function CatalogHint({ children }: { children: React.ReactNode }): React.ReactElement {
  return (
    <p style={{ color: catalogColors.muted, marginBottom: "12px", fontSize: "0.9rem" }}>{children}</p>
  );
}

export function CatalogStatus({
  testId,
  children,
  error,
}: {
  testId: string;
  children: React.ReactNode;
  error?: boolean;
}): React.ReactElement {
  return (
    <div
      data-testid={testId}
      role={error ? "alert" : undefined}
      style={{ padding: "0.5rem 0", color: error ? catalogColors.error : undefined }}
    >
      {children}
    </div>
  );
}

/** One data row for {@link SimpleCatalogTable}. */
export type SimpleCatalogRow = {
  key: string;
  cells: React.ReactNode[];
  /** When set, row is clickable (pointer cursor + keyboard + onClick). */
  onClick?: () => void;
};

/**
 * Shared list-catalog chrome used by Developer *Panel* browse tables.
 * Open-to-detail controls live in cell content (preserve panel testids there).
 *
 * Row testids are unique per index: `${rowTestId}-${index}` (legacy indexed contract).
 * Clickable rows get role="button", tabIndex=0, and Enter/Space activation.
 */
export function SimpleCatalogTable({
  tableTestId,
  rowTestId,
  columns,
  rows,
}: {
  tableTestId: string;
  /** Base test id; each row becomes `${rowTestId}-${index}`. */
  rowTestId: string;
  columns: string[];
  rows: SimpleCatalogRow[];
}): React.ReactElement {
  return (
    <div style={{ overflowX: "auto" }}>
      <table
        data-testid={tableTestId}
        style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
      >
        <thead>
          <tr style={tableHeaderRow}>
            {columns.map((c) => (
              <th key={c} style={{ padding: "8px" }}>
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, index) => {
            const clickable = r.onClick != null;
            return (
              <tr
                key={r.key}
                data-testid={`${rowTestId}-${index}`}
                style={{
                  ...tableRow,
                  cursor: clickable ? "pointer" : undefined,
                }}
                onClick={r.onClick}
                role={clickable ? "button" : undefined}
                tabIndex={clickable ? 0 : undefined}
                onKeyDown={
                  clickable
                    ? (e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          r.onClick?.();
                        }
                      }
                    : undefined
                }
              >
                {r.cells.map((cell, i) => (
                  <td key={i} style={{ padding: "8px" }}>
                    {cell}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
