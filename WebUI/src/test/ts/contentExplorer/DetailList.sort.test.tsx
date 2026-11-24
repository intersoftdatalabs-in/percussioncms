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
 * Detail-list column sorting (#5759 / parent #4530).
 *
 * <p>Column headers re-sort the loaded page client-side with no REST
 * traffic: a new column starts ascending, the active column toggles.
 * Sorting reorders rows only — selection and checkbox state follow the
 * row identity.</p>
 */

import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  DetailList,
  compareDetailRows,
  nextDetailSort,
  sortDetailRows,
} from "../../../main/ts/contentExplorer/DetailList";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const SORT_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const SORT_TEST_TIMEOUT = 30_000;

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

const UNSORTED: PSPathItem[] = [
  item("c-3", "Cherry"),
  item("a-1", "apple"),
  item("b-2", "Banana"),
  item("p-10", "Page10"),
  item("p-2", "Page2"),
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

function rowOrder(): string[] {
  const rows = screen.getAllByTestId(/^detail-row-/, { exact: false });
  return rows.map((r) => r.getAttribute("data-item-name") ?? "");
}

describe("detail-list sort helpers", () => {
  it("nextDetailSort starts a new column ascending and toggles the active one", () => {
    expect(nextDetailSort(null, "name")).toEqual({ column: "name", direction: "asc" });
    expect(nextDetailSort({ column: "name", direction: "asc" }, "name")).toEqual({
      column: "name",
      direction: "desc",
    });
    expect(nextDetailSort({ column: "name", direction: "desc" }, "name")).toEqual({
      column: "name",
      direction: "asc",
    });
    expect(nextDetailSort({ column: "name", direction: "desc" }, "type")).toEqual({
      column: "type",
      direction: "asc",
    });
  });

  it("compareDetailRows is case-insensitive and numeric-aware", () => {
    expect(compareDetailRows(item("a", "apple"), item("b", "Banana"), "name")).toBeLessThan(0);
    expect(compareDetailRows(item("b", "Banana"), item("a", "apple"), "name")).toBeGreaterThan(0);
    expect(compareDetailRows(item("x", "Page2"), item("y", "Page10"), "name")).toBeLessThan(0);
    expect(compareDetailRows(item("x", "Same"), item("y", "Same"), "name")).toBe(0);
  });

  it("sortDetailRows keeps server order for a null sort", () => {
    const out = sortDetailRows(UNSORTED, null);
    expect(out.map((r) => r.id)).toEqual(UNSORTED.map((r) => r.id));
  });

  it("sortDetailRows sorts ascending and descending by name", () => {
    const asc = sortDetailRows(UNSORTED, { column: "name", direction: "asc" });
    expect(asc.map((r) => r.name)).toEqual(["apple", "Banana", "Cherry", "Page2", "Page10"]);
    const desc = sortDetailRows(UNSORTED, { column: "name", direction: "desc" });
    expect(desc.map((r) => r.name)).toEqual(["Page10", "Page2", "Cherry", "Banana", "apple"]);
  });

  it("sortDetailRows is stable for equal keys", () => {
    const rows = [item("1", "Same"), item("2", "Same"), item("3", "Same")];
    const out = sortDetailRows(rows, { column: "name", direction: "desc" });
    expect(out.map((r) => r.id)).toEqual(["1", "2", "3"]);
  });
});

describe("DetailList column sort rendering", () => {
  it("toggles name ascending then descending with aria-sort, issuing no extra fetch", { timeout: SORT_TEST_TIMEOUT }, async () => {
    let fetchCalls = 0;
    mockFetch(async () => {
      fetchCalls += 1;
      return listingResponse(UNSORTED);
    });
    const selected: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(), { timeout: SORT_ROUND_TRIP_TIMEOUT });
    // Server order first.
    expect(rowOrder()).toEqual(["Cherry", "apple", "Banana", "Page10", "Page2"]);
    expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute("aria-sort", "none");

    fireEvent.click(screen.getByTestId("detail-col-sort-name"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute(
        "aria-sort",
        "ascending",
      ),
      { timeout: SORT_ROUND_TRIP_TIMEOUT },
    );
    expect(rowOrder()).toEqual(["apple", "Banana", "Cherry", "Page2", "Page10"]);

    fireEvent.click(screen.getByTestId("detail-col-sort-name"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute(
        "aria-sort",
        "descending",
      ),
      { timeout: SORT_ROUND_TRIP_TIMEOUT },
    );
    expect(rowOrder()).toEqual(["Page10", "Page2", "Cherry", "Banana", "apple"]);

    // Sorting is client-side: exactly one listing fetch for the folder.
    expect(fetchCalls).toBe(1);
    expect(selected).toEqual([]);
  });

  it("keeps the selected row selected across a re-sort", { timeout: SORT_TEST_TIMEOUT }, async () => {
    mockFetch(async () => listingResponse(UNSORTED));
    const { rerender } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-a-1")).toBeInTheDocument(), { timeout: SORT_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("detail-row-a-1"));
    rerender(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId="a-1"
        onSelectItem={() => undefined}
      />,
    );
    fireEvent.click(screen.getByTestId("detail-col-sort-name"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute(
        "aria-sort",
        "ascending",
      ),
      { timeout: SORT_ROUND_TRIP_TIMEOUT },
    );
    const row = screen.getByTestId("detail-row-a-1");
    expect(row).toHaveAttribute("data-selected", "true");
    expect(rowOrder()[0]).toBe("apple");
  });

  it("switching folders restores server order", { timeout: SORT_TEST_TIMEOUT }, async () => {
    mockFetch(async () => listingResponse(UNSORTED));
    const { rerender } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument(), { timeout: SORT_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("detail-col-sort-name"));
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute(
        "aria-sort",
        "ascending",
      ),
      { timeout: SORT_ROUND_TRIP_TIMEOUT },
    );
    rerender(
      <DetailList
        folderPath="/Sites/Other"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-col-header-name")).toHaveAttribute(
        "aria-sort",
        "none",
      ),
      { timeout: SORT_ROUND_TRIP_TIMEOUT },
    );
    expect(rowOrder()).toEqual(["Cherry", "apple", "Banana", "Page10", "Page2"]);
  });

  it("sort buttons expose an accessible sort label", { timeout: SORT_TEST_TIMEOUT }, async () => {
    mockFetch(async () => listingResponse(UNSORTED));
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-col-sort-name")).toBeInTheDocument(), { timeout: SORT_ROUND_TRIP_TIMEOUT });
    const nameButton = screen.getByTestId("detail-col-sort-name");
    expect(nameButton.getAttribute("aria-label")).toMatch(/Name/i);
    expect(nameButton.getAttribute("aria-label")).toMatch(/Sort/i);
    expect(
      within(screen.getByTestId("detail-col-header-name")).getByRole("button", {
        name: /Name/i,
      }),
    ).toBe(nameButton);
  });
});
