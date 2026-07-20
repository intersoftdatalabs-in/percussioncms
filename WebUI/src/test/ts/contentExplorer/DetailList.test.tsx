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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DetailList } from "../../../main/ts/contentExplorer/DetailList";
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
          startIndex: 0,
          maxResults: 50,
          totalCount: 2,
          children: CHILDREN,
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

  it("paginates forward and resets to page 0 on folder change", async () => {
    let currentStart = 0;
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const match = url.match(/startIndex=(\d+)/);
      currentStart = match ? Number(match[1]) : 0;
      return new Response(
        JSON.stringify({
          startIndex: currentStart,
          maxResults: 50,
          totalCount: 120,
          children: CHILDREN,
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
          startIndex: 0,
          maxResults: 50,
          children: CHILDREN,
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
      new Response(JSON.stringify({ children: CHILDREN, totalCount: CHILDREN.length }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
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
});