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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import { ActionToolbar } from "../../../main/ts/contentExplorer/ActionToolbar";
import { renderA11yGate } from "./a11y";

const ACTIONS: MenuAction[] = [
  { name: "open", label: "Open", sortRank: 0, menuType: "MENUITEM" },
  { name: "edit", label: "Edit", sortRank: 1, menuType: "MENUITEM" },
];

describe("ActionToolbar", () => {
  it("renders a button per action with aria-label", () => {
    render(<ActionToolbar actions={ACTIONS} />);
    const toolbar = screen.getByRole("toolbar");
    expect(toolbar).toBeTruthy();
    expect(screen.getByLabelText("Open")).toBeTruthy();
    expect(screen.getByLabelText("Edit")).toBeTruthy();
  });

  it("clicking a button invokes onInvoke with the action name", () => {
    const onInvoke = vi.fn();
    render(<ActionToolbar actions={ACTIONS} onInvoke={onInvoke} />);
    fireEvent.click(screen.getByLabelText("Edit"));
    expect(onInvoke).toHaveBeenCalledTimes(1);
    expect(onInvoke.mock.calls[0]?.[0]).toBe("edit");
  });

  it("uses an empty-state placeholder when there are no actions", () => {
    render(<ActionToolbar actions={[]} />);
    expect(screen.getByTestId("action-toolbar-empty")).toBeTruthy();
  });

  it("uses ariaLabel on the toolbar role when supplied", () => {
    render(<ActionToolbar actions={ACTIONS} ariaLabel="Item actions" />);
    expect(screen.getByRole("toolbar").getAttribute("aria-label")).toBe(
      "Item actions",
    );
  });

  it("passes the zero serious/critical axe-core gate (populated state)", async () => {
    const { container } = render(<ActionToolbar actions={ACTIONS} ariaLabel="Item actions" />);
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (empty state)", async () => {
    const { container } = render(<ActionToolbar actions={[]} ariaLabel="Item actions" />);
    await renderA11yGate(container);
  });

  it("renders MENU parents with children as nested dropdowns (#2730/#2731)", () => {
    const nested: MenuAction[] = [
      {
        name: "new",
        label: "New",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "new-folder",
            label: "Folder",
            sortRank: 0,
            menuType: "MENUITEM",
          },
          {
            name: "new-page",
            label: "Page",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
      { name: "open", label: "Open", sortRank: 1, menuType: "MENUITEM" },
    ];
    const onInvoke = vi.fn();
    render(<ActionToolbar actions={nested} onInvoke={onInvoke} />);

    // Parent is a single toolbar control — not three flat buttons.
    expect(screen.getByTestId("action-toolbar-item-new")).toBeTruthy();
    expect(
      screen.getByTestId("action-toolbar-item-new").getAttribute("aria-haspopup"),
    ).toBe("menu");
    expect(screen.queryByTestId("action-toolbar-item-new-folder")).toBeNull();
    expect(screen.getByTestId("action-toolbar-item-open")).toBeTruthy();
    // Only two top-level controls (New dropdown + Open), not three flat buttons.
    expect(screen.getAllByRole("button").length).toBe(2);

    fireEvent.click(screen.getByTestId("action-toolbar-item-new"));
    expect(screen.getByTestId("action-toolbar-menu-new")).toBeTruthy();
    fireEvent.click(screen.getByTestId("action-toolbar-item-new-folder"));
    expect(onInvoke).toHaveBeenCalledWith(
      "new-folder",
      expect.objectContaining({ name: "new-folder" }),
    );
  });

  it("does not dump multi-level MENU grandchildren as top-level toolbar buttons (#2730)", () => {
    const nested: MenuAction[] = [
      {
        name: "content",
        label: "Content",
        sortRank: 0,
        menuType: "MENU",
        children: [
          {
            name: "new",
            label: "New",
            sortRank: 0,
            menuType: "MENU",
            children: [
              {
                name: "new-folder",
                label: "Folder",
                sortRank: 0,
                menuType: "MENUITEM",
              },
            ],
          },
        ],
      },
    ];
    render(<ActionToolbar actions={nested} />);
    expect(screen.getByTestId("action-toolbar-item-content")).toBeTruthy();
    expect(screen.queryByTestId("action-toolbar-item-new")).toBeNull();
    expect(screen.queryByTestId("action-toolbar-item-new-folder")).toBeNull();
  });

  it("renders cascading Workflow children as a labeled group (#2732)", () => {
    const onInvoke = vi.fn();
    const actions: MenuAction[] = [
      {
        name: "workflow",
        label: "Workflow",
        sortRank: 9000,
        menuType: "MENU",
        children: [
          {
            name: "workflow-transition:Submit",
            label: "Submit",
            sortRank: 1,
            menuType: "MENUITEM",
          },
        ],
      },
    ];
    render(<ActionToolbar actions={actions} onInvoke={onInvoke} />);
    expect(screen.getByTestId("action-toolbar-group-workflow")).toBeTruthy();
    fireEvent.click(screen.getByTestId("action-toolbar-item-workflow-transition:Submit"));
    expect(onInvoke).toHaveBeenCalledWith(
      "workflow-transition:Submit",
      expect.objectContaining({ label: "Submit" }),
    );
  });
});
