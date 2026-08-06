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

import type { CSSProperties } from "react";

export const shellStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  minHeight: "70vh",
  fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif",
  color: "#1a1a1a",
  background: "#fafafa",
};

export const headerStyle: CSSProperties = {
  padding: "14px 20px",
  borderBottom: "1px solid #e2e2e2",
  background: "linear-gradient(180deg, #ffffff 0%, #f5f7f6 100%)",
};

export const navStyle: CSSProperties = {
  display: "flex",
  gap: "6px",
  flexWrap: "wrap",
  padding: "10px 16px",
  borderBottom: "1px solid #e8e8e8",
  background: "#fff",
};

export const navButtonStyle = (active: boolean): CSSProperties => ({
  padding: "8px 14px",
  border: active ? "1px solid #0a7a4a" : "1px solid #d0d0d0",
  background: active ? "#e6f7ef" : "#fff",
  color: active ? "#065c38" : "#333",
  borderRadius: 6,
  cursor: "pointer",
  fontWeight: active ? 600 : 500,
  fontSize: "0.9rem",
  transition: "background 0.12s ease, border-color 0.12s ease",
});

export const mainStyle: CSSProperties = {
  padding: "16px 20px 24px",
  flex: 1,
  maxWidth: 960,
  width: "100%",
  boxSizing: "border-box",
};

export const listStyle: CSSProperties = {
  listStyle: "none",
  margin: 0,
  padding: 0,
  border: "1px solid #e6e6e6",
  borderRadius: 8,
  background: "#fff",
  overflow: "hidden",
};

export const listItemStyle: CSSProperties = {
  padding: "12px 14px",
  borderBottom: "1px solid #eee",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: "12px",
  flexWrap: "wrap",
};

export const itemLabelStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 2,
  minWidth: 0,
  flex: "1 1 200px",
};

export const itemPrimaryStyle: CSSProperties = {
  fontWeight: 600,
  fontSize: "0.95rem",
  wordBreak: "break-word",
};

export const itemMetaStyle: CSSProperties = {
  fontSize: "0.8rem",
  color: "#666",
  wordBreak: "break-all",
};

export const itemActionsStyle: CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: 8,
  alignItems: "center",
  flex: "0 0 auto",
};

export type ActionVariant = "primary" | "secondary" | "danger" | "ghost";

export const actionButtonStyle = (
  variant: ActionVariant = "secondary",
): CSSProperties => {
  const base: CSSProperties = {
    padding: "6px 12px",
    borderRadius: 6,
    cursor: "pointer",
    fontSize: "0.85rem",
    fontWeight: 500,
    border: "1px solid transparent",
    lineHeight: 1.3,
  };
  switch (variant) {
    case "primary":
      return {
        ...base,
        background: "#0a7a4a",
        color: "#fff",
        borderColor: "#08663e",
      };
    case "danger":
      return {
        ...base,
        background: "#fff",
        color: "#a32020",
        borderColor: "#d8a0a0",
      };
    case "ghost":
      return {
        ...base,
        background: "transparent",
        color: "#444",
        borderColor: "transparent",
        textDecoration: "underline",
      };
    case "secondary":
    default:
      return {
        ...base,
        background: "#fff",
        color: "#222",
        borderColor: "#ccc",
      };
  }
};

export const emptyStateStyle: CSSProperties = {
  padding: "28px 20px",
  textAlign: "center",
  color: "#555",
  background: "#fff",
  border: "1px dashed #ccc",
  borderRadius: 8,
  margin: 0,
};

export const sectionHintStyle: CSSProperties = {
  margin: "0 0 12px",
  color: "#555",
  fontSize: "0.9rem",
};

export const errorStyle: CSSProperties = {
  color: "#a00",
  padding: "10px 12px",
  margin: "0 0 12px",
  background: "#fff5f5",
  border: "1px solid #f0c0c0",
  borderRadius: 6,
};

export const formRowStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 4,
  marginBottom: 12,
  maxWidth: 480,
};

export const searchInputStyle: CSSProperties = {
  padding: "8px 10px",
  border: "1px solid #ccc",
  borderRadius: 6,
  fontSize: "0.95rem",
  width: "100%",
  boxSizing: "border-box",
};

export const searchFormActionsStyle: CSSProperties = {
  display: "flex",
  gap: 8,
  alignItems: "center",
  marginBottom: 16,
};
