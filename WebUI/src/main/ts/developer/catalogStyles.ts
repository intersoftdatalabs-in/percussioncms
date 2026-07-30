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

import type React from "react";

/** Shared cell styles for Developer catalog tables. */
export const monoCell: React.CSSProperties = {
  fontFamily: "monospace",
};

export const mutedMonoCell: React.CSSProperties = {
  fontFamily: "monospace",
  color: "#4a5568",
};

export const mutedCell: React.CSSProperties = {
  color: "#4a5568",
};

export const errorAlert: React.CSSProperties = {
  color: "#b00020",
};

export const backButton: React.CSSProperties = {
  marginBottom: "12px",
  background: "transparent",
  border: "1px solid #cbd5e0",
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
  color: "#007ea8",
  cursor: "pointer",
  font: "inherit",
  textAlign: "left",
  textDecoration: "underline",
};
