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
 * Detail-list keyboard context menu (#6863 / parent #6826).
 *
 * <p>Focus a detail row and press the context-menu key or Shift+F10 to
 * open the same item-scoped server menu pointer right-click offers
 * (tree folder rows gained the same equivalent in #6826). The row is
 * selected first and the popup anchors at the row rect; plain F10 is
 * ignored. Without a host handler the key still selects the row (tree
 * parity) and opens nothing.</p>
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DetailList } from "../../../main/ts/contentExplorer/DetailList";
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

const ROWS: PSPathItem[] = [item("a-1", "apple"), item("b-2", "Banana")];

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

function renderList(handlers: {
  onSelectItem?: (item: PSPathItem) => void;
  onItemContextMenu?: (item: PSPathItem, x: number, y: number) => void;
}) {
  return render(
    <DetailList
      folderPath="/Sites/Demo"
      selectedItemId={null}
      onSelectItem={handlers.onSelectItem ?? (() => undefined)}
      onItemContextMenu={handlers.onItemContextMenu}
    />,
  );
}

describe("DetailList keyboard context menu (#6863)", () => {
  it("right-click selects the row and opens the item-scoped menu", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const onSelectItem = vi.fn();
    const onItemContextMenu = vi.fn();
    const { container } = renderList({ onSelectItem, onItemContextMenu });
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-a-1")).toBeInTheDocument(),
    );

    fireEvent.contextMenu(screen.getByTestId("detail-row-a-1"), {
      clientX: 100,
      clientY: 200,
    });

    expect(onSelectItem).toHaveBeenCalledWith(
      expect.objectContaining({ id: "a-1" }),
    );
    expect(onItemContextMenu).toHaveBeenCalledWith(
      expect.objectContaining({ id: "a-1" }),
      100,
      200,
    );
    await renderA11yGate(container);
  });

  it("context-menu key selects the row and anchors the menu at the row", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const onSelectItem = vi.fn();
    const onItemContextMenu = vi.fn();
    renderList({ onSelectItem, onItemContextMenu });
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-b-2")).toBeInTheDocument(),
    );

    fireEvent.keyDown(screen.getByTestId("detail-row-b-2"), {
      key: "ContextMenu",
    });

    expect(onSelectItem).toHaveBeenCalledWith(
      expect.objectContaining({ id: "b-2" }),
    );
    expect(onItemContextMenu).toHaveBeenCalledTimes(1);
    const [row, x, y] = onItemContextMenu.mock.calls[0] as [
      PSPathItem,
      number,
      number,
    ];
    expect(row.id).toBe("b-2");
    expect(Number.isFinite(x)).toBe(true);
    expect(Number.isFinite(y)).toBe(true);
  });

  it("Shift+F10 opens the menu; plain F10 does not", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const onItemContextMenu = vi.fn();
    renderList({ onItemContextMenu });
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-a-1")).toBeInTheDocument(),
    );

    fireEvent.keyDown(screen.getByTestId("detail-row-a-1"), {
      key: "F10",
      shiftKey: true,
    });
    expect(onItemContextMenu).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(screen.getByTestId("detail-row-a-1"), { key: "F10" });
    expect(onItemContextMenu).toHaveBeenCalledTimes(1);
  });

  it("keyboard open without a host handler still selects (plain click parity)", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const onSelectItem = vi.fn();
    renderList({ onSelectItem });
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-a-1")).toBeInTheDocument(),
    );

    fireEvent.keyDown(screen.getByTestId("detail-row-a-1"), {
      key: "ContextMenu",
    });
    expect(onSelectItem).toHaveBeenCalledWith(
      expect.objectContaining({ id: "a-1" }),
    );
  });
});
