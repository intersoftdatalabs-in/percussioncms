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
});
