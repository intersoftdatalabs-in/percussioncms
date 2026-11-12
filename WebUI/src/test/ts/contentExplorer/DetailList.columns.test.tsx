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

/**
 * Detail-list column visibility toggle (#5881 / parent #4530).
 *
 * <p>A Columns menu toggles the resolved display-format columns per
 * operator, persisted client-side in {@code localStorage} with zero new
 * REST. At least one column always stays visible; selection, arrow-key
 * navigation, and sorting keep working with hidden columns.</p>
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import {
  DETAIL_COLUMN_VISIBILITY_STORAGE_KEY,
  DetailList,
  applyHiddenDetailColumns,
  parseHiddenDetailColumns,
  toggleHiddenDetailColumn,
} from "../../../main/ts/contentExplorer/DetailList";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

function item(id: string, name: string, type = "percPage"): PSPathItem {
  return {
    id,
    path: `/Sites/Demo/${name}`,
    name,
    type,
    category: "page",
    accessLevel: "WRITE",
    leaf: true,
  } as PSPathItem;
}

const ROWS: PSPathItem[] = [
  item("c-3", "Cherry"),
  item("a-1", "apple"),
  item("b-2", "Banana"),
];

function listingResponse(rows: PSPathItem[]): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: rows.length,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

async function renderListing(): Promise<{ container: HTMLElement }> {
  mockFetch(async () => listingResponse(ROWS));
  const { container } = render(
    <DetailList
      folderPath="/Sites/Demo"
      selectedItemId={null}
      onSelectItem={() => undefined}
    />,
  );
  await waitFor(() =>
    expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(),
  );
  return { container };
}

describe("detail-list column visibility helpers", () => {
  it("parseHiddenDetailColumns keeps known ids and drops the rest", () => {
    expect(parseHiddenDetailColumns(null)).toEqual([]);
    expect(parseHiddenDetailColumns("")).toEqual([]);
    expect(parseHiddenDetailColumns("not json")).toEqual([]);
    expect(parseHiddenDetailColumns('{"name":true}')).toEqual([]);
    expect(
      parseHiddenDetailColumns('["type","nope",42,"type","path"]'),
    ).toEqual(["type", "path"]);
  });

  it("toggleHiddenDetailColumn hides, shows, and refuses the last hide", () => {
    const base = ["name", "type", "path"] as const;
    const cols = [...base];
    expect(toggleHiddenDetailColumn(cols, [], "type")).toEqual(["type"]);
    expect(toggleHiddenDetailColumn(cols, ["type"], "type")).toEqual([]);
    // Hiding the last visible column is refused.
    expect(
      toggleHiddenDetailColumn(cols, ["name", "path"], "type"),
    ).toEqual(["name", "path"]);
    // Unknown columns are ignored.
    expect(
      toggleHiddenDetailColumn(cols, [], "workflow"),
    ).toEqual([]);
  });

  it("applyHiddenDetailColumns filters and never returns empty", () => {
    const cols = ["name", "type", "path"] as (
      | "name"
      | "type"
      | "path"
    )[];
    expect(applyHiddenDetailColumns(cols, ["type"])).toEqual([
      "name",
      "path",
    ]);
    // Stale storage covering everything keeps the first column.
    expect(applyHiddenDetailColumns(cols, ["name", "type", "path"])).toEqual([
      "name",
    ]);
    expect(applyHiddenDetailColumns([], ["name"])).toEqual([]);
  });
});

describe("DetailList column visibility rendering", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("shows the Columns toggle with a closed menu by default", async () => {
    await renderListing();
    expect(screen.getByTestId("detail-columns-toggle")).toHaveAttribute(
      "aria-expanded",
      "false",
    );
    expect(screen.queryByTestId("detail-columns-menu")).not.toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-name")).toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-type")).toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-path")).toBeInTheDocument();
  });

  it("unchecking a column hides its header and cells and persists the choice", async () => {
    await renderListing();
    fireEvent.click(screen.getByTestId("detail-columns-toggle"));
    expect(screen.getByTestId("detail-columns-menu")).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("detail-column-toggle-type"));
    await waitFor(() =>
      expect(
        screen.queryByTestId("detail-col-header-type"),
      ).not.toBeInTheDocument(),
    );
    expect(
      screen.queryByTestId("detail-cell-type-c-3"),
    ).not.toBeInTheDocument();
    // Name + path stay.
    expect(screen.getByTestId("detail-col-header-name")).toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-path")).toBeInTheDocument();
    expect(
      JSON.parse(
        localStorage.getItem(DETAIL_COLUMN_VISIBILITY_STORAGE_KEY) ?? "[]",
      ),
    ).toEqual(["type"]);
  });

  it("re-checking a hidden column shows it again", async () => {
    await renderListing();
    fireEvent.click(screen.getByTestId("detail-columns-toggle"));
    fireEvent.click(screen.getByTestId("detail-column-toggle-type"));
    await waitFor(() =>
      expect(
        screen.queryByTestId("detail-col-header-type"),
      ).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-column-toggle-type"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-type")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("detail-cell-type-c-3")).toBeInTheDocument();
  });

  it("locks the last visible column on so the list never renders columnless", async () => {
    await renderListing();
    fireEvent.click(screen.getByTestId("detail-columns-toggle"));
    fireEvent.click(screen.getByTestId("detail-column-toggle-type"));
    fireEvent.click(screen.getByTestId("detail-column-toggle-path"));
    await waitFor(() =>
      expect(
        screen.queryByTestId("detail-col-header-path"),
      ).not.toBeInTheDocument(),
    );
    // Only name remains: its checkbox is disabled and stays checked.
    const nameToggle = screen.getByTestId("detail-column-toggle-name");
    expect(nameToggle).toBeDisabled();
    expect(nameToggle).toHaveAttribute("title", expect.stringContaining("visible"));
    fireEvent.click(nameToggle);
    expect(screen.getByTestId("detail-col-header-name")).toBeInTheDocument();
    expect(screen.getByTestId("detail-cell-name-c-3")).toBeInTheDocument();
  });

  it("restores persisted hidden columns on mount", async () => {
    localStorage.setItem(
      DETAIL_COLUMN_VISIBILITY_STORAGE_KEY,
      JSON.stringify(["path"]),
    );
    await renderListing();
    expect(
      screen.queryByTestId("detail-col-header-path"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-name")).toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-type")).toBeInTheDocument();
  });

  it("keeps arrow-key row navigation working with a hidden column", async () => {
    const selected: string[] = [];
    mockFetch(async () => listingResponse(ROWS));
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-columns-toggle"));
    fireEvent.click(screen.getByTestId("detail-column-toggle-type"));
    await waitFor(() =>
      expect(
        screen.queryByTestId("detail-col-header-type"),
      ).not.toBeInTheDocument(),
    );

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(selected).toContain("a-1");
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "detail-row-a-1",
    );
  });

  it("columns-menu surface has no serious a11y violations", async () => {
    const { container } = await renderListing();
    fireEvent.click(screen.getByTestId("detail-columns-toggle"));
    expect(screen.getByTestId("detail-columns-menu")).toBeInTheDocument();
    await renderA11yGate(container);
  });
});
