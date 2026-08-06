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

import type { CSSProperties } from "react";

/**
 * Inline styles for the modern Content Explorer.
 *
 * <p>Inline styles avoid shipping a CSS asset and match the
 * {@code WebUI/src/main/ts/home/home.styles.ts} pattern. Bootstrap classes
 * can layer on top via {@code className} per component.</p>
 */

export const shellStyle: CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(220px, 320px) 1fr",
  gridTemplateRows: "auto 1fr",
  gridTemplateAreas: '"header header" "tree list"',
  minHeight: "70vh",
  fontFamily: "system-ui, sans-serif",
  color: "#222",
  border: "1px solid #ddd",
  borderRadius: 4,
  background: "#fff",
};

export const headerStyle: CSSProperties = {
  gridArea: "header",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 12,
  padding: "10px 14px",
  borderBottom: "1px solid #ddd",
  background: "#f7f7f7",
};

export const headerTitleStyle: CSSProperties = {
  margin: 0,
  fontSize: "1.05rem",
  fontWeight: 600,
};

export const actionsBarStyle: CSSProperties = {
  display: "flex",
  gap: 6,
  flexWrap: "wrap",
};

export const actionButtonStyle = (disabled: boolean): CSSProperties => ({
  padding: "5px 10px",
  border: "1px solid #ccc",
  background: disabled ? "#f0f0f0" : "#fff",
  color: disabled ? "#999" : "#222",
  borderRadius: 3,
  fontSize: "0.85rem",
  cursor: disabled ? "not-allowed" : "pointer",
});

export const treeStyle: CSSProperties = {
  gridArea: "tree",
  borderRight: "1px solid #ddd",
  overflow: "auto",
  padding: "8px 0",
  minHeight: 200,
  background: "#fafafa",
};

export const listStyle: CSSProperties = {
  gridArea: "list",
  overflow: "auto",
  padding: 0,
  background: "#fff",
};

export const nodeRowStyle = (
  selected: boolean,
  depth: number,
): CSSProperties => ({
  display: "flex",
  alignItems: "center",
  gap: 4,
  padding: "3px 8px",
  paddingLeft: 8 + depth * 14,
  cursor: "pointer",
  background: selected ? "#e8f0fe" : "transparent",
  borderLeft: selected ? "3px solid #1a73e8" : "3px solid transparent",
  fontSize: "0.9rem",
});

export const toggleStyle: CSSProperties = {
  display: "inline-block",
  width: 14,
  textAlign: "center",
  cursor: "pointer",
  userSelect: "none",
  color: "#666",
};

export const nodeLabelStyle: CSSProperties = {
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
  flex: 1,
};

export const tableStyle: CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.9rem",
};

export const theadStyle: CSSProperties = {
  position: "sticky",
  top: 0,
  background: "#f7f7f7",
  borderBottom: "1px solid #ddd",
  textAlign: "left",
  zIndex: 1,
};

export const thCellStyle: CSSProperties = {
  padding: "6px 10px",
  fontWeight: 600,
};

export const rowStyle = (selected: boolean): CSSProperties => ({
  cursor: "pointer",
  background: selected ? "#e8f0fe" : "#fff",
});

export const tdCellStyle: CSSProperties = {
  padding: "6px 10px",
  borderBottom: "1px solid #f0f0f0",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
  maxWidth: 320,
};

export const emptyStateStyle: CSSProperties = {
  padding: "24px 16px",
  color: "#777",
  fontStyle: "italic",
  textAlign: "center",
};

export const errorStateStyle: CSSProperties = {
  padding: "16px",
  color: "#b00020",
  background: "#fff5f5",
  border: "1px solid #f5c2c7",
  borderRadius: 4,
  margin: 8,
};