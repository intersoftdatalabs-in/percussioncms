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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  DetailList,
  columnHeaderLabel,
  renderDisplayFormatCell,
  resolveDisplayFormatColumns,
} from "../../../main/ts/contentExplorer/DetailList";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

const CHILDREN: PSPathItem[] = [
  {
    id: "p-1",
    path: "/Sites/Foo/Page1",
    name: "Page1",
    type: "page",
    accessLevel: "WRITE",
  },
  {
    id: "p-2",
    path: "/Sites/Foo/Page2",
    name: "Page2",
    type: "page",
    accessLevel: "READ",
  },
];

describe("DetailList", () => {
  it("renders the empty state when folderPath is null", () => {
    render(
      <DetailList
        folderPath={null}
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    expect(screen.getByTestId("detail-list")).toHaveTextContent(/No items/);
  });

  it("loads the first page and renders rows", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/paginatedFolder/Sites/Foo");
      expect(url).toContain("startIndex=0");
      expect(url).toContain("maxResults=50");
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("detail-row-p-2")).toBeInTheDocument();
    expect(screen.getByTestId("detail-pagination")).toHaveTextContent(
      /Page 1 of 1/,
    );
  });

  it("renders sample-site Pages childrenInPage rows (#3457)", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain(
        "/paginatedFolder/Sites/Corporate_Investments/Pages",
      );
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [
              {
                id: "ci-home",
                path: "/Sites/Corporate_Investments/Pages/Home",
                name: "Home",
                type: "rffHome",
                category: "PAGE",
              },
            ],
            childrenCount: 1,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    render(
      <DetailList
        folderPath="/Sites/Corporate_Investments/Pages"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-ci-home")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("detail-list")).not.toHaveTextContent(
      /No items in this folder/i,
    );
  });

  it("paginates forward and resets to page 0 on folder change", async () => {
    let currentStart = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const match = url.match(/startIndex=(\d+)/);
      currentStart = match ? Number(match[1]) : 0;
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: 120,
            startIndex: currentStart,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const { rerender } = render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    expect(currentStart).toBe(0);

    fireEvent.click(screen.getByText(/Next/));
    await waitFor(() => expect(currentStart).toBe(50));

    rerender(
      <DetailList
        folderPath="/Sites/Bar"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    // New folder fetches page 0 (not page 50 of the previous folder).
    // Guards against a regression where the captured `page` value was used
    // before the queued setPage(0) state update took effect.
    await waitFor(() => expect(currentStart).toBe(0));
  });

  it("fires onSelectItem when a row is clicked", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    let selectedId: string | null = null;
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={(item) => {
          selectedId = item.id ?? null;
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-row-p-1"));
    expect(selectedId).toBe("p-1");
  });

  it("surfaces fetch errors as an alert", async () => {
    mockFetch(async () => new Response("boom", { status: 500 }));
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(/Failed to load/),
    );
  });

  it("passes the zero serious/critical axe-core gate (populated state)", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    const { container } = render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId="p-1"
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() => expect(screen.getAllByTestId(/^detail-row-/).length).toBeGreaterThan(0));
    await renderA11yGate(container);
  });

  // Multi-select / #2400 #2408 ----------------------------------------------------

  it("does not render the checkbox column when onToggleSelectItem is absent", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("detail-col-header-select")).toBeNull();
    expect(screen.queryByTestId("detail-select-p-1")).toBeNull();
  });

  it("renders the checkbox column when onToggleSelectItem is supplied", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    expect(
      screen.getByTestId("detail-col-header-select"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("detail-select-p-1")).toBeInTheDocument();
    expect(screen.getByTestId("detail-select-p-2")).toBeInTheDocument();
    expect(screen.getByTestId("detail-select-all")).toBeInTheDocument();
  });

  it("fires onToggleSelectItem with the next checked state", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const calls: Array<{ id: string | undefined; next: boolean }> = [];
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={(item, next) => {
          calls.push({ id: item.id, next });
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-select-p-1"));
    expect(calls).toEqual([{ id: "p-1", next: true }]);
  });

  it("does not trigger row onSelectItem when a checkbox is clicked", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    let rowClicks = 0;
    const toggleCalls: string[] = [];
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => {
          rowClicks += 1;
        }}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={(item, next) => {
          if (next) toggleCalls.push(item.id ?? "");
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-select-p-1"));
    expect(rowClicks).toBe(0);
    expect(toggleCalls).toEqual(["p-1"]);
  });

  it("reflects the parent-controlled selectedItemIds on the row checkbox", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        selectedItemIds={new Set<string>(["p-1"])}
        onToggleSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    const cb1 = screen.getByTestId("detail-select-p-1") as HTMLInputElement;
    const cb2 = screen.getByTestId("detail-select-p-2") as HTMLInputElement;
    expect(cb1.checked).toBe(true);
    expect(cb2.checked).toBe(false);
    expect(screen.getByTestId("detail-row-p-1").getAttribute("data-selected")).toBe(
      "true",
    );
  });

  it("toggles all visible rows via the header select-all checkbox", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const calls: Array<{ id: string | undefined; next: boolean }> = [];
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={(item, next) => {
          calls.push({ id: item.id, next });
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-p-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-select-all"));
    expect(calls).toEqual([
      { id: "p-1", next: true },
      { id: "p-2", next: true },
    ]);
  });

  it("passes the zero serious/critical axe-core gate (multi-select populated)", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: CHILDREN,
            childrenCount: CHILDREN.length,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { container } = render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId="p-1"
        onSelectItem={() => undefined}
        selectedItemIds={new Set<string>(["p-1", "p-2"])}
        onToggleSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getAllByTestId(/^detail-row-/).length).toBeGreaterThan(0),
    );
    await renderA11yGate(container);
  });
});

const FOLDER_CHILDREN: PSPathItem[] = [
  {
    id: "sys-1",
    path: "/Folders/$System$/",
    name: "$System$",
    accessLevel: "READ",
    leaf: false,
  },
  {
    id: "uf-1",
    path: "/Folders/New-Folder/",
    name: "New-Folder",
    type: "Folder",
    accessLevel: "WRITE",
  },
  {
    id: "p-page",
    path: "/Folders/Welcome",
    name: "Welcome",
    type: "page",
    accessLevel: "WRITE",
    leaf: true,
  },
];

function mockFolderPage(children: PSPathItem[] = FOLDER_CHILDREN): void {
  mockFetch(async () =>
    new Response(
      JSON.stringify({
        PagedItemList: {
          childrenInPage: children,
          childrenCount: children.length,
          startIndex: 0,
        },
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    ),
  );
}

describe("DetailList folder row chrome (#3328)", () => {
  it("shows folder icons for $System$ and user folders, not checkboxes in the icon column", async () => {
    mockFolderPage();
    render(
      <DetailList
        folderPath="/Folders"
        selectedItemId={null}
        onSelectItem={() => undefined}
        onActivateItem={() => undefined}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-row-sys-1")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("detail-col-header-icon")).toBeInTheDocument();
    expect(screen.getByTestId("detail-col-header-select")).toBeInTheDocument();

    const systemIcon = screen.getByTestId("detail-folder-icon-sys-1");
    expect(systemIcon).toHaveAttribute("data-kind", "folder");
    expect(systemIcon).toHaveAttribute("data-folder-state", "closed");
    expect(screen.getByTestId("detail-row-sys-1")).toHaveAttribute(
      "data-row-kind",
      "folder",
    );

    const userIcon = screen.getByTestId("detail-folder-icon-uf-1");
    expect(userIcon).toHaveAttribute("data-kind", "folder");
    expect(userIcon).toHaveAttribute("data-folder-state", "closed");

    expect(screen.getByTestId("detail-item-icon-p-page")).toHaveAttribute(
      "data-kind",
      "item",
    );
    expect(screen.queryByTestId("detail-folder-icon-p-page")).toBeNull();

    expect(screen.getByTestId("detail-select-sys-1")).toBeInTheDocument();
    expect(screen.getByTestId("detail-select-uf-1")).toBeInTheDocument();
  });

  it("uses the open folder icon when a folder row is selected", async () => {
    mockFolderPage();
    render(
      <DetailList
        folderPath="/Folders"
        selectedItemId="sys-1"
        onSelectItem={() => undefined}
        onActivateItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-folder-icon-sys-1")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("detail-folder-icon-sys-1")).toHaveAttribute(
      "data-folder-state",
      "open",
    );
    expect(screen.getByTestId("detail-folder-icon-uf-1")).toHaveAttribute(
      "data-folder-state",
      "closed",
    );
  });

  it("activates browse when the folder icon is clicked without toggling the checkbox", async () => {
    mockFolderPage();
    const activated: string[] = [];
    const toggled: string[] = [];
    render(
      <DetailList
        folderPath="/Folders"
        selectedItemId={null}
        onSelectItem={() => undefined}
        onActivateItem={(item) => {
          activated.push(item.id ?? "");
        }}
        selectedItemIds={new Set<string>()}
        onToggleSelectItem={(item) => {
          toggled.push(item.id ?? "");
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-folder-icon-uf-1")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("detail-folder-icon-uf-1"));
    expect(activated).toEqual(["uf-1"]);
    expect(toggled).toEqual([]);
  });

  it("still renders folder icons when multi-select checkboxes are off", async () => {
    mockFolderPage();
    render(
      <DetailList
        folderPath="/Folders"
        selectedItemId={null}
        onSelectItem={() => undefined}
        onActivateItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-folder-icon-sys-1")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("detail-col-header-select")).toBeNull();
    expect(screen.getByTestId("detail-col-header-icon")).toBeInTheDocument();
  });

  it("passes the zero serious/critical axe-core gate (folder rows + checkboxes)", async () => {
    mockFolderPage();
    const { container } = render(
      <DetailList
        folderPath="/Folders"
        selectedItemId="sys-1"
        onSelectItem={() => undefined}
        onActivateItem={() => undefined}
        selectedItemIds={new Set<string>(["sys-1"])}
        onToggleSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("detail-folder-icon-sys-1")).toBeInTheDocument(),
    );
    await renderA11yGate(container);
  });
});

describe("T092b / FR-027: display-format column resolution", () => {
  const sample: PSPathItem = {
    id: "p-1",
    name: "Welcome",
    path: "/Sites/Foo/Welcome",
    type: "page",
    title: "Welcome Page",
    category: "page",
    lastModified: "2026-07-01",
    workflowId: "wf-default",
  };

  it("resolveDisplayFormatColumns returns default when columns empty", () => {
    expect(resolveDisplayFormatColumns([])).toEqual(["name", "type", "path"]);
    expect(resolveDisplayFormatColumns(undefined as unknown as [])).toEqual([
      "name",
      "type",
      "path",
    ]);
  });

  it("resolveDisplayFormatColumns honours supplied order + dedup", () => {
    expect(
      resolveDisplayFormatColumns(["modified", "name", "modified"]),
    ).toEqual(["modified", "name"]);
  });

  it("resolveDisplayFormatColumns filters unknown ids", () => {
    expect(
      resolveDisplayFormatColumns([
        "name",
        "bogus" as unknown as never,
        "modified",
      ]),
    ).toEqual(["name", "modified"]);
  });

  it("renderDisplayFormatCell covers every supported column id", () => {
    expect(renderDisplayFormatCell("name", sample)).toBe("Welcome");
    expect(renderDisplayFormatCell("type", sample)).toBe("page");
    expect(renderDisplayFormatCell("path", sample)).toBe("/Sites/Foo/Welcome");
    expect(renderDisplayFormatCell("title", sample)).toBe("Welcome Page");
    expect(renderDisplayFormatCell("category", sample)).toBe("page");
    expect(renderDisplayFormatCell("modified", sample)).toBe("2026-07-01");
    expect(renderDisplayFormatCell("workflow", sample)).toBe("wf-default");
  });

  it("renderDisplayFormatCell tolerates null optional fields", () => {
    const minimal: PSPathItem = { id: "x", name: "X", path: "/x" };
    expect(renderDisplayFormatCell("title", minimal)).toBe("X"); // falls back to name
    expect(renderDisplayFormatCell("modified", minimal)).toBe("");
  });

  it("columnHeaderLabel returns translated headers", () => {
    expect(columnHeaderLabel("name", EXPLORER_MSG)).toContain("Name");
    expect(columnHeaderLabel("modified", EXPLORER_MSG)).toContain("Modified");
    expect(columnHeaderLabel("bogus" as unknown as never, EXPLORER_MSG)).toBe(
      "bogus",
    );
  });

  it("renders the supplied display format columns in the supplied order", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [sample],
            childrenCount: 1,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const { container } = render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
        displayFormat={{
          columns: ["name", "title", "modified"],
        }}
      />,
    );
    await waitFor(() =>
      expect(screen.getAllByTestId(/^detail-row-/).length).toBeGreaterThan(0),
    );
    expect(
      screen.getByTestId("detail-col-header-name").textContent,
    ).toMatch(/Name/);
    expect(
      screen.getByTestId("detail-col-header-title").textContent,
    ).toMatch(/Title/);
    expect(
      screen.getByTestId("detail-col-header-modified").textContent,
    ).toMatch(/Modified/);
    expect(
      screen.queryByTestId("detail-col-header-type"),
    ).toBeNull();
    expect(
      screen.getByTestId(`detail-cell-name-p-1`).textContent,
    ).toBe("Welcome");
    expect(
      screen.getByTestId(`detail-cell-title-p-1`).textContent,
    ).toBe("Welcome Page");
    expect(
      screen.getByTestId(`detail-cell-modified-p-1`).textContent,
    ).toBe("2026-07-01");
    await renderA11yGate(container);
  });

  it("falls back to default Name + Type + Path when displayFormat is absent", async () => {
    mockFetch(async () =>
      new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [sample],
            childrenCount: 1,
            startIndex: 0,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    render(
      <DetailList
        folderPath="/Sites/Foo"
        selectedItemId={null}
        onSelectItem={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getAllByTestId(/^detail-row-/).length).toBeGreaterThan(0),
    );
    expect(
      screen.getByTestId("detail-col-header-name").textContent,
    ).toMatch(/Name/);
    expect(
      screen.getByTestId("detail-col-header-type").textContent,
    ).toMatch(/Type/);
    expect(
      screen.getByTestId("detail-col-header-path").textContent,
    ).toMatch(/Path/);
  });
});
