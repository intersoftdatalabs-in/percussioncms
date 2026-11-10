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

import React from "react";
import { catalogColors } from "./catalogStyles";
import { DEV_MSG } from "./messages";

/**
 * Client-side substring match for Developer thin-read catalogs (#5995).
 *
 * Case-insensitive, trims the query, and ignores null/undefined fields. An
 * empty (or blank) query matches every row so clearing the box restores the
 * full server list without a refetch. Pure function — never mutates catalog
 * data and never calls the server.
 */
export function matchesCatalogQuery(
  fields: (string | number | null | undefined)[],
  query: string | null | undefined,
): boolean {
  const needle = (query ?? "").trim().toLowerCase();
  if (!needle) {
    return true;
  }
  return fields.some((field) => {
    if (field == null) {
      return false;
    }
    return String(field).toLowerCase().includes(needle);
  });
}

const searchInputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  minWidth: "220px",
};

/**
 * Shared catalog search box for Developer thin-read catalog panels
 * (communities, roles, slots, display formats, workflows — #5995).
 *
 * Fully client-side: {@code query} / {@code onQueryChange} are owned by the
 * panel, which derives the visible rows with {@link matchesCatalogQuery}.
 * Typing never triggers a fetch and never writes catalog data.
 */
export function CatalogSearchFilter({
  testId,
  query,
  onQueryChange,
}: {
  /** Base test id; the input is `${testId}` and the label `${testId}-label`. */
  testId: string;
  query: string;
  onQueryChange: (next: string) => void;
}): React.ReactElement {
  const inputId = `${testId}-input`;
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "8px",
        marginBottom: "12px",
        flexWrap: "wrap",
      }}
    >
      <label
        htmlFor={inputId}
        data-testid={`${testId}-label`}
        style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
      >
        {DEV_MSG.CATALOG_SEARCH_LABEL}
      </label>
      <input
        id={inputId}
        type="search"
        data-testid={testId}
        style={searchInputStyle}
        value={query}
        autoComplete="off"
        placeholder={DEV_MSG.CATALOG_SEARCH_PLACEHOLDER}
        onChange={(event) => onQueryChange(event.target.value)}
      />
    </div>
  );
}
