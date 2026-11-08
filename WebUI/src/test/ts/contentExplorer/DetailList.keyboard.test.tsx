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
 * Detail-list arrow-key row navigation (#5761 / parent #4530).
 *
 * <p>ArrowDown/ArrowUp move DOM focus and selection row-to-row (wrapping
 * at the ends); Home/End jump to the first/last row. Rows use roving
 * tabindex with {@code aria-rowindex} so screen-reader position stays
 * correct, and pager/folder changes reset focus to the first row.
 * Client-side only — no new REST shape.</p>
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  DetailList,
  detailRowMoveForKey,
  nextDetailRowIndex,
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

function listingResponse(rows: PSPathItem[], childrenCount?: number): Response {
  return new Response(
    JSON.stringify({
      PagedItemList: {
        childrenInPage: rows,
        childrenCount: childrenCount ?? rows.length,
        startIndex: 0,
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

function rowTestIds(): string[] {
  return screen
    .getAllByTestId(/^detail-row-/, { exact: false })
    .map((r) => r.getAttribute("data-testid") ?? "");
}

describe("detail-row keyboard helpers", () => {
  it("detailRowMoveForKey maps arrows/Home/End and ignores the rest", () => {
    expect(detailRowMoveForKey("ArrowDown")).toBe("next");
    expect(detailRowMoveForKey("ArrowUp")).toBe("prev");
    expect(detailRowMoveForKey("Home")).toBe("first");
    expect(detailRowMoveForKey("End")).toBe("last");
    expect(detailRowMoveForKey("Enter")).toBeNull();
    expect(detailRowMoveForKey(" ")).toBeNull();
    expect(detailRowMoveForKey("Tab")).toBeNull();
  });

  it("nextDetailRowIndex steps and wraps at the ends", () => {
    expect(nextDetailRowIndex(3, 0, "next")).toBe(1);
    expect(nextDetailRowIndex(3, 2, "next")).toBe(0);
    expect(nextDetailRowIndex(3, 0, "prev")).toBe(2);
    expect(nextDetailRowIndex(3, 1, "prev")).toBe(0);
    expect(nextDetailRowIndex(3, 2, "first")).toBe(0);
    expect(nextDetailRowIndex(3, 0, "last")).toBe(2);
    expect(nextDetailRowIndex(1, 0, "next")).toBe(0);
    expect(nextDetailRowIndex(0, 0, "next")).toBe(0);
  });
});

describe("DetailList arrow-key navigation", () => {
  it("uses roving tabindex with row positions on first paint", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());
    await renderA11yGate(container);
    expect(rowTestIds()).toEqual(["detail-row-c-3", "detail-row-a-1", "detail-row-b-2"]);
    // Only the first row is in the tab order; positions stay 1-based.
    expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute("tabindex", "0");
    expect(screen.getByTestId("detail-row-a-1")).toHaveAttribute("tabindex", "-1");
    expect(screen.getByTestId("detail-row-b-2")).toHaveAttribute("tabindex", "-1");
    expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute("aria-rowindex", "1");
    expect(screen.getByTestId("detail-row-b-2")).toHaveAttribute("aria-rowindex", "3");
    expect(screen.getByTestId("detail-list").querySelector("table")).toHaveAttribute(
      "aria-rowcount",
      "3",
    );
  });

  it("ArrowDown moves focus and selection row-to-row and wraps", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const selected: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-a-1"));
    expect(selected).toEqual(["a-1"]);

    fireEvent.keyDown(screen.getByTestId("detail-row-a-1"), { key: "ArrowDown" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-b-2"));
    expect(selected).toEqual(["a-1", "b-2"]);

    // Wraps from the last row back to the first.
    fireEvent.keyDown(screen.getByTestId("detail-row-b-2"), { key: "ArrowDown" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-c-3"));
    expect(selected).toEqual(["a-1", "b-2", "c-3"]);
  });

  it("ArrowUp moves up and wraps from the first row to the last", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const selected: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowUp" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-b-2"));
    expect(selected).toEqual(["b-2"]);
  });

  it("Home/End jump to the first/last row", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const selected: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    const middle = screen.getByTestId("detail-row-a-1");
    middle.focus();
    fireEvent.keyDown(middle, { key: "End" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-b-2"));
    fireEvent.keyDown(screen.getByTestId("detail-row-b-2"), { key: "Home" });
    expect(document.activeElement).toBe(screen.getByTestId("detail-row-c-3"));
    expect(selected).toEqual(["b-2", "c-3"]);
  });

  it("keeps Enter activation and ignores unhandled keys", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const selected: string[] = [];
    const activated: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={(next) => selected.push(String(next.id))}
        onActivateItem={(next) => activated.push(String(next.id))}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "Enter" });
    expect(activated).toEqual(["c-3"]);
    expect(selected).toEqual([]);
    fireEvent.keyDown(first, { key: "Tab" });
    expect(document.activeElement).toBe(first);
    expect(selected).toEqual([]);
  });

  it("pager change resets the tab stop to the first row", async () => {
    let fetchCalls = 0;
    mockFetch(async () => {
      fetchCalls += 1;
      return listingResponse(ROWS, 55);
    });
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    // Move the tab stop off the first row.
    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(screen.getByTestId("detail-row-a-1")).toHaveAttribute("tabindex", "0");

    // Next page reloads the list and the tab stop returns to row one.
    fireEvent.click(screen.getByText("Next ›"));
    await waitFor(() => expect(fetchCalls).toBe(2));
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute("tabindex", "0"),
    );
    expect(screen.getByTestId("detail-row-a-1")).toHaveAttribute("tabindex", "-1");
  });

  it("folder change resets the tab stop to the first row", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const { rerender } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getByTestId("detail-row-c-3")).toBeInTheDocument());

    const first = screen.getByTestId("detail-row-c-3");
    first.focus();
    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(screen.getByTestId("detail-row-a-1")).toHaveAttribute("tabindex", "0");

    rerender(
      <DetailList
        folderPath="/Sites/Other"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-c-3")).toHaveAttribute("tabindex", "0"),
    );
    expect(screen.getByTestId("detail-row-a-1")).toHaveAttribute("tabindex", "-1");
  });
});
