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

import type { CSSProperties } from "react";

export const shellStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  minHeight: "70vh",
  fontFamily: "system-ui, sans-serif",
  color: "#222",
};

export const headerStyle: CSSProperties = {
  padding: "12px 16px",
  borderBottom: "1px solid #ddd",
  background: "#f7f7f7",
};

export const navStyle: CSSProperties = {
  display: "flex",
  gap: "8px",
  flexWrap: "wrap",
  padding: "8px 16px",
  borderBottom: "1px solid #eee",
};

export const navButtonStyle = (active: boolean): CSSProperties => ({
  padding: "8px 14px",
  border: active ? "1px solid #0b6" : "1px solid #ccc",
  background: active ? "#e8fff4" : "#fff",
  borderRadius: 4,
  cursor: "pointer",
  fontWeight: active ? 600 : 400,
});

export const mainStyle: CSSProperties = {
  padding: "16px",
  flex: 1,
};

export const listStyle: CSSProperties = {
  listStyle: "none",
  margin: 0,
  padding: 0,
};

export const listItemStyle: CSSProperties = {
  padding: "10px 12px",
  borderBottom: "1px solid #eee",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: "12px",
};

export const errorStyle: CSSProperties = {
  color: "#a00",
  padding: "8px 0",
};

export const formRowStyle: CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 4,
  marginBottom: 12,
  maxWidth: 420,
};
