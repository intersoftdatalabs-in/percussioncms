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
 * Detail-list row-density switch (#5883 / parent #4530).
 *
 * <p>A Comfortable/Compact switch above the list changes row metrics per
 * operator, persisted client-side in {@code localStorage} with zero new
 * REST. Density is styling only: row identity, roving-tabindex arrow-key
 * navigation, and screen-reader row positions are identical in both
 * densities.</p>
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import {
  DETAIL_DENSITY_STORAGE_KEY,
  DetailList,
  loadDetailDensity,
  parseDetailDensity,
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

async function renderListing(): Promise<void> {
  mockFetch(async () => listingResponse(ROWS));
  render(
    <DetailList
      folderPath="/Sites/Demo"
      selectedItemId={null}
      onSelectItem={() => undefined}
    />,
  );
  await waitFor(() =>
    expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(),
  );
}

function storedDensity(): string | null {
  return localStorage.getItem(DETAIL_DENSITY_STORAGE_KEY);
}

describe("detail-list density helpers", () => {
  it("parseDetailDensity defaults to comfortable on anything but compact", () => {
    expect(parseDetailDensity(null)).toBe("comfortable");
    expect(parseDetailDensity(undefined)).toBe("comfortable");
    expect(parseDetailDensity("")).toBe("comfortable");
    expect(parseDetailDensity("not json")).toBe("comfortable");
    expect(parseDetailDensity('"comfortable"')).toBe("comfortable");
    expect(parseDetailDensity('"COMPACT"')).toBe("comfortable");
    expect(parseDetailDensity("42")).toBe("comfortable");
    expect(parseDetailDensity("null")).toBe("comfortable");
    expect(parseDetailDensity('"compact"')).toBe("compact");
  });

  it("loadDetailDensity reads storage and falls back to comfortable", () => {
    localStorage.clear();
    expect(loadDetailDensity()).toBe("comfortable");
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, '"compact"');
    expect(loadDetailDensity()).toBe("compact");
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, "corrupt!!");
    expect(loadDetailDensity()).toBe("comfortable");
  });
});

describe("DetailList density rendering", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("defaults to comfortable with the switch reading pressed", async () => {
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
    const root = screen.getByTestId("detail-list");
    expect(root).toHaveAttribute("data-density", "comfortable");
    expect(screen.getByTestId("detail-density-comfortable")).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByTestId("detail-density-compact")).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(root.querySelector("table")).toHaveAttribute("style", expect.stringContaining("font-size: 0.9rem"));
    await renderA11yGate(container);
  });

  it("switching to compact tightens rows and persists the choice", async () => {
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

    fireEvent.click(screen.getByTestId("detail-density-compact"));
    const root = screen.getByTestId("detail-list");
    await waitFor(() =>
      expect(root).toHaveAttribute("data-density", "compact"),
    );
    expect(screen.getByTestId("detail-density-compact")).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByTestId("detail-density-comfortable")).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    // Compact metrics: smaller type + halved vertical cell padding.
    expect(root.querySelector("table")).toHaveAttribute("style", expect.stringContaining("font-size: 0.8rem"));
    expect(screen.getByTestId("detail-cell-name-c-3")).toHaveAttribute(
      "style",
      expect.stringContaining("padding: 2px 10px"),
    );
    expect(JSON.parse(storedDensity() ?? '"comfortable"')).toBe("compact");
    // Rows, headers, and cells are unchanged — density is styling only.
    expect(screen.getByTestId("detail-col-header-name")).toBeInTheDocument();
    expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute(
      "aria-rowindex",
      "1",
    );
    expect(root.querySelector("table")).toHaveAttribute("aria-rowcount", "3");
    await renderA11yGate(container);
  });

  it("switching back to comfortable restores metrics and persists", async () => {
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, '"compact"');
    await renderListing();
    expect(screen.getByTestId("detail-list")).toHaveAttribute(
      "data-density",
      "compact",
    );

    fireEvent.click(screen.getByTestId("detail-density-comfortable"));
    const root = screen.getByTestId("detail-list");
    await waitFor(() =>
      expect(root).toHaveAttribute("data-density", "comfortable"),
    );
    expect(root.querySelector("table")).toHaveAttribute("style", expect.stringContaining("font-size: 0.9rem"));
    expect(screen.getByTestId("detail-cell-name-c-3")).toHaveAttribute(
      "style",
      expect.stringContaining("padding: 6px 10px"),
    );
    expect(JSON.parse(storedDensity() ?? '"compact"')).toBe("comfortable");
  });

  it("restores persisted compact density on mount", async () => {
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, '"compact"');
    await renderListing();
    expect(screen.getByTestId("detail-list")).toHaveAttribute(
      "data-density",
      "compact",
    );
    expect(screen.getByTestId("detail-density-compact")).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(
      screen.getByTestId("detail-list").querySelector("table"),
    ).toHaveAttribute("style", expect.stringContaining("font-size: 0.8rem"));
  });

  it("corrupt persisted density falls back to comfortable", async () => {
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, "corrupt!!");
    await renderListing();
    expect(screen.getByTestId("detail-list")).toHaveAttribute(
      "data-density",
      "comfortable",
    );
  });

  it("keeps ArrowUp/Down/Home/End nav and row positions in compact density", async () => {
    localStorage.setItem(DETAIL_DENSITY_STORAGE_KEY, '"compact"');
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
    expect(screen.getByTestId("detail-list")).toHaveAttribute(
      "data-density",
      "compact",
    );

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "detail-row-a-1",
    );
    expect(selected).toContain("a-1");

    fireEvent.keyDown(screen.getByTestId("detail-row-a-1"), { key: "End" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "detail-row-b-2",
    );
    fireEvent.keyDown(screen.getByTestId("detail-row-b-2"), { key: "Home" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "detail-row-c-3",
    );
    fireEvent.keyDown(screen.getByTestId("detail-row-c-3"), { key: "ArrowUp" });
    expect(document.activeElement?.getAttribute("data-testid")).toBe(
      "detail-row-b-2",
    );

    // Screen-reader positions are identical in compact density.
    expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute(
      "aria-rowindex",
      "1",
    );
    expect(screen.getByTestId("detail-row-b-2")).toHaveAttribute(
      "aria-rowindex",
      "3",
    );
    expect(
      screen.getByTestId("detail-list").querySelector("table"),
    ).toHaveAttribute("aria-rowcount", "3");
  });
});
