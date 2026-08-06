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

import type React from "react";

/**
 * Shared color tokens for Developer catalog tables and detail panels.
 * Prefer these over inlining hex so theme tweaks stay in one place (UI-STYLE-01).
 */
export const catalogColors = {
  /** Secondary / muted body text (#4a5568). */
  muted: "#4a5568",
  /** Empty-state and placeholder text (#718096). */
  empty: "#718096",
  /** Table header bottom border (#e2e8f0). */
  headerBorder: "#e2e8f0",
  /** Table body row bottom border (#edf2f7). */
  rowBorder: "#edf2f7",
  /** Open-link / accent / active chrome (#007ea8). */
  accent: "#007ea8",
  /** Error / alert text (#b00020). */
  error: "#b00020",
  /** Soft control borders (#cbd5e0). */
  softBorder: "#cbd5e0",
  /** Disabled / inactive primary button fill (#a0aec0). */
  disabled: "#a0aec0",
  /** Default body text for inactive chrome (#2d3748). */
  text: "#2d3748",
} as const;

/** Shared cell styles for Developer catalog tables. */
export const monoCell: React.CSSProperties = {
  fontFamily: "monospace",
};

export const mutedMonoCell: React.CSSProperties = {
  fontFamily: "monospace",
  color: catalogColors.muted,
};

export const mutedCell: React.CSSProperties = {
  color: catalogColors.muted,
};

export const mutedText: React.CSSProperties = {
  color: catalogColors.muted,
};

export const mutedHintText: React.CSSProperties = {
  color: catalogColors.muted,
  fontSize: "0.9rem",
};

export const emptyStateText: React.CSSProperties = {
  color: catalogColors.empty,
};

export const errorAlert: React.CSSProperties = {
  color: catalogColors.error,
};

export const backButton: React.CSSProperties = {
  marginBottom: "12px",
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "6px 12px",
  cursor: "pointer",
};

export const metaGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  gap: "4px 16px",
  marginTop: "12px",
  fontSize: "0.9rem",
};

/** Text-button style for opening a catalog row detail. */
export const openButtonStyle: React.CSSProperties = {
  background: "none",
  border: "none",
  padding: 0,
  color: catalogColors.accent,
  cursor: "pointer",
  font: "inherit",
  textAlign: "left",
  textDecoration: "underline",
};

/** Catalog table header row (left-aligned, 2px header border). */
export const tableHeaderRow: React.CSSProperties = {
  textAlign: "left",
  borderBottom: `2px solid ${catalogColors.headerBorder}`,
};

/** Catalog table body row (1px row border). */
export const tableRow: React.CSSProperties = {
  borderBottom: `1px solid ${catalogColors.rowBorder}`,
};
