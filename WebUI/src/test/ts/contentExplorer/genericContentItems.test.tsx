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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DetailList } from "../../../main/ts/contentExplorer/DetailList";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import { PurgeConfirmDialog } from "../../../main/ts/contentExplorer/PurgeConfirmDialog";
import { RecycleConfirmDialog } from "../../../main/ts/contentExplorer/RecycleConfirmDialog";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

/**
 * Rhythmyx parity (#6514, parent #4530): an upgrade site has no Pages or
 * Assets — it has Items of custom content types / snippets. The folder
 * list must show generic items with type-aware rows (name/title/type),
 * keyboard activation must open them like pages, and the recycle/purge
 * confirms must name items, not just pages/assets.
 */

function genericItem(
  id: string,
  name: string,
  type: string,
  path = `/Sites/Demo/${name}`,
): PSPathItem {
  return {
    id,
    path,
    name,
    title: `${name} title`,
    type,
    leaf: true,
    accessLevel: "WRITE",
  } as PSPathItem;
}

const ROWS: PSPathItem[] = [
  genericItem("g-1", "PromoSnippet", "rffSnippet"),
  genericItem("g-2", "Home", "percPage"),
  genericItem("g-3", "CustomArticle", "MyCustomArticle"),
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

describe("generic content items (#6514)", () => {
  it("lists custom-type items with type-aware rows, not folder rows", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const { container } = render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-g-3")).toBeInTheDocument(),
    );
    const row = screen.getByTestId("detail-row-g-3");
    expect(row).toHaveAttribute("data-row-kind", "item");
    expect(row).toHaveAttribute("data-item-type", "MyCustomArticle");
    expect(row).toHaveAttribute("tabindex", "-1");
    // Roving tabindex: only the first row is in the tab order.
    expect(screen.getByTestId("detail-row-g-1")).toHaveAttribute("tabindex", "0");
    // Type-aware cell: the custom content-type name is visible in the row.
    expect(row).toHaveTextContent("MyCustomArticle");
    expect(row).toHaveTextContent("CustomArticle");
    // Snippet-typed rows are items too, not folders.
    expect(screen.getByTestId("detail-row-g-1")).toHaveAttribute(
      "data-row-kind",
      "item",
    );
    await renderA11yGate(container);
  });

  it("Enter and double-click activate a custom-type row for open", async () => {
    mockFetch(async () => listingResponse(ROWS));
    const activated: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Demo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        onActivateItem={(next) => activated.push(String(next.id))}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-g-3")).toBeInTheDocument(),
    );
    const row = screen.getByTestId("detail-row-g-3");
    row.focus();
    fireEvent.keyDown(row, { key: "Enter" });
    expect(activated).toEqual(["g-3"]);
    fireEvent.doubleClick(row);
    expect(activated).toEqual(["g-3", "g-3"]);
  });

  it("recycle confirm names items, not just pages/assets (#6514)", async () => {
    const { container } = render(
      <RecycleConfirmDialog onConfirm={() => undefined} onCancel={() => undefined} />,
    );
    expect(
      screen.getByTestId("explorer-recycle-confirm"),
    ).toHaveTextContent(message(EXPLORER_MSG.MULTI_RECYCLE_CONFIRM));
    expect(
      screen.getByTestId("explorer-recycle-confirm"),
    ).toHaveTextContent(/checked items/i);
    expect(
      screen.getByTestId("explorer-recycle-confirm").textContent ?? "",
    ).not.toMatch(/pages and assets/i);
    await renderA11yGate(container);
  });

  it("purge confirm names recycled items, not just pages/assets (#6514)", async () => {
    const { container } = render(
      <PurgeConfirmDialog onConfirm={() => undefined} onCancel={() => undefined} />,
    );
    expect(screen.getByTestId("explorer-purge-confirm")).toHaveTextContent(
      message(EXPLORER_MSG.MULTI_PURGE_CONFIRM),
    );
    expect(screen.getByTestId("explorer-purge-confirm")).toHaveTextContent(
      /recycled items/i,
    );
    expect(
      screen.getByTestId("explorer-purge-confirm").textContent ?? "",
    ).not.toMatch(/pages and assets/i);
    await renderA11yGate(container);
  });

  it("empty recycle/purge outcomes name items, not just pages/assets (#6514)", () => {
    expect(message(EXPLORER_MSG.MULTI_RECYCLE_NONE)).toMatch(/No items were recycled/);
    expect(message(EXPLORER_MSG.MULTI_PURGE_NONE)).toMatch(/No items were purged/);
  });
});
