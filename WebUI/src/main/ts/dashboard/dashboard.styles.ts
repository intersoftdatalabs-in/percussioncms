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

/**
 * Shared styles for dashboard and widgets.
 *
 * <p>Matches the legacy dashboard color scheme and layout for visual
 * consistency during the transition period.</p>
 */

import type { CSSProperties } from "react";

export const styles = {
  container: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "20px",
    padding: "20px",
    backgroundColor: "#f5f5f5",
    minHeight: "100vh",
  } as CSSProperties,

  column: {
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  } as CSSProperties,

  widget: {
    backgroundColor: "#fff",
    border: "1px solid #ddd",
    borderRadius: "4px",
    boxShadow: "0 2px 4px rgba(0, 0, 0, 0.1)",
    overflow: "hidden",
  } as CSSProperties,

  widgetTitle: {
    margin: "0",
    padding: "12px 16px",
    backgroundColor: "#007ea8",
    color: "#fff",
    fontSize: "16px",
    fontWeight: "600",
    borderBottom: "1px solid #ddd",
  } as CSSProperties,

  widgetContent: {
    padding: "16px",
    fontSize: "14px",
    color: "#333",
  } as CSSProperties,

  widgetLoading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    minHeight: "200px",
    color: "#999",
  } as CSSProperties,

  widgetError: {
    backgroundColor: "#fee",
    color: "#c33",
    padding: "16px",
    borderRadius: "4px",
    fontSize: "13px",
  } as CSSProperties,

  link: {
    color: "#007ea8",
    textDecoration: "none",
    cursor: "pointer",
  } as CSSProperties,

  linkedHover: {
    color: "#005a7a",
    textDecoration: "underline",
  } as CSSProperties,

  listStyleNone: {
    listStyle: "none",
    padding: "0",
    margin: "0",
  } as CSSProperties,
};
