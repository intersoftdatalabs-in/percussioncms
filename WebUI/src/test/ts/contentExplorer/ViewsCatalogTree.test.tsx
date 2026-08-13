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
import { describe, expect, it, vi } from "vitest";
import type { ViewDef } from "../../../main/ts/api/developer/types";
import { ViewsCatalogTree } from "../../../main/ts/contentExplorer/ViewsCatalogTree";
import { renderA11yGate } from "./a11y";

const CATALOG: ViewDef[] = [
  { name: "MyPages", label: "My Pages", parentCategory: 1, standardView: true },
  { name: "Inbox", label: "Inbox", parentCategory: 1, customView: true },
  { name: "CommunityNews", label: "Community News", parentCategory: 2 },
  { name: "View_All", label: "All Content", parentCategory: 3, standardView: true },
];

describe("ViewsCatalogTree (#3116)", () => {
  it("renders Views root and four category groups from the catalog", async () => {
    const listViews = vi.fn().mockResolvedValue(CATALOG);
    const { container } = render(<ViewsCatalogTree listViews={listViews} />);

    await waitFor(() => {
      expect(screen.getByTestId("explorer-views-tree")).toBeInTheDocument();
      expect(screen.getByTestId("explorer-views-group-1")).toBeInTheDocument();
    });
    expect(screen.getByTestId("explorer-views-root")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-group-2")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-group-3")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-group-4")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-leaf-MyPages")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-leaf-Inbox")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-inbox")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-inbox-icon")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-views-leaf-Inbox")).toHaveAttribute(
      "data-cx-path",
      "//Views//MyContent/Inbox",
    );
    expect(screen.queryByTestId("explorer-views-leaf-View_All")).toBeNull();

    fireEvent.click(screen.getByTestId("explorer-views-group-3-row"));
    expect(screen.getByTestId("explorer-views-leaf-View_All")).toBeInTheDocument();

    await renderA11yGate(container);
  });

  it("selecting a leaf reports the view to the host", async () => {
    const onSelectView = vi.fn();
    render(
      <ViewsCatalogTree
        listViews={async () => CATALOG}
        onSelectView={onSelectView}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-leaf-MyPages")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-leaf-MyPages"));
    expect(onSelectView).toHaveBeenCalledTimes(1);
    expect(onSelectView.mock.calls[0]?.[0]?.name).toBe("MyPages");
  });

  it("shows load error and retries", async () => {
    const listViews = vi
      .fn()
      .mockRejectedValueOnce(new Error("views down"))
      .mockResolvedValueOnce([]);
    render(<ViewsCatalogTree listViews={listViews} />);
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-error")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("explorer-views-retry"));
    await waitFor(() =>
      expect(screen.getByTestId("explorer-views-group-1")).toBeInTheDocument(),
    );
    expect(listViews).toHaveBeenCalledTimes(2);
  });

  it("always shows an Inbox leaf under My Content when catalog is empty (#3240)", async () => {
    const { container } = render(
      <ViewsCatalogTree listViews={async () => []} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-views-leaf-Inbox")).toBeInTheDocument();
    });
    expect(screen.getByTestId("explorer-views-group-1")).toContainElement(
      screen.getByTestId("explorer-views-leaf-Inbox"),
    );
    expect(screen.getByTestId("explorer-views-inbox-icon")).toBeInTheDocument();
    await renderA11yGate(container);
  });
});
